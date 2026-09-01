"""Phase 16: reference sets bind together the exact inputs needed to
interpret one FITS test run - a Logic Specification version, a Supporting
Data release, and the FITS fixture set actually on disk - so a run can
record one identifier instead of three separately-drifting version
numbers, and so `cdsi-fits-tests` can fail clearly the moment any of the
three no longer matches what a reference set recorded.

Deviates from the plan's own illustrative YAML shape in one way, worth
being explicit about: the plan shows logic_spec/supporting_data/
fits_fixture_set as bare strings ("4.6", "<release-id>", "<fixture-set-
id>"). Here each is a small object carrying its own checksum
(logic_spec.source_sha256, supporting_data.bundle_sha256,
fits_fixture_set.sha256) - the plan's very next sentence says "Reference
sets must use verified identifiers and checksums," and a reference set
that's supposed to stay immutable and independently checkable shouldn't
have to trust that some other file's current checksum still matches what
it meant when created.

fits_fixture_set has no existing identity anywhere in this project - it's
computed here as a deterministic hash of every fixture file's own hash,
sorted by relative path (see _compute_fixture_set). cdsi-fits-tests
recomputes the identical hash the identical way in Java (see
ReferenceSetVerifier.java) - if that ever needs to change, change both
sides together and note it in both docstrings.
"""

import datetime
import hashlib
import json
from pathlib import Path

import jsonschema
import yaml

from . import extract, paths


class ReferenceSetError(Exception):
    pass


def _load_schema() -> dict:
    return json.loads((paths.schemas_dir() / "reference-set.schema.json").read_text(encoding="utf-8"))


def compute_fixture_set(fixtures_dir: Path) -> dict:
    """sha256 of every *.json fixture's own sha256, keyed by its path
    relative to fixtures_dir (POSIX-style, so this agrees across Windows
    and Linux) and sorted - not a single hash of concatenated file bytes,
    so it stays cheap to recompute one file's contribution mentally when
    investigating a mismatch."""
    if not fixtures_dir.exists():
        raise ReferenceSetError(f"No FITS fixtures directory at {fixtures_dir}")
    entries = []
    for path in sorted(fixtures_dir.rglob("*.json")):
        relative = path.relative_to(fixtures_dir).as_posix()
        entries.append((relative, extract.sha256_of(path)))
    entries.sort(key=lambda e: e[0])
    canonical = "".join(f"{path}:{digest}\n" for path, digest in entries)
    checksum = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
    return {"sha256": checksum, "case_count": len(entries)}


def _logic_spec_binding(version: str) -> dict:
    manifest_file = paths.manifest_path(version)
    if not manifest_file.exists():
        raise ReferenceSetError(f"Logic Specification version {version!r} is not registered")
    manifest = yaml.safe_load(manifest_file.read_text(encoding="utf-8"))
    return {"version": version, "source_sha256": manifest["sha256"]}


def _supporting_data_binding(release_id: str) -> dict:
    manifest_file = paths.supporting_data_manifest_path(release_id)
    if not manifest_file.exists():
        raise ReferenceSetError(f"Supporting Data release {release_id!r} is not registered")
    manifest = yaml.safe_load(manifest_file.read_text(encoding="utf-8"))
    return {
        "release_id": release_id,
        "source_filename": manifest["source_filename"],
        "bundle_sha256": manifest["bundle_sha256"],
    }


def build_reference_set_id(logic_spec_version: str, supporting_data_release: str, fixture_set_sha256: str) -> str:
    return f"acip-{logic_spec_version}-sd-{supporting_data_release}-fits-{fixture_set_sha256[:8]}"


def create_reference_set(logic_spec_version: str, supporting_data_release: str, notes: str = "") -> dict:
    logic_spec = _logic_spec_binding(logic_spec_version)
    supporting_data = _supporting_data_binding(supporting_data_release)
    fits_fixture_set = compute_fixture_set(paths.fits_tests_fixtures_dir())
    reference_set_id = build_reference_set_id(logic_spec_version, supporting_data_release, fits_fixture_set["sha256"])

    record = {
        "id": reference_set_id,
        "logic_spec": logic_spec,
        "supporting_data": supporting_data,
        "fits_fixture_set": fits_fixture_set,
        "created_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "status": "active",
        "notes": notes,
    }
    jsonschema.validate(record, _load_schema())

    reference_set_path = paths.reference_set_path(reference_set_id)
    if reference_set_path.exists():
        existing = yaml.safe_load(reference_set_path.read_text(encoding="utf-8"))
        if _binding_key(existing) == _binding_key(record):
            return existing  # identical inputs already produced this exact reference set - nothing to do
        raise ReferenceSetError(
            f"{reference_set_id!r} already exists with different bindings - reference sets are immutable "
            "once created. This should be unreachable in practice (the id is derived from the checksums "
            "themselves), so if you see this, something computed a checksum differently than before."
        )
    reference_set_path.parent.mkdir(parents=True, exist_ok=True)
    reference_set_path.write_text(yaml.safe_dump(record, sort_keys=False), encoding="utf-8")
    return record


def _binding_key(record: dict) -> tuple:
    return (
        record["logic_spec"]["source_sha256"],
        record["supporting_data"]["bundle_sha256"],
        record["fits_fixture_set"]["sha256"],
    )


def list_reference_sets() -> list[str]:
    d = paths.reference_sets_dir()
    if not d.exists():
        return []
    return sorted(p.stem for p in d.glob("*.yaml"))


def load_reference_set(reference_set_id: str) -> dict:
    path = paths.reference_set_path(reference_set_id)
    if not path.exists():
        raise ReferenceSetError(f"No reference set {reference_set_id!r} under {paths.reference_sets_dir()}")
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def validate_reference_set(reference_set_id: str) -> list[str]:
    """Re-derives every checksum a reference set recorded and compares -
    catches a reference set going stale (its Logic Specification version,
    Supporting Data release, or FITS fixture set changed since it was
    created) without needing cdsi-fits-tests or a live test run."""
    problems: list[str] = []
    try:
        record = load_reference_set(reference_set_id)
    except ReferenceSetError as e:
        return [str(e)]

    try:
        jsonschema.validate(record, _load_schema())
    except jsonschema.ValidationError as e:
        return [f"does not satisfy reference-set.schema.json: {e.message}"]

    try:
        current_logic_spec = _logic_spec_binding(record["logic_spec"]["version"])
        if current_logic_spec != record["logic_spec"]:
            problems.append(
                f"logic_spec drifted: reference set recorded {record['logic_spec']}, now {current_logic_spec}")
    except ReferenceSetError as e:
        problems.append(str(e))

    try:
        current_supporting_data = _supporting_data_binding(record["supporting_data"]["release_id"])
        if current_supporting_data != record["supporting_data"]:
            problems.append(
                f"supporting_data drifted: reference set recorded {record['supporting_data']}, "
                f"now {current_supporting_data}")
    except ReferenceSetError as e:
        problems.append(str(e))

    try:
        current_fixture_set = compute_fixture_set(paths.fits_tests_fixtures_dir())
        if current_fixture_set != record["fits_fixture_set"]:
            problems.append(
                f"fits_fixture_set drifted: reference set recorded {record['fits_fixture_set']}, "
                f"now {current_fixture_set}")
    except ReferenceSetError as e:
        problems.append(str(e))

    return problems


def export_for_fits_tests(reference_set_id: str, dest_path: Path | None = None) -> Path:
    """Writes the subset of fields cdsi-fits-tests' Java code needs as a
    plain JSON file checked into cdsi-fits-tests' own module - not a
    cross-module read at test time. cdsi-reference stays the sole
    authoring/reviewing source; this is a one-way, reviewed export, the
    same relationship mappings/spec-to-code.yaml has with cdsi-engine's
    source (read from, never read by, at runtime)."""
    record = load_reference_set(reference_set_id)
    export = {
        "id": record["id"],
        "logicSpecVersion": record["logic_spec"]["version"],
        "supportingDataRelease": record["supporting_data"]["release_id"],
        "supportingDataZipName": record["supporting_data"]["source_filename"],
        "supportingDataBundleSha256": record["supporting_data"]["bundle_sha256"],
        "fitsFixtureSetSha256": record["fits_fixture_set"]["sha256"],
        "fitsFixtureSetCaseCount": record["fits_fixture_set"]["case_count"],
        "createdAt": record["created_at"],
    }
    dest = dest_path or paths.fits_tests_reference_set_export_path()
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(export, indent=2, sort_keys=False) + "\n", encoding="utf-8")
    return dest
