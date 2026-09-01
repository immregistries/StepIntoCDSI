# Supporting Data

Versioned, checksummed copies of CDSi Supporting Data releases - the XML/XSD/spreadsheet bundle `cdsi-engine` actually loads at runtime - kept separate from `logic-spec/` since the two resources change on different schedules and don't have a one-to-one version relationship.

Status: **Phases 13-15** are complete for releases 4.64 and 4.65: both are registered (source preserved and checksummed under `versions/<release-id>/source/`), normalized (`normalized/`, `documentation/` - see "Normalizing a release" below), and compared (`diffs/4.64-to-4.65.{json,md}` - see "Comparing two releases" below). Phase 16 (reference sets binding a Logic Specification version + Supporting Data release + FITS fixture set together) is not built yet - see `StepIntoCDSi-Specification-Reference-Module-Plan.md` at the repository root.

## Normalizing a release

```bash
python -m cdsi_reference_tools supporting-data normalize --release 4.65
```

Validates every antigen's XML against `AntigenSupportingData.xsd` (and the schedule file against `ScheduleSupportingData.xsd`) using `lxml`, then parses it into `normalized/antigens/<slug>.json` - a faithful, mechanical translation of the XML tree, preserving its own element order and every field, not a redesigned schema model. `normalized/schedules/schedule.json` covers the cross-antigen data (vaccine groups, CVX associations, live virus conflicts, observations), and `normalized/index.json` cross-references antigens, vaccine groups, and series/dose counts in one place. `documentation/index.md` and `documentation/antigens/<slug>.md` are generated Markdown summaries built from the same normalized data, for a human or an agent who wants the patient-series/target-dose/vaccine-type picture without reading raw XML or JSON.

Deliberately **not** implemented in this pass: cross-checking the XML against the `.xlsx` spreadsheets in the same release for the plan's `SUPPORTING_DATA_CONFLICT` scenario. `cdsi-engine` itself only ever reads the XML (see `DataModelLoader.java`) - the spreadsheets are a human-readable export of the same data, not a second source the engine consumes, and their per-antigen sheet layouts aren't uniform enough for a generic parser. If a real XML/spreadsheet disagreement needs recording, file it as a finding by hand for now.

Re-running `normalize` is deterministic - the same source XML always produces byte-identical normalized output (see `tests/test_supporting_data_normalize.py`). It updates the release's `manifest.yaml` (`normalizer_version`, `normalized_at`, `warnings`) every time it runs, the same fields Phase 13 reserved for this.

## Comparing two releases

```bash
python -m cdsi_reference_tools supporting-data compare --from 4.64 --to 4.65
```

Both releases must already be normalized. The comparison is keyed by stable domain identifiers - antigen name, series name, dose number, CVX code, vaccine-group name, observation code - not a raw file or JSON line diff, and writes both `diffs/<old>-to-<new>.json` (structured, every entry links back to its source file and normalized file) and `diffs/<old>-to-<new>.md` (grouped, human-readable, with a category-count summary at the top).

A renamed item (a series, a vaccine group, anything else keyed by name) is reported as one removal plus one addition, never as a detected "rename" - there's no reliable signal in the source data to distinguish a genuine rename from an unrelated remove-and-add, so guessing would be a real risk of hiding an actual change. This happens for real between 4.64 and 4.65 (Typhoid's two named series were consolidated into one differently-named series) - `tests/test_supporting_data_compare.py` locks in that exact, independently-verified case, along with a large systematic change (135 live-virus-conflict end intervals shortened from 30 to 28 days) as a check that the comparator doesn't choke on or mis-key a big uniform update.

Deliberately out of scope, same as Phase 14: no cross-check against the `.xlsx` spreadsheets ("source-format disagreements introduced or resolved" from the plan's checklist has nothing to compare against yet). The XSD schemas are compared for byte equality (and further diffed by declared element name if they ever differ) rather than a full structural cardinality-aware diff - in practice the schema hasn't changed between any two releases registered so far.

**A Supporting Data change is not evidence that an existing FITS result should change.** This tool records what's different; verifying whether it actually affects engine behavior means re-running the FITS suite against both releases and comparing which cases changed (see `cdsi-fits-tests`), not assuming from the diff alone.

## Registering a release

```bash
python -m cdsi_reference_tools supporting-data import --source <path-to-a-supporting-data-*.zip-or-a-directory-of-them>
python -m cdsi_reference_tools supporting-data list
```

Only files matching `supporting-data-*.zip` are ever registered as a real CDSi release (see `tools/cdsi_reference_tools/supporting_data.py`'s `SOURCE_ZIP_GLOB`) - `cdsi-engine/src/main/resources/supporting-data/` also bundles a non-CDC schedule (e.g. a demo/preview set) for the web UI's benefit, and that is deliberately never picked up here. The release id is derived from the filename itself (`supporting-data-4.65-508.zip` -> `"4.65"`), cross-checked against the version named in the zip's own internal top-level folder; a mismatch is recorded as a manifest warning, not silently ignored.

Re-running `import` against an already-registered release is safe: it verifies the new file's checksum matches what's on record rather than re-extracting or overwriting anything. A genuinely different file for the same release id is refused - register a new release id instead, the same rule `logic-spec` uses for Logic Specification versions.

Update `current-version.yaml` once a newly-registered release is ready to be the active one - a plain YAML file, not a symlink, mirroring `logic-spec/current-version.yaml`.
