"""Regression tests for Phase 9's formal findings record: the schema, the
id-allocation helper, and validate_findings() against the real findings
already recorded under logic-spec/versions/4.6/findings/."""

from cdsi_reference_tools import findings, validate


def test_seven_findings_recorded_for_4_6():
    dirs = findings.list_finding_dirs("4.6")
    assert len(dirs) == 7
    assert {d.name for d in dirs} == {f"SPEC-4.6-{i:04d}" for i in range(1, 8)}


def test_next_finding_id_continues_the_sequence():
    assert findings.next_finding_id("4.6") == "SPEC-4.6-0008"
    assert findings.next_finding_id("4.7") == "SPEC-4.7-0001"


def test_validate_findings_has_no_problems():
    assert validate.validate_findings("4.6") == []


def test_every_finding_loads_with_a_valid_category():
    valid_categories = {
        "IMPLEMENTATION_MISMATCH",
        "SPECIFICATION_AMBIGUITY",
        "SUPPORTING_DATA_CONFLICT",
        "FITS_DIFFERENCE",
    }
    for d in findings.list_finding_dirs("4.6"):
        data = findings.load_finding(d)
        assert data["category"] in valid_categories
        assert data["status"] in {"draft", "open", "confirmed", "resolved", "wontfix"}
