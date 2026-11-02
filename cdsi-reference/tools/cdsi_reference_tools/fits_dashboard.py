"""Renders the most recent FITS diagnostic bundle (Phase 17) into a single
self-contained, COMMITTED static HTML snapshot - a shareable view of FITS
conformance progress, in two views behind one switch:

- Evaluation (shown first): how well the engine classifies each
  administered dose as valid / not valid / extraneous against FITS'
  expected evaluation. Scored per DOSE, not per case - the measurement use
  (spotting records with too many invalid doses) is a dose count, and one
  six-dose case must not weigh the same as a one-dose case. Shows a
  confusion matrix, the two error rates that matter for that use (doses
  wrongly called not valid, and not-valid doses missed), a breakdown by
  vaccine group, the per-dose differences, and a case-level cross-tab of
  evaluation vs. forecast correctness.
- Forecast: the original case-level pass/fail view - totals, by vaccine
  group, and the field-level difference for every non-passing case.

The two are never added into one pass count: they're different units.

This is a snapshot of one run, not a trend chart or a promoted historical
archive - the reference-module plan deliberately defers that kind of
machinery until it's actually wanted. Regenerating and committing this file
over time is the progress record for now; each commit's diff shows what
changed. No network access, no LLM - reads only the local diagnostic bundle
already on disk.
"""

import datetime as dt
import html
import json
from pathlib import Path
from typing import Optional

from . import paths

# Statuses that count as "the engine says this dose is invalid" for the
# error rates below. EXTRANEOUS is deliberately NOT in this set - CDSi
# treats an extra dose as not needed rather than invalid - and stays its
# own category in the confusion matrix so it's never silently folded in.
INVALID_STATUSES = ("NOT_VALID", "SUB_STANDARD")
EXPECTED_ORDER = ["VALID", "NOT_VALID", "SUB_STANDARD", "EXTRANEOUS"]
ACTUAL_ORDER = ["VALID", "NOT_VALID", "SUB_STANDARD", "EXTRANEOUS", "NOT_EVALUATED", "UNMAPPED"]
STATUS_LABELS = {
    "VALID": "Valid",
    "NOT_VALID": "Not valid",
    "SUB_STANDARD": "Sub-standard",
    "EXTRANEOUS": "Extraneous",
    "NOT_EVALUATED": "Not evaluated",
    "UNMAPPED": "Unmapped",
}


# NIST FITS EvaluationReason letter codes (fixtures keep the raw code, as
# FITS sends it) - wording shortened from the enum's own descriptions in
# cds-base-domain 1.0.1.
FITS_REASONS = {
    "A": "given after lot expiration",
    "B": "ineffective or sub-potent dose",
    "C": "too young",
    "D": "too soon after previous dose",
    "E": "too close to another vaccine (e.g. live virus conflict)",
    "F": "less than the recommended amount",
    "G": "not a preferable or allowable vaccine for this series",
    "H": "too old",
    "I": "schedule already complete",
    "J": "inadvertent vaccine",
}


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


def _actual_key(dose: dict) -> str:
    return dose.get("actual") or "UNMAPPED"


def evaluation_metrics(cases: list) -> Optional[dict]:
    """Dose-level evaluation numbers for a run, or None if the bundle predates
    evaluation comparison (no `evaluations` recorded on any case). Shared
    with dashboard_index so the landing page can't disagree with this page."""
    if not any("evaluations" in c for c in cases):
        return None
    doses = [d for c in cases for d in c.get("evaluations") or []]
    matched = sum(1 for d in doses if d.get("match"))
    expected_valid = [d for d in doses if d.get("expected") == "VALID"]
    expected_invalid = [d for d in doses if d.get("expected") in INVALID_STATUSES]
    false_invalid = sum(1 for d in expected_valid if _actual_key(d) in INVALID_STATUSES)
    caught = sum(1 for d in expected_invalid if _actual_key(d) in INVALID_STATUSES)
    eval_cases = [c for c in cases if c.get("evaluationStatus") in ("PASS", "FAIL")]
    eval_pass = sum(1 for c in eval_cases if c["evaluationStatus"] == "PASS")
    return {
        "doses": len(doses),
        "matched": matched,
        "match_pct": _pct(matched, len(doses)),
        "expected_valid": len(expected_valid),
        "false_invalid": false_invalid,
        "false_invalid_pct": _pct(false_invalid, len(expected_valid)),
        "expected_invalid": len(expected_invalid),
        "caught_invalid": caught,
        "missed_invalid": len(expected_invalid) - caught,
        "missed_invalid_pct": _pct(len(expected_invalid) - caught, len(expected_invalid)),
        "cases": len(eval_cases),
        "cases_pass": eval_pass,
        "cases_pass_pct": _pct(eval_pass, len(eval_cases)),
    }


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


def _render_forecast(summary: dict, cases: list) -> str:
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
            f'<td class="num">{g["total"]}</td>'
            f'<td class="num count-pass">{group_pass}</td>'
            f'<td class="num count-fail">{counts.get("FAIL", 0)}</td>'
            f'<td class="num count-error">{counts.get("ERROR", 0)}</td>'
            f'<td class="num count-skip">{counts.get("SKIPPED", 0) + counts.get("ABORTED", 0)}</td>'
            f'<td class="num">{_pct(group_pass, g["total"])}%</td>'
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
  <div class="table-wrap"><table>
    <thead><tr><th>Case</th><th>Status</th><th>Difference</th></tr></thead>
    <tbody>{"".join(case_rows)}</tbody>
  </table></div>
</details>""")

    return f"""
<p class="lede">Scored per <strong>case</strong>: a case passes when every expected vaccine-group forecast
  (status, earliest date, recommended date) matches.</p>
<div class="cards">
  <div class="card"><div class="value">{total}</div><div class="label">Executed cases</div></div>
  <div class="card pass"><div class="value">{passed}</div><div class="label">Passed</div></div>
  <div class="card fail"><div class="value">{failed}</div><div class="label">Failed</div></div>
  <div class="card error"><div class="value">{errors}</div><div class="label">Errors</div></div>
  <div class="card"><div class="value">{skipped}</div><div class="label">Skipped</div></div>
  <div class="card">
    <div class="value">{pass_pct}%</div><div class="label">Pass rate</div>
    <div class="bar"><div style="width:{pass_pct}%"></div></div>
  </div>
</div>

<section>
  <h2>By vaccine group</h2>
  <div class="table-wrap"><table>
    <thead>
      <tr><th>Group</th><th class="num">Total</th><th class="num">Passed</th><th class="num">Failed</th>
          <th class="num">Errors</th><th class="num">Skipped</th><th class="num">Pass rate</th></tr>
    </thead>
    <tbody>{"".join(group_rows)}</tbody>
  </table></div>
</section>

<section>
  <h2>Non-passing cases by group</h2>
  <input class="filter" data-scope="forecast" type="text" placeholder="Filter by case id or text...">
  <div data-filter-scope="forecast">
  {"".join(detail_sections) if detail_sections else '<p class="muted">Every case passed - nothing to show.</p>'}
  </div>
</section>
"""


def _render_evaluation(cases: list) -> str:
    m = evaluation_metrics(cases)
    if m is None:
        return """
<section><p class="muted">This run bundle has no dose-evaluation data - it predates evaluation comparison.
  Re-run <code>mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest</code> and regenerate.</p></section>"""

    doses = [(c, d) for c in cases for d in c.get("evaluations") or []]

    # Confusion matrix: expected rows x actual columns. Only show the
    # columns/rows that are either core statuses or actually occur.
    confusion: dict[str, dict[str, int]] = {}
    for _, d in doses:
        row = confusion.setdefault(d.get("expected"), {})
        row[_actual_key(d)] = row.get(_actual_key(d), 0) + 1
    present_actual = {a for row in confusion.values() for a in row}
    actual_cols = [a for a in ACTUAL_ORDER if a in ("VALID", "NOT_VALID", "EXTRANEOUS") or a in present_actual]
    actual_cols += sorted(present_actual - set(actual_cols))
    expected_rows = [e for e in EXPECTED_ORDER if e in confusion or e in ("VALID", "NOT_VALID", "EXTRANEOUS")]
    expected_rows += sorted(set(confusion) - set(expected_rows))
    max_off = max([n for e, row in confusion.items() for a, n in row.items() if a != e] or [1])

    matrix_rows = []
    for e in expected_rows:
        row = confusion.get(e, {})
        row_total = sum(row.values())
        cells = []
        for a in actual_cols:
            n = row.get(a, 0)
            label = f"Expected {STATUS_LABELS.get(e, e)}, engine said {STATUS_LABELS.get(a, a)}: {n} dose(s)"
            if row_total:
                label += f" ({_pct(n, row_total)}% of this row)"
            if a == e:
                cls = "cell-diag"
                style = ""
            elif n:
                cls = "cell-off"
                # Opacity scales with count so the worst off-diagonal cells
                # stand out; the count is always printed, never color alone.
                style = f' style="--w:{0.18 + 0.62 * n / max_off:.2f}"'
            else:
                cls = "cell-zero"
                style = ""
            share = f'<span class="share">{_pct(n, row_total)}%</span>' if n and row_total else ""
            cells.append(f'<td class="num {cls}"{style} title="{_esc(label)}">{n}{share}</td>')
        matrix_rows.append(
            f'<tr><th scope="row">{_esc(STATUS_LABELS.get(e, e))}</th>{"".join(cells)}'
            f'<td class="num muted">{row_total}</td></tr>')
    matrix_head = "".join(f'<th class="num">{_esc(STATUS_LABELS.get(a, a))}</th>' for a in actual_cols)

    # By vaccine group (the FITS fixture group).
    by_group: dict[str, dict] = {}
    for c, d in doses:
        g = by_group.setdefault(c["group"], {"doses": 0, "matched": 0, "ev": 0, "fi": 0, "ei": 0, "mi": 0, "cases": set()})
        g["doses"] += 1
        g["matched"] += 1 if d.get("match") else 0
        if d.get("expected") == "VALID":
            g["ev"] += 1
            g["fi"] += 1 if _actual_key(d) in INVALID_STATUSES else 0
        if d.get("expected") in INVALID_STATUSES:
            g["ei"] += 1
            g["mi"] += 0 if _actual_key(d) in INVALID_STATUSES else 1
        if not d.get("match"):
            g["cases"].add(c["caseId"])
    group_rows = []
    for name in sorted(by_group):
        g = by_group[name]
        fi = f'{g["fi"]} <span class="muted">/ {g["ev"]}</span>'
        mi = f'{g["mi"]} <span class="muted">/ {g["ei"]}</span>' if g["ei"] else '<span class="muted">-</span>'
        group_rows.append(
            "<tr>"
            f"<td>{_esc(name)}</td>"
            f'<td class="num">{g["doses"]}</td>'
            f'<td class="num">{_pct(g["matched"], g["doses"])}%</td>'
            f'<td class="num{" count-fail" if g["fi"] else ""}">{fi}</td>'
            f'<td class="num{" count-fail" if g["mi"] else ""}">{mi}</td>'
            f'<td class="num{" count-fail" if g["cases"] else ""}">{len(g["cases"])}</td>'
            "</tr>")

    # Case-level cross-tab: evaluation correctness x forecast correctness.
    quad = {("PASS", "PASS"): 0, ("PASS", "FAIL"): 0, ("FAIL", "PASS"): 0, ("FAIL", "FAIL"): 0}
    for c in cases:
        ev = c.get("evaluationStatus")
        fc = "PASS" if c["status"] == "PASS" else "FAIL"
        if ev in ("PASS", "FAIL") and c["status"] != "ERROR":
            quad[(ev, fc)] += 1

    def quad_cell(ev, fc, note):
        n = quad[(ev, fc)]
        attrs = f' data-quad="eval-{ev.lower()} forecast-{fc.lower()}"' if ev == "FAIL" and n else ""
        cls = "quad-ok" if (ev, fc) == ("PASS", "PASS") else ("quad-bad" if n else "quad-zero")
        click = ' role="button" tabindex="0"' if attrs else ""
        return f'<td class="{cls}"{attrs}{click}><div class="quad-n">{n}</div><div class="quad-note">{note}</div></td>'

    # Mismatched doses, grouped by vaccine group, collapsed.
    mismatch_by_group: dict[str, list] = {}
    for c, d in doses:
        if not d.get("match"):
            mismatch_by_group.setdefault(c["group"], []).append((c, d))
    detail_sections = []
    for name in sorted(mismatch_by_group):
        rows = []
        for c, d in sorted(mismatch_by_group[name], key=lambda x: (x[0]["caseId"], x[1].get("dose", 0))):
            per = ", ".join(f"{_esc(k)}: {_esc(STATUS_LABELS.get(v, v))}" for k, v in (d.get("perAntigen") or {}).items())
            if d.get("problem"):
                per = f'<span class="diff-error">{_esc(d["problem"])}</span>'
            fc = "PASS" if c["status"] == "PASS" else c["status"]
            quad_tag = f'eval-fail forecast-{"pass" if c["status"] == "PASS" else "fail"}'
            code = d.get("expectedReason")
            reason = (f'<div class="muted small">{_esc(FITS_REASONS.get(code, "reason " + str(code)))}</div>'
                      if code else "")
            disagree = ' <span class="tag">antigens disagree</span>' if d.get("antigenDisagreement") else ""
            rows.append(
                f'<tr data-tags="{quad_tag}">'
                f'<td class="case-id">{_esc(c["caseId"])}<div class="case-title">{_esc(c.get("title") or "")}</div></td>'
                f'<td class="num">{_esc(d.get("dose"))}</td>'
                f'<td class="nowrap">{_esc(d.get("date"))}</td>'
                f'<td>cvx {_esc(d.get("cvx"))}<div class="muted small">vs {_esc(d.get("targetName") or d.get("targetCvx"))}</div></td>'
                f'<td><strong>{_esc(STATUS_LABELS.get(d.get("expected"), d.get("expected")))}</strong>{reason}</td>'
                f'<td><strong>{_esc(STATUS_LABELS.get(_actual_key(d), _actual_key(d)))}</strong></td>'
                f'<td class="small">{per}{disagree}</td>'
                f'<td><span class="badge badge-{_status_badge_class(fc)}">{_esc(fc)}</span></td>'
                "</tr>")
        n_cases = len({c["caseId"] for c, _ in mismatch_by_group[name]})
        detail_sections.append(f"""
<details class="group-detail">
  <summary>{_esc(name)} - {len(mismatch_by_group[name])} mismatched dose(s) in {n_cases} case(s)</summary>
  <div class="table-wrap"><table>
    <thead><tr><th>Case</th><th class="num">Dose</th><th>Given</th><th>Vaccine</th><th>FITS expected</th>
      <th>Engine</th><th>Engine, per antigen</th><th>Forecast</th></tr></thead>
    <tbody>{"".join(rows)}</tbody>
  </table></div>
</details>""")

    return f"""
<p class="lede">Scored per <strong>dose</strong>: each administered dose's FITS expected evaluation (valid, not valid,
  extraneous) against the engine's. This is the view for the invalid-dose measurement - a dose wrongly called
  <em>not valid</em> inflates a record's invalid count; a missed one hides it.</p>
<div class="cards">
  <div class="card">
    <div class="value">{m['match_pct']}%</div><div class="label">Doses matching FITS</div>
    <div class="sub">{m['matched']:,} of {m['doses']:,} doses</div>
    <div class="bar"><div style="width:{m['match_pct']}%"></div></div>
  </div>
  <div class="card fail">
    <div class="value">{m['false_invalid_pct']}%</div><div class="label">Valid doses called not valid</div>
    <div class="sub">{m['false_invalid']} of {m['expected_valid']:,} - inflates invalid counts</div>
  </div>
  <div class="card fail">
    <div class="value">{m['missed_invalid_pct']}%</div><div class="label">Not-valid doses missed</div>
    <div class="sub">{m['missed_invalid']} of {m['expected_invalid']} - hides invalid doses</div>
  </div>
  <div class="card">
    <div class="value">{m['cases_pass_pct']}%</div><div class="label">Cases with every dose right</div>
    <div class="sub">{m['cases_pass']:,} of {m['cases']:,} cases with doses</div>
  </div>
</div>

<section>
  <h2>Expected vs. engine, per dose</h2>
  <p class="note">Rows are FITS' expected evaluation; columns are the engine's. The diagonal is agreement; shaded
    cells are disagreements (darker = more doses). Percentages are of the row. Hover a cell for its meaning.</p>
  <div class="table-wrap"><table class="matrix">
    <thead><tr><th>FITS expected &darr; / Engine &rarr;</th>{matrix_head}<th class="num">Total</th></tr></thead>
    <tbody>{"".join(matrix_rows)}</tbody>
  </table></div>
  <details class="method">
    <summary>How a dose is scored</summary>
    <ul>
      <li>FITS judges each dose against one vaccine group (its <code>relatedTo</code> vaccine). CDSi evaluates each
        dose per antigen. The engine's verdict for the dose is rolled up over the antigens in that group that the
        administered vaccine carries, with precedence <em>not valid &gt; sub-standard &gt; valid &gt; extraneous</em>:
        any antigen calling it not valid makes it not valid; a dose that counted for at least one antigen is valid.
        The Logic Specification defines no group-level dose verdict, so this rollup is a test-harness choice - doses
        where the antigens disagree are tagged below.</li>
      <li><em>Not evaluated</em>: the engine never evaluated the dose for any antigen in the group (for example, the
        vaccine isn't associated with the antigen at the patient's age). It counts as a miss, not as invalid.</li>
      <li><em>Extraneous</em> is kept separate and not counted as invalid in the rates above.</li>
      <li>FITS deliberately over-samples invalid doses, so the <em>rates</em> transfer to production data better
        than raw counts or the share of invalid doses do.</li>
    </ul>
  </details>
</section>

<section>
  <h2>Evaluation vs. forecast, per case</h2>
  <p class="note">Does a correct forecast mean the doses were evaluated correctly? Click a shaded cell to list those
    cases below.</p>
  <div class="table-wrap"><table class="quad">
    <thead><tr><th></th><th>Forecast right</th><th>Forecast wrong</th></tr></thead>
    <tbody>
      <tr><th scope="row">Every dose evaluated right</th>
        {quad_cell("PASS", "PASS", "both right")}
        {quad_cell("PASS", "FAIL", "forecast-logic issue")}</tr>
      <tr><th scope="row">Some dose evaluated wrong</th>
        {quad_cell("FAIL", "PASS", "hidden by the forecast view")}
        {quad_cell("FAIL", "FAIL", "may cause the forecast miss")}</tr>
    </tbody>
  </table></div>
</section>

<section>
  <h2>By vaccine group</h2>
  <div class="table-wrap"><table>
    <thead><tr><th>Group</th><th class="num">Doses</th><th class="num">Matching</th>
      <th class="num">Valid called not valid</th><th class="num">Not valid missed</th>
      <th class="num">Cases with a mismatch</th></tr></thead>
    <tbody>{"".join(group_rows)}</tbody>
  </table></div>
</section>

<section>
  <h2>Mismatched doses by group</h2>
  <div class="filter-row">
    <input class="filter" data-scope="evaluation" type="text" placeholder="Filter by case id, vaccine, status...">
    <button type="button" class="clear-quad" hidden>Show all mismatches</button>
  </div>
  <div data-filter-scope="evaluation">
  {"".join(detail_sections) if detail_sections else '<p class="muted">Every dose matched - nothing to show.</p>'}
  </div>
</section>
"""


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

    evaluation_html = _render_evaluation(cases)
    forecast_html = _render_forecast(summary, cases)

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>FITS Results Dashboard</title>
<style>
  :root {{
    color-scheme: light dark;
    --bg: #f7f7f8; --fg: #1a1a1a; --card-bg: #ffffff; --border: #e0e0e0;
    --muted: #6b6b6b; --gray: #6b7280; --blue: #2563eb; --amber: #b45309;
    --green: #15803d; --red: #b91c1c; --red-rgb: 185, 28, 28; --green-tint: rgba(21, 128, 61, .10);
  }}
  @media (prefers-color-scheme: dark) {{
    :root {{ --bg: #16171a; --fg: #e6e6e6; --card-bg: #1f2023; --border: #33343a; --muted: #9a9a9a;
             --green: #4ade80; --red: #f87171; --red-rgb: 248, 113, 113; --green-tint: rgba(74, 222, 128, .10); }}
  }}
  * {{ box-sizing: border-box; }}
  body {{ margin: 0; padding: 24px; background: var(--bg); color: var(--fg);
          font-family: -apple-system, Segoe UI, Roboto, Arial, sans-serif; font-size: 14px; }}
  h1 {{ font-size: 20px; margin: 0 0 4px; }}
  .subtitle {{ color: var(--muted); margin: 0 0 16px; font-size: 13px; }}
  .lede {{ margin: 0 0 16px; max-width: 75ch; }}
  .tabs {{ display: inline-flex; border: 1px solid var(--border); border-radius: 8px; overflow: hidden; margin-bottom: 18px; }}
  .tabs button {{ font: inherit; padding: 8px 18px; border: 0; background: var(--card-bg); color: var(--fg); cursor: pointer; }}
  .tabs button + button {{ border-left: 1px solid var(--border); }}
  .tabs button[aria-selected="true"] {{ background: var(--fg); color: var(--bg); font-weight: 600; }}
  .view[hidden] {{ display: none; }}
  .cards {{ display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }}
  .card {{ background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px;
           padding: 14px 18px; min-width: 160px; flex: 1; }}
  .card .value {{ font-size: 26px; font-weight: 600; font-variant-numeric: tabular-nums; }}
  .card .label {{ color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }}
  .card .sub {{ color: var(--muted); font-size: 12px; margin-top: 4px; }}
  .card.pass .value {{ color: var(--green); }}
  .card.fail .value {{ color: var(--red); }}
  .card.error .value {{ color: var(--red); }}
  .bar {{ height: 8px; border-radius: 4px; background: var(--border); margin-top: 10px; overflow: hidden; }}
  .bar > div {{ height: 100%; background: var(--green); }}
  section {{ background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px;
             padding: 16px 18px; margin-bottom: 20px; }}
  section h2 {{ font-size: 14px; margin: 0 0 10px; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); }}
  .note {{ color: var(--muted); font-size: 13px; margin: -4px 0 12px; max-width: 80ch; }}
  .table-wrap {{ overflow-x: auto; }}
  table {{ width: 100%; border-collapse: collapse; }}
  th, td {{ text-align: left; padding: 8px 10px; border-bottom: 1px solid var(--border); vertical-align: top; }}
  th {{ font-size: 11px; text-transform: uppercase; letter-spacing: .03em; color: var(--muted); }}
  .num {{ text-align: right; font-variant-numeric: tabular-nums; }}
  .nowrap {{ white-space: nowrap; }}
  .small {{ font-size: 12px; }}
  td.case-id {{ font-family: ui-monospace, Consolas, monospace; font-size: 12px; }}
  .case-title {{ font-family: -apple-system, Segoe UI, Roboto, Arial, sans-serif; color: var(--muted); font-size: 12px; margin-top: 2px; }}
  .muted {{ color: var(--muted); }}
  .badge {{ display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; font-weight: 600;
            color: #fff; white-space: nowrap; }}
  .badge-gray {{ background: var(--gray); }}
  .badge-green {{ background: #15803d; }}
  .badge-red {{ background: #b91c1c; }}
  .tag {{ display: inline-block; margin-left: 4px; padding: 1px 6px; border: 1px solid var(--border); border-radius: 4px;
          font-size: 11px; color: var(--muted); white-space: nowrap; }}
  .count-pass {{ color: var(--green); }}
  .count-fail, .count-error {{ color: var(--red); font-weight: 600; }}
  .count-skip {{ color: var(--muted); }}
  code {{ background: var(--border); padding: 1px 5px; border-radius: 4px; }}
  .diff-error {{ color: var(--red); }}
  table.matrix th[scope="row"] {{ white-space: nowrap; }}
  table.matrix td {{ min-width: 92px; }}
  table.matrix .share {{ display: block; font-size: 11px; color: var(--muted); }}
  .cell-diag {{ background: var(--green-tint); font-weight: 600; }}
  .cell-off {{ background: rgba(var(--red-rgb), calc(var(--w) * .45)); font-weight: 600; }}
  .cell-zero {{ color: var(--muted); }}
  table.quad td {{ text-align: center; padding: 14px 10px; width: 38%; }}
  .quad-n {{ font-size: 22px; font-weight: 600; font-variant-numeric: tabular-nums; }}
  .quad-note {{ font-size: 12px; color: var(--muted); }}
  .quad-ok {{ background: var(--green-tint); }}
  .quad-bad {{ background: rgba(var(--red-rgb), .12); }}
  .quad-bad[data-quad] {{ cursor: pointer; }}
  .quad-bad[data-quad]:hover, .quad-bad[data-quad]:focus {{ outline: 2px solid var(--red); outline-offset: -2px; }}
  .quad-bad.active {{ outline: 2px solid var(--fg); outline-offset: -2px; }}
  details.group-detail {{ border: 1px solid var(--border); border-radius: 6px; margin-bottom: 8px; padding: 8px 12px; }}
  details.group-detail summary, details.method summary {{ cursor: pointer; font-weight: 600; }}
  details.group-detail table {{ margin-top: 10px; }}
  details.method {{ margin-top: 12px; font-size: 13px; }}
  details.method ul {{ margin: 8px 0 0; padding-left: 20px; max-width: 85ch; }}
  details.method li {{ margin-bottom: 6px; }}
  .filter-row {{ display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 12px; }}
  input.filter {{ padding: 6px 10px; border: 1px solid var(--border); border-radius: 6px; background: var(--card-bg);
                 color: var(--fg); width: 320px; max-width: 100%; margin-bottom: 12px; }}
  .filter-row input.filter {{ margin-bottom: 0; }}
  button.clear-quad {{ font: inherit; font-size: 13px; padding: 6px 10px; border: 1px solid var(--border); border-radius: 6px;
                       background: var(--card-bg); color: var(--fg); cursor: pointer; }}
  @media (max-width: 640px) {{ body {{ padding: 16px; }} .card {{ min-width: 140px; }} }}
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

<div class="tabs" role="tablist">
  <button type="button" role="tab" id="tab-evaluation" aria-controls="view-evaluation" aria-selected="true">Evaluation</button>
  <button type="button" role="tab" id="tab-forecast" aria-controls="view-forecast" aria-selected="false">Forecast</button>
</div>

<div class="view" id="view-evaluation" role="tabpanel" aria-labelledby="tab-evaluation">
{evaluation_html}
</div>

<div class="view" id="view-forecast" role="tabpanel" aria-labelledby="tab-forecast" hidden>
{forecast_html}
</div>

<script>
  (function () {{
    // View switch; the choice lives in the URL hash so a shared link opens
    // the same view.
    var tabs = Array.prototype.slice.call(document.querySelectorAll('.tabs [role="tab"]'));
    function show(name) {{
      tabs.forEach(function (t) {{
        var on = t.id === 'tab-' + name;
        t.setAttribute('aria-selected', on ? 'true' : 'false');
        document.getElementById(t.getAttribute('aria-controls')).hidden = !on;
      }});
    }}
    tabs.forEach(function (t) {{
      t.addEventListener('click', function () {{
        var name = t.id.replace('tab-', '');
        show(name);
        history.replaceState(null, '', '#' + name);
      }});
    }});
    if (location.hash === '#forecast') show('forecast');

    // Text filter per view, plus an optional quadrant filter on the
    // evaluation view. Opens any group with a visible match.
    var quadFilter = null;
    function apply(scope) {{
      var input = document.querySelector('input.filter[data-scope="' + scope + '"]');
      var q = input ? input.value.toLowerCase() : '';
      var container = document.querySelector('[data-filter-scope="' + scope + '"]');
      if (!container) return;
      container.querySelectorAll('details.group-detail').forEach(function (detail) {{
        var any = false;
        detail.querySelectorAll('tbody tr').forEach(function (row) {{
          var ok = q === '' || row.textContent.toLowerCase().indexOf(q) !== -1;
          if (ok && scope === 'evaluation' && quadFilter) {{
            ok = (row.getAttribute('data-tags') || '') === quadFilter;
          }}
          row.style.display = ok ? '' : 'none';
          if (ok) any = true;
        }});
        detail.style.display = any ? '' : 'none';
        if ((q !== '' || quadFilter) && any) detail.open = true;
      }});
    }}
    document.querySelectorAll('input.filter').forEach(function (input) {{
      input.addEventListener('input', function () {{ apply(input.getAttribute('data-scope')); }});
    }});
    var clear = document.querySelector('button.clear-quad');
    function setQuad(cell) {{
      document.querySelectorAll('td[data-quad]').forEach(function (c) {{ c.classList.remove('active'); }});
      quadFilter = cell ? cell.getAttribute('data-quad') : null;
      if (cell) cell.classList.add('active');
      if (clear) clear.hidden = !quadFilter;
      apply('evaluation');
      if (cell) document.querySelector('[data-filter-scope="evaluation"]').scrollIntoView({{ behavior: 'smooth', block: 'start' }});
    }}
    document.querySelectorAll('td[data-quad]').forEach(function (cell) {{
      cell.addEventListener('click', function () {{ setQuad(quadFilter === cell.getAttribute('data-quad') ? null : cell); }});
      cell.addEventListener('keydown', function (e) {{ if (e.key === 'Enter' || e.key === ' ') {{ e.preventDefault(); cell.click(); }} }});
    }});
    if (clear) clear.addEventListener('click', function () {{ setQuad(null); }});
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
