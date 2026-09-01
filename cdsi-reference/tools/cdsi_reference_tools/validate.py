"""Deterministic validation - no network access, no LLM (enforced by
network_guard, installed for every `logic-spec` CLI invocation, not just
this one). Checks that a version's source/manifest/extraction/step
packages/mappings/findings are internally consistent (Phase 11 of the
reference-module plan)."""

import json
from pathlib import Path

import jsonschema
import pymupdf as fitz
import yaml

from . import engine_index, extract, findings, paths


class ValidationError(Exception):
    pass


# Tables that genuinely exist in the source PDF's body but are missing from
# its own List of Figures and Tables front matter, which is what drives
# extraction - discovered while documenting Chapters 6-7 (see README.md's
# "Known extraction limitations"), transcribed by hand into their step
# packages instead. Acknowledged here so validate_step_packages doesn't
# report a known, already-investigated gap as a new problem; add to this
# set only after confirming a table is truly absent from the LOFT, not as
# a shortcut past a real extraction bug.
TABLES_MISSING_FROM_LOFT = frozenset({"6-11", "6-19", "7-8"})


def _load_schema(name: str) -> dict:
    return json.loads((paths.schemas_dir() / name).read_text(encoding="utf-8"))


def validate_manifest(version: str) -> list[str]:
    problems = []
    manifest_file = paths.manifest_path(version)
    if not manifest_file.exists():
        return [f"No manifest.yaml for version {version}"]
    manifest = yaml.safe_load(manifest_file.read_text(encoding="utf-8"))

    schema = _load_schema("manifest.schema.json")
    try:
        jsonschema.validate(manifest, schema)
    except jsonschema.ValidationError as e:
        problems.append(f"manifest.yaml does not satisfy manifest.schema.json: {e.message}")

    source_pdf = paths.source_dir(version) / manifest.get("source_filename", "")
    if not source_pdf.exists():
        problems.append(f"Source file listed in manifest does not exist: {source_pdf}")
    else:
        actual_sha256 = extract.sha256_of(source_pdf)
        if actual_sha256 != manifest.get("sha256"):
            problems.append(
                f"Source checksum mismatch: manifest says {manifest.get('sha256')}, "
                f"file hashes to {actual_sha256}"
            )
        doc = fitz.open(source_pdf)
        try:
            actual_page_count = doc.page_count
        finally:
            doc.close()
        if actual_page_count != manifest.get("page_count"):
            problems.append(
                f"Page count mismatch: manifest says {manifest.get('page_count')}, "
                f"source PDF actually has {actual_page_count} pages"
            )
    return problems


def validate_step_packages(version: str) -> list[str]:
    problems = []
    step_schema = _load_schema("step.schema.json")
    transition_schema = _load_schema("transition.schema.json")

    steps_root = paths.steps_dir(version)
    if not steps_root.exists():
        return [f"No steps/ directory for version {version}"]

    known_sections = set()
    step_dirs = sorted(p for p in steps_root.iterdir() if p.is_dir())
    for step_dir in step_dirs:
        step_yaml = step_dir / "step.yaml"
        if not step_yaml.exists():
            problems.append(f"{step_dir.name}: missing step.yaml")
            continue
        data = yaml.safe_load(step_yaml.read_text(encoding="utf-8"))
        try:
            jsonschema.validate(data, step_schema)
        except jsonschema.ValidationError as e:
            problems.append(f"{step_dir.name}/step.yaml does not satisfy step.schema.json: {e.message}")
            continue
        known_sections.add(data["section"])

        if not (step_dir / "index.md").exists():
            problems.append(f"{step_dir.name}: missing index.md")

        for figure_label in data.get("figures", []):
            number = figure_label.replace("Figure ", "")
            image_name = f"figure-{number.replace('.', '-')}.png"
            if not (step_dir / "figures" / image_name).exists():
                problems.append(f"{step_dir.name}: step.yaml references {figure_label!r} but {image_name} is missing from figures/")

        # Unlike figures, tables are never copied into the step package itself -
        # they're referenced back to the shared extraction output, so look there.
        tables_dir = paths.tables_dir(version)
        for table_label in data.get("tables", []):
            number = table_label.replace("Table ", "")
            if number in TABLES_MISSING_FROM_LOFT:
                continue
            text_name = f"table-{number.replace('.', '-')}.txt"
            if not (tables_dir / text_name).exists():
                problems.append(f"{step_dir.name}: step.yaml references {table_label!r} but {text_name} is missing from extracted/tables/")

    for step_dir in step_dirs:
        transitions_yaml = step_dir / "transitions.yaml"
        if not transitions_yaml.exists():
            problems.append(f"{step_dir.name}: missing transitions.yaml")
            continue
        data = yaml.safe_load(transitions_yaml.read_text(encoding="utf-8"))
        try:
            jsonschema.validate(data, transition_schema)
        except jsonschema.ValidationError as e:
            problems.append(f"{step_dir.name}/transitions.yaml does not satisfy transition.schema.json: {e.message}")
            continue
        for t in data.get("transitions", []):
            target = t["to"]
            # A target is fine if it's a known reviewed step section number,
            # OR a section number we haven't reviewed yet (not every step
            # has a package during the pilot phase), OR an explicit
            # terminal/end marker.
            if not (target[0].isdigit() or target.upper().startswith("END")):
                problems.append(f"{step_dir.name}: transition target {target!r} is neither a section number nor an 'end-*' marker")

    return problems


def _mapping_path() -> Path:
    return paths.reference_root() / "mappings" / "spec-to-code.yaml"


def validate_mappings(version: str) -> list[str]:
    """Phase 8: cross-checks mappings/spec-to-code.yaml against both the
    step packages on disk and cdsi-engine's actual source - never trusts
    either side alone. Reports the five categories the plan specifies."""
    problems: list[str] = []
    mapping_file = _mapping_path()
    if not mapping_file.exists():
        return ["mappings/spec-to-code.yaml does not exist"]
    mapping = yaml.safe_load(mapping_file.read_text(encoding="utf-8")) or {}
    schema = _load_schema("mapping.schema.json")
    try:
        jsonschema.validate(mapping, schema)
    except jsonschema.ValidationError as e:
        problems.append(f"spec-to-code.yaml does not satisfy mapping.schema.json: {e.message}")
        return problems
    sections: dict = mapping.get("sections", {})
    unmapped_classes: dict = mapping.get("unmapped_classes", {})

    for key, entry in unmapped_classes.items():
        if not engine_index.class_file_exists(entry["class"]):
            problems.append(f"mapping: unmapped_classes[{key!r}] cites class {entry['class']!r}, which does not exist on disk")

    mapped_classes: set[str] = set()
    for section_number, entry in sections.items():
        is_unmapped = entry.get("mapping_status") == "unmapped"
        classes = entry.get("implementation", {}).get("classes", [])
        if not classes and not is_unmapped:
            problems.append(f"mapping: section {section_number!r} has no implementation.classes")
        for cls in classes:
            mapped_classes.add(cls)
            if not engine_index.class_file_exists(cls):
                problems.append(f"mapping: section {section_number!r} cites class {cls!r}, which does not exist on disk")
        if not entry.get("tests") and not is_unmapped:
            problems.append(f"mapping: section {section_number!r} has no tests entry and no explicit test-gap status")

    # Step packages without implementation mappings / specification steps
    # without mapped engine code: every on-disk step package must have a
    # corresponding, populated mapping entry.
    steps_root = paths.steps_dir(version)
    if steps_root.exists():
        for step_dir in sorted(p for p in steps_root.iterdir() if p.is_dir()):
            step_yaml = step_dir / "step.yaml"
            if not step_yaml.exists():
                continue  # already reported by validate_step_packages
            data = yaml.safe_load(step_yaml.read_text(encoding="utf-8")) or {}
            section = data.get("section")
            if section not in sections:
                problems.append(f"mapping: step package {step_dir.name!r} (section {section!r}) has no entry in spec-to-code.yaml")

    # Logic-step classes without mapped specification sections: ask
    # cdsi-engine itself which LogicStepTypes exist and are instantiable,
    # then check each one's class is cited by SOME section's mapping -
    # this is what catches an engine class Phase 5 never documented,
    # independent of whatever section number (if any) is baked into the
    # enum constant itself.
    engine_types = engine_index.parse_logic_step_types()
    factory = engine_index.parse_logic_step_factory()
    for step_type in engine_types:
        if step_type.enum_name == "END":
            continue  # terminal marker, not a step
        class_name = factory.get(step_type.enum_name)
        if class_name is None:
            problems.append(f"engine: LogicStepType.{step_type.enum_name} has no LogicStepFactory entry (cannot be instantiated)")
            continue
        if step_type.enum_name in unmapped_classes:
            continue  # acknowledged gap - see mappings/spec-to-code.yaml's unmapped_classes note
        if not any(cls == class_name or cls.endswith("." + class_name) for cls in mapped_classes):
            problems.append(
                f"engine: LogicStepType.{step_type.enum_name} (class {class_name}, "
                f"enum's own section label {step_type.section!r}) is not cited by any section's "
                "implementation.classes in spec-to-code.yaml, and is not acknowledged in unmapped_classes"
            )

    return problems


def acknowledged_gaps(version: str) -> list[str]:
    """Informational, non-fatal companion to validate_mappings: the notes
    recorded for classes deliberately excluded from that function's problem
    list, so `logic-spec validate` stays green without those gaps going
    silent."""
    mapping_file = _mapping_path()
    if not mapping_file.exists():
        return []
    mapping = yaml.safe_load(mapping_file.read_text(encoding="utf-8")) or {}
    return [
        f"{key} ({entry['class']}): {entry['note'].strip()}"
        for key, entry in mapping.get("unmapped_classes", {}).items()
    ]


def validate_findings(version: str) -> list[str]:
    """Phase 9: every finding.yaml satisfies finding.schema.json, has a
    finding.md alongside it, and its id matches both its own directory name
    and this version - deliberately does NOT check whether spec_sections/
    code_locations point at real things, since a finding can legitimately
    describe something cross-cutting (no single section) or something with
    no corresponding class at all (a spec ambiguity)."""
    problems = []
    finding_schema = _load_schema("finding.schema.json")
    seen_ids: set[str] = set()

    for finding_dir in findings.list_finding_dirs(version):
        finding_yaml = finding_dir / "finding.yaml"
        if not finding_yaml.exists():
            problems.append(f"{finding_dir.name}: missing finding.yaml")
            continue
        data = yaml.safe_load(finding_yaml.read_text(encoding="utf-8"))
        try:
            jsonschema.validate(data, finding_schema)
        except jsonschema.ValidationError as e:
            problems.append(f"{finding_dir.name}/finding.yaml does not satisfy finding.schema.json: {e.message}")
            continue
        if data["id"] != finding_dir.name:
            problems.append(f"{finding_dir.name}: finding.yaml's id {data['id']!r} does not match its directory name")
        if not data["id"].startswith(f"SPEC-{version}-"):
            problems.append(f"{finding_dir.name}: id {data['id']!r} does not belong to version {version!r}")
        if data["id"] in seen_ids:
            problems.append(f"{finding_dir.name}: duplicate finding id {data['id']!r}")
        seen_ids.add(data["id"])
        if not (finding_dir / "finding.md").exists():
            problems.append(f"{finding_dir.name}: missing finding.md")

    return problems


def validate_supporting_data_release(release_id: str) -> list[str]:
    """Phase 13: a registered release's manifest satisfies its schema, its
    preserved zip's checksum still matches bundle_sha256, and every file it
    lists on disk still matches its recorded per-file checksum - catches
    the source silently drifting or a file going missing after
    registration, the Supporting Data equivalent of validate_manifest."""
    problems = []
    manifest_file = paths.supporting_data_manifest_path(release_id)
    if not manifest_file.exists():
        return [f"No manifest.yaml for Supporting Data release {release_id}"]
    manifest = yaml.safe_load(manifest_file.read_text(encoding="utf-8"))

    schema = _load_schema("supporting-data-manifest.schema.json")
    try:
        jsonschema.validate(manifest, schema)
    except jsonschema.ValidationError as e:
        problems.append(f"manifest.yaml does not satisfy supporting-data-manifest.schema.json: {e.message}")
        return problems

    source_dir = paths.supporting_data_source_dir(release_id)
    preserved_zip = source_dir / manifest.get("source_filename", "")
    if not preserved_zip.exists():
        problems.append(f"Preserved source zip does not exist: {preserved_zip}")
    else:
        actual_bundle_sha256 = extract.sha256_of(preserved_zip)
        if actual_bundle_sha256 != manifest.get("bundle_sha256"):
            problems.append(
                f"Bundle checksum mismatch: manifest says {manifest.get('bundle_sha256')}, "
                f"preserved zip hashes to {actual_bundle_sha256}"
            )

    for file_entry in manifest.get("files", []):
        file_path = source_dir / file_entry["path"]
        if not file_path.exists():
            problems.append(f"manifest lists {file_entry['path']!r} but it is missing from source/")
            continue
        actual_sha256 = extract.sha256_of(file_path)
        if actual_sha256 != file_entry["sha256"]:
            problems.append(
                f"{file_entry['path']}: checksum mismatch (manifest says {file_entry['sha256']}, "
                f"file hashes to {actual_sha256})"
            )

    return problems


def validate_version(version: str) -> list[str]:
    problems = []
    problems.extend(f"manifest: {p}" for p in validate_manifest(version))
    problems.extend(f"steps: {p}" for p in validate_step_packages(version))
    problems.extend(validate_mappings(version))
    problems.extend(f"findings: {p}" for p in validate_findings(version))
    return problems
