"""Deterministic validation - no network access, no LLM. Checks that a
version's source/manifest/extraction/step packages are internally
consistent (Phase 11 of the reference-module plan, scoped to what exists
so far: the two reviewed pilot step packages, not yet the full
Chapters 4-9 extraction inventory)."""

import json
from pathlib import Path

import jsonschema
import yaml

from . import extract, paths


class ValidationError(Exception):
    pass


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
            if not (target[0].isdigit() or target.startswith("end")):
                problems.append(f"{step_dir.name}: transition target {target!r} is neither a section number nor an 'end-*' marker")

    return problems


def validate_version(version: str) -> list[str]:
    problems = []
    problems.extend(f"manifest: {p}" for p in validate_manifest(version))
    problems.extend(f"steps: {p}" for p in validate_step_packages(version))
    return problems
