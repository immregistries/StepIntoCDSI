"""Phase 16: reference sets. Runs against the real registered/normalized
4.6 Logic Specification version and 4.65 Supporting Data release, plus
the real cdsi-fits-tests fixture directory - this is the one module
whose correctness is only really meaningful checked against real data,
since its entire job is producing checksums real files must match."""

import json

import pytest

from cdsi_reference_tools import paths, reference_sets as rs


def _skip_unless_real_data_available():
    pytest.importorskip("yaml")
    if not paths.manifest_path("4.6").exists():
        pytest.skip("Logic Specification 4.6 is not registered")
    if not paths.supporting_data_manifest_path("4.65").exists():
        pytest.skip("Supporting Data release 4.65 is not registered")
    if not paths.fits_tests_fixtures_dir().exists():
        pytest.skip("cdsi-fits-tests fixtures directory is not present as a sibling module")


def test_compute_fixture_set_is_deterministic():
    _skip_unless_real_data_available()
    first = rs.compute_fixture_set(paths.fits_tests_fixtures_dir())
    second = rs.compute_fixture_set(paths.fits_tests_fixtures_dir())
    assert first == second
    assert first["case_count"] == 4896


def test_create_reference_set_is_idempotent_for_identical_inputs():
    _skip_unless_real_data_available()
    first = rs.create_reference_set("4.6", "4.65", notes="test")
    second = rs.create_reference_set("4.6", "4.65", notes="test")
    assert first == second


def test_reference_set_id_is_derived_from_verified_checksums():
    _skip_unless_real_data_available()
    record = rs.create_reference_set("4.6", "4.65")
    fixture_sha = record["fits_fixture_set"]["sha256"]
    assert record["id"] == f"acip-4.6-sd-4.65-fits-{fixture_sha[:8]}"


def test_a_real_reference_set_validates_cleanly():
    _skip_unless_real_data_available()
    record = rs.create_reference_set("4.6", "4.65")
    assert rs.validate_reference_set(record["id"]) == []


def test_creating_against_an_unregistered_logic_spec_version_fails_clearly():
    _skip_unless_real_data_available()
    with pytest.raises(rs.ReferenceSetError):
        rs.create_reference_set("9.9-does-not-exist", "4.65")


def test_creating_against_an_unregistered_supporting_data_release_fails_clearly():
    _skip_unless_real_data_available()
    with pytest.raises(rs.ReferenceSetError):
        rs.create_reference_set("4.6", "9.9-does-not-exist")


def test_export_writes_the_fields_java_needs(tmp_path):
    _skip_unless_real_data_available()
    record = rs.create_reference_set("4.6", "4.65")
    dest = tmp_path / "reference-set.json"
    rs.export_for_fits_tests(record["id"], dest)
    exported = json.loads(dest.read_text(encoding="utf-8"))
    assert exported["id"] == record["id"]
    assert exported["supportingDataBundleSha256"] == record["supporting_data"]["bundle_sha256"]
    assert exported["fitsFixtureSetSha256"] == record["fits_fixture_set"]["sha256"]
    assert exported["fitsFixtureSetCaseCount"] == record["fits_fixture_set"]["case_count"]


def test_the_real_exported_reference_set_for_cdsi_fits_tests_is_current():
    """The committed cdsi-fits-tests/src/test/resources/reference-set.json
    must be the export of a real, currently-valid reference set - not a
    stale copy left over from before the bindings changed."""
    _skip_unless_real_data_available()
    export_path = paths.fits_tests_reference_set_export_path()
    if not export_path.exists():
        pytest.skip("reference-set.json has not been exported for cdsi-fits-tests yet")
    exported = json.loads(export_path.read_text(encoding="utf-8"))
    problems = rs.validate_reference_set(exported["id"])
    assert problems == [], problems
