"""Shared path resolution so every tool agrees on where things live.

cdsi-reference/
    logic-spec/versions/<version>/...
"""

from pathlib import Path


def reference_root() -> Path:
    """The cdsi-reference/ directory (parent of tools/cdsi_reference_tools/)."""
    return Path(__file__).resolve().parents[2]


def logic_spec_root() -> Path:
    return reference_root() / "logic-spec"


def version_dir(version: str) -> Path:
    return logic_spec_root() / "versions" / version


def source_dir(version: str) -> Path:
    return version_dir(version) / "source"


def manifest_path(version: str) -> Path:
    return version_dir(version) / "manifest.yaml"


def extracted_dir(version: str) -> Path:
    return version_dir(version) / "extracted"


def sections_dir(version: str) -> Path:
    return extracted_dir(version) / "sections"


def figures_dir(version: str) -> Path:
    return extracted_dir(version) / "figures"


def tables_dir(version: str) -> Path:
    return extracted_dir(version) / "tables"


def steps_dir(version: str) -> Path:
    return version_dir(version) / "steps"


def findings_dir(version: str) -> Path:
    return version_dir(version) / "findings"


def schemas_dir() -> Path:
    return reference_root() / "schemas"


def mapping_path() -> Path:
    return reference_root() / "mappings" / "spec-to-code.yaml"


# --- Per-step spec-conformance test tracking (Phase 21) ---


def step_tests_dir() -> Path:
    return reference_root() / "step-tests"


def step_test_status_path() -> Path:
    return step_tests_dir() / "status.yaml"


# --- Committed, shareable HTML snapshots ---


def dashboards_dir() -> Path:
    return reference_root() / "dashboards"


def step_test_dashboard_path() -> Path:
    return dashboards_dir() / "step-tests.html"


def fits_dashboard_path() -> Path:
    return dashboards_dir() / "fits-results.html"


def dashboard_index_path() -> Path:
    return dashboards_dir() / "index.html"


def fits_runs_dir() -> Path:
    return reference_root().parent / "cdsi-fits-tests" / "target" / "fits-runs"


# --- Supporting Data (Phase 13) - a separate versioned tree from
# logic-spec/, since the two resources change on different schedules and
# don't have a one-to-one version relationship. ---


def supporting_data_root() -> Path:
    return reference_root() / "supporting-data"


def supporting_data_version_dir(release_id: str) -> Path:
    return supporting_data_root() / "versions" / release_id


def supporting_data_source_dir(release_id: str) -> Path:
    return supporting_data_version_dir(release_id) / "source"


def supporting_data_manifest_path(release_id: str) -> Path:
    return supporting_data_version_dir(release_id) / "manifest.yaml"


def supporting_data_normalized_dir(release_id: str) -> Path:
    return supporting_data_version_dir(release_id) / "normalized"


def supporting_data_documentation_dir(release_id: str) -> Path:
    return supporting_data_version_dir(release_id) / "documentation"


def supporting_data_findings_dir(release_id: str) -> Path:
    return supporting_data_version_dir(release_id) / "findings"


def supporting_data_validation_dir(release_id: str) -> Path:
    return supporting_data_version_dir(release_id) / "validation"


def supporting_data_diffs_dir() -> Path:
    return supporting_data_root() / "diffs"


# --- Reference sets (Phase 16) ---


def reference_sets_dir() -> Path:
    return reference_root() / "reference-sets"


def reference_set_path(reference_set_id: str) -> Path:
    return reference_sets_dir() / f"{reference_set_id}.yaml"


def fits_tests_fixtures_dir() -> Path:
    return reference_root().parent / "cdsi-fits-tests" / "src" / "test" / "resources" / "fits"


def fits_tests_reference_set_export_path() -> Path:
    return reference_root().parent / "cdsi-fits-tests" / "src" / "test" / "resources" / "reference-set.json"
