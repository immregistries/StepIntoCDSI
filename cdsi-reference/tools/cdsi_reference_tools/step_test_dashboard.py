"""Phase 21: renders step-tests/status.yaml plus live surefire results into
a single self-contained static HTML file - a local, open-in-a-browser view
of per-step spec-conformance test progress, regenerated on demand (`step-
tests dashboard`), not served or published anywhere. No network access, no
LLM, no external CSS/JS - everything needed to view it is in the one file.

Run `mvn -pl cdsi-engine test` before regenerating for fresh pass/fail
counts; like `step-tests status`, this never caches them - it reads
cdsi-engine's surefire reports at generation time.
"""

import datetime as dt
import html
from pathlib import Path

from . import paths, step_test_status

_TEST_STATUS_LABEL = {
    "not_started": "not started",
    "tests_written": "tests written",
}

_FIX_STATUS_LABEL = {
    "not_started": "not started",
    "in_progress": "in progress",
    "fixed_pending_review": "pending review",
    "merged": "merged",
    "blocked": "blocked",
}

_FIX_STATUS_CLASS = {
    "not_started": "gray",
    "in_progress": "blue",
    "fixed_pending_review": "amber",
    "merged": "green",
    "blocked": "red",
}

_TEST_STATUS_CLASS = {
    "not_started": "gray",
    "tests_written": "blue",
}


def default_output_path() -> Path:
    return paths.step_test_dashboard_path()


def _esc(value) -> str:
    return html.escape(str(value)) if value is not None else ""


def _badge(label: str, css_class: str) -> str:
    return f'<span class="badge badge-{css_class}">{_esc(label)}</span>'


def _counts_cell(counts: dict | None, test_status: str) -> str:
    if counts is None:
        return "<span class=\"muted\">-</span>" if test_status == "not_started" else "<span class=\"muted\">not run</span>"
    parts = []
    if counts["passed"]:
        parts.append(f'<span class="count count-pass">{counts["passed"]} passed</span>')
    if counts["failures"]:
        parts.append(f'<span class="count count-fail">{counts["failures"]} failed</span>')
    if counts["errors"]:
        parts.append(f'<span class="count count-error">{counts["errors"]} error</span>')
    if counts["skipped"]:
        parts.append(f'<span class="count count-skip">{counts["skipped"]} skipped</span>')
    return " ".join(parts) if parts else '<span class="muted">0 tests</span>'


def render_dashboard(version: str) -> str:
    status = step_test_status.load_status()
    units = status.get("units", {})
    known_units = step_test_status.mapping_units(version)
    generated_at = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    written = merged = blocked_count = 0
    rows_html = []
    blocked_html = []

    for uid in sorted(units, key=step_test_status.sort_key):
        entry = units[uid]
        title = known_units.get(uid, {}).get("title", "")
        test_status = entry.get("test_status", "not_started")
        fix_status = entry.get("fix_status", "not_started")
        test_class = entry.get("test_class")
        counts = step_test_status.read_surefire_counts(test_class) if test_class else None
        notes = entry.get("notes") or ""
        finding_ids = entry.get("finding_ids") or []

        if test_status == "tests_written":
            written += 1
        if fix_status == "merged":
            merged += 1
        if fix_status == "blocked":
            blocked_count += 1
            blocked_html.append(
                "<div class=\"blocked-item\">"
                f"<strong>{_esc(uid)}</strong> {_esc(title)} "
                f"{_badge(entry.get('blocked_category') or 'unknown', 'red')}"
                f"<div class=\"blocked-reason\">{_esc(entry.get('blocked_reason') or '')}</div>"
                + (f"<div class=\"blocked-findings\">findings: {_esc(', '.join(finding_ids))}</div>" if finding_ids else "")
                + "</div>"
            )

        findings_cell = _esc(", ".join(finding_ids)) if finding_ids else "<span class=\"muted\">-</span>"
        rows_html.append(
            "<tr>"
            f"<td class=\"unit-id\">{_esc(uid)}</td>"
            f"<td>{_esc(title)}</td>"
            f"<td>{_badge(_TEST_STATUS_LABEL.get(test_status, test_status), _TEST_STATUS_CLASS.get(test_status, 'gray'))}</td>"
            f"<td>{_counts_cell(counts, test_status)}</td>"
            f"<td>{_badge(_FIX_STATUS_LABEL.get(fix_status, fix_status), _FIX_STATUS_CLASS.get(fix_status, 'gray'))}</td>"
            f"<td>{findings_cell}</td>"
            f"<td class=\"notes\">{_esc(notes)}</td>"
            "</tr>"
        )

    total = len(units) or 1
    written_pct = round(100 * written / total)
    merged_pct = round(100 * merged / total)

    missing = sorted(set(known_units) - set(units), key=step_test_status.sort_key)
    missing_note = (
        f'<p class="muted">{len(missing)} unit(s) not yet synced from spec-to-code.yaml - run '
        f'<code>step-tests sync --version {_esc(version)}</code>.</p>'
        if missing else ""
    )

    blocked_section = (
        "".join(blocked_html)
        if blocked_html
        else '<p class="muted">None - nothing is currently blocked.</p>'
    )

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Step Test Dashboard - v{_esc(version)}</title>
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
           padding: 14px 18px; min-width: 180px; flex: 1; }}
  .card .value {{ font-size: 26px; font-weight: 600; }}
  .card .label {{ color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }}
  .bar {{ height: 6px; border-radius: 3px; background: var(--border); margin-top: 8px; overflow: hidden; }}
  .bar > div {{ height: 100%; background: var(--blue); }}
  .bar.merged > div {{ background: var(--green); }}
  section {{ background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px;
             padding: 16px 18px; margin-bottom: 20px; }}
  section h2 {{ font-size: 14px; margin: 0 0 10px; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); }}
  .blocked-item {{ padding: 8px 0; border-bottom: 1px solid var(--border); }}
  .blocked-item:last-child {{ border-bottom: none; }}
  .blocked-reason {{ color: var(--muted); margin-top: 4px; }}
  .blocked-findings {{ color: var(--muted); font-size: 12px; margin-top: 2px; }}
  table {{ width: 100%; border-collapse: collapse; }}
  th, td {{ text-align: left; padding: 8px 10px; border-bottom: 1px solid var(--border); vertical-align: top; }}
  th {{ font-size: 11px; text-transform: uppercase; letter-spacing: .03em; color: var(--muted); cursor: pointer; user-select: none; }}
  th:hover {{ color: var(--fg); }}
  td.unit-id {{ font-variant-numeric: tabular-nums; white-space: nowrap; }}
  td.notes {{ color: var(--muted); font-size: 13px; max-width: 320px; }}
  .muted {{ color: var(--muted); }}
  .badge {{ display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; font-weight: 600;
            color: #fff; white-space: nowrap; }}
  .badge-gray {{ background: var(--gray); }}
  .badge-blue {{ background: var(--blue); }}
  .badge-amber {{ background: var(--amber); }}
  .badge-green {{ background: var(--green); }}
  .badge-red {{ background: var(--red); }}
  .count {{ font-size: 12px; margin-right: 6px; white-space: nowrap; }}
  .count-pass {{ color: var(--green); }}
  .count-fail {{ color: var(--red); font-weight: 600; }}
  .count-error {{ color: var(--red); font-weight: 600; }}
  .count-skip {{ color: var(--muted); }}
  input#filter {{ padding: 6px 10px; border: 1px solid var(--border); border-radius: 6px; background: var(--card-bg);
                  color: var(--fg); width: 260px; margin-bottom: 12px; }}
  code {{ background: var(--border); padding: 1px 5px; border-radius: 4px; }}
</style>
</head>
<body>

<h1>Step Test Dashboard</h1>
<p class="subtitle">Logic Specification v{_esc(version)} - generated {_esc(generated_at)} - regenerate with
  <code>python -m cdsi_reference_tools step-tests dashboard --version {_esc(version)}</code>
  after <code>mvn -pl cdsi-engine test</code> for fresh counts.</p>

<div class="cards">
  <div class="card">
    <div class="value">{written}/{len(units)}</div>
    <div class="label">Units with tests written</div>
    <div class="bar"><div style="width:{written_pct}%"></div></div>
  </div>
  <div class="card">
    <div class="value">{merged}/{len(units)}</div>
    <div class="label">Units fixed and merged</div>
    <div class="bar merged"><div style="width:{merged_pct}%"></div></div>
  </div>
  <div class="card">
    <div class="value">{blocked_count}</div>
    <div class="label">Units blocked</div>
  </div>
</div>

<section>
  <h2>Needs your attention</h2>
  {blocked_section}
</section>

<section>
  <h2>All units</h2>
  {missing_note}
  <input id="filter" type="text" placeholder="Filter by unit, title, or status...">
  <table id="unit-table">
    <thead>
      <tr>
        <th data-sort="text">Unit</th>
        <th data-sort="text">Title</th>
        <th data-sort="text">Test status</th>
        <th data-sort="text">Results</th>
        <th data-sort="text">Fix status</th>
        <th data-sort="text">Findings</th>
        <th>Notes</th>
      </tr>
    </thead>
    <tbody>
      {"".join(rows_html)}
    </tbody>
  </table>
</section>

<script>
  // Client-side only, no external libraries: a text filter and click-to-sort
  // on a static table that never re-fetches anything - this page is a
  // snapshot, not a live view.
  (function () {{
    var filterInput = document.getElementById('filter');
    var table = document.getElementById('unit-table');
    var rows = Array.prototype.slice.call(table.tBodies[0].rows);

    filterInput.addEventListener('input', function () {{
      var q = filterInput.value.toLowerCase();
      rows.forEach(function (row) {{
        row.style.display = row.textContent.toLowerCase().indexOf(q) === -1 ? 'none' : '';
      }});
    }});

    var sortState = {{}};
    Array.prototype.forEach.call(table.tHead.rows[0].cells, function (th, colIndex) {{
      if (!th.dataset.sort) return;
      th.addEventListener('click', function () {{
        var asc = !sortState[colIndex];
        sortState = {{}};
        sortState[colIndex] = asc;
        var sorted = rows.slice().sort(function (a, b) {{
          var av = a.cells[colIndex].textContent.trim();
          var bv = b.cells[colIndex].textContent.trim();
          return asc ? av.localeCompare(bv) : bv.localeCompare(av);
        }});
        var tbody = table.tBodies[0];
        sorted.forEach(function (row) {{ tbody.appendChild(row); }});
        rows = sorted;
      }});
    }});
  }})();
</script>

</body>
</html>
"""


def write_dashboard(version: str, out: Path | None = None) -> Path:
    out_path = out or default_output_path()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(render_dashboard(version), encoding="utf-8")
    return out_path
