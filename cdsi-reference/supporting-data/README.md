# Supporting Data

Versioned, checksummed copies of CDSi Supporting Data releases - the XML/XSD/spreadsheet bundle `cdsi-engine` actually loads at runtime - kept separate from `logic-spec/` since the two resources change on different schedules and don't have a one-to-one version relationship.

Status: **Phase 13** (source preservation) is complete for releases 4.64 and 4.65 - both registered under `versions/<release-id>/source/`, categorized into `xml/`, `xsd/`, `spreadsheets/`, and `release-notes/`, with the exact original zip preserved alongside them and every file checksummed in `manifest.yaml`. Phases 14-15 (parsing the XML into agent-readable structured data under `normalized/`/`documentation/`, and semantic diffing between releases) are not built yet - see `StepIntoCDSi-Specification-Reference-Module-Plan.md` at the repository root.

## Registering a release

```bash
python -m cdsi_reference_tools supporting-data import --source <path-to-a-supporting-data-*.zip-or-a-directory-of-them>
python -m cdsi_reference_tools supporting-data list
```

Only files matching `supporting-data-*.zip` are ever registered as a real CDSi release (see `tools/cdsi_reference_tools/supporting_data.py`'s `SOURCE_ZIP_GLOB`) - `cdsi-engine/src/main/resources/supporting-data/` also bundles a non-CDC schedule (e.g. a demo/preview set) for the web UI's benefit, and that is deliberately never picked up here. The release id is derived from the filename itself (`supporting-data-4.65-508.zip` -> `"4.65"`), cross-checked against the version named in the zip's own internal top-level folder; a mismatch is recorded as a manifest warning, not silently ignored.

Re-running `import` against an already-registered release is safe: it verifies the new file's checksum matches what's on record rather than re-extracting or overwriting anything. A genuinely different file for the same release id is refused - register a new release id instead, the same rule `logic-spec` uses for Logic Specification versions.

Update `current-version.yaml` once a newly-registered release is ready to be the active one - a plain YAML file, not a symlink, mirroring `logic-spec/current-version.yaml`.
