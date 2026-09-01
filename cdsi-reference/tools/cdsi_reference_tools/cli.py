"""Command-line entry point.

    python -m cdsi_reference_tools logic-spec extract --version 4.6
    python -m cdsi_reference_tools logic-spec validate --version 4.6
    python -m cdsi_reference_tools logic-spec compare --from 4.6 --to 4.7
"""

import argparse
import sys

from . import compare_versions, extract, network_guard, validate


def _cmd_extract(args: argparse.Namespace) -> int:
    index = extract.run_extract(args.version)
    warnings = index.all_warnings()
    print(
        f"Extracted version {args.version}: {len(index.sections)} sections, "
        f"{len(index.figures)} figures, {len(index.tables)} tables, {len(warnings)} warnings."
    )
    for w in warnings:
        print(f"  WARNING: {w}")
    return 0


def _cmd_validate(args: argparse.Namespace) -> int:
    problems = validate.validate_version(args.version)
    gaps = validate.acknowledged_gaps(args.version)
    if not problems:
        print(f"Version {args.version}: valid.")
    else:
        print(f"Version {args.version}: {len(problems)} problem(s):")
        for p in problems:
            print(f"  - {p}")
    if gaps:
        print(f"{len(gaps)} acknowledged gap(s) (tracked, not fixed - see mappings/spec-to-code.yaml):")
        for g in gaps:
            print(f"  - {g}")
    return 1 if problems else 0


def _cmd_compare(args: argparse.Namespace) -> int:
    try:
        print(compare_versions.compare(getattr(args, "from"), args.to))
        return 0
    except (compare_versions.NoSuchVersion, NotImplementedError) as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="cdsi_reference_tools")
    subparsers = parser.add_subparsers(dest="resource", required=True)

    logic_spec = subparsers.add_parser("logic-spec", help="Logic Specification tools")
    logic_spec_sub = logic_spec.add_subparsers(dest="action", required=True)

    p_extract = logic_spec_sub.add_parser("extract", help="Deterministically extract a registered version")
    p_extract.add_argument("--version", required=True)
    p_extract.set_defaults(func=_cmd_extract)

    p_validate = logic_spec_sub.add_parser("validate", help="Validate a registered version")
    p_validate.add_argument("--version", required=True)
    p_validate.set_defaults(func=_cmd_validate)

    p_compare = logic_spec_sub.add_parser("compare", help="Compare two registered versions")
    p_compare.add_argument("--from", dest="from", required=True)
    p_compare.add_argument("--to", required=True)
    p_compare.set_defaults(func=_cmd_compare)

    return parser


def main(argv: list[str] | None = None) -> int:
    # Phase 11: enforce, not just claim, that no command here ever needs
    # network access or an LLM - see network_guard's own module docstring.
    network_guard.install()
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
