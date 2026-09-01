"""Orchestrates deterministic extraction for one Logic Specification
version: verify/compute the manifest, split sections, extract figures and
tables, and build the inventory. No LLM, no network access.
"""

import datetime
import hashlib
import re
import shutil
from pathlib import Path

import pymupdf as fitz
import yaml

from . import build_index, extract_figures, extract_tables, paths, split_sections, toc
from .__init__ import __version__ as tool_version

CHAPTERS_IN_SCOPE = (4, 5, 6, 7, 8, 9)


def _numeric_sort_key(number: str) -> tuple:
    """"6-9" and "6-10" must sort numerically (6-9 before 6-10), not as
    strings (where "6-10" < "6-9" because '1' < '9'). Splits on non-digit
    runs and converts each numeric piece to an int; non-numeric pieces
    (e.g. an appendix letter) sort after all-numeric ones."""
    parts = re.split(r"(\d+)", number)
    return tuple(int(p) if p.isdigit() else p for p in parts if p != "")


def sha256_of(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


_COVER_DATE_RE = re.compile(
    r"\b((?:January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},\s*\d{4})\b"
)


def _find_publication_date(doc: "fitz.Document") -> str | None:
    """CDSi Logic Specification cover pages print "Version X.Y" followed by
    a "Month DD, YYYY" line - read directly from the cover page, not
    assumed."""
    if doc.page_count == 0:
        return None
    text = doc[0].get_text()
    match = _COVER_DATE_RE.search(text)
    if not match:
        return None
    try:
        parsed = datetime.datetime.strptime(match.group(1), "%B %d, %Y")
        return parsed.strftime("%Y-%m-%d")
    except ValueError:
        return None


def load_or_create_manifest(version: str, source_pdf: Path) -> dict:
    """Loads manifest.yaml if present and verifies its checksum still
    matches the source; creates it (with values computed from the source,
    never assumed) if this is the first extraction for this version."""
    manifest_file = paths.manifest_path(version)
    computed_sha256 = sha256_of(source_pdf)

    if manifest_file.exists():
        manifest = yaml.safe_load(manifest_file.read_text(encoding="utf-8"))
        if manifest.get("sha256") != computed_sha256:
            raise ValueError(
                f"Source checksum mismatch for version {version}: manifest says "
                f"{manifest.get('sha256')}, source file hashes to {computed_sha256}. "
                "Never overwrite a registered version's source PDF - register a new version instead."
            )
        return manifest

    doc = fitz.open(source_pdf)
    page_count = doc.page_count
    publication_date = _find_publication_date(doc)
    doc.close()

    manifest = {
        "specification": "Logic Specification for ACIP Recommendations",
        "version": version,
        "publication_date": publication_date,
        "source_filename": source_pdf.name,
        "sha256": computed_sha256,
        "page_count": page_count,
        "extraction_tool_version": tool_version,
        "extracted_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    }
    manifest_file.parent.mkdir(parents=True, exist_ok=True)
    manifest_file.write_text(yaml.safe_dump(manifest, sort_keys=False), encoding="utf-8")
    return manifest


def run_extract(version: str) -> build_index.ExtractionIndex:
    source_dir = paths.source_dir(version)
    pdf_candidates = list(source_dir.glob("*.pdf"))
    if len(pdf_candidates) != 1:
        raise FileNotFoundError(
            f"Expected exactly one PDF under {source_dir}, found {len(pdf_candidates)}. "
            "Register the version first: copy the exact source PDF into that directory."
        )
    source_pdf = pdf_candidates[0]

    load_or_create_manifest(version, source_pdf)

    doc = fitz.open(source_pdf)
    entries = toc.parse_toc(doc, toc.find_toc_pages(doc))
    loft = toc.parse_loft(doc, toc.find_loft_pages(doc))

    extracted_root = paths.extracted_dir(version)
    if extracted_root.exists():
        shutil.rmtree(extracted_root)  # extracted/ is fully generated and replaceable
    sections_dir = paths.sections_dir(version)
    figures_dir = paths.figures_dir(version)
    tables_dir = paths.tables_dir(version)
    for d in (sections_dir, figures_dir, tables_dir):
        d.mkdir(parents=True, exist_ok=True)

    full_text_parts = []
    section_index_entries: list[build_index.SectionIndexEntry] = []
    for extracted in split_sections.extract_all_sections(doc, entries, CHAPTERS_IN_SCOPE):
        filename = split_sections.section_filename(extracted.entry)
        (sections_dir / filename).write_text(extracted.text, encoding="utf-8")
        full_text_parts.append(extracted.text)
        section_index_entries.append(
            build_index.SectionIndexEntry(
                number=extracted.entry.number,
                title=extracted.entry.title,
                filename=filename,
                warnings=extracted.warnings,
                business_rules=build_index.find_business_rules(extracted.text),
            )
        )
    (paths.extracted_dir(version) / "full-text.txt").write_text(
        "\n".join(full_text_parts), encoding="utf-8"
    )

    in_scope_numbers = {e.number for e in entries if int(e.number.split(".")[0]) in CHAPTERS_IN_SCOPE}
    # A figure/table belongs "in scope" if its printed page falls within
    # one of the in-scope sections' page ranges (LOFT entries don't carry
    # a section number themselves).
    in_scope_pages = set()
    sorted_entries = sorted(entries, key=lambda e: e.page)
    for i, e in enumerate(sorted_entries):
        if e.number in in_scope_numbers:
            next_page = sorted_entries[i + 1].page if i + 1 < len(sorted_entries) else 10**6
            in_scope_pages.update(range(e.page, next_page))

    figures = [e for e in loft if e.kind == "figure" and e.page in in_scope_pages]
    tables = [e for e in loft if e.kind == "table" and e.page in in_scope_pages]

    table_warnings: dict[str, list[str]] = {}
    for entry in tables:
        caption_page = toc.page_index_for_printed_page(entry.page)
        extracted_table = extract_tables.extract_table(doc, entry, caption_page)
        table_warnings[entry.number] = extracted_table.warnings
        text_name = f"table-{entry.number.replace('.', '-')}.txt"
        (tables_dir / text_name).write_text(
            f"TABLE {entry.number} {entry.title}\n\n" + extract_tables.render_table_text(extracted_table),
            encoding="utf-8",
        )
        if extracted_table.rows is None:
            # Fall back to an authoritative page-crop image when structure
            # could not be recovered confidently.
            pixmap = doc[caption_page].get_pixmap(dpi=200)
            image_name = f"table-{entry.number.replace('.', '-')}.png"
            (tables_dir / image_name).write_bytes(pixmap.tobytes("png"))

    figure_warnings: dict[str, list[str]] = {}
    pages_sorted = sorted(figures, key=lambda e: (e.page, _numeric_sort_key(e.number)))
    previous_bottom_by_page: dict[int, float] = {}
    for entry in pages_sorted:
        page_index = toc.page_index_for_printed_page(entry.page)
        top_bound = previous_bottom_by_page.get(page_index, 0.0)
        extracted_figure = extract_figures.extract_figure(doc, entry.number, page_index, top_bound=top_bound)
        figure_warnings[entry.number] = extracted_figure.warnings
        if extracted_figure.png_bytes is not None:
            image_name = f"figure-{entry.number.replace('.', '-')}.png"
            (figures_dir / image_name).write_bytes(extracted_figure.png_bytes)
            cap = extract_figures.caption_rect(doc[page_index], entry.number)
            if cap is not None:
                previous_bottom_by_page[page_index] = cap.y1 + 4

    index = build_index.ExtractionIndex(
        version=version,
        sections=section_index_entries,
        figures=figures,
        tables=tables,
        table_warnings=table_warnings,
        figure_warnings=figure_warnings,
    )
    (paths.version_dir(version) / "index.md").write_text(
        build_index.render_index_markdown(index), encoding="utf-8"
    )
    doc.close()
    return index
