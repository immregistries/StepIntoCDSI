"""Reads cdsi-engine's own source as ground truth for which LogicStepTypes
and classes actually exist, so mapping validation (Phase 8) never has to
trust a hand-maintained list of "the steps that exist" - it asks the code.

Deliberately simple regex parsing, not a real Java parser: LogicStepType.java
and LogicStepFactory.java are both short, mechanically regular files (an enum
constant list; an if/return dispatch chain), and staying this simple means
no new dependency and no risk of the parser silently mis-reading a change in
formatting style elsewhere in the codebase.
"""

import re
from dataclasses import dataclass
from pathlib import Path

from . import paths


@dataclass(frozen=True)
class EngineStepType:
    enum_name: str  # e.g. "EVALUATE_AGE"
    section: str  # e.g. "6.4" - may also be "End", "xx", or a bare chapter number like "7"
    title: str
    indent: bool


_ENUM_CONSTANT_RE = re.compile(
    r'^\s*([A-Z][A-Z0-9_]*)\(\s*"([^"]*)"\s*,\s*"([^"]*)"\s*,\s*(true|false)\s*\)', re.MULTILINE
)
_FACTORY_ENTRY_RE = re.compile(
    r"LogicStepType\.([A-Z][A-Z0-9_]*)\)\s*\)\s*\{\s*return new ([A-Za-z0-9_]+)\(", re.DOTALL
)


def logic_package_dir() -> Path:
    """cdsi-engine's core.logic package - the sibling module to cdsi-reference."""
    return paths.reference_root().parent / "cdsi-engine" / "src" / "main" / "java" / "org" / \
        "openimmunizationsoftware" / "cdsi" / "core" / "logic"


def parse_logic_step_types(path: Path | None = None) -> list[EngineStepType]:
    path = path or (logic_package_dir() / "LogicStepType.java")
    text = path.read_text(encoding="utf-8")
    return [
        EngineStepType(enum_name=m.group(1), section=m.group(2), title=m.group(3), indent=m.group(4) == "true")
        for m in _ENUM_CONSTANT_RE.finditer(text)
    ]


def parse_logic_step_factory(path: Path | None = None) -> dict[str, str]:
    """Returns {enum_name: class_name} for every step LogicStepFactory can
    actually instantiate - the authoritative "this class is real and wired
    up" signal, independent of LogicStepType's own declaration."""
    path = path or (logic_package_dir() / "LogicStepFactory.java")
    text = path.read_text(encoding="utf-8")
    return {m.group(1): m.group(2) for m in _FACTORY_ENTRY_RE.finditer(text)}


def class_file_exists(fully_qualified_class_name: str) -> bool:
    """Checks a mapping's cited class actually exists on disk, searching
    across cdsi-engine's whole source tree (not just core.logic) since
    mapping entries can legitimately cite core.data classes too (e.g. 4.1
    cites ForecastInput)."""
    relative = fully_qualified_class_name.replace(".", "/") + ".java"
    engine_src = paths.reference_root().parent / "cdsi-engine" / "src" / "main" / "java"
    return (engine_src / relative).exists()
