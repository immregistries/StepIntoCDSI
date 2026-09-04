# cdsi-reference

Versioned, agent-readable copies of the two sources `cdsi-engine` is built from - the CDC CDSi **Logic Specification** and CDSi **Supporting Data** - deterministically extracted, organized by processing step, and mapped to the engine's actual classes and tests.

This is a **development and documentation asset**. It is never a runtime dependency of `cdsi-engine`, `cdsi-web`, or `cdsi-fits-tests`, and it makes no network calls and no LLM calls - extraction is mechanical PDF parsing; anything that looks like interpretation (a step's plain-language walkthrough, a review finding) was drafted by a human or an agent working *outside* this tool and is marked `draft` until reviewed.

Status: **Phases 1-9 and 11-16** of the design plan are complete (Phase 10, version comparison, is documented but deliberately not built yet - see below) - version 4.6 is registered, the full deterministic extractor runs across Chapter 3 and Chapters 4-9 with zero warnings, every executable step in Chapters 4-9 has its own package (34 step packages plus 6 chapter-overview indexes), all nine required concept documents exist under `logic-spec/versions/4.6/concepts/`, all six required process-loop diagrams (figure + structured `transitions.yaml` + Mermaid + explanation) exist under `logic-spec/versions/4.6/diagrams/`, `mappings/spec-to-code.yaml` is cross-checked against `cdsi-engine`'s actual source by `logic-spec validate`, every real, independently-verified gap found so far is a formal record under `logic-spec/versions/4.6/findings/`, and `logic-spec validate` now checks everything Phase 11 requires (manifest checksum and page count, every step package's structure/figures/tables/transitions, the spec-to-code mapping, and every finding record) with network access physically blocked for the whole `logic-spec` command tree, not just claimed unnecessary.

This pass surfaced a number of real, independently-verified `cdsi-engine` gaps and bugs - tracked deliberately, not fixed yet (this is a demonstration/reference system; the project owner wants a dedicated fix pass once the full documentation and testing system is in place, not fixes mixed into documentation work). Each now has its own `finding.yaml`/`finding.md` pair under `logic-spec/versions/4.6/findings/`; most significant:
- **[SPEC-4.6-0001](logic-spec/versions/4.6/findings/SPEC-4.6-0001/finding.md): Section 7.3 (Determine Contraindications) is essentially unimplemented** - no decision-table logic exists in code at all.
- **[SPEC-4.6-0002](logic-spec/versions/4.6/findings/SPEC-4.6-0002/finding.md): `LogicTable.evaluate()`, the one shared mechanism every decision table in the system uses, never enforces that exactly one column matched** - the safety check is commented out, so if a table's conditions aren't perfectly mutually exclusive, every matching outcome fires with the last one silently winning.
- **[SPEC-4.6-0003](logic-spec/versions/4.6/findings/SPEC-4.6-0003/finding.md): `EvaluateGender`'s gender-match condition unconditionally returns YES** after its matching loop regardless of whether anything matched, making the documented "incorrect gender" rejection outcome dead code - found while building the Phase 8 mapping, since this class (section label `"xx"`) doesn't correspond to any real specification subsection.
- [SPEC-4.6-0004](logic-spec/versions/4.6/findings/SPEC-4.6-0004/finding.md) through [0007](logic-spec/versions/4.6/findings/SPEC-4.6-0007/finding.md): narrower scoring/date/reason-code bugs in Chapters 6 and 8 (`EvaluatePreferableInterval`, `InProcessPatientSeries`+`NoValidDoses`'s shared Date reference-equality bug, `CompletePatientSeries`, `NoValidDoses`'s scoring copy-paste and two undocumented conditions).
- **`ForecastDatesAndReasons` enforces a third, previously-undocumented loop guard** (`MAX_FORECAST_HANDOFF_CYCLES = 1100`) - found via Phase 8, since this class (section label `"7"`, a bare chapter number) is the real Chapter 6-to-7 orchestration handoff, not the non-executable chapter-overview text it was treated as during Phase 5. Recorded in `mappings/spec-to-code.yaml`'s `unmapped_classes`, not as a formal finding - it isn't a mismatch, ambiguity, or conflict under Phase 9's taxonomy, just an implementation detail with no spec correspondence.

Phase 8's validator (`logic-spec validate`) checks every `LogicStepType` cdsi-engine can actually instantiate against `mappings/spec-to-code.yaml`, flagging any class the mapping doesn't cite, any mapping entry citing a class or file that doesn't exist, and any step package missing a mapping entry. Two classes are acknowledged as known, deliberately-tracked gaps (see `unmapped_classes` above) rather than left as an unexplained validator failure; the command exits 0 while still printing them. The same command now also validates every finding record under `findings/` against `schemas/finding.schema.json`.

Version comparison for the Logic Specification (Phase 10) is documented but deliberately not implemented, since only one version (4.6) has ever been registered - see "Comparing two versions" below. Supporting Data, by contrast, has two real registered releases, so its whole pipeline (registration, normalization, comparison, and now reference sets - Phases 13-16) is built and tested against them: `supporting-data compare --from 4.64 --to 4.65` finds 400 real changes, including a large systematic one (135 live-virus-conflict end intervals shortened from 30 to 28 days) - see `supporting-data/README.md`. One active reference set (`acip-4.6-sd-4.65-fits-8183b45d`) binds the current Logic Specification version, the latest Supporting Data release, and the real FITS fixture set - `cdsi-fits-tests` now verifies its checksums before every run and fails clearly if either has drifted (see `reference-sets/README.md`), and every run writes a structured diagnostic bundle (Phase 17 - see `cdsi-fits-tests/README.md`). Phase 20's agent repair runbook is built (`cdsi-fits-tests/AGENTS.md`) and already produced one confirmed, merged fix (`SPEC-4.6-0007`) - which in turn showed a step class can be genuinely wrong with zero effect on any FITS case, motivating Phase 21: a separate, systematic pass giving every step class its own dedicated spec-conformance JUnit test, independent of FITS pass rate (`cdsi-engine/AGENTS.md`, tracked in `step-tests/status.yaml` - see `python -m cdsi_reference_tools step-tests status --version 4.6`). Phases 18-19 (structured tracing and a case-level regression baseline) are not yet built - the plan's original Phases 18-24 were reviewed and reduced after Phase 17 landed; see the plan document's "Revision history" for why. See `StepIntoCDSi-Specification-Reference-Module-Plan.md` (repository root, one level above this module) for the full 21-phase design.

Phase 12 (this pass) audited every piece of this module's own documentation against the plan's checklist rather than assuming earlier phases kept it current: `logic-spec/versions/4.6/{concepts,diagrams,findings}/README.md` still said "not yet built" for work finished phases ago, and `mappings/spec-to-code.yaml`'s header comment still said Chapter 9 wasn't mapped - all four are now corrected to describe what actually exists. `templates/*.md` and `supporting-data/README.md`/`reference-sets/README.md` (genuinely not started - Phases 13+) were checked too and needed no changes.

## What's authoritative, generated, or reviewed

| Path | Treatment |
| --- | --- |
| `logic-spec/versions/<v>/source/` | Immutable - the exact source PDF, never edited or replaced in place |
| `logic-spec/versions/<v>/extracted/` | Fully generated by `logic-spec extract`; safe to delete and regenerate |
| `logic-spec/versions/<v>/steps/*/step.yaml`, `index.md`, `transitions.yaml` | Drafted (by a human or an agent), reviewed, and hand-maintained - `logic-spec extract` never touches these |
| `logic-spec/versions/<v>/index.md` | Generated - the extraction inventory (section/figure/table list and warnings) |
| `mappings/spec-to-code.yaml` | Hand-maintained; every class name in it has been read directly from `cdsi-engine`'s source, and `logic-spec validate` now cross-checks it against that source directly (Phase 8) |
| `logic-spec/versions/<v>/findings/<id>/finding.yaml`, `finding.md` | Hand-maintained (a human or an agent proposes a finding; it stays `draft`/`open` until a human confirms it) - `logic-spec validate` checks structure, not truth |
| `logic-spec/diffs/` | Not built yet (Phase 10 - see "Comparing two versions" above); would be generated by `logic-spec compare` once implemented |
| `supporting-data/versions/<release-id>/source/` | Immutable - the exact original release zip plus its categorized (`xml/`, `xsd/`, `spreadsheets/`, `release-notes/`) contents, both preserved; never edited in place |
| `supporting-data/versions/<release-id>/manifest.yaml` | Generated once at registration (Phase 13) by `supporting-data import`; every file checksummed, verified (not overwritten) on re-import; `normalizer_version`/`normalized_at`/`warnings` updated by every `supporting-data normalize` run |
| `supporting-data/versions/<release-id>/normalized/`, `documentation/` | Fully generated by `supporting-data normalize` (Phase 14); safe to delete and regenerate, deterministically |
| `supporting-data/diffs/<old>-to-<new>.{json,md}` | Fully generated by `supporting-data compare` (Phase 15); safe to delete and regenerate |
| `reference-sets/<id>.yaml` | Hand-reviewed but tool-generated (Phase 16) by `reference-set create`; immutable once created - a changed binding gets a new id, never an edit |
| `cdsi-fits-tests/src/test/resources/reference-set.json` | Generated by `reference-set export`; the one-way, reviewed snapshot `cdsi-fits-tests`' Java code actually reads |
| `step-tests/status.yaml` | Hand-maintained by the Role A/Role B agents in `cdsi-engine/AGENTS.md` (Phase 21); `step-tests sync` only ever adds missing units, never edits or removes an existing one; pass/fail counts are never stored here - always read live from `cdsi-engine`'s surefire reports |
| `step-tests/cross-cutting-notes.md` | Hand-maintained by Role A/Role B agents (Phase 21) - a small, dated log for a discovery whose reach plausibly extends beyond the one unit that found it (a shared framework class, a domain object more than one step depends on); not a finding, not a substitute for a unit's own `status.yaml` notes, and not itself a Role B execution plan - see its own instructions at the top of the file |
| `dashboards/step-tests.html`, `dashboards/fits-results.html`, `dashboards/index.html` | Fully generated (`step-tests dashboard`, `fits-tests dashboard`, `dashboards index`); committed on purpose so the project owner can share progress with the team via GitHub Pages - regenerate and commit again whenever there's something new to show, never hand-edited |

## Setup

```bash
cd cdsi-reference
python -m venv .venv
.venv/Scripts/pip install -e ".[dev]"   # Windows; use .venv/bin/pip on macOS/Linux
```

Use Python 3.11, not the newest installed Python - PyMuPDF, PyYAML, and lxml's prebuilt wheels lag behind the very latest CPython release, and building them from source needs a C/C++ toolchain you probably don't want to install just for this. `requirements-lock.txt` records the exact versions this was built and tested against.

## Commands

```bash
python -m cdsi_reference_tools logic-spec extract --version 4.6
python -m cdsi_reference_tools logic-spec validate --version 4.6
python -m cdsi_reference_tools logic-spec compare --from 4.6 --to 4.7   # not implemented yet - prints the Phase 10 checklist and exits 1; see "Comparing two versions" below
python -m cdsi_reference_tools supporting-data import --source <path>   # see "Supporting Data releases" below
python -m cdsi_reference_tools supporting-data list
python -m cdsi_reference_tools supporting-data validate --release 4.65
python -m cdsi_reference_tools supporting-data normalize --release 4.65
python -m cdsi_reference_tools supporting-data compare --from 4.64 --to 4.65
python -m cdsi_reference_tools reference-set create --logic-spec 4.6 --supporting-data 4.65   # see "Reference sets" below
python -m cdsi_reference_tools step-tests sync --version 4.6     # see "Per-step spec-conformance tests" below
python -m cdsi_reference_tools step-tests status --version 4.6
python -m cdsi_reference_tools step-tests dashboard --version 4.6
python -m cdsi_reference_tools fits-tests dashboard
python -m cdsi_reference_tools dashboards index
```

`extract` is always safe to rerun for an already-registered version - it deletes and fully rebuilds `logic-spec/versions/<v>/extracted/` from the checked-in source PDF alone (verifying the manifest checksum first), and never touches anything else in that version's directory (`steps/`, `concepts/`, `diagrams/`, `findings/`, `index.md`, `manifest.yaml`). Rerun it after a change to `extract.py`/`extract_tables.py`/`extract_figures.py`/`split_sections.py`/`build_index.py`, or if you ever suspect `extracted/` drifted from the source; `tests/test_extraction_invariants.py` proves both of those properties (determinism and reviewed-content isolation) against the real 4.6 PDF.

Run the tests with `pytest tests/` (they exercise the real 4.6 PDF and skip cleanly if it isn't registered yet; the full run takes under a minute, mostly one deliberately real end-to-end extraction re-run - see "No network access, no LLM" below).

## No network access, no LLM (Phase 11)

This has been a design principle since Phase 1 (see the intro above), and Phase 11 turned it from a claim into something enforced and tested. `cli.py`'s `main()` calls `network_guard.install()` before dispatching to any subcommand - it monkeypatches `socket.socket.connect()` to raise `NetworkAccessDisabledError`, which is enough to block every HTTP/network library and every LLM SDK at once, since all of them ultimately open a real socket to reach anything off this machine. Every `logic-spec extract/validate/compare` invocation runs with this guard active, not just in tests.

`tests/test_network_guard.py` checks both directions: that the guard actually blocks a real connection attempt, and - the more important proof - that `logic-spec validate` completes normally against the real 4.6 version with the guard active, confirming validation itself has no hidden network dependency. If you ever need a script that legitimately talks to a live service (like `FitsDownloader` in `cdsi-fits-tests`, or a future PDF-fetching helper here), it must live outside the `logic-spec`/`supporting-data` command trees entirely - never reachable from `extract`, `validate`, or `compare`.

## Adding a new Logic Specification version

1. Create `logic-spec/versions/<version>/source/`, copy the exact new PDF into it - never overwrite an existing version's PDF.
2. Run `logic-spec extract --version <version>` - it computes and writes `manifest.yaml` (checksum, page count) the first time it sees a version, and verifies the checksum on every later run.
3. Review the generated `logic-spec/versions/<version>/index.md` for warnings before trusting the extraction.
4. Follow Phase 4 of the plan document: validate two pilot sections by hand before writing/updating any other step packages.
5. Update `logic-spec/current-version.yaml` once the new version is ready to be the active one - it's a plain YAML file, not a symlink, so this works identically on Windows and Unix.

If you're only registering a new version to *replace* 4.6 as current (no one needs the two compared), the five steps above are all you need - stop there. Read on only if you also need to compare the two versions.

## Comparing two versions (Phase 10)

**Not implemented.** `logic-spec compare --from <old> --to <new>` exists only as a stub (`tools/cdsi_reference_tools/compare_versions.py`) that fails with `NotImplementedError` and prints the checklist below, rather than a working comparison. This is deliberate, not an oversight: every extraction bug found during Phases 3-5 (appendix-scope leakage, wrapped-row table merging, numeric vs. lexicographic sorting) was only caught by testing against the real 4.6 PDF - a diff implementation designed against a plan document, with no second real version to run it against, would look plausible and be wrong in ways nothing here would catch. Version 4.6 has been the only registered version for this module's entire existence, so Phase 10 was documented, not built, per the project owner's explicit direction.

**When a second version is actually being added, do this, in order** (also embedded in `compare_versions.py` as `PHASE_10_STEPS`, and printed by the CLI when you run `logic-spec compare`):

1. Add the new PDF under `logic-spec/versions/<new>/source/` - never overwrite an existing version's PDF.
2. Calculate its `manifest.yaml` metadata and checksum (`logic-spec extract` already does this for a newly-registered version).
3. Run deterministic extraction (`logic-spec extract --version <new>`) and review its `index.md` for warnings before trusting it.
4. Match content between `<old>` and `<new>` using section, table, figure, and business-rule identifiers - never page numbers alone (they shift between versions).
5. Compare normalized text and structured data (`step.yaml`/`transitions.yaml` fields, extracted table text) per matched identifier, not a line-by-line PDF text diff - page wrapping and layout changes create noise a raw text diff can't distinguish from real content changes.
6. Generate `logic-spec/diffs/<old>-to-<new>.md` (human-readable) and `logic-spec/diffs/<old>-to-<new>.json` (machine-readable companion) - see that directory's own README.md.
7. Identify which step packages changed (any of: step description, attribute table, decision table, process transition, business rules).
8. Cross-reference `mappings/spec-to-code.yaml` to list which engine classes and tests a changed step package could affect.
9. Carry a reviewed step package's plain-language walkthrough/interpretation forward into the new version **only** when none of its own source dependencies (section text, tables, figures, transitions) changed - otherwise mark it for re-review rather than copying it forward silently.
10. Mark every changed step package's `review_status` as needing review again, even if the change looks cosmetic - a human decides that, not the diff tool.

The change report (`<old>-to-<new>.md`) should read like this, one entry per step:

```text
6.4 Evaluate Age
- Step description: unchanged
- Attribute table: modified
- Decision table: modified
- Process transition: unchanged
- Business rules: one modified
- Mapped engine code: EvaluateAge
- Review required: yes
```

Build and validate this against the real two versions as you go - the same pilot-then-scale discipline Phase 4 used for extraction (two sections by hand before trusting it across Chapters 4-9) applies here too. Once it's real and tested, update this section, the Status line at the top of this README, and `compare_versions.py`'s module docstring to say so.

## Supporting Data releases (Phases 13-15)

A separate versioned tree from `logic-spec/`, since the Logic Specification and Supporting Data change on different schedules and don't have a one-to-one version relationship - see `supporting-data/README.md` for the full detail. In short:

```bash
python -m cdsi_reference_tools supporting-data import --source cdsi-engine/src/main/resources/supporting-data
python -m cdsi_reference_tools supporting-data list
python -m cdsi_reference_tools supporting-data validate --release 4.65
python -m cdsi_reference_tools supporting-data normalize --release 4.65
python -m cdsi_reference_tools supporting-data compare --from 4.64 --to 4.65
```

Only `supporting-data-*.zip` files are ever registered as a real CDSi release - **not** an alternative schedule like the demo/preview set `cdsi-engine` also bundles for the web UI. This mirrors the same rule `cdsi-fits-tests`' `DefaultSupportingDataSet` and `cdsi-web`'s `SupportingDataManager` already use for picking the standard CDC set: comparisons and defaults are only meaningful against real published CDSi releases, and mixing in an alternative schedule would misrepresent both. Releases 4.64 and 4.65 are registered, normalized, and compared.

`normalize` validates every antigen's XML against the release's own XSD (via `lxml`), then converts it into agent-readable JSON (`normalized/`) and Markdown (`documentation/`) - a faithful, deterministic translation of the source tree, not a redesigned model; see `supporting-data/README.md`'s "Normalizing a release" for what it deliberately does and doesn't cover (notably: it never cross-checks the XML against the parallel `.xlsx` spreadsheets in the same release).

`compare` matches both releases' normalized data by stable domain identifier (antigen name, series name, dose number, CVX code, vaccine-group name, observation code) and writes `supporting-data/diffs/<old>-to-<new>.{json,md}`. Renamed items show up as a removal plus an addition, never a detected rename - see `supporting-data/README.md`'s "Comparing two releases" for why that's correct, not a bug, and for what a Supporting Data diff does and doesn't imply about FITS results.

## Reference sets (Phase 16)

```bash
python -m cdsi_reference_tools reference-set create --logic-spec 4.6 --supporting-data 4.65
python -m cdsi_reference_tools reference-set list
python -m cdsi_reference_tools reference-set validate --id acip-4.6-sd-4.65-fits-8183b45d
python -m cdsi_reference_tools reference-set export --id acip-4.6-sd-4.65-fits-8183b45d
```

A reference set (`reference-sets/<id>.yaml`) binds a Logic Specification version, a Supporting Data release, and the FITS fixture set currently committed in `cdsi-fits-tests`, each with its own verified checksum - not bare version numbers. `export` writes a plain JSON snapshot into `cdsi-fits-tests/src/test/resources/reference-set.json`; that module's `ReferenceSetVerifier` re-derives the Supporting Data and fixture-set checksums before every FITS run and fails clearly (an `IllegalStateException` from the `@TestFactory` method, not a silent pass) if either has drifted since the reference set was created. This is a one-way, reviewed export - `cdsi-fits-tests` never reads `cdsi-reference`'s files directly at test time, and `cdsi-reference` never becomes its runtime dependency.

The Logic Specification binding is recorded but not verified in Java - `cdsi-engine` has no runtime marker of which specification version its code implements, so there's no artifact to check it against; that traceability job belongs to `mappings/spec-to-code.yaml` and the findings under `logic-spec/versions/<v>/findings/`, not this checksum. See `reference-sets/README.md` for the currently-active reference set.

## Per-step spec-conformance tests (Phase 21)

```bash
python -m cdsi_reference_tools step-tests sync --version 4.6     # adds any unit spec-to-code.yaml has that status.yaml doesn't yet
python -m cdsi_reference_tools step-tests status --version 4.6   # run `mvn -pl cdsi-engine test` first for fresh counts
```

`step-tests/status.yaml` tracks, per unit in `mappings/spec-to-code.yaml` (36 for version 4.6: 34 numbered sections plus the 2 `unmapped_classes`), whether a dedicated JUnit test class isolating that step exists yet (`test_status`) and whether the implementation has been brought in line with it and merged (`fix_status`) - independent of FITS: the goal is a spec-conformance test per step class, not a change to FITS pass rate. `status` never caches pass/fail counts; it reads them live from `cdsi-engine/target/surefire-reports/` every time, and surfaces any `blocked` unit - one a fixing agent determined it could not safely resolve itself, either because the real defect looks like it belongs to a different step (`blocked_category: upstream_step_defect`) or because fixing it would regress something else (`would_regress_other_tests`) - before the full table. `logic-spec validate` cross-checks `status.yaml` against its schema and against `spec-to-code.yaml`. The full two-role (write tests / fix step) workflow, including the cross-step escalation rule, is `cdsi-engine/AGENTS.md` - not duplicated here.

`step-tests/cross-cutting-notes.md` is where a discovery that looks bigger than the one unit that found it gets recorded - a shared framework defect, a domain object several steps depend on, an accumulating value nothing resets. Before Role B execution begins across many units, the project owner reviews this file together with every `blocked_category: upstream_step_defect` unit to decide a deliberate fix order, since a shared or foundational fix made once can retroactively resolve red tests in units nobody has even reached yet.

`step-tests dashboard` renders the same information as a single self-contained static HTML file (`dashboards/step-tests.html` by default) - progress totals with bars, blocked units surfaced first, and a filterable/sortable table, all inline CSS/JS with no external dependencies or network access. Unlike the disposable diagnostic bundles, this file **is committed on purpose** - the project owner regenerates it after review-worthy progress and commits it, so a coworker can open it straight from the repository (or GitHub's rendered-file view) without running anything locally. Git history of this one file's diffs is the progress record for now, not a promoted historical archive or a trend chart - see the plan's Non-Goals for why that machinery isn't built yet.

## FITS results dashboard

```bash
python -m cdsi_reference_tools fits-tests dashboard   # writes dashboards/fits-results.html from the latest run
```

Renders the most recent `cdsi-fits-tests/target/fits-runs/<run-id>/` bundle (Phase 17) into the same kind of committed, self-contained static HTML file: overall pass/fail/error/skipped totals and pass rate, a breakdown by vaccine group (the FITS fixture's `group`, e.g. `HepA`, `MMR`, `COVID-19`), and - collapsed under each group, filterable by case id or text - the actual field-level difference for every one of its non-passing cases. It reads whatever run bundle is already on disk; run `mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest` first for a fresh one. Like `step-tests dashboard`, this is a snapshot, not a live view or a trend chart - regenerate and commit it again whenever there's real progress to show.

`dashboards index` writes a small landing page (`dashboards/index.html`) linking both, with their headline numbers (units-with-tests-written/merged/blocked, FITS pass rate) pulled live from whatever `status.yaml` and the latest FITS bundle currently say - so it never goes stale relative to the other two, as long as you regenerate it after them. This is what's published at the repository's GitHub Pages root; see `StepIntoCDSi-Specification-Reference-Module-Plan.md`'s Revision history for the `develop` branch this is served from.

## Finding the documentation for a CDSi step

Step packages live at `logic-spec/versions/<version>/steps/<chapter>-<section>-<slug>/` (e.g. `06-04-evaluate-age/`). Each has `index.md` (the full write-up - source citations, business rules, decision tables, implementation notes, review findings), `step.yaml` (structured metadata), `transitions.yaml` (structured next-step data), and `figures/` (the reviewed figure crops referenced from `index.md`).

## Mapping a section to engine code and tests

Check `mappings/spec-to-code.yaml` first. If a section isn't listed there yet, cross-reference `cdsi-engine`'s `org.openimmunizationsoftware.cdsi.core.logic.LogicStepType` (every constant is declared with its own section number and title, e.g. `EVALUATE_AGE("6.4", "Evaluate Age", true)`) and `LogicStepFactory` (an exhaustive, mechanical `if (stepName.equals(...)) return new <Class>(...)` chain) - together they are the authoritative, verifiable spec-to-class mapping; never invent a class name that isn't read directly from one of those two files. `tools/cdsi_reference_tools/engine_index.py` parses both files programmatically and backs `logic-spec validate`'s mapping checks (Phase 8) - use it instead of re-parsing by hand. Not every `LogicStepType` corresponds to a real specification subsection (two don't - see `unmapped_classes` in `mappings/spec-to-code.yaml`); don't force one of those into a fake numbered step package.

## Reporting an ambiguity or a suspected mismatch

Note it in the step package's "Review Findings" section first (see `templates/step.md`) - that's still the right place for something small, or not yet fully investigated. Promote anything substantial into its own record under `logic-spec/versions/<version>/findings/<id>/`: a `finding.yaml` (machine-readable, validated against `schemas/finding.schema.json`) and a `finding.md` (human-readable narrative), using `templates/finding.md` as the starting point. `id` is `SPEC-<version>-<four-digit sequence>` (e.g. `SPEC-4.6-0008`) - `tools/cdsi_reference_tools/findings.py`'s `next_finding_id(version)` allocates the next one by scanning existing finding directories, not a separate counter file.

Use the one finding taxonomy - the same categories whether the finding came from documenting the specification or from investigating a failing FITS case in `cdsi-fits-tests` (never a second, parallel taxonomy for the latter):

- `IMPLEMENTATION_MISMATCH` - StepIntoCDSi appears inconsistent with the specification.
- `SPECIFICATION_AMBIGUITY` - the specification doesn't establish a clear implementable result.
- `SUPPORTING_DATA_CONFLICT` - Logic Specification and Supporting Data appear inconsistent.
- `FITS_DIFFERENCE` - FITS expectations appear inconsistent with the implementation or another source.
- `SPECIFICATION_DEFECT` - the specification's text or table is clear but appears to be a genuine error, not merely ambiguous - report this back to CDC/CDSi.
- `FIXTURE_IMPORT_DEFECT` - the FITS fixture itself was captured or converted incorrectly by this project's own tooling (`FitsDownloader`), not a defect in FITS, the specification, or the engine.
- `UNDETERMINED` - investigated with the available evidence, but a root cause could not be established (distinct from `SPECIFICATION_AMBIGUITY`, which means the specification itself is unclear).
- `NOT_REPRODUCIBLE` - the failure does not reliably reproduce.

A class with no corresponding specification section at all (like `EvaluateGender` or `ForecastDatesAndReasons` - see `mappings/spec-to-code.yaml`'s `unmapped_classes`) doesn't automatically need a finding of its own; only file one for an actual mismatch/ambiguity/conflict/difference, not for the mapping gap itself. A FITS-case investigation that matches an existing finding's root cause adds the case's id to that finding's `fits_cases` list rather than creating a duplicate.

A finding stays `draft`/`open` until a human (or a reviewed process) confirms it - a failing FITS test alone does not justify `confirmed`. Never resolve an ambiguity by assuming one source is correct - record it and let a human confirm it. See `logic-spec/versions/4.6/findings/` for seven real examples recorded during this documentation pass.

## Known extraction limitations

- `extract_tables.py`'s wrapped-row merge handles the two patterns actually seen in Chapters 4-9's decision tables (a condition that wraps within column 0; several outcome columns that wrap on the same blank-label continuation row). A table with two independent paragraphs inside one outcome cell - seen once, in Table 8-3, outside the reviewed pilot scope - doesn't merge as cleanly. When in doubt, compare the extracted `.txt` against the rendered PDF page (or the cropped table image, written automatically when structure can't be recovered with any confidence) before trusting it for anything beyond a reviewed step package.
- The specification's own front-matter (List of Figures and Tables) is missing a handful of tables that genuinely exist in the document body (seen for Tables 6-11, 6-19, and 7-8 while documenting Chapters 6-7) - since extraction is driven by that front matter, those tables were never auto-extracted and had to be transcribed by hand into their step packages from the raw section text. `extract_tables.py`/`build_index.py` aren't changed to detect this class of gap automatically yet; `logic-spec validate` knows about these three specifically (`validate.py`'s `TABLES_MISSING_FROM_LOFT`) so it doesn't report them as a new problem, but it can't discover a fourth one on its own - if you find another table missing from the LOFT while documenting a future chapter or version, add it there too.
- Fixed during the Phase 5 pass (kept here for context, not because it's still open): the last in-scope section/figure/table of Chapters 4-9 originally had no upper page bound and would run into the Appendices - `toc.find_first_appendix_page()` now bounds it at the real "Appendix A:" heading.
