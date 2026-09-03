"""Phase 21: per-step spec-conformance JUnit test tracking.

One unit here is one entry in mappings/spec-to-code.yaml - a spec section
(e.g. "8.6") or an unmapped_classes enum name (e.g. "EVALUATE_GENDER").
Each gets its own dedicated JUnit test class, written in isolation from
FITS and the rest of the pipeline (see cdsi-engine/AGENTS.md's Role A/
Role B workflow), and this module tracks two independent axes of
progress per unit:

  test_status - has a dedicated test class been written yet (Role A)?
  fix_status  - has the implementation been brought in line with it,
                reviewed, and merged (Role B)?

Pass/fail/error/skipped counts are never cached in status.yaml - they are
always read live from cdsi-engine's own surefire reports, so `step-tests
status` can never show a stale number. Run `mvn -pl cdsi-engine test` (or
the whole reactor) first; if you skip that, units show whatever
surefire-reports already happen to be on disk (or "not run" if there's
nothing there yet).
"""

import json
import xml.etree.ElementTree as ET
from pathlib import Path

import jsonschema
import yaml

from . import paths

BLOCKED_CATEGORIES = frozenset({"would_regress_other_tests", "upstream_step_defect", "undetermined"})

def _default_unit() -> dict:
    # A fresh dict every call - never a module-level literal shared (and
    # mutated) across units, which would make every unit's finding_ids
    # secretly the same list object.
    return {
        "test_status": "not_started",
        "test_class": None,
        "fix_status": "not_started",
        "blocked_category": None,
        "blocked_reason": None,
        "finding_ids": [],
    }


def load_status() -> dict:
    path = paths.step_test_status_path()
    if not path.exists():
        return {"spec_version": None, "units": {}}
    return yaml.safe_load(path.read_text(encoding="utf-8")) or {"spec_version": None, "units": {}}


def _save_status(status: dict) -> None:
    path = paths.step_test_status_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    ordered = {
        "spec_version": status["spec_version"],
        "units": {uid: status["units"][uid] for uid in sorted(status["units"], key=sort_key)},
    }
    path.write_text(yaml.safe_dump(ordered, sort_keys=False, allow_unicode=True), encoding="utf-8")


def mapping_units(version: str) -> dict[str, dict]:
    """Every unit id mappings/spec-to-code.yaml knows about for this
    version: id -> {"title": ..., "classes": [...]}."""
    mapping_file = paths.mapping_path()
    if not mapping_file.exists():
        return {}
    mapping = yaml.safe_load(mapping_file.read_text(encoding="utf-8")) or {}
    if mapping.get("spec_version") != version:
        return {}
    units: dict[str, dict] = {}
    for section, entry in mapping.get("sections", {}).items():
        units[section] = {
            "title": entry.get("title", ""),
            "classes": entry.get("implementation", {}).get("classes", []),
        }
    for enum_name, entry in mapping.get("unmapped_classes", {}).items():
        class_name = entry.get("class", "")
        simple_name = class_name.rsplit(".", 1)[-1] if class_name else ""
        units[enum_name] = {"title": simple_name, "classes": [class_name]}
    return units


def sync_status(version: str) -> list[str]:
    """Adds any unit from spec-to-code.yaml missing from status.yaml, with
    not_started/not_started defaults. Never touches an existing entry, and
    never removes one (a unit spec-to-code.yaml no longer has is a mapping
    problem to fix there, not a reason to silently drop its history here).
    Returns the ids actually added."""
    status = load_status()
    if not status.get("spec_version"):
        status["spec_version"] = version
    status.setdefault("units", {})
    added = []
    for unit_id in mapping_units(version):
        if unit_id not in status["units"]:
            status["units"][unit_id] = _default_unit()
            added.append(unit_id)
    if added:
        _save_status(status)
    return sorted(added, key=sort_key)


def validate_status(version: str) -> list[str]:
    problems: list[str] = []
    status_path = paths.step_test_status_path()
    if not status_path.exists():
        return [f"step-tests: {status_path} does not exist (run `step-tests sync --version {version}`)"]
    status = load_status()
    schema = json.loads((paths.schemas_dir() / "step-test-status.schema.json").read_text(encoding="utf-8"))
    try:
        jsonschema.validate(status, schema)
    except jsonschema.ValidationError as e:
        return [f"step-tests: status.yaml does not satisfy step-test-status.schema.json: {e.message}"]

    units = status.get("units", {})
    known_units = mapping_units(version)
    for unit_id, entry in units.items():
        if unit_id not in known_units:
            problems.append(f"step-tests: status.yaml has unit {unit_id!r}, which is not in spec-to-code.yaml")

        fix_status = entry.get("fix_status")
        category = entry.get("blocked_category")
        reason = entry.get("blocked_reason")
        if fix_status == "blocked":
            if category not in BLOCKED_CATEGORIES:
                problems.append(f"step-tests: unit {unit_id!r} is blocked but blocked_category is {category!r}")
            if not reason or not reason.strip():
                problems.append(f"step-tests: unit {unit_id!r} is blocked but has no blocked_reason")
        else:
            if category is not None:
                problems.append(
                    f"step-tests: unit {unit_id!r} has blocked_category set but fix_status is "
                    f"{fix_status!r}, not blocked")
            if reason is not None:
                problems.append(
                    f"step-tests: unit {unit_id!r} has blocked_reason set but fix_status is "
                    f"{fix_status!r}, not blocked")

        test_status = entry.get("test_status")
        if test_status == "not_started" and entry.get("test_class") is not None:
            problems.append(f"step-tests: unit {unit_id!r} has test_status not_started but test_class is set")
        if test_status == "tests_written" and not entry.get("test_class"):
            problems.append(f"step-tests: unit {unit_id!r} has test_status tests_written but no test_class")

    missing = sorted(set(known_units) - set(units), key=sort_key)
    if missing:
        problems.append(
            f"step-tests: {len(missing)} unit(s) from spec-to-code.yaml missing from status.yaml "
            f"(run `step-tests sync --version {version}`): {', '.join(missing)}")
    return problems


def _surefire_report_path(test_class: str) -> Path:
    cdsi_engine_root = paths.reference_root().parent / "cdsi-engine"
    return cdsi_engine_root / "target" / "surefire-reports" / f"TEST-{test_class}.xml"


def read_surefire_counts(test_class: str) -> dict | None:
    report = _surefire_report_path(test_class)
    if not report.exists():
        return None
    root = ET.parse(report).getroot()
    tests = int(root.get("tests", 0))
    failures = int(root.get("failures", 0))
    errors = int(root.get("errors", 0))
    skipped = int(root.get("skipped", 0))
    return {
        "passed": tests - failures - errors - skipped,
        "failures": failures,
        "errors": errors,
        "skipped": skipped,
    }


def sort_key(unit_id: str):
    # Numbered spec sections ("4.1", "8.6") sort numerically by
    # chapter.section; unmapped_classes enum names sort after all of them.
    parts = unit_id.split(".")
    if len(parts) == 2 and all(p.isdigit() for p in parts):
        return (0, int(parts[0]), int(parts[1]))
    return (1, unit_id)


def render_status_table(version: str) -> str:
    status = load_status()
    units = status.get("units", {})
    known_units = mapping_units(version)
    lines: list[str] = []

    blocked = [(uid, units[uid]) for uid in sorted(units, key=sort_key) if units[uid].get("fix_status") == "blocked"]
    if blocked:
        lines.append("NEEDS YOUR ATTENTION (blocked):")
        for uid, entry in blocked:
            title = known_units.get(uid, {}).get("title", "")
            lines.append(f"  {uid} {title}")
            lines.append(f"      [{entry.get('blocked_category')}] {entry.get('blocked_reason')}")
            if entry.get("finding_ids"):
                lines.append(f"      findings: {', '.join(entry['finding_ids'])}")
        lines.append("")
    else:
        lines.append("NEEDS YOUR ATTENTION (blocked): none")
        lines.append("")

    header = f"{'Unit':<8} {'Title':<45} {'Test status':<14} {'Pass/Fail/Err/Skip':<19} {'Fix status'}"
    lines.append(header)
    lines.append("-" * len(header))

    written = 0
    merged = 0
    for uid in sorted(units, key=sort_key):
        entry = units[uid]
        title = known_units.get(uid, {}).get("title", "")[:45]
        test_status = entry.get("test_status", "not_started")
        fix_status = entry.get("fix_status", "not_started")
        if test_status == "tests_written":
            written += 1
        if fix_status == "merged":
            merged += 1

        test_class = entry.get("test_class")
        counts = read_surefire_counts(test_class) if test_class else None
        if counts is None:
            counts_str = "-" if test_status == "not_started" else "not run"
        else:
            counts_str = f"{counts['passed']}/{counts['failures']}/{counts['errors']}/{counts['skipped']}"

        lines.append(f"{uid:<8} {title:<45} {test_status:<14} {counts_str:<19} {fix_status}")

    missing = sorted(set(known_units) - set(units), key=sort_key)
    lines.append("")
    if missing:
        lines.append(f"({len(missing)} unit(s) not yet synced from spec-to-code.yaml - run `step-tests sync`)")
    lines.append(
        f"Totals: {written}/{len(units)} unit(s) have tests written, "
        f"{merged}/{len(units)} fixed and merged, {len(blocked)} blocked.")
    return "\n".join(lines)
