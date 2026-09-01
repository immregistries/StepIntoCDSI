"""Extracts table content for each table listed in the specification's own
List of Figures and Tables (see toc.py), using PyMuPDF's table-structure
detection with a caption-page-then-next-page search (a table's caption
often sits at the bottom of one page while its data continues on the
next), plus a wrapped-row merge pass for decision tables whose multi-line
condition text splits find_tables() results across extra rows.

When structure can't be recovered confidently, the caller should fall back
to a cropped page image (see build_index.py's warning handling) rather
than publish a guessed structure - this module reports that case via
`ExtractedTable.rows is None` rather than inventing content.
"""

import re
from dataclasses import dataclass

import pymupdf as fitz

from .toc import FigureOrTableEntry


@dataclass
class ExtractedTable:
    entry: FigureOrTableEntry
    page_index: int  # 0-based page the data was actually found on
    rows: list[list[str | None]] | None
    warnings: list[str]


def _is_blank(cell: str | None) -> bool:
    return (cell or "").strip() == ""


def _normalize_cell(cell: str | None) -> str | None:
    """find_tables() sometimes keeps a wrapped line's break as a literal
    newline INSIDE one cell rather than splitting it into another table
    row (both happen on the same page depending on column layout) -
    collapse to single-spaced text so both cases render identically."""
    if cell is None:
        return None
    return re.sub(r"\s*\n\s*", " ", cell).strip()


def _merge_wrapped_rows(rows: list[list[str]]) -> list[list[str]]:
    """A cell whose text wraps onto multiple lines in the PDF becomes an
    extra table row in find_tables()'s output. Two distinct wrap shapes
    show up in CDSi's decision tables:

    1. A label/condition column wraps while every OTHER column on that
       continuation row is empty (the wrapped text stays in column 0) -
       merge by appending column 0 onto the previous row's column 0.
    2. A row-label column is blank on a continuation row while one or more
       OTHER columns carry wrapped text (e.g. several "outcome" columns
       all wrapping on the same continuation row) - merge by appending
       each non-empty cell onto the previous row's same column.

    A row that is entirely empty (a spacer row between a table's sections)
    is dropped rather than merged either way."""
    merged: list[list[str]] = []
    for row in rows:
        if all(_is_blank(c) for c in row):
            continue

        first = (row[0] or "").strip()
        rest_blank = all(_is_blank(c) for c in row[1:])

        if merged and first and rest_blank:
            merged[-1][0] = (merged[-1][0].rstrip() + " " + first).strip()
        elif merged and not first:
            previous = merged[-1]
            for col, cell in enumerate(row):
                text = (cell or "").strip()
                if not text:
                    continue
                if col < len(previous):
                    previous[col] = (previous[col].rstrip() + " " + text).strip()
                else:
                    previous.append(text)
        else:
            merged.append([cell for cell in row])
    return merged


def _score_table(candidate_rows: list[list[str]], entry: FigureOrTableEntry) -> int:
    """Rough confidence score: more non-empty cells is better; a single
    near-empty row (just the caption) scores near zero."""
    non_empty = sum(1 for row in candidate_rows for cell in row if (cell or "").strip())
    return non_empty


def _caption_y(page: "fitz.Page", entry: FigureOrTableEntry) -> float | None:
    """Multiple tables can share a page (a caption near the bottom of one
    page, its actual grid starting on the next). Locate THIS table's own
    caption by text search so its grid isn't confused with a neighboring
    table's - without this, "the only/first table found on the page" is
    wrong as soon as a page holds more than one table."""
    label = f"TABLE {entry.number}"
    hits = page.search_for(label, quads=False)
    return min((r.y0 for r in hits), default=None)


def extract_table(doc: "fitz.Document", entry: FigureOrTableEntry, caption_page_index: int) -> ExtractedTable:
    """Tries the caption's own page first, matching a table to its OWN
    caption by vertical position (see _caption_y) rather than assuming the
    first table found belongs to the entry being searched for. Only looks
    at the next page (a table's grid commonly continues past where its
    caption sits at a page bottom) if nothing usable was found below this
    table's own caption on its own page."""
    warnings: list[str] = []

    for page_index in (caption_page_index, caption_page_index + 1):
        if page_index >= doc.page_count:
            continue
        page = doc[page_index]
        try:
            found = page.find_tables()
        except Exception as e:  # pragma: no cover - defensive, PyMuPDF internals
            warnings.append(f"find_tables() raised on page {page_index}: {e}")
            continue

        caption_y = _caption_y(page, entry) if page_index == caption_page_index else None

        best_on_page: tuple[int, list[list[str]]] | None = None  # (score, rows)
        for t in found.tables:
            if caption_y is not None and t.bbox[1] < caption_y:
                continue  # this table sits ABOVE our caption - it belongs to a different entry
            rows = t.extract()
            if not rows:
                continue
            rows = [[_normalize_cell(c) for c in row] for row in rows]
            score = _score_table(rows, entry)
            if best_on_page is None or score > best_on_page[0]:
                best_on_page = (score, rows)

        if best_on_page is not None and best_on_page[0] > 0:
            merged = _merge_wrapped_rows(best_on_page[1])
            return ExtractedTable(entry=entry, page_index=page_index, rows=merged, warnings=warnings)

    warnings.append(
        f"No table structure with content found for {entry.kind} {entry.number} "
        f"({entry.title!r}) near printed page {entry.page}; a cropped page image "
        "should be stored instead of a guessed structure."
    )
    return ExtractedTable(entry=entry, page_index=caption_page_index, rows=None, warnings=warnings)


def render_table_text(table: ExtractedTable) -> str:
    """Simple, human- and diff-reviewable pipe-delimited rendering."""
    if table.rows is None:
        return "(table structure not recovered - see figures/ page-crop image)\n"
    lines = []
    for row in table.rows:
        lines.append(" | ".join((cell or "").strip() for cell in row))
    return "\n".join(lines) + "\n"
