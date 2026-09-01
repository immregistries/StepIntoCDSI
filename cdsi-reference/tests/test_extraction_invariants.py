"""Phase 11's harder-to-express checklist items, proven empirically against
the real registered 4.6 version rather than asserted in prose:

- "Generated files are reproducible from the same source and tool version."
- "Reviewed files are not overwritten during extraction."

Both come from one experiment: snapshot every file under this version's
directory, re-run extraction, and confirm nothing outside extracted/
changed and everything inside it reproduced byte-for-byte. This is the
slowest test in the suite (a real ~45s extraction run) - worth it, since
it is the one test that would actually catch a regenerated PDF-derived
file silently drifting, or extraction accidentally writing into
steps/concepts/diagrams/findings/mappings.
"""

import hashlib

import pytest

from cdsi_reference_tools import extract, paths


def _sha256(path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def _snapshot(version_dir) -> dict:
    return {
        p.relative_to(version_dir).as_posix(): _sha256(p)
        for p in version_dir.rglob("*")
        if p.is_file()
    }


def test_extraction_is_reproducible_and_never_touches_reviewed_content():
    pytest.importorskip("pymupdf")
    version_dir = paths.version_dir("4.6")
    if not paths.manifest_path("4.6").exists():
        pytest.skip("4.6 is not registered under logic-spec/versions/4.6/")

    before = _snapshot(version_dir)
    extract.run_extract("4.6")
    after = _snapshot(version_dir)

    extracted_prefix = "extracted/"
    reviewed_before = {k: v for k, v in before.items() if not k.startswith(extracted_prefix)}
    reviewed_after = {k: v for k, v in after.items() if not k.startswith(extracted_prefix)}
    assert reviewed_before == reviewed_after, (
        "logic-spec extract changed a file outside extracted/ - reviewed content "
        "(steps/, concepts/, diagrams/, findings/, manifest.yaml, index.md) must "
        "never be touched by extraction"
    )

    generated_before = {k: v for k, v in before.items() if k.startswith(extracted_prefix)}
    generated_after = {k: v for k, v in after.items() if k.startswith(extracted_prefix)}
    assert generated_before == generated_after, (
        "Re-running logic-spec extract against the same source PDF and tool version "
        "produced different output - extraction is supposed to be fully deterministic"
    )
