"""Phase 23: the reviewed Phase B progress ledger - one immutable YAML
record per reviewed round (cdsi-reference/progress-ledger/entries/<id>.yaml,
schemas/progress-ledger-entry.schema.json), rendered into a single
self-contained, COMMITTED static HTML timeline: where the project started,
each reviewed round in between, and where it is now.

This is the reviewed-ledger half of Phase 23 (`StepIntoCDSi-Specification-
Reference-Module-Plan.md`), not the second, simplified story view for a
non-technical audience that phase also describes - that stays deferred
until it's actually wanted, per the same "committed snapshot, not a live
system" philosophy fits_dashboard.py and step_test_dashboard.py already
follow. An entry here is a small, structured summary of a round already
fully written up elsewhere (docs/NN-*.md, a finding, or both, via
details_doc) - it exists so a dashboard has numbers to render without
re-parsing prose, not as a replacement for that prose.

No network access, no LLM - reads only the local entries/ directory
already on disk.
"""

import datetime as dt
import html
import json
from pathlib import Path
from typing import Optional

import jsonschema
import yaml

from . import paths


class ProgressLedgerError(Exception):
    pass


def _load_schema() -> dict:
    return json.loads(
        (paths.schemas_dir() / "progress-ledger-entry.schema.json").read_text(encoding="utf-8"))


def default_output_path() -> Path:
    return paths.progress_ledger_dashboard_path()


def list_entry_ids() -> list[str]:
    d = paths.progress_ledger_entries_dir()
    if not d.exists():
        return []
    return sorted(p.stem for p in d.glob("*.yaml"))


def load_entry(entry_id: str) -> dict:
    path = paths.progress_ledger_entry_path(entry_id)
    if not path.exists():
        raise ProgressLedgerError(f"No progress ledger entry {entry_id!r} under {paths.progress_ledger_entries_dir()}")
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def load_all_entries() -> list[dict]:
    """Every entry, oldest first - the order a timeline should render in."""
    entries = [load_entry(entry_id) for entry_id in list_entry_ids()]
    entries.sort(key=lambda e: (e.get("date", ""), e.get("id", "")))
    return entries


def validate_entries() -> list[str]:
    """Validates every entry against the schema plus the one cross-file
    rule the schema itself can't express (filename must match id). Returns
    a list of problem descriptions, empty if every entry is clean."""
    schema = _load_schema()
    problems: list[str] = []
    for entry_id in list_entry_ids():
        try:
            entry = load_entry(entry_id)
        except (yaml.YAMLError, ProgressLedgerError) as e:
            problems.append(f"{entry_id}: failed to load - {e}")
            continue
        try:
            jsonschema.validate(entry, schema)
        except jsonschema.ValidationError as e:
            problems.append(f"{entry_id}: {e.message} (at {'/'.join(str(p) for p in e.absolute_path) or '<root>'})")
            continue
        if entry.get("id") != entry_id:
            problems.append(f"{entry_id}: id field is {entry.get('id')!r}, must match the filename")
    return problems


def render_ledger_table() -> str:
    entries = load_all_entries()
    if not entries:
        return "No progress ledger entries yet - add one under cdsi-reference/progress-ledger/entries/."
    lines: list[str] = []
    header = f"{'Date':<12} {'Phase':<7} {'Title':<50} {'Status'}"
    lines.append(header)
    lines.append("-" * len(header))
    for entry in entries:
        lines.append(
            f"{entry.get('date', ''):<12} {entry.get('phase', ''):<7} "
            f"{entry.get('title', '')[:50]:<50} {entry.get('status', '')}"
        )
    return "\n".join(lines)


def _esc(value) -> str:
    return html.escape(str(value)) if value is not None else ""


def _pct(numerator: float, denominator: float) -> float:
    return round(100 * numerator / denominator, 1) if denominator else 0.0


def _fmt_seconds(seconds: Optional[float]) -> str:
    if seconds is None:
        return "-"
    if seconds >= 60:
        return f"{seconds:.1f}s ({seconds / 60:.1f}m)"
    return f"{seconds:.1f}s"


def _fits_pass_pct(fits: dict) -> float:
    return _pct(fits.get("passed", 0), fits.get("total_cases", 0))


def _snapshot_rows(snapshot: dict) -> str:
    engine = snapshot.get("engine_tests", {})
    fits = snapshot.get("fits", {})
    return (
        f"<tr><td>Commit</td><td><code>{_esc(snapshot.get('commit'))}</code></td></tr>"
        f"<tr><td>Engine tests</td><td>{_esc(engine.get('total'))} total, "
        f"{_esc(engine.get('failures'))} failures, {_esc(engine.get('errors'))} errors</td></tr>"
        f"<tr><td>FITS cases</td><td>{_esc(fits.get('total_cases'))} total, "
        f"{_esc(fits.get('passed'))} passed, {_esc(fits.get('failed_assertions'))} failed-assertion, "
        f"{_esc(fits.get('execution_errors'))} execution-error ({_fits_pass_pct(fits)}%)</td></tr>"
        f"<tr><td>FITS runtime</td><td>{_fmt_seconds(snapshot.get('fits_runtime_seconds'))}</td></tr>"
    )


def _status_badge_class(status: str) -> str:
    return {"reviewed": "green", "pending_review": "amber"}.get(status, "gray")


def render_dashboard() -> str:
    entries = load_all_entries()
    generated_at = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    if not entries:
        headline_cards = '<p class="muted">No reviewed rounds recorded yet.</p>'
        entry_sections = '<p class="muted">Add an entry under cdsi-reference/progress-ledger/entries/ to start the timeline.</p>'
    else:
        started = entries[0]["before"]
        now = entries[-1]["after"]
        started_runtime = started.get("fits_runtime_seconds")
        now_runtime = now.get("fits_runtime_seconds")
        runtime_reduction = None
        if started_runtime and now_runtime and started_runtime > 0:
            runtime_reduction = round(100 * (1 - now_runtime / started_runtime), 1)

        headline_cards = f"""
  <div class="card">
    <div class="value">{len(entries)}</div>
    <div class="label">Reviewed round{"s" if len(entries) != 1 else ""}</div>
  </div>
  <div class="card pass">
    <div class="value">{_fits_pass_pct(now.get("fits", {}))}%</div>
    <div class="label">FITS pass rate now</div>
  </div>
  <div class="card">
    <div class="value">{_fmt_seconds(now_runtime)}</div>
    <div class="label">FITS runtime now</div>
  </div>
  <div class="card">
    <div class="value">{f"{runtime_reduction}%" if runtime_reduction is not None else "-"}</div>
    <div class="label">Runtime reduction since start</div>
  </div>"""

        entry_sections = []
        for entry in reversed(entries):  # newest first for reading
            status = entry.get("status", "")
            details_doc = entry.get("details_doc")
            details_link = (
                f'<p><a href="../../{_esc(details_doc)}">Full record: {_esc(details_doc)}</a></p>'
                if details_doc else ""
            )
            finding_ids = entry.get("finding_ids") or []
            findings_line = f'<p class="muted">Findings: {_esc(", ".join(finding_ids))}</p>' if finding_ids else ""
            entry_sections.append(f"""
<section>
  <h2>{_esc(entry.get("date"))} - Phase {_esc(entry.get("phase"))} - {_esc(entry.get("title"))}
    <span class="badge badge-{_status_badge_class(status)}">{_esc(status)}</span>
  </h2>
  <p>{_esc(entry.get("summary"))}</p>
  <div class="before-after">
    <table>
      <thead><tr><th colspan="2">Before</th></tr></thead>
      <tbody>{_snapshot_rows(entry.get("before", {}))}</tbody>
    </table>
    <table>
      <thead><tr><th colspan="2">After</th></tr></thead>
      <tbody>{_snapshot_rows(entry.get("after", {}))}</tbody>
    </table>
  </div>
  {details_link}
  {findings_line}
</section>""")
        entry_sections = "".join(entry_sections)

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Phase B Progress Ledger</title>
<style>
  :root {{
    color-scheme: light dark;
    --bg: #f7f7f8; --fg: #1a1a1a; --card-bg: #ffffff; --border: #e0e0e0;
    --muted: #6b6b6b; --gray: #6b7280; --blue: #2563eb; --amber: #b45309;
    --green: #15803d; --red: #b91c1c;
  }}
  @media (prefers-color-scheme: dark) {{
    :root {{ --bg: #16171a; --fg: #e6e6e6; --card-bg: #1f2023; --border: #33343a; --muted: #9a9a9a; }}
  }}
  * {{ box-sizing: border-box; }}
  body {{ margin: 0; padding: 24px; background: var(--bg); color: var(--fg);
          font-family: -apple-system, Segoe UI, Roboto, Arial, sans-serif; font-size: 14px; }}
  h1 {{ font-size: 20px; margin: 0 0 4px; }}
  .subtitle {{ color: var(--muted); margin: 0 0 20px; font-size: 13px; }}
  .cards {{ display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }}
  .card {{ background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px;
           padding: 14px 18px; min-width: 140px; flex: 1; }}
  .card .value {{ font-size: 26px; font-weight: 600; }}
  .card .label {{ color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }}
  .card.pass .value {{ color: var(--green); }}
  section {{ background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px;
             padding: 16px 18px; margin-bottom: 20px; }}
  section h2 {{ font-size: 15px; margin: 0 0 10px; }}
  .before-after {{ display: flex; gap: 16px; flex-wrap: wrap; }}
  .before-after table {{ flex: 1; min-width: 260px; border-collapse: collapse; }}
  table {{ width: 100%; }}
  th, td {{ text-align: left; padding: 6px 10px; border-bottom: 1px solid var(--border); vertical-align: top; }}
  th {{ font-size: 11px; text-transform: uppercase; letter-spacing: .03em; color: var(--muted); }}
  .muted {{ color: var(--muted); }}
  .badge {{ display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; font-weight: 600;
            color: #fff; white-space: nowrap; vertical-align: middle; }}
  .badge-gray {{ background: var(--gray); }}
  .badge-green {{ background: var(--green); }}
  .badge-amber {{ background: var(--amber); }}
  code {{ background: var(--border); padding: 1px 5px; border-radius: 4px; }}
  a {{ color: var(--blue); }}
</style>
</head>
<body>

<h1>Phase B Progress Ledger</h1>
<p class="subtitle">
  Reviewed before/after record of each Phase B round - where the project started, each change, where it is now.
  Every number here traces back to a reviewed entry under
  <code>cdsi-reference/progress-ledger/entries/</code>; nothing is measured live.<br>
  Dashboard generated {_esc(generated_at)} - regenerate with
  <code>python -m cdsi_reference_tools progress-ledger dashboard</code> after adding or updating an entry, then
  commit this file.
</p>

<div class="cards">
{headline_cards}
</div>

{entry_sections}

</body>
</html>
"""


def write_dashboard(out: Optional[Path] = None) -> Path:
    out_path = out or default_output_path()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(render_dashboard(), encoding="utf-8")
    return out_path
