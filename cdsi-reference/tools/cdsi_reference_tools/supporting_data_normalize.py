"""Phase 14: parses a registered Supporting Data release's XML into
agent-readable structured JSON, validated against the release's own XSD
first. No network access, no LLM - purely deterministic XML parsing (see
network_guard, installed for every CLI command including this one).

Design decisions, made explicit rather than left implicit:

- The XML is normalized, not the spreadsheets. cdsi-engine itself only
  ever reads the XML (see DataModelLoader.java) - the .xlsx files in the
  same release are a human-readable export of the same data, not a
  second authoritative source the engine consumes. Cross-checking the two
  for the plan's SUPPORTING_DATA_CONFLICT scenario would need a bespoke
  parser for spreadsheet layouts that vary per antigen (each workbook has
  several sheets with merged header cells, not a simple table this module
  could read generically) - deliberately not built in this pass. If a
  real conflict between the two ever needs recording, it can still be
  filed as a finding by hand today; this module doesn't detect one
  automatically.
- Element-to-value conversion is generic and occurrence-driven: a child
  tag that repeats *in the file being parsed* becomes a list, one that
  doesn't becomes a scalar/dict - it does not consult the XSD's declared
  maxOccurs, so a field that's schema-eligible for repetition but only
  occurs once in a given antigen (e.g. Cholera has exactly one <series>)
  comes back as a bare object, not a single-element list. The two fields
  every consumer most needs to iterate reliably - series and seriesDose
  in antigen files, and each schedule-level collection - are force-listed
  regardless of occurrence count (see _ALWAYS_LIST_*) so index-building
  doesn't need to special-case "was there only one." Anything else should
  be read defensively (isinstance(x, list)) rather than assumed.
- Output preserves the source XML's own element order - it is not
  re-sorted into some other canonical key order. That order *is* the
  faithful, stable, deterministic representation; re-sorting it would be
  a redesign, which the plan explicitly says not to do.
- Leaf text is whitespace-trimmed (the source PDF-to-Word-to-XML pipeline
  leaves stray trailing spaces on some values, e.g. "Cholera " as a
  vaccineGroup name) - this is normalizing incidental formatting noise,
  not the substantive value, so it doesn't count as a redesign either.
"""

import datetime
import json
import re
from pathlib import Path

from lxml import etree

from . import paths

_ANTIGEN_FILENAME_RE = re.compile(r"^AntigenSupportingData-\s*(.+?)(?:-\s*508)?\.xml$", re.IGNORECASE)

_ALWAYS_LIST_ANTIGEN = frozenset({"series", "seriesDose"})
_ALWAYS_LIST_SCHEDULE = frozenset({"liveVirusConflict", "vaccineGroup", "vaccineGroupMap", "cvxMap", "observation"})


class NormalizeError(Exception):
    pass


def antigen_name_from_filename(filename: str) -> str | None:
    m = _ANTIGEN_FILENAME_RE.match(filename)
    return m.group(1).strip() if m else None


def slugify(name: str) -> str:
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", name.lower())).strip("-")


def validate_against_xsd(xml_path: Path, xsd_path: Path) -> list[str]:
    schema = etree.XMLSchema(etree.parse(str(xsd_path)))
    doc = etree.parse(str(xml_path))
    if schema.validate(doc):
        return []
    return [str(e) for e in schema.error_log]


def _element_to_value(element: etree._Element, always_list: frozenset[str]):
    children = list(element)
    if not children:
        return (element.text or "").strip()

    result: dict = {}
    for child in children:
        tag = etree.QName(child).localname
        value = _element_to_value(child, always_list)
        if tag in result:
            if not isinstance(result[tag], list):
                result[tag] = [result[tag]]
            result[tag].append(value)
        elif tag in always_list:
            result[tag] = [value]
        else:
            result[tag] = value
    return result


def normalize_antigen_xml(xml_path: Path, xsd_path: Path) -> dict:
    warnings = validate_against_xsd(xml_path, xsd_path)
    doc = etree.parse(str(xml_path))
    root = doc.getroot()
    data = _element_to_value(root, _ALWAYS_LIST_ANTIGEN)
    return {"data": data, "warnings": warnings}


def normalize_schedule_xml(xml_path: Path, xsd_path: Path) -> dict:
    warnings = validate_against_xsd(xml_path, xsd_path)
    doc = etree.parse(str(xml_path))
    root = doc.getroot()
    data = _element_to_value(root, _ALWAYS_LIST_SCHEDULE)
    return {"data": data, "warnings": warnings}


def _write_json(path: Path, obj: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False, sort_keys=False) + "\n", encoding="utf-8")


def _series_summary(series: dict) -> dict:
    doses = series.get("seriesDose", [])
    if not isinstance(doses, list):
        doses = [doses]
    return {
        "seriesName": series.get("seriesName"),
        "seriesType": series.get("seriesType"),
        "vaccineGroup": series.get("vaccineGroup"),
        "doseCount": len(doses),
    }


def normalize_release(release_id: str) -> dict:
    """Normalizes every antigen file and the schedule file in a registered
    release, writes normalized/antigens/<slug>.json, normalized/schedules/
    schedule.json, normalized/index.json, and documentation/antigens/
    <slug>.md, and records the result in manifest.yaml (normalizer_version,
    normalized_at, warnings) - the fields Phase 13 reserved for this."""
    source_dir = paths.supporting_data_source_dir(release_id)
    xml_dir = source_dir / "xml"
    xsd_dir = source_dir / "xsd"
    if not xml_dir.exists():
        raise NormalizeError(f"Release {release_id!r} is not registered (no {xml_dir})")

    antigen_xsd = xsd_dir / "AntigenSupportingData.xsd"
    schedule_xsd = xsd_dir / "ScheduleSupportingData.xsd"

    all_warnings: list[str] = []
    antigens_index: list[dict] = []

    normalized_root = paths.supporting_data_normalized_dir(release_id)
    antigens_dir = normalized_root / "antigens"
    schedules_dir = normalized_root / "schedules"
    documentation_dir = paths.supporting_data_documentation_dir(release_id) / "antigens"

    for xml_path in sorted(xml_dir.glob("AntigenSupportingData-*.xml")):
        antigen_name = antigen_name_from_filename(xml_path.name)
        if antigen_name is None:
            all_warnings.append(f"Could not determine antigen name from {xml_path.name!r} - skipped")
            continue
        slug = slugify(antigen_name)
        result = normalize_antigen_xml(xml_path, antigen_xsd)
        for w in result["warnings"]:
            all_warnings.append(f"{xml_path.name}: {w}")

        series_list = result["data"].get("series", [])
        if not isinstance(series_list, list):
            series_list = [series_list]

        record = {
            "antigen": antigen_name,
            "source_file": f"source/xml/{xml_path.name}",
            "data": result["data"],
        }
        _write_json(antigens_dir / f"{slug}.json", record)

        documentation_dir.mkdir(parents=True, exist_ok=True)
        (documentation_dir / f"{slug}.md").write_text(
            _antigen_markdown(antigen_name, xml_path.name, series_list), encoding="utf-8")

        antigens_index.append({
            "antigen": antigen_name,
            "slug": slug,
            "source_file": f"source/xml/{xml_path.name}",
            "series": [_series_summary(s) for s in series_list],
        })

    schedule_xml = xml_dir / "ScheduleSupportingData.xml"
    schedule_summary = None
    if schedule_xml.exists():
        schedule_result = normalize_schedule_xml(schedule_xml, schedule_xsd)
        for w in schedule_result["warnings"]:
            all_warnings.append(f"{schedule_xml.name}: {w}")
        schedule_record = {
            "source_file": "source/xml/ScheduleSupportingData.xml",
            "data": schedule_result["data"],
        }
        _write_json(schedules_dir / "schedule.json", schedule_record)

        vaccine_group_map = schedule_result["data"].get("vaccineGroupToAntigenMap", {}).get("vaccineGroupMap", [])
        vaccine_groups_index = []
        for entry in vaccine_group_map:
            entry_antigens = entry.get("antigen", [])
            if not isinstance(entry_antigens, list):
                entry_antigens = [entry_antigens]
            vaccine_groups_index.append({"vaccineGroup": entry.get("name"), "antigens": entry_antigens})
        schedule_summary = {"vaccineGroups": vaccine_groups_index}
    else:
        all_warnings.append(f"{schedule_xml.name} not found under {xml_dir} - schedule-level index is incomplete")

    index = {
        "release_id": release_id,
        "antigens": antigens_index,
        "schedule": schedule_summary,
    }
    _write_json(normalized_root / "index.json", index)

    documentation_root = paths.supporting_data_documentation_dir(release_id)
    documentation_root.mkdir(parents=True, exist_ok=True)
    (documentation_root / "index.md").write_text(_documentation_index_markdown(release_id, antigens_index), encoding="utf-8")

    normalized_at = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    _update_manifest_after_normalize(release_id, normalized_at, all_warnings)

    return {"release_id": release_id, "antigens": len(antigens_index), "warnings": all_warnings}


def _update_manifest_after_normalize(release_id: str, normalized_at: str, warnings: list[str]) -> None:
    import yaml

    from .__init__ import __version__ as tool_version

    manifest_file = paths.supporting_data_manifest_path(release_id)
    manifest = yaml.safe_load(manifest_file.read_text(encoding="utf-8"))
    manifest["normalizer_version"] = tool_version
    manifest["normalized_at"] = normalized_at
    manifest["warnings"] = warnings
    manifest_file.write_text(yaml.safe_dump(manifest, sort_keys=False), encoding="utf-8")


def _documentation_index_markdown(release_id: str, antigens_index: list[dict]) -> str:
    lines = [f"# Supporting Data {release_id} - antigen documentation", ""]
    lines.append(
        "One page per antigen, generated by `supporting-data normalize` directly from the release's "
        "XML - see `normalized/index.json` for the same information as structured data, and "
        "`normalized/antigens/<slug>.json` for the complete faithful representation."
    )
    lines.append("")
    for entry in antigens_index:
        series_count = len(entry["series"])
        dose_total = sum(s["doseCount"] for s in entry["series"])
        lines.append(f"- [{entry['antigen']}](antigens/{entry['slug']}.md) - {series_count} series, {dose_total} total doses")
    lines.append("")
    return "\n".join(lines)


def _antigen_markdown(antigen_name: str, source_filename: str, series_list: list[dict]) -> str:
    lines = [f"# {antigen_name}", "", f"Source: `source/xml/{source_filename}` (see the release's `manifest.yaml` for its checksum).", ""]
    lines.append("Generated by `python -m cdsi_reference_tools supporting-data normalize` - a faithful, mechanical rendering of the source XML, not an interpretation. Cross-reference `normalized/antigens/<slug>.json` for the complete structured data; this file only summarizes it.")
    lines.append("")
    lines.append("## Patient series")
    lines.append("")
    for series in series_list:
        doses = series.get("seriesDose", [])
        if not isinstance(doses, list):
            doses = [doses]
        lines.append(f"### {series.get('seriesName', '(unnamed series)')}")
        lines.append("")
        lines.append(f"- Vaccine group: {series.get('vaccineGroup', '').strip()}")
        lines.append(f"- Series type: {series.get('seriesType', '')}")
        lines.append(f"- Required gender: {series.get('requiredGender', '') or 'any'}")
        lines.append(f"- Doses: {len(doses)}")
        lines.append("")
        for dose in doses:
            dose_number = dose.get("doseNumber", "?")
            lines.append(f"#### {dose_number}")
            lines.append("")
            age = dose.get("age", {})
            if isinstance(age, list):
                age = age[0] if age else {}
            if age:
                lines.append(
                    f"- Age: min {age.get('minAge') or '-'}, earliest recommended {age.get('earliestRecAge') or '-'}, "
                    f"latest recommended {age.get('latestRecAge') or '-'}, max {age.get('maxAge') or '-'}"
                )
            interval = dose.get("interval", {})
            if isinstance(interval, list):
                interval = interval[0] if interval else {}
            if interval:
                lines.append(
                    f"- Interval from previous: min {interval.get('minInt') or '-'}, "
                    f"earliest recommended {interval.get('earliestRecInt') or '-'}"
                )
            for vaccine_kind in ("preferableVaccine", "allowableVaccine", "inadvertentVaccine"):
                entries = dose.get(vaccine_kind, [])
                if not entries:
                    continue
                if not isinstance(entries, list):
                    entries = [entries]
                cvxs = sorted({e.get("cvx", "") for e in entries if isinstance(e, dict) and e.get("cvx")})
                if cvxs:
                    lines.append(f"- {vaccine_kind}: CVX {', '.join(cvxs)}")
            conditional_skip = dose.get("conditionalSkip")
            if conditional_skip:
                lines.append("- Has a conditional skip rule - see the JSON for the full condition set.")
            lines.append("")
    lines.append("## What changed from the preceding release")
    lines.append("")
    lines.append("Not available yet - release comparison (Phase 15 of the reference-module plan) is not built. See `supporting-data/diffs/README.md`.")
    lines.append("")
    return "\n".join(lines)
