# StepIntoCDSi Reference, Supporting Data, and Test History Plan

## Objective

After the StepIntoCDSi multi-module split is complete, add a fourth top-level peer named `cdsi-reference`.

This module will contain versioned, agent-readable copies of the two principal sources used by the engine: the CDC Clinical Decision Support for Immunization Logic Specification and CDSi Supporting Data. It will preserve the original source files, deterministically extract and normalize their contents, organize them into reviewable Markdown and structured data, represent processing loops in machine-readable diagrams, record changes across releases, and map specification sections and Supporting Data concepts to the corresponding StepIntoCDSi engine code and tests.

After the Logic Specification portion is complete, extend `cdsi-fits-tests` with reproducible run evidence, case-level regression baselines, promoted historical results, investigation records, and generated trend charts. This will allow a coding agent to move from a failing FITS case to the exact engine behavior, specification section, Supporting Data release, prior test behavior, and relevant source code.

`cdsi-reference` is a development and documentation asset. It must not become a runtime dependency of `cdsi-engine`, `cdsi-web`, or `cdsi-fits-tests`. Test-run history belongs in `cdsi-fits-tests`, not in a fifth module.

## Resulting Repository Structure

The repository should have four top-level peers:

```text
StepIntoCDSi/
├── pom.xml
├── cdsi-engine/
├── cdsi-web/
├── cdsi-fits-tests/
└── cdsi-reference/
```

`cdsi-reference` is a module in the architectural and repository sense. It does not need to be a Java library or participate in runtime dependency resolution. Add it to the root Maven reactor only if its validation can be made useful and portable through Maven. Do not add an empty POM merely to make the directory look like a Java module.

The root project README should describe all four modules and state that `cdsi-reference` supports development, review, specification-to-code comparison, Supporting Data comparison, and reproducible interpretation of FITS results.

## Preconditions

Do not begin this work until the three-module application split is complete enough that the following are true:

- `cdsi-engine`, `cdsi-web`, and `cdsi-fits-tests` exist as stable top-level modules.
- The engine runs without an HTTP request or deployed servlet container.
- The current full build succeeds.
- The web application continues to run against the extracted engine.
- FITS tests can run locally against `cdsi-engine`, or the remaining FITS work is clearly documented.
- The working tree is clean.

Before starting this plan:

1. Synchronize with the repository's current integration branch.
2. Run the complete build and record the baseline result.
3. Create a new branch with a clear name such as `cdsi-reference`.
4. Do not mix unrelated clinical-logic fixes into the initial module scaffolding.

## Core Design Principles

### Preserve the Source

Every specification version must retain the exact source document from which the derived files were created. Store its checksum and metadata. Never replace an older version with a newer PDF.

### Separate Extraction from Interpretation

The module must distinguish among:

1. Source material and mechanically extracted content.
2. Normalized structured representations of steps, tables, and transitions.
3. Plain-language explanations, implementation mappings, and review findings.

Generated content must be replaceable. Reviewed content must not be silently overwritten by regeneration.

### Retain Traceability

Every derived item must identify its specification version, section number, title, source pages, and relevant figure, table, or business-rule identifiers.

### Use Markdown and Structured Data Together

Markdown is the primary human- and agent-readable format. YAML or JSON should hold stable identities, transitions, mappings, hashes, and other information that must be validated or compared automatically.

### Keep Clinical Interpretation Reviewable

Automation may draft explanations and identify apparent inconsistencies, but it must not silently decide that the specification, StepIntoCDSi, Supporting Data, or FITS is correct when those sources disagree.

## Target Module Structure

Create the following initial structure. Build the Logic Specification portion first; the Supporting Data and reference-set directories may initially contain README placeholders describing the next stage.

```text
cdsi-reference/
├── README.md
├── AGENTS.md
├── pyproject.toml
├── tools/
│   └── cdsi_reference_tools/
│       ├── __init__.py
│       ├── cli.py
│       ├── extract.py
│       ├── split_sections.py
│       ├── extract_figures.py
│       ├── extract_tables.py
│       ├── build_index.py
│       ├── compare_versions.py
│       └── validate.py
├── schemas/
│   ├── manifest.schema.json
│   ├── step.schema.json
│   ├── transition.schema.json
│   └── mapping.schema.json
├── mappings/
│   └── spec-to-code.yaml
├── templates/
│   ├── step.md
│   ├── concept.md
│   └── finding.md
├── logic-spec/
│   ├── current-version.yaml
│   ├── versions/
│   │   └── 4.6/
│   │       ├── source/
│   │       ├── manifest.yaml
│   │       ├── extracted/
│   │       │   ├── full-text.txt
│   │       │   ├── sections/
│   │       │   ├── figures/
│   │       │   └── tables/
│   │       ├── concepts/
│   │       ├── steps/
│   │       ├── diagrams/
│   │       ├── findings/
│   │       └── index.md
│   └── diffs/
├── supporting-data/
│   ├── current-version.yaml
│   ├── versions/
│   └── diffs/
└── reference-sets/
```

Use Python for the extraction utilities unless the repository already has a better portable document-processing standard. Lock dependencies in `pyproject.toml` and its lock file. The deterministic extraction process must not require an LLM or external web service.

Recommended library categories include:

- PDF text and page access.
- Table extraction.
- Image extraction and cropping.
- YAML and JSON processing.
- JSON Schema validation.

Select the smallest dependable set after testing against the version 4.6 PDF. Document any non-Python system dependencies explicitly.

## Generated and Reviewed Boundaries

Mark each directory clearly:

| Directory | Treatment |
| --- | --- |
| `logic-spec/*/source/` | Immutable source for that specification version |
| `logic-spec/*/extracted/` | Fully generated and replaceable |
| `logic-spec/*/concepts/` | Agent-drafted or manually written; requires review |
| `logic-spec/*/steps/` | Normalized and reviewed step documentation |
| `logic-spec/*/diagrams/` | Original images are generated; transitions and Mermaid require review |
| `supporting-data/*/source/` | Immutable XML, XSD, spreadsheet, and release source files |
| `supporting-data/*/normalized/` | Generated, agent-readable Supporting Data representation |
| `reference-sets/` | Reviewed bindings among specification, Supporting Data, and FITS fixture versions |
| `findings/` | Reviewed analysis and issue records |
| `mappings/` | Maintained with the application code |

Add generated-file notices where appropriate. Extraction commands may delete and rebuild `extracted/`, but must refuse to overwrite reviewed files unless an explicit update workflow is used.

## Phase 1: Scaffold the Module

1. Create `cdsi-reference` as a top-level peer.
2. Add the directory structure, README, AGENTS instructions, templates, and schemas.
3. Add the module to the repository overview.
4. Add commands for installing tools, extracting a version, validating a version, and comparing two versions.
5. Decide how developers will invoke the tools consistently. A suitable interface would be:

```bash
python -m cdsi_reference_tools logic-spec extract --version 4.6
python -m cdsi_reference_tools logic-spec validate --version 4.6
python -m cdsi_reference_tools logic-spec compare --from 4.6 --to 4.7
```

6. Add a test directory for the extraction and validation utilities.

Do not begin by converting all 151 pages. First establish and verify the format using pilot sections.

## Phase 2: Register Specification Version 4.6

Copy the exact version 4.6 source PDF into:

```text
logic-spec/versions/4.6/source/logic-spec-acip-rec-4.6.pdf
```

Create `manifest.yaml` containing at least:

```yaml
specification: "Logic Specification for ACIP Recommendations"
version: "4.6"
publication_date: "2024-12-13"
source_filename: "logic-spec-acip-rec-4.6.pdf"
sha256: "<calculated value>"
page_count: 151
extraction_tool_version: "<tool version or commit>"
extracted_at: "<UTC timestamp>"
```

Calculate values from the source rather than copying assumptions into the manifest. Set `logic-spec/current-version.yaml` to version 4.6 without using a filesystem symlink, so the repository behaves consistently on Windows and Unix-like systems.

The PDF is small enough to store directly in Git. Do not introduce Git LFS unless repository growth later makes it necessary.

## Phase 3: Build Deterministic Extraction

The extraction command should perform the following operations without using an LLM:

1. Verify the source checksum.
2. Extract page-aware text while retaining page boundaries.
3. Identify numbered headings and their page ranges.
4. Split mechanically extracted text into section files.
5. Extract embedded figures at their original resolution when possible.
6. Associate figures with captions, figure identifiers, and source pages.
7. Extract table text and geometry when practical.
8. Store a page image or cropped image for tables whose structure cannot be reconstructed reliably.
9. Build an inventory of all sections, figures, tables, and detected business-rule identifiers.
10. Record warnings for anything that could not be mapped confidently.

The extraction should produce stable filenames based on source identifiers, for example:

```text
extracted/sections/06-04-evaluate-age.txt
extracted/figures/figure-06-05.png
extracted/tables/table-06-14.txt
extracted/tables/table-06-14.png
```

Do not use page numbers as the sole identity. Page numbers may change between versions. Use section, table, figure, and rule identifiers wherever available, with page information retained as provenance.

## Phase 4: Validate Two Pilot Sections

Before processing every chapter, complete two pilot sections:

- **Section 4.1, Gather Necessary Data:** validates orchestration and overall process documentation.
- **Section 6.4, Evaluate Age:** validates a typical calculation step with attributes, business rules, decision tables, figures, outcomes, and transitions.

For each pilot:

1. Compare extracted text against the rendered PDF pages.
2. Verify heading, page, figure, and table associations.
3. Create the structured step metadata.
4. Create the Markdown step document.
5. Represent its transitions in YAML.
6. Create or verify the Mermaid diagram.
7. Map the section to its current `cdsi-engine` implementation.
8. Record unclear or missing specification details without resolving them silently.

Stop and refine the extractor, schemas, and templates until both pilots are dependable. Do not scale an unreliable pilot across the entire document.

## Standard Step Package

Create one directory for each executable processing step:

```text
steps/06-04-evaluate-age/
├── index.md
├── step.yaml
├── transitions.yaml
└── figures/
```

### `step.yaml`

Use a structure similar to:

```yaml
spec_version: "4.6"
section: "6.4"
title: "Evaluate Age"
source_pages: [52, 53]
figures:
  - "Figure 6-5"
tables:
  - "Table 6-14"
  - "Table 6-15"
business_rules: []
implementation:
  module: "cdsi-engine"
  classes:
    - "<verified class path>"
review_status: "draft"
```

Do not invent class paths or rule identifiers. Populate them only after inspecting the specification and code.

### `index.md`

Use this standard structure:

```markdown
# 6.4 Evaluate Age

## Source

Version, pages, figures, tables, and rule identifiers.

## Purpose

Plain-language explanation of what this step determines.

## Entry Conditions

What must already be established before the step runs.

## Inputs and Attributes

Values read or calculated by the step.

## Business Rules

Rule identifiers and their defined calculations.

## Decision Tables

Faithful representation of conditions and outcomes.

## State Changes

Statuses, dates, collections, or other engine state modified by each outcome.

## Next Steps

Conditional transitions to subsequent processing steps.

## Plain-Language Walkthrough

Developer-oriented explanation of the complete step.

## StepIntoCDSi Implementation

Classes, methods, structured logs, and tests corresponding to the section.

## Review Findings

Ambiguities, implementation differences, Supporting Data questions, or unresolved issues.
```

Clearly label source-derived statements, explanatory interpretation, and implementation observations. Do not allow an agent reader to mistake a project interpretation for normative specification text.

## Phase 5: Extract and Document All Processing Steps

After the pilots pass validation:

1. Extract Chapters 4 through 9 by numbered section.
2. Create one package for every executable step.
3. Preserve the chapter overview and process-step tables.
4. Normalize attribute, business-rule, decision-table, state-change, and transition information.
5. Create a chapter index linking all subordinate steps.
6. Record review status for every step.

The initial target sections include:

- Chapter 4: overall processing and orchestration.
- Chapter 5: creation of relevant patient series.
- Chapter 6: evaluation of administered doses.
- Chapter 7: forecast dates and reasons.
- Chapter 8: selection of the best patient series.
- Chapter 9: identification and evaluation of vaccine groups.

Do not treat introductory chapters and appendices as executable steps. Route them into concept and reference documentation instead.

## Phase 6: Create Concept Documentation

Create reviewed Markdown files under `concepts/` for material that spans multiple steps. At minimum include:

```text
concepts/
├── overall-processing-model.md
├── target-dose.md
├── statuses.md
├── selecting-supporting-data.md
├── date-calculations.md
├── decision-tables.md
├── patient-series.md
├── vaccine-groups.md
└── domain-model.md
```

`overall-processing-model.md` should explain the end-to-end engine flow in plain language:

1. Gather patient, history, observation, and schedule information.
2. Organize immunization history.
3. Select relevant patient series.
4. Evaluate administered doses against target doses.
5. Forecast incomplete patient series.
6. select the best patient series for each series group.
7. Consolidate results into vaccine-group evaluations and forecasts.

Explain where processing repeats, what collection is being iterated, and what condition causes each loop to terminate.

The concept files should link to the authoritative step packages and source pages rather than duplicating entire decision tables.

## Phase 7: Model Repetition and Transitions

The original PDF figures must be retained, but images alone are insufficient for agents and automated comparison.

For every important process loop, create:

1. The original extracted figure.
2. A reviewed `transitions.yaml` representation.
3. A Mermaid diagram derived from or checked against the transitions.
4. A short Markdown explanation of the iteration unit, entry point, exit condition, and state affected.

Begin with:

- Overall Chapter 4 processing flow.
- Relevant-patient-series selection loop.
- Chapter 6 dose-evaluation loop.
- Chapter 6 to Chapter 7 evaluation-and-forecast loop.
- Chapter 8 series-group and prioritized-series loops.
- Chapter 9 vaccine-group evaluation flow.

Use structured transition data such as:

```yaml
from: "6.4"
transitions:
  - condition: "<verified condition>"
    to: "6.5"
  - condition: "<verified condition>"
    to: "end-dose-evaluation"
```

Do not infer an unlabeled transition merely to make the diagram complete. Record it as an unresolved finding when the source does not establish the transition clearly.

The Appendix A neighborhood diagrams are complex domain models rather than simple processing loops. Initially extract and index them as images with accompanying glossary links. Converting them into structured domain models should be a separate, reviewed task.

## Phase 8: Map the Specification to the Engine

Create and maintain `mappings/spec-to-code.yaml`.

Example structure:

```yaml
spec_version: "4.6"
sections:
  "6.4":
    title: "Evaluate Age"
    implementation:
      module: "cdsi-engine"
      classes:
        - "<verified fully qualified class name>"
    tests:
      - "<verified test class or FITS group>"
    mapping_status: "reviewed"
```

Build a validator that reports:

- Specification steps without mapped engine code.
- Logic-step classes without mapped specification sections.
- Step packages without implementation mappings.
- Mappings that reference missing classes or files.
- Steps without tests or an explicit test-gap status.

After the mapping format is stable, consider adding a lightweight annotation such as `@SpecSection("6.4")` to engine step classes. Do not add this during the extraction pilot unless it clearly improves validation without coupling runtime behavior to the documentation module.

The engine must not load the specification module at runtime. Mapping validation is a development and CI activity.

## Phase 9: Record Findings Without Conflating Sources

Create a finding format that distinguishes at least four categories:

- `IMPLEMENTATION_MISMATCH`: StepIntoCDSi appears inconsistent with the specification.
- `SPECIFICATION_AMBIGUITY`: The specification does not establish a clear implementable result.
- `SUPPORTING_DATA_CONFLICT`: Logic Specification and Supporting Data appear inconsistent.
- `FITS_DIFFERENCE`: FITS expectations appear inconsistent with the implementation or another source.

Each finding should contain:

```yaml
id: "SPEC-4.6-0001"
status: "open"
category: "SPECIFICATION_AMBIGUITY"
spec_sections: ["6.4"]
source_pages: [52, 53]
tables: []
business_rules: []
code_locations: []
fits_cases: []
summary: "<short description>"
evidence: "<source-based evidence>"
interpretation: "<clearly labeled analysis>"
```

An agent may propose a finding, but the finding remains draft until reviewed. A failing FITS test does not by itself prove that engine code is wrong.

### Amendment (adopted during Phase 17 review): one finding system, not two

The original design for Phase 21 ("Add Investigation Records") specified a second, parallel taxonomy and a separate `cdsi-fits-tests/investigations/<case-id>/` directory for conclusions reached while investigating a failing FITS case. That phase has been folded into this one instead: **there is one finding format and one taxonomy**, used both for issues found while documenting the specification and for issues found while investigating a FITS case. A FITS-case investigation either links the case's id into an existing finding's `fits_cases` list, or creates a new finding if the root cause is novel - it never creates a second kind of record.

Four categories are added to the four above, covering investigation outcomes that documentation-time review didn't need:

- `SPECIFICATION_DEFECT`: the specification's own text or table is clear, but appears to be a genuine error, not merely ambiguous - report this back to CDC/CDSi rather than working around it silently.
- `FIXTURE_IMPORT_DEFECT`: the FITS fixture itself was captured or converted incorrectly by this project's own tooling (`FitsDownloader`), not a defect in FITS, the specification, or the engine.
- `UNDETERMINED`: investigated with the available evidence, but a root cause could not be established. Not the same as `SPECIFICATION_AMBIGUITY` (that means the specification itself is unclear); this means the investigation was inconclusive.
- `NOT_REPRODUCIBLE`: the failure does not reliably reproduce (for example, it depends on the current date, or was a transient environment issue).

The original design's `ENGINE_DEFECT`, `SUPPORTING_DATA_DEFECT`, and `FITS_EXPECTATION_DEFECT` are not added as separate categories - they are `IMPLEMENTATION_MISMATCH`, `SUPPORTING_DATA_CONFLICT`, and `FITS_DIFFERENCE` respectively, under the names already in use.

## Phase 10: Build Version Comparison

The specification update workflow must create a new version rather than editing the prior snapshot.

When a future version is added:

1. Add the new PDF under a new version directory.
2. Calculate its metadata and checksum.
3. Run deterministic extraction.
4. Match content using section, table, figure, and rule identifiers.
5. Compare normalized text and structured data.
6. Generate `logic-spec/diffs/<old>-to-<new>.md` and a machine-readable companion file.
7. Identify changed step packages.
8. Use the code mapping to list potentially affected engine classes and tests.
9. Carry reviewed explanations forward only when their source dependencies are unchanged.
10. Mark all changed derived content as requiring review.

A change report should distinguish:

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

Do not rely on a line-by-line PDF text diff as the primary comparison. Page wrapping and layout changes create noise. Compare by stable source identifiers and normalized content.

## Phase 11: Validation and Continuous Integration

Add automated validation that can run without network access or an LLM.

At minimum validate:

- Source file exists and matches the manifest checksum.
- Page count and version metadata are consistent.
- Every detected numbered processing section has an extracted section file.
- Every step package has valid front matter and structured metadata.
- Referenced figures and tables exist.
- Transition targets resolve to known steps or defined terminal states.
- Structured YAML and JSON satisfy their schemas.
- Specification-to-code mappings point to existing repository locations.
- Generated files are reproducible from the same source and tool version.
- Reviewed files are not overwritten during extraction.

If the Python toolchain is not part of the Maven reactor, give it a separate CI job. The repository's main verification workflow should run both the Java build and the reference-module validation, but neither should create a runtime dependency on the other.

## Phase 12: Documentation for Developers and Agents

The module README must explain:

- Why the module exists.
- Which content is authoritative source, generated extraction, or reviewed interpretation.
- How to add a specification version.
- How to regenerate extracted content.
- How to validate the module.
- How to compare two versions.
- How to find the documentation for a particular CDSi step.
- How to map a section to engine code and tests.
- How to report ambiguity or a suspected mismatch.

The module's `AGENTS.md` should direct coding agents to:

1. Read the relevant step package before changing clinical logic.
2. Inspect the cited original PDF pages, tables, and figures when meaning is uncertain.
3. Review the specification-to-code mapping.
4. Run the smallest relevant test first.
5. Run the related FITS group and then the full suite.
6. Classify discrepancies rather than assuming the implementation is wrong.
7. Record unresolved specification issues as findings.
8. Avoid editing generated extraction manually.

## Logic Specification Milestone

Complete and validate Phases 1 through 12 before implementing the following Supporting Data and test-history phases. The Logic Specification milestone is reached when:

- Version 4.6 is registered with its source PDF and checksum.
- Chapters 4 through 9 have been extracted and organized by step.
- The overall processing model and primary loops are documented.
- Step packages retain source provenance and code mappings.
- Extraction and validation are reproducible.

Commit this milestone before broadening the module. The next phases should extend the established tooling and schemas rather than destabilize the completed Logic Specification work.

## Phase 13: Add Versioned Supporting Data

Create a separate versioned collection under `cdsi-reference/supporting-data`. Do not place Supporting Data inside a Logic Specification version directory because the two resources change on different schedules and may not have a one-to-one version relationship.

Use the following structure for each Supporting Data release:

```text
supporting-data/
├── current-version.yaml
├── versions/
│   └── <release-id>/
│       ├── source/
│       │   ├── xml/
│       │   ├── xsd/
│       │   ├── spreadsheets/
│       │   └── release-notes/
│       ├── manifest.yaml
│       ├── normalized/
│       │   ├── antigens/
│       │   ├── schedules/
│       │   ├── patient-series/
│       │   ├── target-doses/
│       │   ├── intervals/
│       │   ├── vaccines/
│       │   ├── contraindications/
│       │   ├── observations/
│       │   └── index.json
│       ├── documentation/
│       │   ├── index.md
│       │   └── antigens/
│       ├── findings/
│       └── validation/
└── diffs/
```

Derive the release identifier from authoritative release metadata when available. If the source has no stable published identifier, use an ISO release or retrieval date plus a short content hash. Do not label two different source bundles with the same version.

Preserve all supplied XML, XSD, spreadsheets, and release notes. Calculate a checksum for every file and a deterministic checksum for the complete bundle. The release manifest should include:

```yaml
release_id: "<verified release identifier>"
source: "<source description>"
published_at: "<date if known>"
retrieved_at: "<UTC timestamp>"
bundle_sha256: "<calculated value>"
files:
  - path: "source/xml/<file>"
    sha256: "<calculated value>"
normalizer_version: "<tool version or commit>"
normalized_at: "<UTC timestamp>"
warnings: []
```

Credentials, temporary downloads, and private access information must never be committed.

## Phase 14: Normalize Supporting Data for Agents

Add deterministic commands such as:

```bash
python -m cdsi_reference_tools supporting-data import --release <release-id> --source <path>
python -m cdsi_reference_tools supporting-data normalize --release <release-id>
python -m cdsi_reference_tools supporting-data validate --release <release-id>
python -m cdsi_reference_tools supporting-data compare --from <old> --to <new>
```

The normalizer should:

1. Validate source XML against the supplied XSD where possible.
2. Parse the source into project-owned structured models.
3. Preserve source identifiers and relationships.
4. Write canonical JSON or YAML using stable ordering and formatting.
5. Generate indexes by antigen, vaccine group, schedule, patient series, target dose, and other major domain concepts present in the release.
6. Retain the source file and source location for every normalized item.
7. Record conversion warnings rather than silently dropping unrecognized fields.
8. Produce agent-readable Markdown summaries from the normalized model.

The normalized form must be a faithful representation, not a redesigned schedule model. Any convenience fields or resolved references added by the project must be labeled as derived.

For each antigen or major schedule collection, generate documentation that helps an agent answer:

- Which patient series exist?
- Which target doses and intervals apply?
- Which vaccine types are preferable, allowable, or inadvertent?
- Which ages, conditional skips, recurring doses, observations, contraindications, or other conditions are represented?
- Which source files and identifiers define the values?
- What changed from the preceding release?

Do not treat spreadsheet content as automatically superior to XML or vice versa. When parallel source formats disagree, record a `SUPPORTING_DATA_CONFLICT` finding with evidence from both.

## Phase 15: Compare Supporting Data Releases

Build semantic comparison based on stable domain identifiers rather than raw XML line differences. The comparison should identify at least:

- Added and removed antigens, vaccine groups, schedules, patient series, target doses, or vaccines.
- Changed ages, intervals, effective dates, cessation dates, and dose numbers.
- Changed preferable, allowable, or inadvertent vaccine relationships.
- Changed conditions, observations, contraindications, recurring-dose behavior, or series selection attributes.
- Schema additions, removals, or cardinality changes.
- Source-format disagreements introduced or resolved.

Generate both machine-readable and Markdown reports:

```text
supporting-data/diffs/<old>-to-<new>.json
supporting-data/diffs/<old>-to-<new>.md
```

Every change entry should link to the old normalized item, new normalized item, source files, and affected specification or engine mappings when known.

Do not assume every Supporting Data change should alter an existing FITS result. Record potential impact and verify it through testing.

## Phase 16: Define Reproducible Reference Sets

Create reviewed files under `cdsi-reference/reference-sets` that bind together the inputs required to interpret a test run:

```yaml
id: "acip-4.6-sd-<release>-fits-<fixture-set>"
logic_spec: "4.6"
supporting_data: "<release-id>"
fits_fixture_set: "<fixture-set-id>"
created_at: "<UTC timestamp>"
status: "active"
notes: "<compatibility or review notes>"
```

Reference sets must use verified identifiers and checksums. They should be immutable after being used by a promoted test run. If a binding changes, create a new reference-set identifier.

Modify `cdsi-fits-tests` so every run records one reference-set identifier. The test harness must fail clearly when the requested Logic Specification, Supporting Data, or fixture set is missing or does not match the recorded checksums.

## Phase 17: Produce a Structured Diagnostic Bundle for Every Run

Extend `cdsi-fits-tests` so all normal local executions write complete run output under an ignored build directory:

```text
cdsi-fits-tests/target/fits-runs/<run-id>/
├── run.json
├── summary.json
├── results.jsonl
├── changed-cases.json
├── failures/
│   └── <case-id>/
│       ├── input.json
│       ├── expected.json
│       ├── actual.json
│       ├── difference.json
│       └── trace.jsonl
└── logs/
```

The run identifier should include a timestamp, abbreviated Git commit, and reference-set identifier. Sanitize it for use as a directory name on supported operating systems.

`run.json` must include:

- Run identifier and timestamps.
- Git commit and dirty-tree status.
- Branch when available.
- Java and build-tool versions.
- Reference-set identifier.
- Logic Specification, Supporting Data, and FITS fixture checksums.
- Invocation and test-selection filters.
- Engine configuration that could affect results.

`summary.json` must distinguish:

- Discovered cases.
- Executed cases.
- Passed cases.
- Failed assertions.
- Execution errors.
- Skipped cases.
- Known failures.
- New regressions.
- Newly passing cases.
- Changed known failures.
- Added or removed cases.

Each entry in `results.jsonl` must include:

- Case identifier and group.
- Execution status.
- Expected-result hash and actual-result hash.
- Normalized expected and actual outcomes or references to them.
- Field-level difference classification.
- Duration.
- Baseline comparison.
- Failure-bundle path when applicable.

The normal local output is disposable and must not be committed automatically.

## Phase 18: Extend Structured Tracing for Diagnostics

**Revised after the Phase 17 review** (see the "Revision history" note at the end of this document): scaled back from a ground-up structured-event redesign, because most of it already exists. `cdsi-engine`'s `LogicStepSink`/`LogEvent` already captures a sequenced, leveled log per `LogicStep` instance, and log messages already cite business rule IDs and calculated values. What's actually missing is smaller:

1. Tag each `LogEvent` with the `LogicStepType` it was logged from (and, when `mappings/spec-to-code.yaml` maps that type, the specification section) - a step type isn't currently recorded on the event itself.
2. Walk the full chain of `LogicStep` instances one forecast run actually passed through (`dataModel.getLogicStep()` at each iteration of `FitsEngineRunner`'s loop) and assemble their event lists, in order, into one run-level trace - today each step's `LogicStepSink` is scoped to that one step alone.
3. Write that assembled trace into `trace.jsonl`, one line per event, inside `cdsi-fits-tests`'s Phase 17 diagnostic bundle - the file that bundle's design already reserved a slot for but never populated.

Do not fabricate a rule, table, or section identifier for an event that doesn't already cite one - a missing field is more honest than a guessed one. The result should let an agent find, from `trace.jsonl` alone: the last step where the trace looks unremarkable, and the first step whose outcome plausibly explains the case's actual-vs-expected divergence.

## Phase 19: Establish the Case-Level Regression Baseline

Create a machine-readable baseline under:

```text
cdsi-fits-tests/history/baseline.yaml
```

Record every case independently. A baseline entry should include:

```yaml
case_id: "<case-id>"
group: "<group>"
baseline_status: "failed"
result_hash: "<normalized result hash>"
finding_id: null
reference_set: "<reference-set-id>"
```

`case_id` is `<testPlanId>-<groupName>-<uid>`, matching Phase 17's `FitsRunRecorder` - `<groupName>-<uid>` alone collides for real across different NIST test plans (confirmed: 1053 of 4896 real fixtures). `finding_id` is null until investigated, then names the Phase 9 finding (extended per that phase's Amendment) that explains this case. There is no separate classification taxonomy here - a case's classification *is* whichever finding, if any, lists it in that finding's `fits_cases`. A case with no `finding_id` is uninvestigated, full stop.

Define regression behavior precisely:

- Passing to failing or error is a regression.
- Passing to skipped is a regression unless explicitly approved.
- Known failure to a different failure is changed behavior requiring review.
- Known failure to passing is an improvement.
- Failed to skipped is not an improvement.
- Added or removed cases are test-corpus changes requiring explanation.

Do not use total pass percentage as the regression oracle. One newly fixed case and one newly broken case can leave the percentage unchanged.

Provide a reviewed command for intentionally updating the baseline. Normal test execution must never rewrite it automatically.

## Phase 20: Define the Agent Repair Workflow

Add a runbook to `cdsi-fits-tests/AGENTS.md` directing an agent to:

1. Confirm a clean repository and a valid reference set (Phase 16).
2. Run the complete suite, or load the case-level baseline (Phase 19) to see what's already known.
3. Select one failing case for diagnosis.
4. Reproduce it without code changes.
5. Read its input, expected result, actual result, and field-level difference (Phase 17's bundle) and its structured trace (Phase 18's `trace.jsonl`).
6. Identify the earliest meaningful divergence.
7. Read the mapped Logic Specification step, original source citations, normalized Supporting Data, and mapped engine code (`mappings/spec-to-code.yaml`).
8. Classify the likely discrepancy before editing, using the one finding taxonomy (Phase 9, as amended) - never a second, ad hoc one.
9. Add a focused unit test when the defect can be isolated below the FITS level.
10. Make the smallest general correction; never special-case a FITS identifier.
11. Run the focused test and selected FITS case.
12. Run the related FITS group.
13. Run all engine and FITS tests.
14. Compare the complete results with the case-level baseline.
15. Record the finding (create a new one, or add this case's id to an existing finding's `fits_cases`) and stop for the project owner's review before committing a clinical-logic change - this workflow is agent-assisted with a human in the loop, not autonomous CI that merges on green. Document-only changes (a finding, a baseline update) may proceed without waiting.

The agent must not:

- Change FITS expected results merely to obtain a passing test.
- Modify generated specification or Supporting Data extraction manually.
- Assume FITS is authoritative when it conflicts with specification evidence.
- Treat a failure as isolated without checking other cases changed by the fix.
- Accept an unchanged total pass rate as proof that no regression occurred.
- Continue making speculative changes when clinical or specification authority is required.
- Commit a clinical-logic fix without the project owner's review, even when every test the agent can run passes.

Define stop conditions for source conflicts, missing data, non-reproducible behavior, unresolved clinical interpretation, or unexplained regressions.

One case at a time is a diagnostic selection strategy, not an assumption that every case has a separate root cause. When one correction changes multiple cases, treat them as a related cluster and review every changed outcome.

## Phase 21: Per-Step Spec-Conformance Test Coverage

Phase 20's runbook diagnoses one failing FITS case at a time - a real end-to-end scenario, tested against the whole engine. It says nothing about whether any individual step class, in isolation, actually does what its own specification section requires; a class can look fine because nothing in the current FITS fixture set happens to exercise its broken branch (as `SPEC-4.6-0007` demonstrated - a real, confirmed scoring defect with zero effect on any of 4,896 fixtures). Phase 21 is a separate, systematic pass that closes that gap directly: give every executable step class its own dedicated JUnit test, red or green, independent of what FITS currently proves or fails to prove.

The unit of work is one entry in `mappings/spec-to-code.yaml` (Phase 8) - a numbered spec section with a step package, or an `unmapped_classes` entry. There are 36 such units for version 4.6. Each is worked in two separate passes, run as separate agent sessions on the project owner's own schedule (not automated, not batched):

- **Role A - write the tests.** Reads the unit's step package and implementation class(es); writes one JUnit test per business rule / decision-table row, asserting the specification directly; touches no production code. A red result here is an expected, useful outcome - it is the first honest evidence that a step doesn't do what its spec says, something FITS alone may never surface. Reports the initial red/green count and stops.
- **Role B - fix the step.** Runs later, only after Role A's test class is reviewed and merged. Classifies each red test with the one finding taxonomy (Phase 9, as amended) before touching code, makes the smallest fix, and explicitly does not chase FITS pass-rate movement this round - `known-passing-cases.txt` (see cdsi-fits-tests's allowlist) is not touched or regenerated during this workflow. Runs the full `cdsi-engine` suite and the full FITS suite before and after; any regression anywhere is an automatic stop, not a judgment call.

**Cross-step escalation.** Before accepting a red test as a defect in the assigned step's own class, Role B checks whether the actual cause is upstream or downstream - another step populating or consuming shared state (a `PatientSeries` field, a loop-control flag, an orchestration handoff) incorrectly, using the step's own documented State Changes and `LogicStepFactory`'s dispatch order to trace it. If the evidence points at a different step's class, the agent must not fix it there - that decision, and its own before/after regression check, belongs to whoever later runs that other step's own Role B session. Instead it records a finding whose `code_locations` names the other class, with the evidence and a recommendation, and marks its own unit blocked. This is how a step is allowed to say "I can't improve myself without breaking something else" without silently working around it or silently fixing something out of scope.

**Tracking.** `cdsi-reference/step-tests/status.yaml` (schema: `schemas/step-test-status.schema.json`) holds exactly what a test run can't tell you for each unit: `test_status` (`not_started`/`tests_written`), `fix_status` (`not_started`/`in_progress`/`fixed_pending_review`/`merged`/`blocked`), and - only when blocked - `blocked_category` (`would_regress_other_tests`/`upstream_step_defect`/`undetermined`) and `blocked_reason`, plus any `finding_ids` raised. Pass/fail/error/skipped counts are never cached there; the `step-tests status` CLI command (`python -m cdsi_reference_tools step-tests status --version 4.6`) always reads those live from `cdsi-engine`'s own surefire reports and renders one table, with blocked units surfaced first so the project owner sees what needs a decision before the full per-unit list. `step-tests sync` adds any unit the mapping has that the status file doesn't yet, without touching existing entries. `logic-spec validate` cross-checks `status.yaml` against the schema and against `spec-to-code.yaml` (every unit accounted for, `blocked_category`/`blocked_reason`/`test_class` present exactly when the corresponding status requires them).

**Cross-cutting risk.** Some of what a unit's Role A or Role B session finds isn't really about that one unit - a shared framework class more than one step depends on, a domain object several steps read, or a fact (an accumulating value nothing resets) whose consequences could reach units nobody has tested yet. `cdsi-reference/step-tests/cross-cutting-notes.md` is where that gets recorded, distinct from a unit's own `status.yaml` notes and from a formal finding - a dated entry naming the shared component, what's known to be affected, and what's only suspected. Before Role B execution begins in earnest across many units, the project owner reviews this file together with every `blocked_category: upstream_step_defect` unit to decide a deliberate order: a shared or foundational fix, once made, can retroactively resolve red tests in units nobody has even reached yet, where fixing narrowly in spec-section order risks re-discovering (or separately working around) the same root cause more than once. That sequencing decision itself isn't defined by this file - it's the evidence base one gets built from, once enough of Chapter 4 through 9 has a Role A pass to make a real decision from.

The full two-role workflow, including the "must not" list, lives in `cdsi-engine/AGENTS.md` - this section is the plan-level summary, not a duplicate of the runbook itself.

## Suggested Commit Sequence

Keep the work reviewable through a series of focused commits:

1. Scaffold `cdsi-reference` and document boundaries.
2. Add version 4.6 source and manifest.
3. Add deterministic text, section, and image extraction.
4. Add extraction and schema validation.
5. Complete and review Section 4.1 pilot.
6. Complete and review Section 6.4 pilot.
7. Extract remaining Chapters 4 through 9.
8. Add concept documentation.
9. Add structured transitions and repetition diagrams.
10. Add specification-to-code mappings.
11. Add findings workflow.
12. Add version comparison.
13. Add Logic Specification CI validation and developer documentation.
14. Add Supporting Data source registration and manifests.
15. Add Supporting Data normalization and validation.
16. Add Supporting Data semantic comparison and documentation.
17. Add immutable reference sets.
18. Add complete disposable FITS run bundles.
19. Extend structured tracing and wire it into the diagnostic bundle.
20. Establish the case-level regression baseline.
21. Add the agent repair runbook.
22. Add per-step spec-conformance test tracking (`step-tests/status.yaml`, its schema, and the `step-tests` CLI commands) and the `cdsi-engine/AGENTS.md` two-role runbook.

Do not combine all generated content, tooling, manual interpretation, code annotations, historical results, and engine fixes into one commit. Complete and commit the Logic Specification milestone before beginning Supporting Data normalization and test-history work.

## Completion Criteria

The complete reference and diagnostic system is ready for agent-assisted repair when:

- `cdsi-reference` exists as a top-level peer of the three application modules.
- The exact version 4.6 PDF and checksum are stored in the repository.
- Extraction can be rerun deterministically without an LLM.
- The extraction inventory accounts for all numbered processing sections, figures, and tables relevant to Chapters 4 through 9.
- Every executable step in Chapters 4 through 9 has its own documented package.
- The overall processing model is explained in plain language.
- Major repetition loops have original figures, structured transitions, Mermaid diagrams, and explanatory text.
- Each step retains page, figure, table, and business-rule provenance.
- Specification statements, project interpretations, and implementation observations are visibly distinguished.
- A specification-to-code mapping connects steps to `cdsi-engine` classes and relevant tests.
- Findings can distinguish implementation, specification, Supporting Data, and FITS discrepancies - and the same finding format and taxonomy is used for issues found during documentation and issues found while investigating a failing FITS case, never two parallel systems.
- A future specification version can be added without overwriting 4.6.
- The comparison tool can identify changed sections and affected code mappings.
- At least one Supporting Data release is preserved with source files and checksums.
- Supporting Data can be normalized and validated deterministically.
- Supporting Data releases can be compared by stable domain identifiers.
- Agent-readable documentation exposes the schedules, patient series, target doses, intervals, vaccines, and other major concepts present in the Supporting Data.
- Reference sets bind exact Logic Specification, Supporting Data, and FITS fixture versions.
- Every FITS run records its reference set and engine commit.
- A normal run creates per-case structured results and diagnostic bundles without committing them.
- Each case's structured trace identifies the earliest step where its actual and expected behavior diverge.
- A case-level baseline distinguishes regressions, improvements, changed known failures, and corpus changes, and links investigated cases to the finding that explains them.
- The agent runbook enforces focused diagnosis, minimal general fixes, related-group tests, full-suite regression verification, and a stop for the project owner's review before any clinical-logic fix is committed.
- Reference and regression validation runs locally without network access, at the pace the project owner chooses to run it - not as unattended, automated CI.
- An agent can navigate from a failing test to its structured trace, relevant engine class, step documentation, original specification evidence, normalized Supporting Data, and recorded findings.
- Every executable step class has, or has a tracked plan to get, its own dedicated spec-conformance JUnit test - written independently of whether any FITS case currently exercises the gap it covers.
- A step found to need a fix that would regress another step's tests, or another currently-allowlisted FITS case, is never fixed unilaterally to force a green result - it's recorded as blocked, with the specific conflict named, for the project owner to resolve.
- A step whose real defect appears to lie in a different step's shared-state contract is never patched at the point it was noticed - it's recorded as a finding against the actual class and elevated, leaving the assigned step's own status honestly blocked.

## Non-Goals for the Initial Implementation

Do not expand the first implementation to include:

- Rewriting the CDSi specification.
- Treating generated summaries as normative guidance.
- Converting every Appendix A domain diagram into a formal ontology.
- Automatically fixing engine code based solely on extracted text.
- Downloading future specifications automatically during normal builds.
- Downloading Supporting Data or FITS cases during normal builds.
- Making `cdsi-reference` a runtime dependency.
- Replacing the checked-in PDF with only derived Markdown.
- Replacing original XML, XSD, or spreadsheets with only normalized data.
- Committing every local or agent test attempt into Git history.
- Treating a total pass percentage as the regression baseline.
- Changing FITS expectations automatically to match engine output.
- Resolving all historical ambiguity before the module can be useful.
- Building compressed or promoted historical run archives, or generated trend charts, before they are actually wanted - a checked-in case-level baseline (Phase 19) plus each run's disposable diagnostic bundle (Phase 17) already retain enough raw material to reconstruct either later, without carrying the archiving/promotion machinery in the meantime.
- Running this system as unattended, fully automated CI. The intended workflow is agent-assisted with the project owner reviewing findings and fixes, not a pipeline that merges clinical-logic changes on green.
- Maintaining two parallel discrepancy-taxonomy or record-keeping systems. One finding format and taxonomy (Phase 9, as amended) covers both specification-documentation findings and FITS case-level investigations.
- Treating FITS pass-rate movement as a goal or a success measure of Phase 21's per-step test work. That phase's target is spec-conformance coverage for its own sake; any FITS effect is incidental and not to be chased.
- Letting an agent fix a step class other than the one it was assigned in Phase 21, even when it's confident and the fix looks small - a suspected cross-step defect is recorded and elevated, never patched at the point it was noticed.

The objective is a dependable, traceable, versioned reference and diagnostic system. Clinical corrections, Supporting Data corrections, FITS feedback, and specification feedback should follow through separate reviewed work based on the evidence this system exposes.

## Revision history

- After Phase 17 (structured diagnostic bundles) was built and reviewed against the project owner's actual goal - giving an agent what it needs to close spec-vs-code gaps with the owner in the loop, not unattended CI - Phases 18 through 24 were reviewed, reduced, and renumbered to the 18-20 above. Removed entirely: the original Phase 20 (Add Promoted Historical Runs), Phase 22 (Generate Historical Charts), and Phase 24 (Integrate Validation and CI) - see the Non-Goals just above for why. The original Phase 21 (Add Investigation Records) was merged into Phase 9's finding format rather than kept as a second taxonomy - see Phase 9's Amendment. The original Phase 18 (Improve Structured Engine Tracing) was reduced in scope after finding that `cdsi-engine` already had most of the needed structure. The original Phase 23 (Define the Agent Repair Workflow) became the new Phase 20, adjusted to reference the systems actually built and to add an explicit human-review stop before any clinical-logic commit.
- After Phase 20's first real run (`SPEC-4.6-0007`) turned up a confirmed, fixable defect with zero effect on any FITS case - proving a step class can be wrong in a way the current fixture set simply never exercises - Phase 21 (Per-Step Spec-Conformance Test Coverage) was added: a systematic, two-role (write tests / fix step), one-unit-at-a-time pass over every step class in `mappings/spec-to-code.yaml`, explicitly independent of FITS pass-rate movement, with a cross-step escalation path (a step found to depend on another step's broken contract is recorded and elevated, never fixed out of scope) and its own lightweight status tracking (`step-tests/status.yaml`) rather than folding workflow state into `spec-to-code.yaml` itself.
- With Phase 21 producing committed, shareable HTML dashboards worth showing the wider team, the long-running `cdsi-reference` branch (the "new branch" the Setup section above says to create) was retired as a permanent working branch on 2026-09-03: `master`'s pre-merge tip is preserved as the tag `pre-cdsi-reference-merge`, `master` was fast-forwarded to include all of `cdsi-reference`'s history (a clean fast-forward - `master` had no commits of its own since the branch point), and ongoing work now continues on a new `develop` branch cut from that updated `master`. GitHub Pages, serving the dashboards for the team, points at `develop` so a regenerate-and-commit during ongoing work updates the published page without requiring a merge to `master` first. The Setup section's instruction to branch as `cdsi-reference` describes what was actually done at the start of this initiative and is left as-is for that history; `develop` is the branch to work from going forward.
- After Phase 21's unit 6.10 turned up a shared-framework defect (`LogicTable.evaluate()` doesn't stop at the first matching decision-table column) rather than a per-unit one, and after unit 6.2 had already turned up a domain-object gap shared with not-yet-tested unit 7.1, Phase 21 was extended with `cdsi-reference/step-tests/cross-cutting-notes.md`: a small, explicitly-not-a-finding log for exactly this class of discovery, plus a "Role B sequencing" step in `cdsi-engine/AGENTS.md` describing that the project owner reviews it (together with every `blocked_category: upstream_step_defect` unit) before committing to a per-unit Role B execution order - so a shared or foundational fix can be made once, deliberately, instead of being re-discovered or worked around separately inside each unit it happens to surface in.
