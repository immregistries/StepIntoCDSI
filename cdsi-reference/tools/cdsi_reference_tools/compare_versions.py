"""Compares two registered Logic Specification versions (Phase 10 of the
reference-module plan). Only one version (4.6) is registered so far, so
this is a working skeleton that fails clearly rather than a full
implementation. Do not build out the real diffing logic speculatively -
this session's own experience (extraction bugs like appendix-scope
leakage and wrapped-row merging were only found by testing against the
real 4.6 PDF) is that comparison logic to a design like this needs a real
second version to test against, or it will look plausible and be wrong.
Implement PHASE_10_STEPS below in full, in order, the day a second version
is actually registered - do not skip ahead on the assumption a partial
implementation is safe to leave untested.

See also `cdsi-reference/README.md`'s "Comparing two versions" section
and StepIntoCDSi-Specification-Reference-Module-Plan.md's "Phase 10:
Build Version Comparison" (repository root) for the full narrative
version of this checklist, including the required change-report format.
"""

from . import paths

PHASE_10_STEPS = [
    "Add the new PDF under logic-spec/versions/<new>/source/ - never overwrite an existing version's PDF.",
    "Calculate its manifest.yaml metadata and checksum (logic-spec extract already does this for a newly-registered version).",
    "Run deterministic extraction (logic-spec extract --version <new>) and review its index.md for warnings before trusting it.",
    "Match content between <old> and <new> using section, table, figure, and business-rule identifiers - never page numbers alone (they shift between versions).",
    "Compare normalized text and structured data (step.yaml/transitions.yaml fields, extracted table text) per matched identifier, not a line-by-line PDF text diff - page wrapping and layout changes create noise a raw text diff can't distinguish from real content changes.",
    "Generate logic-spec/diffs/<old>-to-<new>.md (human-readable) and logic-spec/diffs/<old>-to-<new>.json (machine-readable companion).",
    "Identify which step packages changed (any of: step description, attribute table, decision table, process transition, business rules).",
    "Cross-reference mappings/spec-to-code.yaml to list which engine classes and tests a changed step package could affect.",
    "Carry a reviewed step package's plain-language walkthrough/interpretation forward into the new version ONLY when none of its own source dependencies (section text, tables, figures, transitions) changed - otherwise mark it for re-review rather than copying it forward silently.",
    "Mark every changed step package's review_status as needing review again, even if the change looks cosmetic - a human decides that, not the diff tool.",
]
"""Phase 10's ten required steps, verbatim from the plan, kept here so a
future agent has them without needing to locate the external plan
document. Update this list if the plan's Phase 10 section is ever
revised - it should never drift out of sync with the source of truth."""

CHANGE_REPORT_EXAMPLE = """\
6.4 Evaluate Age
- Step description: unchanged
- Attribute table: modified
- Decision table: modified
- Process transition: unchanged
- Business rules: one modified
- Mapped engine code: EvaluateAge
- Review required: yes
"""
"""One entry from the plan's example change report - the shape every
entry in <old>-to-<new>.md should follow once this is implemented."""


class NoSuchVersion(Exception):
    pass


def compare(from_version: str, to_version: str) -> str:
    for v in (from_version, to_version):
        if not paths.manifest_path(v).exists():
            raise NoSuchVersion(
                f"Version {v!r} is not registered (no manifest.yaml under "
                f"{paths.version_dir(v)}). Register it first."
            )
    steps = "\n".join(f"  {i}. {s}" for i, s in enumerate(PHASE_10_STEPS, 1))
    raise NotImplementedError(
        "Version comparison is not yet implemented - only version 4.6 is "
        f"registered. Now that {from_version!r} and {to_version!r} both "
        "exist, implement Phase 10 in full before returning a real "
        f"comparison. Required steps (see also this module's "
        "PHASE_10_STEPS and CHANGE_REPORT_EXAMPLE, and 'Comparing two "
        "versions' in cdsi-reference/README.md):\n" + steps
    )
