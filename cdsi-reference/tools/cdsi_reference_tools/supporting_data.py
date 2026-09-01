"""Phase 13: registers versioned CDSi Supporting Data releases - the XML/
XSD/spreadsheet bundle cdsi-engine actually loads at runtime, kept as a
separate versioned tree from logic-spec/ since the two resources change on
different schedules and don't have a one-to-one version relationship.

Deliberately scoped to what Phase 13 itself asks for: preserve every
supplied file, compute checksums, write a manifest. Parsing the XML into
agent-readable structured data (normalized/, documentation/) is Phase 14,
not built yet - this module's manifests carry normalizer_version/
normalized_at as null until that exists, rather than fabricating values.

Only files matching supporting-data-*.zip are ever considered a real CDSi
release - cdsi-engine also bundles non-CDC schedules (a demo/preview set)
under the same directory for the web UI's benefit, and those are not CDSi
releases to version or compare here. See SOURCE_ZIP_GLOB.
"""

import datetime
import re
import zipfile
from pathlib import Path

import yaml

from . import extract, paths

SOURCE_ZIP_GLOB = "supporting-data-*.zip"

_FILENAME_VERSION_RE = re.compile(r"^supporting-data-(\d+(?:\.\d+)*)(?:-508)?\.zip$", re.IGNORECASE)
_INTERNAL_FOLDER_VERSION_RE = re.compile(r"Version\s+(\d+(?:\.\d+)*)", re.IGNORECASE)

_CATEGORY_BY_SUFFIX = {
    ".xml": "xml",
    ".xsd": "xsd",
    ".xlsx": "spreadsheets",
    ".xls": "spreadsheets",
    ".docx": "release-notes",
    ".doc": "release-notes",
    ".pdf": "release-notes",
    ".txt": "release-notes",
}


class SupportingDataError(Exception):
    pass


def list_source_zip_candidates(source_dir: Path) -> list[Path]:
    """Every file in source_dir that looks like a real CDSi Supporting Data
    release - never anything else bundled alongside it (a demo schedule,
    Thumbs.db, etc.)."""
    return sorted(source_dir.glob(SOURCE_ZIP_GLOB))


def parse_release_id_from_filename(filename: str) -> str | None:
    m = _FILENAME_VERSION_RE.match(filename)
    return m.group(1) if m else None


def _internal_top_level_version(zf: zipfile.ZipFile) -> str | None:
    """CDC's own zips have a single top-level folder like "Version 4.65 -
    508/" - cross-checking its version against the filename catches a
    renamed or mislabeled file without needing to trust either source
    alone."""
    top_levels = {name.split("/", 1)[0] for name in zf.namelist() if "/" in name}
    if len(top_levels) != 1:
        return None
    m = _INTERNAL_FOLDER_VERSION_RE.search(next(iter(top_levels)))
    return m.group(1) if m else None


def _categorize(basename: str) -> str | None:
    return _CATEGORY_BY_SUFFIX.get(Path(basename).suffix.lower())


def import_release(zip_path: Path) -> dict:
    """Registers one Supporting Data release. Idempotent: if release_id is
    already registered, verifies the new file's checksum matches what's on
    record instead of re-extracting anything - never silently replaces a
    registered release's source, matching logic-spec's manifest handling."""
    release_id = parse_release_id_from_filename(zip_path.name)
    if release_id is None:
        raise SupportingDataError(
            f"{zip_path.name!r} does not match {SOURCE_ZIP_GLOB!r} - only a real CDSi Supporting "
            "Data release zip can be registered, not an alternative schedule or other bundled file."
        )

    bundle_sha256 = extract.sha256_of(zip_path)
    manifest_file = paths.supporting_data_manifest_path(release_id)
    if manifest_file.exists():
        existing = yaml.safe_load(manifest_file.read_text(encoding="utf-8"))
        if existing.get("bundle_sha256") != bundle_sha256:
            raise SupportingDataError(
                f"Release {release_id!r} is already registered from a different source file "
                f"(recorded bundle_sha256={existing.get('bundle_sha256')!r}, {zip_path.name} hashes "
                f"to {bundle_sha256!r}). Never overwrite a registered release - it needs a new "
                "release id, the same as a Logic Specification version would."
            )
        return existing

    warnings: list[str] = []
    internal_version = _internal_top_level_version_from_path(zip_path)
    if internal_version is not None and internal_version != release_id:
        warnings.append(
            f"Filename says version {release_id!r} but the zip's own internal folder says "
            f"{internal_version!r} - double-check this file wasn't renamed incorrectly."
        )

    source_dir = paths.supporting_data_source_dir(release_id)
    for category in ("xml", "xsd", "spreadsheets", "release-notes"):
        (source_dir / category).mkdir(parents=True, exist_ok=True)

    files: list[dict] = []
    with zipfile.ZipFile(zip_path) as zf:
        for entry in zf.infolist():
            if entry.is_dir():
                continue
            basename = entry.filename.rsplit("/", 1)[-1]
            category = _categorize(basename)
            if category is None:
                warnings.append(f"Skipped {entry.filename!r}: not an xml/xsd/spreadsheet/release-notes file")
                continue
            dest = source_dir / category / basename
            dest.write_bytes(zf.read(entry))
            files.append({
                "path": f"{category}/{basename}",
                "sha256": extract.sha256_of(dest),
                "category": category,
            })

    preserved_zip = source_dir / zip_path.name
    preserved_zip.write_bytes(zip_path.read_bytes())

    files.sort(key=lambda f: f["path"])
    manifest = {
        "release_id": release_id,
        "source": f"CDC CDSi Supporting Data release {release_id}",
        "published_at": None,
        "retrieved_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source_filename": zip_path.name,
        "bundle_sha256": bundle_sha256,
        "files": files,
        "normalizer_version": None,
        "normalized_at": None,
        "warnings": warnings,
    }
    manifest_file.parent.mkdir(parents=True, exist_ok=True)
    manifest_file.write_text(yaml.safe_dump(manifest, sort_keys=False), encoding="utf-8")
    return manifest


def _internal_top_level_version_from_path(zip_path: Path) -> str | None:
    with zipfile.ZipFile(zip_path) as zf:
        return _internal_top_level_version(zf)


def import_all_from(source_dir: Path) -> list[dict]:
    """Registers every real CDSi release found in source_dir (see
    list_source_zip_candidates) - the normal way to pick up
    cdsi-engine's bundled supporting-data zips in one call."""
    return [import_release(p) for p in list_source_zip_candidates(source_dir)]


def list_registered_releases() -> list[str]:
    versions_dir = paths.supporting_data_root() / "versions"
    if not versions_dir.exists():
        return []
    return sorted((p.name for p in versions_dir.iterdir() if p.is_dir()),
                  key=lambda v: [int(x) for x in v.split(".") if x.isdigit()])
