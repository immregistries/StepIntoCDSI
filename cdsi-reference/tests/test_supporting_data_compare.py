"""Phase 15: comparing two normalized Supporting Data releases. Runs
against the real registered 4.64/4.65 releases, checking the comparator's
output against ground truth independently confirmed by hand (see the
session notes / commit message for how each of these was verified before
the comparator existed) - this is not just "does it run," it's "does it
find the real, known differences and nothing else.\""""

import json

import pytest

from cdsi_reference_tools import paths, supporting_data_compare as cmp


def _skip_unless_both_normalized():
    pytest.importorskip("lxml")
    for release_id in ("4.64", "4.65"):
        if not (paths.supporting_data_normalized_dir(release_id) / "index.json").exists():
            pytest.skip(f"Release {release_id} is not normalized yet - run `supporting-data normalize`")


@pytest.mark.parametrize("value,expected", [(None, []), ("", []), ("x", ["x"]), ([1, 2], [1, 2])])
def test_as_list(value, expected):
    assert cmp._as_list(value) == expected


@pytest.mark.parametrize("value,expected", [(None, {}), ("", {}), ({"a": 1}, {"a": 1})])
def test_as_dict(value, expected):
    assert cmp._as_dict(value) == expected


def test_comparing_a_release_to_itself_finds_nothing(tmp_path, monkeypatch):
    _skip_unless_both_normalized()
    monkeypatch.setattr(paths, "supporting_data_diffs_dir", lambda: tmp_path)
    report = cmp.compare_releases("4.65", "4.65")
    assert report["change_count"] == 0
    assert report["changes"] == []


def test_known_series_level_changes_between_4_64_and_4_65():
    """Independently confirmed by hand against the raw normalized JSON
    before this comparator was written - see the Phase 15 commit."""
    _skip_unless_both_normalized()
    report = cmp.compare_releases("4.64", "4.65")
    series_changes = {
        (c["change_type"], c["identifier"]["antigen"], c["identifier"]["series"])
        for c in report["changes"] if c["category"] == "series"
    }
    assert series_changes == {
        ("added", "HepA", "HepA adult 2-dose series"),
        ("added", "HepA", "HepA adult Twinrix 3-dose series"),
        ("added", "HepA", "HepA adult Twinrix 4 dose Series"),
        ("added", "HepA", "HepA adult Twinrix secondary 3-dose series"),
        ("added", "HepA", "HepA adult Twinrix tertiary 3-dose series"),
        ("added", "HPV", "HPV 3-dose start under 15 years series"),
        ("added", "HPV", "HPV male 3-dose start under 15 years series"),
        ("added", "Polio", "Polio risk childhood series"),
        ("removed", "Typhoid", "Typhoid risk 1-dose series"),
        ("removed", "Typhoid", "Typhoid risk 4-dose series"),
        ("added", "Typhoid", "Typhoid risk series"),
    }


def test_no_antigens_or_schema_elements_added_or_removed():
    _skip_unless_both_normalized()
    report = cmp.compare_releases("4.64", "4.65")
    assert [c for c in report["changes"] if c["category"] == "antigen"] == []
    assert [c for c in report["changes"] if c["category"] == "schema_element"] == []


def test_systematic_live_virus_conflict_end_interval_change():
    """CDC shortened every live-virus-conflict end interval from 30 days
    to 28 days between 4.64 and 4.65 - a large (135-entry), completely
    uniform change, good for confirming the comparator doesn't choke on
    or mis-key a big systematic update."""
    _skip_unless_both_normalized()
    report = cmp.compare_releases("4.64", "4.65")
    conflict_changes = [c for c in report["changes"] if c["category"] == "live_virus_conflict"]
    assert len(conflict_changes) == 135
    assert all(c["change_type"] == "changed" for c in conflict_changes)
    assert all(c["field"] == "conflictEndInterval" for c in conflict_changes)
    assert all(c["old_value"] == "30 days" and c["new_value"] == "28 days" for c in conflict_changes)


def test_reports_are_written_to_diffs_directory():
    _skip_unless_both_normalized()
    cmp.compare_releases("4.64", "4.65")
    json_path = paths.supporting_data_diffs_dir() / "4.64-to-4.65.json"
    md_path = paths.supporting_data_diffs_dir() / "4.64-to-4.65.md"
    assert json_path.exists()
    assert md_path.exists()
    on_disk = json.loads(json_path.read_text(encoding="utf-8"))
    assert on_disk["from"] == "4.64"
    assert on_disk["to"] == "4.65"


def test_comparing_an_unnormalized_release_fails_clearly():
    with pytest.raises(cmp.CompareError):
        cmp.compare_releases("4.64", "9.9-not-a-real-release")
