"""Dose-evaluation report for a FITS run bundle (stdlib only, Python 3.9+).

Reads target/fits-runs/<run-id>/ (the latest run by default) and prints:

- the headline evaluation numbers (doses matched, valid doses called not
  valid, not-valid doses missed) next to the campaign baseline,
- the dose-level changes against the previous run (changed-cases.json),
- every mismatched dose, or every dose of the cases you name, with the
  engine's per-antigen status and reason.

Run from the repository root after `mvn -pl cdsi-fits-tests test`:

    python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py
    python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --cases DTAP-2013-0058,POL-2024-0071
    python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --group HPV --all-doses

--cases matches on substrings of the case id (testPlanId-group-uid), so
"DTAP-2013-0058" is enough.
"""

import argparse
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
RUNS = REPO / "cdsi-fits-tests" / "target" / "fits-runs"

# Baseline for this campaign: run 2026-11-02, reference set acip-4.6-sd-4.65-fits-4f705ec9.
BASELINE = {
    "dosesCompared": 2556,
    "dosesMatched": 2505,
    "validCalledNotValid": 21,
    "validCalledExtraneous": 10,
    "notValidMissed": 20,
    "forecastPassed": 1107,
}


def latest_run() -> Path:
    if not RUNS.is_dir():
        sys.exit(f"No runs under {RUNS} - run `mvn -pl cdsi-fits-tests test` first.")
    runs = [p for p in RUNS.iterdir() if p.is_dir() and (p / "summary.json").exists()]
    if not runs:
        sys.exit(f"No complete runs under {RUNS}.")
    return max(runs, key=lambda p: p.stat().st_mtime)


def headline(summary: dict) -> dict:
    ev = summary["evaluation"]
    conf = ev["confusion"]
    valid = conf.get("VALID", {})
    not_valid = conf.get("NOT_VALID", {})
    return {
        "dosesCompared": ev["dosesCompared"],
        "dosesMatched": ev["dosesMatched"],
        "validCalledNotValid": valid.get("NOT_VALID", 0),
        "validCalledExtraneous": valid.get("EXTRANEOUS", 0),
        "notValidMissed": sum(n for actual, n in not_valid.items() if actual != "NOT_VALID"),
        "forecastPassed": summary["passedCases"],
    }


def fmt_dose(case: dict, d: dict) -> str:
    reasons = d.get("perAntigenReason") or {}
    per = ", ".join(
        f"{a}={s}" + (f"({reasons[a]})" if a in reasons else "")
        for a, s in (d.get("perAntigen") or {}).items())
    mark = "ok " if d.get("match") else "XX "
    expected = d.get("expected") + (f"/{d['expectedReason']}" if d.get("expectedReason") else "")
    line = (f"  {mark}{case['caseId']} dose {d['dose']} {d['date']} cvx {d['cvx']} vs {d['targetCvx']}: "
            f"expected {expected}, engine {d.get('actual')} [{per}]")
    if d.get("problem"):
        line += f" PROBLEM: {d['problem']}"
    return line


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--run", help="run directory (default: latest under cdsi-fits-tests/target/fits-runs)")
    ap.add_argument("--cases", help="comma-separated case id substrings to show")
    ap.add_argument("--group", help="vaccine group name to show (e.g. HPV, DTAP, COVID-19)")
    ap.add_argument("--all-doses", action="store_true", help="show matching doses too, not only mismatches")
    args = ap.parse_args()

    run = Path(args.run) if args.run else latest_run()
    summary = json.loads((run / "summary.json").read_text(encoding="utf-8"))
    print(f"Run: {run.name}")

    now = headline(summary)
    print("\nHeadline (baseline -> now):")
    for key, base in BASELINE.items():
        delta = now[key] - base
        sign = f" ({delta:+d})" if delta else ""
        print(f"  {key:24s} {base:5d} -> {now[key]:5d}{sign}")

    changed_path = run / "changed-cases.json"
    if changed_path.exists():
        changed = json.loads(changed_path.read_text(encoding="utf-8"))
        print(f"\nChanges vs previous run ({changed.get('comparedAgainst')}):")
        for key in ("statusChanged", "evaluationStatusChanged", "dosesNewlyMatched", "dosesNewlyMismatched",
                    "dosesStillMismatchedButChanged"):
            items = changed.get(key) or []
            print(f"  {key}: {len(items)}")
            for item in items:
                print(f"    {item}")

    wanted = [c.strip() for c in args.cases.split(",")] if args.cases else []
    shown = 0
    print("\nDoses:" if (wanted or args.group or args.all_doses) else "\nAll mismatched doses:")
    with open(run / "results.jsonl", encoding="utf-8") as f:
        for line in f:
            case = json.loads(line)
            if wanted and not any(w in case["caseId"] for w in wanted):
                continue
            if args.group and case["group"] != args.group:
                continue
            for d in case.get("evaluations") or []:
                if d.get("match") and not (args.all_doses or wanted):
                    continue
                print(fmt_dose(case, d))
                shown += 1
    if not shown:
        print("  (none)")


if __name__ == "__main__":
    main()
