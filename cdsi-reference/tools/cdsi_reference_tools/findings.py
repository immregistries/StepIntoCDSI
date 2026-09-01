"""Phase 9: the formal findings record. A finding is a directory under
logic-spec/versions/<version>/findings/<id>/ holding finding.yaml (the
machine-readable record validate.py checks against finding.schema.json)
and finding.md (the human-readable narrative, from templates/finding.md).

A finding stays draft/open until a human confirms it - this module never
promotes a finding's status on its own, it only allocates ids and lists
what exists.
"""

import re
from pathlib import Path

import yaml

from . import paths

_ID_RE = re.compile(r"^SPEC-(?P<version>[0-9]+\.[0-9]+)-(?P<seq>[0-9]{4})$")


def list_finding_dirs(version: str) -> list[Path]:
    root = paths.findings_dir(version)
    if not root.exists():
        return []
    return sorted(p for p in root.iterdir() if p.is_dir())


def next_finding_id(version: str) -> str:
    """Next unused SPEC-<version>-NNNN id, scanning existing finding
    directories rather than keeping a separate counter file - the
    directories themselves are the source of truth."""
    max_seq = 0
    for d in list_finding_dirs(version):
        m = _ID_RE.match(d.name)
        if m and m.group("version") == version:
            max_seq = max(max_seq, int(m.group("seq")))
    return f"SPEC-{version}-{max_seq + 1:04d}"


def load_finding(finding_dir: Path) -> dict:
    return yaml.safe_load((finding_dir / "finding.yaml").read_text(encoding="utf-8"))
