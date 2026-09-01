# cdsi-reference

Versioned, agent-readable copies of the two sources `cdsi-engine` is built from - the CDC CDSi **Logic Specification** and CDSi **Supporting Data** - deterministically extracted, organized by processing step, and mapped to the engine's actual classes and tests.

This is a **development and documentation asset**. It is never a runtime dependency of `cdsi-engine`, `cdsi-web`, or `cdsi-fits-tests`, and it makes no network calls and no LLM calls - extraction is mechanical PDF parsing; anything that looks like interpretation (a step's plain-language walkthrough, a review finding) was drafted by a human or an agent working *outside* this tool and is marked `draft` until reviewed.

Status: **Phases 1-9** of the design plan are complete - version 4.6 is registered, the full deterministic extractor runs across Chapter 3 and Chapters 4-9 with zero warnings, every executable step in Chapters 4-9 has its own package (34 step packages plus 6 chapter-overview indexes), all nine required concept documents exist under `logic-spec/versions/4.6/concepts/`, all six required process-loop diagrams (figure + structured `transitions.yaml` + Mermaid + explanation) exist under `logic-spec/versions/4.6/diagrams/`, `mappings/spec-to-code.yaml` is cross-checked against `cdsi-engine`'s actual source by `logic-spec validate`, and every real, independently-verified gap found so far is now a formal record under `logic-spec/versions/4.6/findings/`.

This pass surfaced a number of real, independently-verified `cdsi-engine` gaps and bugs - tracked deliberately, not fixed yet (this is a demonstration/reference system; the project owner wants a dedicated fix pass once the full documentation and testing system is in place, not fixes mixed into documentation work). Each now has its own `finding.yaml`/`finding.md` pair under `logic-spec/versions/4.6/findings/`; most significant:
- **[SPEC-4.6-0001](logic-spec/versions/4.6/findings/SPEC-4.6-0001/finding.md): Section 7.3 (Determine Contraindications) is essentially unimplemented** - no decision-table logic exists in code at all.
- **[SPEC-4.6-0002](logic-spec/versions/4.6/findings/SPEC-4.6-0002/finding.md): `LogicTable.evaluate()`, the one shared mechanism every decision table in the system uses, never enforces that exactly one column matched** - the safety check is commented out, so if a table's conditions aren't perfectly mutually exclusive, every matching outcome fires with the last one silently winning.
- **[SPEC-4.6-0003](logic-spec/versions/4.6/findings/SPEC-4.6-0003/finding.md): `EvaluateGender`'s gender-match condition unconditionally returns YES** after its matching loop regardless of whether anything matched, making the documented "incorrect gender" rejection outcome dead code - found while building the Phase 8 mapping, since this class (section label `"xx"`) doesn't correspond to any real specification subsection.
- [SPEC-4.6-0004](logic-spec/versions/4.6/findings/SPEC-4.6-0004/finding.md) through [0007](logic-spec/versions/4.6/findings/SPEC-4.6-0007/finding.md): narrower scoring/date/reason-code bugs in Chapters 6 and 8 (`EvaluatePreferableInterval`, `InProcessPatientSeries`+`NoValidDoses`'s shared Date reference-equality bug, `CompletePatientSeries`, `NoValidDoses`'s scoring copy-paste and two undocumented conditions).
- **`ForecastDatesAndReasons` enforces a third, previously-undocumented loop guard** (`MAX_FORECAST_HANDOFF_CYCLES = 1100`) - found via Phase 8, since this class (section label `"7"`, a bare chapter number) is the real Chapter 6-to-7 orchestration handoff, not the non-executable chapter-overview text it was treated as during Phase 5. Recorded in `mappings/spec-to-code.yaml`'s `unmapped_classes`, not as a formal finding - it isn't a mismatch, ambiguity, or conflict under Phase 9's taxonomy, just an implementation detail with no spec correspondence.

Phase 8's validator (`logic-spec validate`) checks every `LogicStepType` cdsi-engine can actually instantiate against `mappings/spec-to-code.yaml`, flagging any class the mapping doesn't cite, any mapping entry citing a class or file that doesn't exist, and any step package missing a mapping entry. Two classes are acknowledged as known, deliberately-tracked gaps (see `unmapped_classes` above) rather than left as an unexplained validator failure; the command exits 0 while still printing them. The same command now also validates every finding record under `findings/` against `schemas/finding.schema.json`.

Version comparison (Phase 10) is documented but deliberately not implemented - see "Comparing two versions" below for why and what to do about it. Supporting Data versioning and FITS run history are not yet built either. See `StepIntoCDSi-Specification-Reference-Module-Plan.md` (repository root, one level above this module) for the full 24-phase design.

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
| `supporting-data/`, `reference-sets/` | Not built yet - see their own README.md placeholders |

## Setup

```bash
cd cdsi-reference
python -m venv .venv
.venv/Scripts/pip install -e ".[dev]"   # Windows; use .venv/bin/pip on macOS/Linux
```

Use Python 3.11, not the newest installed Python - PyMuPDF and PyYAML's prebuilt wheels lag behind the very latest CPython release, and building them from source needs a C/C++ toolchain you probably don't want to install just for this. `requirements-lock.txt` records the exact versions this was built and tested against.

## Commands

```bash
python -m cdsi_reference_tools logic-spec extract --version 4.6
python -m cdsi_reference_tools logic-spec validate --version 4.6
python -m cdsi_reference_tools logic-spec compare --from 4.6 --to 4.7   # not implemented yet - prints the Phase 10 checklist and exits 1; see "Comparing two versions" below
```

Run the tests with `pytest tests/` (they exercise the real 4.6 PDF and skip cleanly if it isn't registered yet).

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

## Finding the documentation for a CDSi step

Step packages live at `logic-spec/versions/<version>/steps/<chapter>-<section>-<slug>/` (e.g. `06-04-evaluate-age/`). Each has `index.md` (the full write-up - source citations, business rules, decision tables, implementation notes, review findings), `step.yaml` (structured metadata), `transitions.yaml` (structured next-step data), and `figures/` (the reviewed figure crops referenced from `index.md`).

## Mapping a section to engine code and tests

Check `mappings/spec-to-code.yaml` first. If a section isn't listed there yet, cross-reference `cdsi-engine`'s `org.openimmunizationsoftware.cdsi.core.logic.LogicStepType` (every constant is declared with its own section number and title, e.g. `EVALUATE_AGE("6.4", "Evaluate Age", true)`) and `LogicStepFactory` (an exhaustive, mechanical `if (stepName.equals(...)) return new <Class>(...)` chain) - together they are the authoritative, verifiable spec-to-class mapping; never invent a class name that isn't read directly from one of those two files. `tools/cdsi_reference_tools/engine_index.py` parses both files programmatically and backs `logic-spec validate`'s mapping checks (Phase 8) - use it instead of re-parsing by hand. Not every `LogicStepType` corresponds to a real specification subsection (two don't - see `unmapped_classes` in `mappings/spec-to-code.yaml`); don't force one of those into a fake numbered step package.

## Reporting an ambiguity or a suspected mismatch

Note it in the step package's "Review Findings" section first (see `templates/step.md`) - that's still the right place for something small, or not yet fully investigated. Promote anything substantial into its own record under `logic-spec/versions/<version>/findings/<id>/`: a `finding.yaml` (machine-readable, validated against `schemas/finding.schema.json`) and a `finding.md` (human-readable narrative), using `templates/finding.md` as the starting point. `id` is `SPEC-<version>-<four-digit sequence>` (e.g. `SPEC-4.6-0008`) - `tools/cdsi_reference_tools/findings.py`'s `next_finding_id(version)` allocates the next one by scanning existing finding directories, not a separate counter file.

Use the four-category taxonomy: `IMPLEMENTATION_MISMATCH` (StepIntoCDSi appears inconsistent with the specification), `SPECIFICATION_AMBIGUITY` (the specification doesn't establish a clear implementable result), `SUPPORTING_DATA_CONFLICT` (Logic Specification and Supporting Data appear inconsistent), `FITS_DIFFERENCE` (FITS expectations appear inconsistent with the implementation or another source). A class with no corresponding specification section at all (like `EvaluateGender` or `ForecastDatesAndReasons` - see `mappings/spec-to-code.yaml`'s `unmapped_classes`) doesn't automatically need a finding of its own; only file one for an actual mismatch/ambiguity/conflict/difference, not for the mapping gap itself.

A finding stays `draft`/`open` until a human (or a reviewed process) confirms it - a failing FITS test alone does not justify `confirmed`. Never resolve an ambiguity by assuming one source is correct - record it and let a human confirm it. See `logic-spec/versions/4.6/findings/` for seven real examples recorded during this documentation pass.

## Known extraction limitations

- `extract_tables.py`'s wrapped-row merge handles the two patterns actually seen in Chapters 4-9's decision tables (a condition that wraps within column 0; several outcome columns that wrap on the same blank-label continuation row). A table with two independent paragraphs inside one outcome cell - seen once, in Table 8-3, outside the reviewed pilot scope - doesn't merge as cleanly. When in doubt, compare the extracted `.txt` against the rendered PDF page (or the cropped table image, written automatically when structure can't be recovered with any confidence) before trusting it for anything beyond a reviewed step package.
- The specification's own front-matter (List of Figures and Tables) is missing a handful of tables that genuinely exist in the document body (seen for Tables 6-11, 6-19, and 7-8 while documenting Chapters 6-7) - since extraction is driven by that front matter, those tables were never auto-extracted and had to be transcribed by hand into their step packages from the raw section text. `extract_tables.py`/`build_index.py` aren't changed to detect this class of gap automatically yet.
- Fixed during the Phase 5 pass (kept here for context, not because it's still open): the last in-scope section/figure/table of Chapters 4-9 originally had no upper page bound and would run into the Appendices - `toc.find_first_appendix_page()` now bounds it at the real "Appendix A:" heading.
