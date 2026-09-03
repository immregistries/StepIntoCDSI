"""Renders the most recent FITS diagnostic bundle (Phase 17) into a single
self-contained, COMMITTED static HTML snapshot - a shareable view of FITS
conformance progress: overall pass/fail/error totals, a breakdown by
vaccine group, and the actual field-level difference for every non-passing
case, grouped underneath it.

This is a snapshot of one run, not a trend chart or a promoted historical
archive - the reference-module plan deliberately defers that kind of
machinery until it's actually wanted (Phase 19's reviewed case-level
baseline would be the real input to a trend chart, and that isn't built
yet). Regenerating and committing this file over time is the progress
record for now; each commit's diff shows what changed. No network access,
no LLM - reads only the local diagnostic bundle already on disk.
"""

import datetime as dt
import html
import json
from pathlib import Path
from typing import Optional

from . import paths


def default_output_path() -> Path:
    return paths.fits_dashboard_path()


def latest_run_dir() -> Optional[Path]:
    root = paths.fits_runs_dir()
    if not root.exists():
        return None
    run_dirs = [d for d in root.iterdir() if d.is_dir() and (d / "summary.json").exists()]
    if not run_dirs:
        return None
    return sorted(run_dirs, key=lambda d: d.name)[-1]


def load_run(run_dir: Path) -> dict:
    run_info = json.loads((run_dir / "run.json").read_text(encoding="utf-8"))
    summary = json.loads((run_dir / "summary.json").read_text(encoding="utf-8"))
    cases = []
    with (run_dir / "results.jsonl").open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                cases.append(json.loads(line))
    return {"run": run_info, "summary": summary, "cases": cases}


def _esc(value) -> str:
    return html.escape(str(value)) if value is not None else ""


def _pct(numerator: int, denominator: int) -> float:
    return round(100 * numerator / denominator, 1) if denominator else 0.0


def _status_badge_class(status: str) -> str:
    return {"FAIL": "red", "ERROR": "red", "PASS": "green"}.get(status, "gray")


def _case_diff_line(case: dict) -> str:
    if case["status"] == "ERROR":
        return f'<span class="diff-error">{_esc(case.get("exception") or "error, no message recorded")}</span>'
    diffs = case.get("fieldDifferences") or []
    if not diffs:
        return '<span class="muted">no field-level difference recorded (see failure bundle)</span>'
    parts = []
    for d in diffs:
        parts.append(
            f'cvx {_esc(d.get("cvx"))} <strong>{_esc(d.get("field"))}</strong>: '
            f'expected <code>{_esc(d.get("expected"))}</code>, actual <code>{_esc(d.get("actual"))}</code>'
        )
    return "<br>".join(parts)


def render_dashboard() -> str:
    run_dir = latest_run_dir()
    if run_dir is None:
        raise FileNotFoundError(
            "No FITS run bundle found under cdsi-fits-tests/target/fits-runs/ - "
            "run `mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest` first.")
    data = load_run(run_dir)
    run = data["run"]
    summary = data["summary"]
    cases = data["cases"]
    generated_at = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    total = summary["executedCases"] or 1
    passed = summary["passedCases"]
    failed = summary["failedAssertions"]
    errors = summary["executionErrors"]
    skipped = summary["skippedCases"]
    pass_pct = _pct(passed, total)

    groups: dict[str, dict] = {}
    for case in cases:
        g = groups.setdefault(case["group"], {"total": 0, "counts": {}, "cases": []})
        g["total"] += 1
        g["counts"][case["status"]] = g["counts"].get(case["status"], 0) + 1
        if case["status"] != "PASS":
            g["cases"].append(case)

    group_rows = []
    detail_sections = []
    for name in sorted(groups):
        g = groups[name]
        counts = g["counts"]
        group_pass = counts.get("PASS", 0)
        group_rows.append(
            "<tr>"
            f"<td>{_esc(name)}</td>"
            f"<td>{g['total']}</td>"
            f'<td class="count-pass">{group_pass}</td>'
            f'<td class="count-fail">{counts.get("FAIL", 0)}</td>'
            f'<td class="count-error">{counts.get("ERROR", 0)}</td>'
            f'<td class="count-skip">{counts.get("SKIPPED", 0) + counts.get("ABORTED", 0)}</td>'
            f"<td>{_pct(group_pass, g['total'])}%</td>"
            "</tr>"
        )
        if g["cases"]:
            case_rows = []
            for case in sorted(g["cases"], key=lambda c: (c["status"], c["caseId"])):
                case_rows.append(
                    "<tr>"
                    f'<td class="case-id">{_esc(case["caseId"])}</td>'
                    f'<td><span class="badge badge-{_status_badge_class(case["status"])}">{_esc(case["status"])}</span></td>'
                    f"<td>{_case_diff_line(case)}</td>"
                    "</tr>"
                )
            detail_sections.append(f"""
<details class="group-detail">
  <summary>{_esc(name)} - {len(g['cases'])} non-passing of {g['total']}</summary>
  <table>
    <thead><tr><th>Case</th><th>Status</th><th>Difference</th></tr></thead>
    <tbody>{"".join(case_rows)}</tbody>
  </table>
</details>""")

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>FITS Results Dashboard</title>
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
  .card.fail .value {{ color: var(--red); }}
  .card.error .value {{ color: var(--red); }}
  .bar {{ height: 8px; border-radius: 4px; background: var(--border); margin-top: 10px; overflow: hidden; }}
  .bar > div {{ height: 100%; background: var(--green); }}
  section {{ background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px;
             padding: 16px 18px; margin-bottom: 20px; }}
  section h2 {{ font-size: 14px; margin: 0 0 10px; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); }}
  table {{ width: 100%; border-collapse: collapse; }}
  th, td {{ text-align: left; padding: 8px 10px; border-bottom: 1px solid var(--border); vertical-align: top; }}
  th {{ font-size: 11px; text-transform: uppercase; letter-spacing: .03em; color: var(--muted); }}
  td.case-id {{ font-family: ui-monospace, Consolas, monospace; font-size: 12px; white-space: nowrap; }}
  .muted {{ color: var(--muted); }}
  .badge {{ display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; font-weight: 600;
            color: #fff; white-space: nowrap; }}
  .badge-gray {{ background: var(--gray); }}
  .badge-green {{ background: var(--green); }}
  .badge-red {{ background: var(--red); }}
  .count-pass {{ color: var(--green); }}
  .count-fail, .count-error {{ color: var(--red); font-weight: 600; }}
  .count-skip {{ color: var(--muted); }}
  code {{ background: var(--border); padding: 1px 5px; border-radius: 4px; }}
  details.group-detail {{ border: 1px solid var(--border); border-radius: 6px; margin-bottom: 8px; padding: 8px 12px; }}
  details.group-detail summary {{ cursor: pointer; font-weight: 600; }}
  details.group-detail table {{ margin-top: 10px; }}
  input#case-filter {{ padding: 6px 10px; border: 1px solid var(--border); border-radius: 6px; background: var(--card-bg);
                        color: var(--fg); width: 300px; margin-bottom: 12px; }}
</style>
</head>
<body>

<h1>FITS Results Dashboard</h1>
<p class="subtitle">
  Reference set {_esc(run.get('referenceSetId'))} - Logic Specification v{_esc(run.get('logicSpecVersion'))},
  Supporting Data {_esc(run.get('supportingDataRelease'))} - run executed {_esc(run.get('startedAt'))}
  at commit {_esc(run.get('gitCommitAbbreviated'))}{' (dirty)' if run.get('gitDirty') else ''} on branch
  {_esc(run.get('gitBranch'))}.<br>
  Dashboard generated {_esc(generated_at)} - regenerate with
  <code>python -m cdsi_reference_tools fits-tests dashboard</code> after
  <code>mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest</code> for a fresh run, then commit this file
  to record the new baseline.
</p>

<div class="cards">
  <div class="card">
    <div class="value">{total}</div>
    <div class="label">Executed cases</div>
  </div>
  <div class="card pass">
    <div class="value">{passed}</div>
    <div class="label">Passed</div>
  </div>
  <div class="card fail">
    <div class="value">{failed}</div>
    <div class="label">Failed</div>
  </div>
  <div class="card error">
    <div class="value">{errors}</div>
    <div class="label">Errors</div>
  </div>
  <div class="card">
    <div class="value">{skipped}</div>
    <div class="label">Skipped</div>
  </div>
  <div class="card">
    <div class="value">{pass_pct}%</div>
    <div class="label">Pass rate</div>
    <div class="bar"><div style="width:{pass_pct}%"></div></div>
  </div>
</div>

<section>
  <h2>By vaccine group</h2>
  <table>
    <thead>
      <tr><th>Group</th><th>Total</th><th>Passed</th><th>Failed</th><th>Errors</th><th>Skipped</th><th>Pass rate</th></tr>
    </thead>
    <tbody>
      {"".join(group_rows)}
    </tbody>
  </table>
</section>

<section>
  <h2>Non-passing cases by group</h2>
  <input id="case-filter" type="text" placeholder="Filter by case id or text...">
  {"".join(detail_sections) if detail_sections else '<p class="muted">Every case passed - nothing to show.</p>'}
</section>

<script>
  // Client-side only: filters visible rows across every open/closed detail
  // table by substring match, and opens a group whose rows match so a
  // search actually surfaces the row, not just the collapsed summary.
  (function () {{
    var input = document.getElementById('case-filter');
    var details = Array.prototype.slice.call(document.querySelectorAll('details.group-detail'));
    input.addEventListener('input', function () {{
      var q = input.value.toLowerCase();
      details.forEach(function (detail) {{
        var rows = Array.prototype.slice.call(detail.querySelectorAll('tbody tr'));
        var anyVisible = false;
        rows.forEach(function (row) {{
          var match = q === '' || row.textContent.toLowerCase().indexOf(q) !== -1;
          row.style.display = match ? '' : 'none';
          if (match) anyVisible = true;
        }});
        detail.style.display = anyVisible ? '' : 'none';
        if (q !== '' && anyVisible) detail.open = true;
      }});
    }});
  }})();
</script>

</body>
</html>
"""


def write_dashboard(out: Optional[Path] = None) -> Path:
    out_path = out or default_output_path()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(render_dashboard(), encoding="utf-8")
    return out_path
