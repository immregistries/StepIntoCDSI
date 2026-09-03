"""Command-line entry point.

    python -m cdsi_reference_tools logic-spec extract --version 4.6
    python -m cdsi_reference_tools logic-spec validate --version 4.6
    python -m cdsi_reference_tools logic-spec compare --from 4.6 --to 4.7
    python -m cdsi_reference_tools supporting-data import --source <zip-or-directory>
    python -m cdsi_reference_tools supporting-data list
    python -m cdsi_reference_tools step-tests sync --version 4.6
    python -m cdsi_reference_tools step-tests status --version 4.6
"""

import argparse
import sys
from pathlib import Path

from . import (
    compare_versions,
    extract,
    network_guard,
    reference_sets,
    step_test_status,
    supporting_data,
    supporting_data_compare,
    supporting_data_normalize,
    validate,
)


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


def _cmd_supporting_data_import(args: argparse.Namespace) -> int:
    source = Path(args.source)
    try:
        if source.is_dir():
            results = supporting_data.import_all_from(source)
            if not results:
                print(f"No {supporting_data.SOURCE_ZIP_GLOB!r} files found under {source}")
                return 1
        else:
            results = [supporting_data.import_release(source)]
    except supporting_data.SupportingDataError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    for manifest in results:
        print(f"Registered release {manifest['release_id']}: {len(manifest['files'])} file(s)")
        for w in manifest["warnings"]:
            print(f"  WARNING: {w}")
    return 0


def _cmd_supporting_data_list(args: argparse.Namespace) -> int:
    releases = supporting_data.list_registered_releases()
    if not releases:
        print("No Supporting Data releases registered yet.")
        return 0
    for release_id in releases:
        print(release_id)
    return 0


def _cmd_supporting_data_normalize(args: argparse.Namespace) -> int:
    try:
        result = supporting_data_normalize.normalize_release(args.release)
    except supporting_data_normalize.NormalizeError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1
    print(f"Normalized release {result['release_id']}: {result['antigens']} antigen(s), {len(result['warnings'])} warning(s)")
    for w in result["warnings"]:
        print(f"  WARNING: {w}")
    return 0


def _cmd_supporting_data_compare(args: argparse.Namespace) -> int:
    try:
        report = supporting_data_compare.compare_releases(getattr(args, "from"), args.to)
    except supporting_data_compare.CompareError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1
    print(
        f"Compared {report['from']} to {report['to']}: {report['change_count']} change(s). "
        f"See supporting-data/diffs/{report['from']}-to-{report['to']}.md"
    )
    return 0


def _cmd_supporting_data_validate(args: argparse.Namespace) -> int:
    problems = validate.validate_supporting_data_release(args.release)
    if not problems:
        print(f"Supporting Data release {args.release}: valid.")
        return 0
    print(f"Supporting Data release {args.release}: {len(problems)} problem(s):")
    for p in problems:
        print(f"  - {p}")
    return 1


def _cmd_reference_set_create(args: argparse.Namespace) -> int:
    try:
        record = reference_sets.create_reference_set(args.logic_spec, args.supporting_data, args.notes or "")
    except reference_sets.ReferenceSetError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1
    print(f"Reference set {record['id']}: {record['fits_fixture_set']['case_count']} FITS case(s)")
    return 0


def _cmd_reference_set_list(args: argparse.Namespace) -> int:
    ids = reference_sets.list_reference_sets()
    if not ids:
        print("No reference sets defined yet.")
        return 0
    for i in ids:
        print(i)
    return 0


def _cmd_reference_set_validate(args: argparse.Namespace) -> int:
    problems = reference_sets.validate_reference_set(args.id)
    if not problems:
        print(f"Reference set {args.id}: valid.")
        return 0
    print(f"Reference set {args.id}: {len(problems)} problem(s):")
    for p in problems:
        print(f"  - {p}")
    return 1


def _cmd_reference_set_export(args: argparse.Namespace) -> int:
    try:
        dest = reference_sets.export_for_fits_tests(args.id, Path(args.to) if args.to else None)
    except reference_sets.ReferenceSetError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1
    print(f"Exported {args.id} to {dest}")
    return 0


def _cmd_step_tests_sync(args: argparse.Namespace) -> int:
    added = step_test_status.sync_status(args.version)
    if not added:
        print("step-tests/status.yaml already covers every unit in spec-to-code.yaml.")
    else:
        print(f"Added {len(added)} unit(s) to step-tests/status.yaml: {', '.join(added)}")
    return 0


def _cmd_step_tests_status(args: argparse.Namespace) -> int:
    print(step_test_status.render_status_table(args.version))
    return 0


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

    supporting_data_parser = subparsers.add_parser("supporting-data", help="CDSi Supporting Data tools")
    supporting_data_sub = supporting_data_parser.add_subparsers(dest="action", required=True)

    p_sd_import = supporting_data_sub.add_parser(
        "import", help="Register a Supporting Data release from a zip file or a directory of zips")
    p_sd_import.add_argument("--source", required=True,
                              help="Path to a supporting-data-*.zip file, or a directory to scan for them")
    p_sd_import.set_defaults(func=_cmd_supporting_data_import)

    p_sd_list = supporting_data_sub.add_parser("list", help="List registered Supporting Data releases")
    p_sd_list.set_defaults(func=_cmd_supporting_data_list)

    p_sd_validate = supporting_data_sub.add_parser("validate", help="Validate a registered Supporting Data release")
    p_sd_validate.add_argument("--release", required=True)
    p_sd_validate.set_defaults(func=_cmd_supporting_data_validate)

    p_sd_normalize = supporting_data_sub.add_parser(
        "normalize", help="Parse a registered release's XML into agent-readable structured JSON")
    p_sd_normalize.add_argument("--release", required=True)
    p_sd_normalize.set_defaults(func=_cmd_supporting_data_normalize)

    p_sd_compare = supporting_data_sub.add_parser("compare", help="Compare two normalized Supporting Data releases")
    p_sd_compare.add_argument("--from", dest="from", required=True)
    p_sd_compare.add_argument("--to", required=True)
    p_sd_compare.set_defaults(func=_cmd_supporting_data_compare)

    reference_set_parser = subparsers.add_parser("reference-set", help="Reproducible reference-set tools")
    reference_set_sub = reference_set_parser.add_subparsers(dest="action", required=True)

    p_rs_create = reference_set_sub.add_parser(
        "create", help="Bind a Logic Specification version, Supporting Data release, and the current FITS fixtures")
    p_rs_create.add_argument("--logic-spec", required=True)
    p_rs_create.add_argument("--supporting-data", required=True)
    p_rs_create.add_argument("--notes", default="")
    p_rs_create.set_defaults(func=_cmd_reference_set_create)

    p_rs_list = reference_set_sub.add_parser("list", help="List defined reference sets")
    p_rs_list.set_defaults(func=_cmd_reference_set_list)

    p_rs_validate = reference_set_sub.add_parser("validate", help="Re-check a reference set's recorded checksums")
    p_rs_validate.add_argument("--id", required=True)
    p_rs_validate.set_defaults(func=_cmd_reference_set_validate)

    p_rs_export = reference_set_sub.add_parser(
        "export", help="Export a reference set as JSON for cdsi-fits-tests to read")
    p_rs_export.add_argument("--id", required=True)
    p_rs_export.add_argument("--to", default=None, help="Defaults to cdsi-fits-tests/src/test/resources/reference-set.json")
    p_rs_export.set_defaults(func=_cmd_reference_set_export)

    step_tests_parser = subparsers.add_parser("step-tests", help="Phase 21: per-step spec-conformance test tracking")
    step_tests_sub = step_tests_parser.add_subparsers(dest="action", required=True)

    p_st_sync = step_tests_sub.add_parser(
        "sync", help="Add any unit from spec-to-code.yaml missing from step-tests/status.yaml")
    p_st_sync.add_argument("--version", required=True)
    p_st_sync.set_defaults(func=_cmd_step_tests_sync)

    p_st_status = step_tests_sub.add_parser(
        "status", help="Render the per-step test/fix dashboard (run `mvn -pl cdsi-engine test` first for fresh counts)")
    p_st_status.add_argument("--version", required=True)
    p_st_status.set_defaults(func=_cmd_step_tests_status)

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
