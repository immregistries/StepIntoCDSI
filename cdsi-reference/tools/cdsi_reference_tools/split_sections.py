"""Splits the specification body into one text file per numbered section,
using the TOC's own page numbers as section boundaries (see toc.py) rather
than re-detecting headings from scratch.

A section's content is: everything on its start page from its own heading
onward, all intervening pages in full, and everything on its end page
before the next section's heading. Headings are located by text position
(block bounding boxes), not by string search, so that a heading's own text
is never accidentally duplicated into the previous section or dropped.
"""

import re
from dataclasses import dataclass

import pymupdf as fitz

from .toc import SectionEntry, page_index_for_printed_page, section_slug


@dataclass
class ExtractedSection:
    entry: SectionEntry
    text: str
    start_page_index: int  # 0-based PDF page index where the heading was found
    warnings: list[str]


def _heading_pattern(entry: SectionEntry) -> re.Pattern:
    # Body headings are printed in caps, e.g. "6.4 EVALUATE AGE" or
    # "4 PROCESSING MODEL"; allow a colon/dash variant and flexible spacing
    # so we don't fail on a stray character difference from the TOC's
    # title-case rendering.
    escaped_title = re.escape(entry.title.upper())
    escaped_title = escaped_title.replace(r"\ ", r"\s+")
    return re.compile(
        rf"^{re.escape(entry.number)}\s+{escaped_title}\s*$", re.IGNORECASE
    )


def _find_heading_y(page: "fitz.Page", entry: SectionEntry) -> tuple[float, str] | None:
    """Returns (y0 of the heading block, matched text) or None if the
    heading text could not be located on this page by position."""
    pattern = _heading_pattern(entry)
    blocks = page.get_text("blocks")
    for block in blocks:
        x0, y0, x1, y1, text = block[0], block[1], block[2], block[3], block[4]
        for line in text.split("\n"):
            if pattern.match(line.strip()):
                return y0, line.strip()
    return None


def extract_section(doc: "fitz.Document", entry: SectionEntry, next_entry: SectionEntry | None) -> ExtractedSection:
    warnings: list[str] = []
    start_page = page_index_for_printed_page(entry.page)
    end_page = page_index_for_printed_page(next_entry.page) if next_entry else doc.page_count - 1

    start_hit = _find_heading_y(doc[start_page], entry)
    if start_hit is None:
        warnings.append(
            f"Could not locate heading text for {entry.number} {entry.title!r} "
            f"on printed page {entry.page} (pdf index {start_page}); "
            "including the full page instead of trimming to the heading."
        )
        start_clip = None
    else:
        start_clip = start_hit[0]

    if next_entry is not None and page_index_for_printed_page(next_entry.page) == start_page:
        # Next section starts on the SAME page as this one - the loop below
        # trims this section's content to end where the next heading
        # begins; here we only need to warn if that won't be possible.
        if _find_heading_y(doc[start_page], next_entry) is None:
            warnings.append(
                f"{entry.number} and {next_entry.number} share printed page {entry.page} "
                f"but the heading for {next_entry.number} could not be located; "
                "this section's text may include content belonging to the next section."
            )

    parts: list[str] = []
    for page_index in range(start_page, end_page + 1):
        page = doc[page_index]
        clip = None
        if page_index == start_page and start_clip is not None:
            clip = fitz.Rect(0, start_clip, page.rect.width, page.rect.height)
        if page_index == end_page and next_entry is not None:
            next_start_page = page_index_for_printed_page(next_entry.page)
            if page_index == next_start_page:
                next_hit = _find_heading_y(page, next_entry)
                if next_hit is not None:
                    top = clip.y0 if clip else 0
                    clip = fitz.Rect(0, top, page.rect.width, next_hit[0])
        parts.append(page.get_text(clip=clip) if clip else page.get_text())

    text = "\n".join(parts).strip() + "\n"
    return ExtractedSection(entry=entry, text=text, start_page_index=start_page, warnings=warnings)


def extract_all_sections(
    doc: "fitz.Document", entries: list[SectionEntry], chapters: tuple[int, ...] = (4, 5, 6, 7, 8, 9)
) -> list[ExtractedSection]:
    """Extracts every section whose top-level chapter number is in `chapters`."""
    in_scope = [e for e in entries if int(e.number.split(".")[0]) in chapters]
    results = []
    for i, entry in enumerate(in_scope):
        next_entry = in_scope[i + 1] if i + 1 < len(in_scope) else None
        results.append(extract_section(doc, entry, next_entry))
    return results


def section_filename(entry: SectionEntry) -> str:
    return section_slug(entry.number, entry.title) + ".txt"
