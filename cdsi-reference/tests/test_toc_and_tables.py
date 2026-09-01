"""Regression tests for the deterministic extraction utilities, run
against the actual registered 4.6 source PDF - these are the tests that
would catch a PyMuPDF upgrade or a regex tweak silently breaking
extraction. Skipped if the source PDF hasn't been registered yet (e.g. a
fresh checkout before `logic-spec extract` has ever been run)."""

import pytest

from cdsi_reference_tools import extract_tables, paths, toc

pytest.importorskip("pymupdf")
import pymupdf as fitz  # noqa: E402


def _source_pdf_or_skip():
    candidates = list(paths.source_dir("4.6").glob("*.pdf"))
    if not candidates:
        pytest.skip("4.6 source PDF is not registered under logic-spec/versions/4.6/source/")
    return candidates[0]


@pytest.fixture(scope="module")
def doc():
    d = fitz.open(_source_pdf_or_skip())
    yield d
    d.close()


def test_toc_finds_chapter_6_sections(doc):
    entries = toc.parse_toc(doc, toc.find_toc_pages(doc))
    by_number = {e.number: e for e in entries}
    assert by_number["6.4"].title == "Evaluate Age"
    assert by_number["6.4"].page == 52
    assert by_number["6.5"].title == "Evaluate Preferable Interval"


def test_loft_finds_evaluate_age_tables(doc):
    entries = toc.parse_loft(doc, toc.find_loft_pages(doc))
    numbers = {e.number for e in entries if e.kind == "table"}
    assert {"6-14", "6-15", "6-16"} <= numbers


def test_table_6_14_extracts_as_clean_grid(doc):
    loft = toc.parse_loft(doc, toc.find_loft_pages(doc))
    entry = next(e for e in loft if e.kind == "table" and e.number == "6-14")
    caption_page = toc.page_index_for_printed_page(entry.page)
    result = extract_tables.extract_table(doc, entry, caption_page)
    assert result.warnings == []
    assert result.rows is not None
    assert result.rows[0] == ["Attribute Type", "Attribute Name", "Assumed Value if Empty"]
    assert len(result.rows) == 6


def test_table_6_15_decision_grid_merges_wrapped_conditions(doc):
    loft = toc.parse_loft(doc, toc.find_loft_pages(doc))
    entry = next(e for e in loft if e.kind == "table" and e.number == "6-15")
    caption_page = toc.page_index_for_printed_page(entry.page)
    result = extract_tables.extract_table(doc, entry, caption_page)
    assert result.rows is not None
    condition_cells = [row[0] for row in result.rows]
    assert any("absolute minimum age date" in c for c in condition_cells)
    # every condition row's wrapped text must have merged into one cell,
    # not be split across several rows with only column 0 populated
    assert len(result.rows) == 6  # header + 4 conditions + OUTCOMES


def test_merge_wrapped_rows_handles_both_wrap_directions():
    # column-0 wraps (a label/condition split across physical lines)
    rows = [
        ["A long condition that", "Yes", "No"],
        ["wraps across two lines", None, None],
    ]
    merged = extract_tables._merge_wrapped_rows(rows)
    assert merged == [["A long condition that wraps across two lines", "Yes", "No"]]

    # non-zero-column wraps (row label blank, several columns wrap at once)
    rows = [
        ["OUTCOMES", "First outcome", "Second"],
        [None, "continues here", "continues too"],
    ]
    merged = extract_tables._merge_wrapped_rows(rows)
    assert merged == [["OUTCOMES", "First outcome continues here", "Second continues too"]]

    # a fully blank spacer row is dropped, not merged
    rows = [["A", "B"], [None, None], ["C", "D"]]
    assert extract_tables._merge_wrapped_rows(rows) == [["A", "B"], ["C", "D"]]
