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


def schemas_dir() -> Path:
    return reference_root() / "schemas"
