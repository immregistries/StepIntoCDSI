"""Landing page linking the two Phase 17/21 dashboards, with their headline
numbers pulled live so the index itself never goes stale. Same committed,
regenerate-on-demand snapshot pattern as step-tests.html and
fits-results.html - not a live view, not a trend chart.
"""

import datetime as dt
import html
from pathlib import Path
from typing import Optional

from . import fits_dashboard, paths, step_test_status


def default_output_path() -> Path:
    return paths.dashboard_index_path()


def _esc(value) -> str:
    return html.escape(str(value)) if value is not None else ""


def _step_tests_summary() -> dict:
    status = step_test_status.load_status()
    units = status.get("units", {})
    version = status.get("spec_version") or "?"
    written = sum(1 for u in units.values() if u.get("test_status") == "tests_written")
    merged = sum(1 for u in units.values() if u.get("fix_status") == "merged")
    blocked = sum(1 for u in units.values() if u.get("fix_status") == "blocked")
    return {"version": version, "total": len(units), "written": written, "merged": merged, "blocked": blocked}


def _fits_summary() -> Optional[dict]:
    run_dir = fits_dashboard.latest_run_dir()
    if run_dir is None:
        return None
    data = fits_dashboard.load_run(run_dir)
    summary = data["summary"]
    run = data["run"]
    total = summary["executedCases"] or 1
    passed = summary["passedCases"]
    return {
        "reference_set": run.get("referenceSetId"),
        "total": total,
        "passed": passed,
        "pass_pct": round(100 * passed / total, 1),
    }


def render_index() -> str:
    generated_at = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    step = _step_tests_summary()
    fits = _fits_summary()

    if fits:
        fits_stat = f"""<div class="stat">{fits['pass_pct']}%</div>
    <div class="stat-label">{fits['passed']}/{fits['total']} FITS cases passing - reference set {_esc(fits['reference_set'])}</div>"""
    else:
        fits_stat = '<p class="muted">No FITS run bundle found yet.</p>'

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>StepIntoCDSi Progress Dashboards</title>
<style>
  :root {{
    color-scheme: light dark;
    --bg: #f7f7f8; --fg: #1a1a1a; --card-bg: #ffffff; --border: #e0e0e0;
    --muted: #6b6b6b; --blue: #2563eb; --green: #15803d;
  }}
  @media (prefers-color-scheme: dark) {{
    :root {{ --bg: #16171a; --fg: #e6e6e6; --card-bg: #1f2023; --border: #33343a; --muted: #9a9a9a; }}
  }}
  * {{ box-sizing: border-box; }}
  body {{ margin: 0; padding: 32px; background: var(--bg); color: var(--fg);
          font-family: -apple-system, Segoe UI, Roboto, Arial, sans-serif; font-size: 14px; }}
  h1 {{ font-size: 22px; margin: 0 0 6px; }}
  .subtitle {{ color: var(--muted); margin: 0 0 28px; font-size: 13px; }}
  .cards {{ display: flex; gap: 20px; flex-wrap: wrap; }}
  a.tile {{ display: block; background: var(--card-bg); border: 1px solid var(--border); border-radius: 10px;
            padding: 22px 26px; text-decoration: none; color: var(--fg); flex: 1; min-width: 280px; }}
  a.tile:hover {{ border-color: var(--blue); }}
  a.tile h2 {{ margin: 0 0 8px; font-size: 16px; }}
  a.tile p.desc {{ color: var(--muted); margin: 0 0 4px; font-size: 13px; }}
  .stat {{ font-size: 34px; font-weight: 700; margin-top: 14px; color: var(--green); }}
  .stat-label {{ color: var(--muted); font-size: 12px; }}
  .muted {{ color: var(--muted); }}
  code {{ background: var(--border); padding: 1px 5px; border-radius: 4px; }}
</style>
</head>
<body>

<h1>StepIntoCDSi Progress Dashboards</h1>
<p class="subtitle">Generated {_esc(generated_at)}. Regenerate with
  <code>python -m cdsi_reference_tools dashboards index</code> after refreshing either dashboard below, then commit.</p>

<div class="cards">
  <a class="tile" href="step-tests.html">
    <h2>Per-Step Spec-Conformance Coverage (Phase 21)</h2>
    <p class="desc">Every executable step class (Logic Specification v{_esc(step['version'])}), tested against its own
      specification section directly - independent of FITS.</p>
    <div class="stat">{step['written']}/{step['total']}</div>
    <div class="stat-label">units with tests written - {step['merged']} fixed and merged, {step['blocked']} blocked</div>
  </a>
  <a class="tile" href="fits-results.html">
    <h2>FITS Conformance (Phase 17)</h2>
    <p class="desc">The NIST FITS end-to-end conformance suite, broken down by vaccine group.</p>
    {fits_stat}
  </a>
</div>

</body>
</html>
"""


def write_index(out: Optional[Path] = None) -> Path:
    out_path = out or default_output_path()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(render_index(), encoding="utf-8")
    return out_path
