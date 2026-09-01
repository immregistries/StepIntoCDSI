"""Phase 14: Supporting Data normalization. Runs against the real
registered 4.64/4.65 releases (skips cleanly if they aren't registered)
plus a couple of hermetic unit tests for the pure functions that don't
need real data."""

import json

import pytest

from cdsi_reference_tools import paths, supporting_data_normalize as norm


def _skip_unless_registered_and_normalized(release_id: str):
    pytest.importorskip("lxml")
    if not paths.supporting_data_manifest_path(release_id).exists():
        pytest.skip(f"Supporting Data release {release_id} is not registered yet")
    if not (paths.supporting_data_normalized_dir(release_id) / "index.json").exists():
        pytest.skip(f"Release {release_id} has not been normalized yet - run `supporting-data normalize`")


@pytest.mark.parametrize("filename,expected", [
    ("AntigenSupportingData- HepA-508.xml", "HepA"),
    ("AntigenSupportingData- Meningococcal B-508.xml", "Meningococcal B"),
    ("AntigenSupportingData-Cholera.xml", "Cholera"),
    ("ScheduleSupportingData.xml", None),
])
def test_antigen_name_from_filename(filename, expected):
    assert norm.antigen_name_from_filename(filename) == expected


@pytest.mark.parametrize("name,expected", [
    ("HepA", "hepa"),
    ("Meningococcal B", "meningococcal-b"),
    ("COVID-19", "covid-19"),
])
def test_slugify(name, expected):
    assert norm.slugify(name) == expected


def test_normalizing_cholera_force_lists_a_single_series_and_dose():
    _skip_unless_registered_and_normalized("4.65")
    data = json.loads((paths.supporting_data_normalized_dir("4.65") / "antigens" / "cholera.json").read_text())
    series = data["data"]["series"]
    assert isinstance(series, list) and len(series) == 1
    assert isinstance(series[0]["seriesDose"], list) and len(series[0]["seriesDose"]) == 1
    # Whitespace-trimmed, not "Cholera " as it appears in the raw source XML.
    assert series[0]["vaccineGroup"] == "Cholera"


def test_normalizing_multi_occurrence_fields_produces_lists_without_forcing():
    _skip_unless_registered_and_normalized("4.65")
    data = json.loads((paths.supporting_data_normalized_dir("4.65") / "antigens" / "cholera.json").read_text())
    dose = data["data"]["series"][0]["seriesDose"][0]
    assert isinstance(dose["preferableVaccine"], list) and len(dose["preferableVaccine"]) == 2
    # allowableVaccine only occurs once in the source for this dose - stays a bare object.
    assert isinstance(dose["allowableVaccine"], dict)


def test_index_cross_references_a_combination_vaccine_groups_antigens():
    _skip_unless_registered_and_normalized("4.65")
    index = json.loads((paths.supporting_data_normalized_dir("4.65") / "index.json").read_text())
    groups = {g["vaccineGroup"]: set(g["antigens"]) for g in index["schedule"]["vaccineGroups"]}
    assert {"Diphtheria", "Pertussis", "Tetanus"} <= groups["DTaP/Tdap/Td"]


def test_manifest_is_updated_with_normalizer_metadata():
    _skip_unless_registered_and_normalized("4.65")
    import yaml
    manifest = yaml.safe_load(paths.supporting_data_manifest_path("4.65").read_text(encoding="utf-8"))
    assert manifest["normalizer_version"]
    assert manifest["normalized_at"]
    assert manifest["warnings"] == []


def test_renormalizing_is_deterministic():
    _skip_unless_registered_and_normalized("4.65")
    before = (paths.supporting_data_normalized_dir("4.65") / "antigens" / "hepb.json").read_text()
    norm.normalize_release("4.65")
    after = (paths.supporting_data_normalized_dir("4.65") / "antigens" / "hepb.json").read_text()
    assert before == after


def test_a_deliberately_broken_xml_fails_xsd_validation(tmp_path):
    pytest.importorskip("lxml")
    xsd_source = None
    for release_id in ("4.65", "4.64"):
        candidate = paths.supporting_data_source_dir(release_id) / "xsd" / "AntigenSupportingData.xsd"
        if candidate.exists():
            xsd_source = candidate
            break
    if xsd_source is None:
        pytest.skip("No registered release has AntigenSupportingData.xsd to validate against")

    broken_xml = tmp_path / "broken.xml"
    broken_xml.write_text("<antigenSupportingData><unexpectedTag/></antigenSupportingData>", encoding="utf-8")

    errors = norm.validate_against_xsd(broken_xml, xsd_source)
    assert errors  # a completely unschema'd element must be rejected
