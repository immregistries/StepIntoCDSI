"""Phase 11: the two straightforward "at minimum validate" checklist items
this pass added - page count consistency and table existence - checked
against the real registered 4.6 version and its known, acknowledged
extraction limitation (three tables missing from the source PDF's own
List of Figures and Tables front matter)."""

import pytest

from cdsi_reference_tools import paths, validate


def _skip_if_not_registered():
    pytest.importorskip("pymupdf")
    if not paths.manifest_path("4.6").exists():
        pytest.skip("4.6 is not registered under logic-spec/versions/4.6/")


def test_manifest_page_count_matches_the_real_pdf():
    _skip_if_not_registered()
    assert validate.validate_manifest("4.6") == []


def test_step_packages_have_no_unexplained_missing_tables():
    _skip_if_not_registered()
    problems = validate.validate_step_packages("4.6")
    table_problems = [p for p in problems if "extracted/tables/" in p]
    assert table_problems == []


def test_known_loft_gap_tables_are_still_acknowledged():
    # If this ever fails, either the extractor learned to recover these
    # tables (great - shrink TABLES_MISSING_FROM_LOFT and update the
    # README) or the acknowledgment list drifted from reality.
    assert validate.TABLES_MISSING_FROM_LOFT == frozenset({"6-11", "6-19", "7-8"})
    for number in validate.TABLES_MISSING_FROM_LOFT:
        text_name = f"table-{number}.txt"
        assert not (paths.tables_dir("4.6") / text_name).exists(), (
            f"table-{number}.txt now exists in extracted/tables/ - remove {number!r} "
            "from TABLES_MISSING_FROM_LOFT, it's no longer a real gap"
        )
