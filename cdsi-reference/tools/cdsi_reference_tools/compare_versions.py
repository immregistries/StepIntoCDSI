"""Compares two registered Logic Specification versions (Phase 10 of the
reference-module plan). Only one version (4.6) is registered so far, so
this is a working skeleton that fails clearly rather than a full
implementation - build out the identifier-based section/table/figure
diffing described in the plan once a second version exists to test it
against."""

from . import paths


class NoSuchVersion(Exception):
    pass


def compare(from_version: str, to_version: str) -> str:
    for v in (from_version, to_version):
        if not paths.manifest_path(v).exists():
            raise NoSuchVersion(
                f"Version {v!r} is not registered (no manifest.yaml under "
                f"{paths.version_dir(v)}). Register it first."
            )
    raise NotImplementedError(
        "Version comparison is not yet implemented - only version 4.6 is "
        "registered. See Phase 10 of StepIntoCDSi-Specification-Reference-Module-Plan.md "
        "for the required behavior (match by section/table/figure/rule "
        "identifiers, not a line-by-line PDF text diff) once a second "
        "version needs to be compared against it."
    )
