"""Phase 15: semantic comparison of two registered, normalized Supporting
Data releases, keyed by stable domain identifiers (antigen name, series
name, dose number, CVX code, vaccine-group name, observation code) rather
than a raw XML/JSON line diff - the same identifiers Phase 14's normalizer
already treats as the natural keys for each domain concept.

Renames are not detected as renames. If a series (or any other keyed
item) is renamed between releases, this reports it as one item removed
and a different one added - the only honest thing a stable-identifier
comparison can do without guessing that two differently-named things are
"the same." That pattern shows up for real between 4.64 and 4.65 (see
tests/test_supporting_data_compare.py) - it's a real, common editorial
pattern in these releases, worth a human's attention, not a bug to
suppress.

Two of the plan's six required comparison categories are handled at
reduced scope, documented rather than faked:
- "Schema additions, removals, or cardinality changes": the two XSDs are
  compared for byte equality, and further diffed by declared element name
  if they differ - not a structural cardinality-aware diff. In practice
  the schema has not changed between any two releases registered so far.
- "Source-format disagreements introduced or resolved": depends on the
  XML-vs-spreadsheet cross-check Phase 14 deliberately did not build (see
  its own module docstring) - there is nothing here to compare yet.
"""

import json
from pathlib import Path

from lxml import etree

from . import paths

_AGE_FIELDS = ("absMinAge", "minAge", "earliestRecAge", "latestRecAge", "maxAge", "effectiveDate", "cessationDate")
_INTERVAL_FIELDS = (
    "fromPrevious", "fromTargetDose", "fromMostRecent", "absMinInt", "minInt",
    "earliestRecInt", "latestRecInt", "intervalPriority", "effectiveDate", "cessationDate",
)
_ALLOWABLE_INTERVAL_FIELDS = ("fromPrevious", "fromTargetDose", "absMinInt", "effectiveDate", "cessationDate")
_SELECT_SERIES_FIELDS = (
    "defaultSeries", "productPath", "seriesGroupName", "seriesGroup",
    "seriesPriority", "seriesPreference", "minAgeToStart", "maxAgeToStart",
)
_VACCINE_RELATIONSHIP_KINDS = ("preferableVaccine", "allowableVaccine", "inadvertentVaccine")


class CompareError(Exception):
    pass


def _as_list(value):
    """Phase 14's normalizer represents an empty XML element (e.g. an
    antigen with no inadvertentVaccine entries at all, <inadvertentVaccine/>)
    as an empty string leaf, not an empty list - normalize that back to
    "no entries" here rather than treating "" as one bogus entry."""
    if value is None or value == "":
        return []
    return value if isinstance(value, list) else [value]


def _as_dict(value):
    """Same idea as _as_list but for elements normally expected to be a
    dict of sub-fields - an empty XML element with no children (e.g.
    <codedValues/>, which the XSD otherwise requires at least one
    codedValue under) normalizes to "" too, not {}."""
    return value if isinstance(value, dict) else {}


def change(category, change_type, identifier, old_source, new_source, old_normalized, new_normalized,
           field=None, old_value=None, new_value=None):
    return {
        "category": category,
        "change_type": change_type,
        "identifier": identifier,
        "field": field,
        "old_value": old_value,
        "new_value": new_value,
        "old_source": old_source,
        "new_source": new_source,
        "old_normalized": old_normalized,
        "new_normalized": new_normalized,
    }


def _diff_fields(old: dict, new: dict, fields, category, identifier, sources, out: list) -> None:
    for f in fields:
        old_v, new_v = old.get(f), new.get(f)
        if old_v != new_v:
            out.append(change(category, "changed", identifier, *sources, field=f, old_value=old_v, new_value=new_v))


def _diff_keyed(old_items: list, new_items: list, key_fn, category, identifier_fn, sources, out: list,
                 field_diff=None) -> None:
    """Matches old_items/new_items (lists of dict) by key_fn, reports
    added/removed for unmatched keys, and runs field_diff(old, new) - if
    given - on every matched pair."""
    old_by_key = {key_fn(i): i for i in old_items}
    new_by_key = {key_fn(i): i for i in new_items}
    for key in sorted(set(old_by_key) - set(new_by_key), key=str):
        out.append(change(category, "removed", identifier_fn(old_by_key[key]), *sources, old_value=old_by_key[key]))
    for key in sorted(set(new_by_key) - set(old_by_key), key=str):
        out.append(change(category, "added", identifier_fn(new_by_key[key]), *sources, new_value=new_by_key[key]))
    if field_diff is None:
        return
    for key in sorted(set(old_by_key) & set(new_by_key), key=str):
        field_diff(old_by_key[key], new_by_key[key])


def _vaccine_relationship_key(entry: dict):
    return (entry.get("cvx"), entry.get("beginAge"), entry.get("endAge"))


def _compare_dose(old_dose: dict, new_dose: dict, antigen: str, series: str, sources, out: list) -> None:
    dose_number = old_dose.get("doseNumber")
    identifier = {"antigen": antigen, "series": series, "dose": dose_number}

    old_age, new_age = _as_dict(old_dose.get("age")), _as_dict(new_dose.get("age"))
    _diff_fields(old_age, new_age, _AGE_FIELDS, "age", identifier, sources, out)

    old_int, new_int = _as_dict(old_dose.get("interval")), _as_dict(new_dose.get("interval"))
    _diff_fields(old_int, new_int, _INTERVAL_FIELDS, "interval", identifier, sources, out)

    old_allow_int = _as_dict(old_dose.get("allowableInterval"))
    new_allow_int = _as_dict(new_dose.get("allowableInterval"))
    _diff_fields(old_allow_int, new_allow_int, _ALLOWABLE_INTERVAL_FIELDS, "allowable_interval", identifier, sources, out)

    for kind in _VACCINE_RELATIONSHIP_KINDS:
        _diff_keyed(
            _as_list(old_dose.get(kind)), _as_list(new_dose.get(kind)), _vaccine_relationship_key,
            "vaccine_relationship", lambda e, kind=kind: {**identifier, "relationship": kind, "cvx": e.get("cvx")},
            sources, out,
        )

    old_skip, new_skip = old_dose.get("conditionalSkip"), new_dose.get("conditionalSkip")
    if bool(old_skip) != bool(new_skip):
        change_type = "added" if new_skip else "removed"
        out.append(change("conditional_skip", change_type, identifier, *sources,
                           old_value=old_skip or None, new_value=new_skip or None))
    elif old_skip and new_skip and json.dumps(old_skip, sort_keys=True) != json.dumps(new_skip, sort_keys=True):
        out.append(change("conditional_skip", "changed", identifier, *sources, old_value=old_skip, new_value=new_skip))

    if old_dose.get("recurringDose") != new_dose.get("recurringDose"):
        out.append(change("recurring_dose", "changed", identifier, *sources,
                           old_value=old_dose.get("recurringDose"), new_value=new_dose.get("recurringDose")))

    old_season = old_dose.get("seasonalRecommendation") or {}
    new_season = new_dose.get("seasonalRecommendation") or {}
    if old_season != new_season:
        out.append(change("seasonal_recommendation", "changed", identifier, *sources,
                           old_value=old_season or None, new_value=new_season or None))


def _compare_series(old_series: dict, new_series: dict, antigen: str, sources, out: list) -> None:
    name = old_series.get("seriesName")
    identifier = {"antigen": antigen, "series": name}
    _diff_fields(old_series, new_series, ("seriesType", "vaccineGroup", "equivalentSeriesGroups"),
                 "series_attribute", identifier, sources, out)

    old_select = _as_dict(old_series.get("selectSeries"))
    new_select = _as_dict(new_series.get("selectSeries"))
    _diff_fields(old_select, new_select, _SELECT_SERIES_FIELDS, "series_selection", identifier, sources, out)

    old_guidance = set(_as_list(old_series.get("seriesAdminGuidance")))
    new_guidance = set(_as_list(new_series.get("seriesAdminGuidance")))
    if old_guidance != new_guidance:
        out.append(change("series_admin_guidance", "changed", identifier, *sources,
                           old_value=sorted(old_guidance), new_value=sorted(new_guidance)))

    _diff_keyed(
        _as_list(old_series.get("seriesDose")), _as_list(new_series.get("seriesDose")),
        lambda d: d.get("doseNumber"), "dose",
        lambda d: {"antigen": antigen, "series": name, "dose": d.get("doseNumber")},
        sources, out,
        field_diff=lambda o, n: _compare_dose(o, n, antigen, name, sources, out),
    )


def _contraindication_key(entry: dict):
    return entry.get("observationCode")


def _compare_contraindications(old_data: dict, new_data: dict, antigen: str, sources, out: list) -> None:
    old_c = _as_dict(old_data.get("contraindications"))
    new_c = _as_dict(new_data.get("contraindications"))
    for context in ("vaccineGroup", "vaccine"):
        old_ctx = _as_dict(old_c.get(context))
        new_ctx = _as_dict(new_c.get(context))
        _diff_keyed(
            _as_list(old_ctx.get("contraindication")), _as_list(new_ctx.get("contraindication")),
            _contraindication_key, "contraindication",
            lambda e, context=context: {"antigen": antigen, "context": context, "observationCode": e.get("observationCode")},
            sources, out,
        )


def compare_antigen(antigen: str, old_record: dict, new_record: dict) -> list:
    sources = (old_record["source_file"], new_record["source_file"], None, None)
    out: list = []
    old_data, new_data = old_record["data"], new_record["data"]
    _diff_keyed(
        _as_list(old_data.get("series")), _as_list(new_data.get("series")),
        lambda s: s.get("seriesName"), "series",
        lambda s: {"antigen": antigen, "series": s.get("seriesName")},
        sources, out,
        field_diff=lambda o, n: _compare_series(o, n, antigen, sources, out),
    )
    _compare_contraindications(old_data, new_data, antigen, sources, out)
    return out


def _cvx_association_key(entry: dict):
    return (entry.get("antigen"), entry.get("associationBeginAge"), entry.get("associationEndAge"))


def _observation_coded_value_key(entry: dict):
    return (entry.get("code"), entry.get("codeSystem"))


def compare_schedule(old_schedule: dict, new_schedule: dict) -> list:
    sources = (old_schedule["source_file"], new_schedule["source_file"], None, None)
    out: list = []
    old_data, new_data = old_schedule["data"], new_schedule["data"]

    old_groups = _as_list(_as_dict(old_data.get("vaccineGroups")).get("vaccineGroup"))
    new_groups = _as_list(_as_dict(new_data.get("vaccineGroups")).get("vaccineGroup"))
    _diff_keyed(old_groups, new_groups, lambda g: g.get("name"), "vaccine_group",
                lambda g: {"vaccineGroup": g.get("name")}, sources, out,
                field_diff=lambda o, n: _diff_fields(o, n, ("administerFullVaccineGroup",), "vaccine_group",
                                                      {"vaccineGroup": o.get("name")}, sources, out))

    old_map = _as_list(_as_dict(old_data.get("vaccineGroupToAntigenMap")).get("vaccineGroupMap"))
    new_map = _as_list(_as_dict(new_data.get("vaccineGroupToAntigenMap")).get("vaccineGroupMap"))

    def _map_field_diff(o, n):
        old_antigens, new_antigens = set(_as_list(o.get("antigen"))), set(_as_list(n.get("antigen")))
        if old_antigens != new_antigens:
            out.append(change("vaccine_group_antigens", "changed", {"vaccineGroup": o.get("name")}, *sources,
                               old_value=sorted(old_antigens), new_value=sorted(new_antigens)))

    _diff_keyed(old_map, new_map, lambda m: m.get("name"), "vaccine_group_map",
                lambda m: {"vaccineGroup": m.get("name")}, sources, out, field_diff=_map_field_diff)

    old_cvx = _as_list(_as_dict(old_data.get("cvxToAntigenMap")).get("cvxMap"))
    new_cvx = _as_list(_as_dict(new_data.get("cvxToAntigenMap")).get("cvxMap"))

    def _cvx_field_diff(o, n):
        identifier = {"cvx": o.get("cvx")}
        _diff_fields(o, n, ("shortDescription",), "cvx_mapping", identifier, sources, out)
        _diff_keyed(_as_list(o.get("association")), _as_list(n.get("association")), _cvx_association_key,
                    "cvx_association", lambda a: {"cvx": o.get("cvx"), "antigen": a.get("antigen")}, sources, out)

    _diff_keyed(old_cvx, new_cvx, lambda c: c.get("cvx"), "cvx_mapping", lambda c: {"cvx": c.get("cvx")},
                sources, out, field_diff=_cvx_field_diff)

    old_conflicts = _as_list(_as_dict(old_data.get("liveVirusConflicts")).get("liveVirusConflict"))
    new_conflicts = _as_list(_as_dict(new_data.get("liveVirusConflicts")).get("liveVirusConflict"))

    def _conflict_key(c):
        return (_as_dict(c.get("previous")).get("cvx"), _as_dict(c.get("current")).get("cvx"))

    def _conflict_field_diff(o, n):
        identifier = {"previousCvx": _as_dict(o.get("previous")).get("cvx"), "currentCvx": _as_dict(o.get("current")).get("cvx")}
        _diff_fields(o, n, ("conflictBeginInterval", "minConflictEndInterval", "conflictEndInterval"),
                     "live_virus_conflict", identifier, sources, out)

    _diff_keyed(old_conflicts, new_conflicts, _conflict_key, "live_virus_conflict",
                lambda c: {"previousCvx": _as_dict(c.get("previous")).get("cvx"), "currentCvx": _as_dict(c.get("current")).get("cvx")},
                sources, out, field_diff=_conflict_field_diff)

    old_obs = _as_list(_as_dict(old_data.get("observations")).get("observation"))
    new_obs = _as_list(_as_dict(new_data.get("observations")).get("observation"))

    def _obs_field_diff(o, n):
        identifier = {"observationCode": o.get("observationCode")}
        _diff_fields(o, n, ("observationTitle", "group", "indicationText", "contraindicationText", "clarifyingText"),
                     "observation", identifier, sources, out)
        _diff_keyed(_as_list(_as_dict(o.get("codedValues")).get("codedValue")), _as_list(_as_dict(n.get("codedValues")).get("codedValue")),
                    _observation_coded_value_key, "observation_coded_value",
                    lambda cv: {"observationCode": o.get("observationCode"), "code": cv.get("code")}, sources, out)

    _diff_keyed(old_obs, new_obs, lambda o: o.get("observationCode"), "observation",
                lambda o: {"observationCode": o.get("observationCode")}, sources, out, field_diff=_obs_field_diff)

    return out


def compare_schema(old_release_id: str, new_release_id: str) -> list:
    out: list = []
    for xsd_name in ("AntigenSupportingData.xsd", "ScheduleSupportingData.xsd"):
        old_path = paths.supporting_data_source_dir(old_release_id) / "xsd" / xsd_name
        new_path = paths.supporting_data_source_dir(new_release_id) / "xsd" / xsd_name
        if not old_path.exists() or not new_path.exists():
            continue
        if old_path.read_bytes() == new_path.read_bytes():
            continue
        old_elements = {etree.QName(e).localname for e in etree.parse(str(old_path)).iter() if isinstance(e.tag, str)}
        new_elements = {etree.QName(e).localname for e in etree.parse(str(new_path)).iter() if isinstance(e.tag, str)}
        identifier = {"schema": xsd_name}
        for name in sorted(old_elements - new_elements):
            out.append(change("schema_element", "removed", {**identifier, "element": name},
                               f"source/xsd/{xsd_name}", f"source/xsd/{xsd_name}", None, None))
        for name in sorted(new_elements - old_elements):
            out.append(change("schema_element", "added", {**identifier, "element": name},
                               f"source/xsd/{xsd_name}", f"source/xsd/{xsd_name}", None, None))
        if old_elements == new_elements:
            out.append(change("schema_element", "changed", identifier,
                               f"source/xsd/{xsd_name}", f"source/xsd/{xsd_name}", None, None,
                               old_value="same element names, different structure - inspect the XSDs directly"))
    return out


def _load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def compare_releases(old_release_id: str, new_release_id: str) -> dict:
    for release_id in (old_release_id, new_release_id):
        if not (paths.supporting_data_normalized_dir(release_id) / "index.json").exists():
            raise CompareError(
                f"Release {release_id!r} has not been normalized yet - run "
                f"`supporting-data normalize --release {release_id}` first."
            )

    changes: list = []

    old_antigens_dir = paths.supporting_data_normalized_dir(old_release_id) / "antigens"
    new_antigens_dir = paths.supporting_data_normalized_dir(new_release_id) / "antigens"
    old_slugs = {p.stem for p in old_antigens_dir.glob("*.json")}
    new_slugs = {p.stem for p in new_antigens_dir.glob("*.json")}

    for slug in sorted(old_slugs - new_slugs):
        record = _load_json(old_antigens_dir / f"{slug}.json")
        changes.append(change("antigen", "removed", {"antigen": record["antigen"]},
                               record["source_file"], None, f"normalized/antigens/{slug}.json", None))
    for slug in sorted(new_slugs - old_slugs):
        record = _load_json(new_antigens_dir / f"{slug}.json")
        changes.append(change("antigen", "added", {"antigen": record["antigen"]},
                               None, record["source_file"], None, f"normalized/antigens/{slug}.json"))
    for slug in sorted(old_slugs & new_slugs):
        old_record = _load_json(old_antigens_dir / f"{slug}.json")
        new_record = _load_json(new_antigens_dir / f"{slug}.json")
        changes.extend(compare_antigen(old_record["antigen"], old_record, new_record))

    old_schedule_path = paths.supporting_data_normalized_dir(old_release_id) / "schedules" / "schedule.json"
    new_schedule_path = paths.supporting_data_normalized_dir(new_release_id) / "schedules" / "schedule.json"
    if old_schedule_path.exists() and new_schedule_path.exists():
        changes.extend(compare_schedule(_load_json(old_schedule_path), _load_json(new_schedule_path)))

    changes.extend(compare_schema(old_release_id, new_release_id))

    report = {
        "from": old_release_id,
        "to": new_release_id,
        "change_count": len(changes),
        "changes": changes,
    }
    _write_reports(report)
    return report


def _write_reports(report: dict) -> None:
    diffs_dir = paths.supporting_data_diffs_dir()
    diffs_dir.mkdir(parents=True, exist_ok=True)
    base = f"{report['from']}-to-{report['to']}"
    (diffs_dir / f"{base}.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False, sort_keys=False) + "\n", encoding="utf-8")
    (diffs_dir / f"{base}.md").write_text(_render_markdown(report), encoding="utf-8")


def _render_markdown(report: dict) -> str:
    from_id, to_id = report["from"], report["to"]
    changes = report["changes"]
    lines = [f"# Supporting Data {from_id} to {to_id}", ""]
    lines.append(
        f"{len(changes)} change(s) detected, matched by stable domain identifiers "
        "(antigen name, series name, dose number, CVX code, vaccine-group name, observation code) - "
        "not a raw file diff. A renamed item appears as one removal and one addition; see this "
        "module's docstring for why that's the correct, honest behavior rather than a bug."
    )
    lines.append("")
    lines.append(
        "**Do not assume any of this changed an existing FITS result.** Record potential impact here; "
        "verify it by actually re-running the FITS suite against both releases and comparing which "
        "cases changed - see `cdsi-fits-tests`."
    )
    lines.append("")

    by_category: dict[str, list] = {}
    for c in changes:
        by_category.setdefault(c["category"], []).append(c)

    lines.append("## Summary")
    lines.append("")
    for category in sorted(by_category):
        lines.append(f"- {category}: {len(by_category[category])}")
    lines.append("")

    for category in sorted(by_category):
        lines.append(f"## {category}")
        lines.append("")
        for c in by_category[category]:
            lines.append(f"- **{c['change_type']}** {c['identifier']}" + (f" - field `{c['field']}`" if c["field"] else ""))
            if c["change_type"] == "changed":
                lines.append(f"  - old: `{c['old_value']}`")
                lines.append(f"  - new: `{c['new_value']}`")
            elif c["change_type"] == "removed" and c["old_value"] is not None and not c["field"]:
                lines.append(f"  - was: `{json.dumps(c['old_value'], ensure_ascii=False)[:300]}`")
            elif c["change_type"] == "added" and c["new_value"] is not None and not c["field"]:
                lines.append(f"  - now: `{json.dumps(c['new_value'], ensure_ascii=False)[:300]}`")
        lines.append("")

    return "\n".join(lines)
