"""Phase 13: Supporting Data release registration. Runs against
cdsi-engine's real bundled zips for the "only real CDSi releases, never a
demo schedule" constraint and the two already-registered releases, and
against a synthetic zip (via a tmp_path-redirected supporting-data root)
for the idempotency/refusal behavior, so those don't touch the real
registered 4.64/4.65 data."""

import zipfile

import pytest

from cdsi_reference_tools import paths, supporting_data


def _engine_supporting_data_dir():
    d = paths.reference_root().parent / "cdsi-engine" / "src" / "main" / "resources" / "supporting-data"
    if not d.exists():
        pytest.skip("cdsi-engine source is not present as a sibling module")
    return d


def test_only_real_cdsi_releases_are_candidates_never_the_demo_schedule():
    engine_dir = _engine_supporting_data_dir()
    candidates = {p.name for p in supporting_data.list_source_zip_candidates(engine_dir)}
    assert candidates == {"supporting-data-4.64-508.zip", "supporting-data-4.65-508.zip"}
    assert "USA-DEMO-HPV9-0.1.zip" not in candidates


@pytest.mark.parametrize("filename,expected", [
    ("supporting-data-4.65-508.zip", "4.65"),
    ("supporting-data-4.64-508.zip", "4.64"),
    ("supporting-data-5.zip", "5"),
    ("USA-DEMO-HPV9-0.1.zip", None),
    ("Supporting-Data-4.65-508.ZIP", "4.65"),
])
def test_release_id_parsing(filename, expected):
    assert supporting_data.parse_release_id_from_filename(filename) == expected


def test_both_real_releases_are_registered_and_valid():
    _engine_supporting_data_dir()
    from cdsi_reference_tools import validate

    if not paths.supporting_data_manifest_path("4.65").exists():
        pytest.skip("Supporting Data release 4.65 is not registered yet - run `supporting-data import`")
    assert validate.validate_supporting_data_release("4.64") == []
    assert validate.validate_supporting_data_release("4.65") == []


def _write_fake_zip(path, *, version_folder="Version 9.9 - 508", contents=b"<xml/>"):
    with zipfile.ZipFile(path, "w") as zf:
        zf.writestr(f"{version_folder}/XML/ScheduleSupportingData.xml", contents)


def test_import_is_idempotent(tmp_path, monkeypatch):
    monkeypatch.setattr(paths, "supporting_data_root", lambda: tmp_path)
    zip_path = tmp_path / "supporting-data-9.9-508.zip"
    _write_fake_zip(zip_path)

    first = supporting_data.import_release(zip_path)
    second = supporting_data.import_release(zip_path)

    assert first == second
    assert first["release_id"] == "9.9"
    assert first["files"] == [{
        "path": "xml/ScheduleSupportingData.xml",
        "sha256": first["files"][0]["sha256"],
        "category": "xml",
    }]


def test_reimporting_a_release_id_with_different_content_is_refused(tmp_path, monkeypatch):
    monkeypatch.setattr(paths, "supporting_data_root", lambda: tmp_path)
    zip_path = tmp_path / "supporting-data-9.9-508.zip"
    _write_fake_zip(zip_path, contents=b"<xml>original</xml>")
    supporting_data.import_release(zip_path)

    _write_fake_zip(zip_path, contents=b"<xml>different</xml>")
    with pytest.raises(supporting_data.SupportingDataError):
        supporting_data.import_release(zip_path)


def test_mismatched_internal_version_is_a_warning_not_a_failure(tmp_path, monkeypatch):
    monkeypatch.setattr(paths, "supporting_data_root", lambda: tmp_path)
    zip_path = tmp_path / "supporting-data-9.9-508.zip"
    _write_fake_zip(zip_path, version_folder="Version 8.8 - 508")

    manifest = supporting_data.import_release(zip_path)
    assert manifest["release_id"] == "9.9"
    assert any("8.8" in w for w in manifest["warnings"])


def test_a_non_release_zip_is_refused(tmp_path, monkeypatch):
    monkeypatch.setattr(paths, "supporting_data_root", lambda: tmp_path)
    zip_path = tmp_path / "USA-DEMO-HPV9-0.1.zip"
    _write_fake_zip(zip_path)

    with pytest.raises(supporting_data.SupportingDataError):
        supporting_data.import_release(zip_path)
