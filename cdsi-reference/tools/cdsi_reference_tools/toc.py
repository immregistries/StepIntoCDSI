"""Parses the specification's own Table of Contents and List of Figures and
Tables pages into structured, authoritative indexes.

This is the foundation the rest of extraction is built on: rather than
inferring section/figure/table identity purely by pattern-matching body
text (fragile - body headings repeat the same visual style as other bold
text, and multi-line table titles are easy to mis-split), we trust the
specification's own front-matter listing of "what exists and on what page"
and then locate that content on its stated page. This matches the plan's
principle of using stable source identifiers rather than inferred
structure.
"""

import re
from dataclasses import dataclass

import pymupdf as fitz


@dataclass(frozen=True)
class SectionEntry:
    number: str  # e.g. "6.4"
    title: str  # e.g. "Evaluate Age"
    page: int  # 1-based page number as printed in the document ("Page N of 151")


@dataclass(frozen=True)
class FigureOrTableEntry:
    kind: str  # "figure" or "table"
    number: str  # e.g. "6-14" or "A-1"
    title: str
    page: int


_NUMBER_ONLY_RE = re.compile(r"^(\d+(?:\.\d+)*)$")
_TITLE_WITH_DOTS_RE = re.compile(r"^(.*?)\.{2,}\s*(\d+)\s*$")
_LOFT_ENTRY_RE = re.compile(
    r"^(Table|Figure)\s+([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)\s+(.+?)\.{2,}\s*(\d+)\s*$"
)


def find_toc_pages(doc: "fitz.Document") -> list[int]:
    """0-based page indexes whose page contains 'TABLE OF CONTENTS'."""
    pages = []
    for i in range(min(10, doc.page_count)):
        if "TABLE OF CONTENTS" in doc[i].get_text():
            pages.append(i)
            # the TOC continues on subsequent pages until a different
            # all-caps front-matter heading appears; caller supplies an
            # explicit end page in practice, so just keep scanning a
            # reasonable window here.
    # Extend forward while pages look like TOC continuations (no new
    # front-matter heading and still contains dot-leader lines).
    if pages:
        i = pages[-1] + 1
        while i < doc.page_count and i < pages[0] + 6:
            text = doc[i].get_text()
            if "LIST OF FIGURES AND TABLES" in text:
                break
            has_dot_leader_line = any(
                _TITLE_WITH_DOTS_RE.match(line.strip()) for line in text.split("\n")
            )
            if has_dot_leader_line:
                pages.append(i)
                i += 1
            else:
                break
    return pages


def find_loft_pages(doc: "fitz.Document") -> list[int]:
    """0-based page indexes for the 'LIST OF FIGURES AND TABLES' section."""
    pages = []
    start = None
    for i in range(min(15, doc.page_count)):
        if "LIST OF FIGURES AND TABLES" in doc[i].get_text():
            start = i
            break
    if start is None:
        return []
    pages.append(start)
    i = start + 1
    while i < doc.page_count and i < start + 6:
        text = doc[i].get_text()
        if "Figure" in text or "Table" in text:
            pages.append(i)
            i += 1
        else:
            break
    return pages


def parse_toc(doc: "fitz.Document", toc_pages: list[int]) -> list[SectionEntry]:
    """Parses "<number>\\n<Title>....<page>" pairs, restricted to numeric
    chapter/section entries (appendices use letters and are handled
    separately - out of scope for Chapters 4-9)."""
    lines: list[str] = []
    for p in toc_pages:
        lines.extend(line.strip() for line in doc[p].get_text().split("\n"))
    entries: list[SectionEntry] = []
    i = 0
    while i < len(lines) - 1:
        m_num = _NUMBER_ONLY_RE.match(lines[i])
        if m_num:
            m_title = _TITLE_WITH_DOTS_RE.match(lines[i + 1])
            if m_title:
                entries.append(
                    SectionEntry(
                        number=m_num.group(1),
                        title=m_title.group(1).strip(),
                        page=int(m_title.group(2)),
                    )
                )
                i += 2
                continue
        i += 1
    return entries


def parse_loft(doc: "fitz.Document", loft_pages: list[int]) -> list[FigureOrTableEntry]:
    """Parses "Table|Figure <number> <title>....<page>" entries. Long titles
    that wrap onto a second physical line are rejoined by detecting a
    following line that itself has no leading Table/Figure marker and ends
    in a dot-leader + page number."""
    entries: list[FigureOrTableEntry] = []
    for p in loft_pages:
        for raw_line in doc[p].get_text().split("\n"):
            line = raw_line.strip()
            if not line:
                continue
            m = _LOFT_ENTRY_RE.match(line)
            if m:
                kind = "figure" if m.group(1) == "Figure" else "table"
                entries.append(
                    FigureOrTableEntry(
                        kind=kind,
                        number=m.group(2),
                        title=m.group(3).strip(),
                        page=int(m.group(4)),
                    )
                )
    return entries


def page_index_for_printed_page(printed_page: int) -> int:
    """The document prints "Page N of 151" on every page at a fixed offset
    from PyMuPDF's 0-based page index; verified against the source (see
    logic-spec/versions/4.6/manifest.yaml notes)."""
    return printed_page - 1


def section_slug(number: str, title: str) -> str:
    """Stable filename base, e.g. "06-04-evaluate-age" for ("6.4", "Evaluate Age")."""
    padded = "-".join(part.zfill(2) for part in number.split("."))
    slug_title = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")
    return f"{padded}-{slug_title}"
