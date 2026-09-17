# Interim Consultant Review For Claude

Date: 2026-09-10

Scope: read-only outside review of the current StepIntoCDSI state, focused on what should be fixed next to move beyond narrow per-unit JUnit repair. No code changes were made as part of this review.

## Executive Summary

The project has made real progress. The latest local FITS run shows 3,637 passing cases out of 4,896, up from the earlier 3,364 baseline. The current codebase also shows several important foundational repairs already landed: `LogicTable.evaluate()` now stops after the first matching rule column, `Forecast` now carries dose number and recommended vaccines, `equivalentSeriesGroups` is parsed, and immunity is at least partially routed onto antigens.

The next level will not come from continuing to clear red JUnit assertions one method at a time. The remaining failures cluster around partially migrated subsystems where old assumptions still leak through adjacent code. Treating these as isolated Role B units risks local fixes that pass one test class while preserving the deeper inconsistency.

Recommended posture: fix by problem cluster, not by specification unit number.

## Current Local Evidence

Latest reviewed FITS bundle:

```text
cdsi-fits-tests/target/fits-runs/2026-09-10T111448-117219400Z-d6b26b4-acip-4.6-sd-4.65-fits-222cc1c7
```

Summary:

```text
4896 discovered/executed
3637 passed
1258 failed assertions
1 execution error
0 skipped
```

The bundle's `changed-cases.json` reports no case status changes versus the immediately previous local run.

Latest `cdsi-engine/target/surefire-reports` totals:

```text
783 tests
62 failures
0 errors
0 skipped
```

Current worktree note: only `.settings/org.eclipse.core.resources.prefs` appears modified, unrelated to this review.

## What Looks Stale In Existing Cross-Cutting Notes

The cross-cutting notes contain the right kind of strategic thinking, but parts of them now lag behind current code:

- `LogicTable.evaluate()` already has the first-match `break`.
- `Forecast` now has `doseNumber`, `recommendedVaccineList`, and `administrativeGuidanceList`.
- `GenerateForecastDatesAndRecommendedVaccines` now writes dose number and recommended vaccine list.
- `AntigenSeries` now has `equivalentSeriesGroups`, and `DataModelLoader` reads it.
- `DataModelLoader` now attaches parsed immunity to the target antigen's `immunityList`.

Before setting the next Phase B sequence, refresh `cdsi-reference/step-tests/cross-cutting-notes.md` and any affected `status.yaml` notes so the next agent is not solving obsolete blockers.

## Recommended Fix Clusters

### 1. Chapter 8 Series Selection And Scoring

This should be the first major cluster.

The `SelectNextSeriesGroup` infrastructure is a good step, but the rest of Chapter 8 is still only partly adapted to true per-series-group processing. The remaining failures show old broad-scope assumptions:

- `PreFilterPatientSeries` excludes null-status series when the spec condition is "not contraindicated".
- risk-priority and valid-dose checks still appear too broad or too narrow depending on the row.
- `IdentifyOnePrioritizedPatientSeries` counts target doses in some places where the spec asks for patient series.
- `ClassifyScorablePatientSeries` still treats "has any satisfied target dose" as in-process without consistently checking patient-series forecast status.
- `InProcessPatientSeries` and `NoValidDoses` duplicate scoring logic, use inconsistent date calculations, and carry scores on mutable `PatientSeries` objects rather than on a clearly scoped selection pass.
- `SelectPrioritizedPatientSeries` reads raw accumulated scores and has a string-reference comparison bug for empty `seriesPreference`.

Recommendation: do not fix 8.1, 8.2, 8.3, 8.5, 8.6, and 8.7 as independent one-off edits. Create a coherent Chapter 8 selection/scoring model:

- selected input = patient series for the current antigen and current series group;
- scoring values are per selection pass, either reset at the top of the pass or stored outside `PatientSeries`;
- shared helper methods compute valid-dose count, not-satisfied count, start/finish dates, product path, maximum-age windows, and tie handling once;
- 8.5 and 8.6 reuse those helpers instead of carrying divergent copies;
- 8.7 selects from the score for this pass, not historical mutable score.

This is likely the best route to large downstream FITS improvement because Chapter 8 decides which patient series becomes the basis for forecasting.

### 2. Conditional Skip Context And Validate Recommendation

This is a domain-model plus shared-evaluator cluster, not a local 6.2 or 7.1 fix.

Current shape:

- `SeriesDose` still holds exactly one `ConditionalSkip`.
- `ConditionalSkip` has no context field.
- `DataModelLoader` still overwrites repeated `<conditionalSkip>` elements via `setConditionalSkip`.
- `EvaluateConditionalSkip` cannot filter by Evaluation, Forecast, or Both because that context is not represented.
- `ValidateRecommendation.process()` bypasses the inherited conditional-skip logic and immediately routes to 4.4.
- In validating mode, `EvaluateConditionalSkip` uses `PAST` as the reference date instead of the forecast's earliest date.

Recommendation:

- model multiple conditional skips on `SeriesDose`;
- preserve each skip's context from Supporting Data;
- filter by context at use time for 6.2, 7.1, and 7.6;
- make 7.6 actually run the inherited conditional-skip decision tables;
- use the forecast earliest date as the validating reference date;
- keep the three consumers together in one repair plan to avoid document-order accidents.

This may regress some currently passing FITS cases if they were passing because the loader kept the last skip by accident. That is acceptable only if the regression is understood, documented, and reviewed.

### 3. Chapter 9 Vaccine Group Forecast Aggregation

The old blocker that `Forecast` had nowhere to store dose number and recommended vaccines is mostly gone. The remaining problem is aggregation.

Current shape:

- `SingleAntigenVaccineGroup` does not populate `VaccineGroupForecast.forecastList`.
- `SingleAntigenVaccineGroup` still leaves `antigensNeededList` empty.
- `SingleAntigenVaccineGroup` defaults `vaccineGroupStatus` on null patient-series status but leaves `patientSeriesStatus` null.
- `MultipleAntigenVaccineGroup` does not populate contained forecasts.
- `MultipleAntigenVaccineGroup` only runs aggregation when group status is `NOT_COMPLETE`, while the single-antigen path copies fields for all statuses.
- `MULTIANTVG_7` concatenates forecast reasons rather than forming a union.
- `MULTIANTVG_1` handles priority per contained forecast rather than choosing the group-level priority branch once, and does not apply the "no earlier than latest administered dose in the vaccine group" floor.
- the live `MULTIANTVG-9` block collects `VaccineGroup` objects into a local list and does not assign anything; it is dead code and the wrong type.

Recommendation:

- first define a single aggregation contract for vaccine group forecasts;
- populate `forecastList` in both single- and multiple-antigen paths;
- aggregate dose number and recommended vaccines from the patient-series forecasts now that those fields exist;
- make single- and multiple-antigen paths consistent on which fields are populated for non-`NOT_COMPLETE` statuses;
- implement reason union semantics with separators/deduplication;
- rewrite `MULTIANTVG_1` around the actual Table 9-4 / priority rule shape instead of the current per-element switch.

This cluster should follow Chapter 8 if possible, because bad best-patient-series selection will feed bad inputs into Chapter 9.

### 4. Immunity And Contraindication Supporting-Data Scoping

This is real, but I would not make it the first cluster unless FITS evidence says it dominates.

Current shape:

- immunity is now parsed onto the antigen, but `DataModel` still exposes a mutable per-run `immunityList` that `DetermineEvidenceOfImmunity` fills opportunistically;
- contraindications are loaded per schedule but `DataModel.getContraindicationList()` flattens every schedule's contraindications, regardless of current antigen/vaccine scope;
- `DetermineContraindications` is now substantially implemented, but its supporting-data input is still not cleanly scoped;
- adverse reactions remain effectively unimplemented input.

Recommendation:

- move toward supporting-data indexes scoped by antigen and vaccine type;
- make 7.2 and 7.3 read scoped supporting data directly rather than per-run scratch mirrors;
- preserve the distinction between antigen contraindications and vaccine contraindications;
- handle adverse reactions as a separate representational gap rather than pretending they are present.

This cluster matters for correctness, but it is likely larger and less directly connected to the most common age/interval FITS failures than Chapter 8 and conditional skip.

## Suggested Near-Term Sequence

1. Refresh cross-cutting notes and `status.yaml` to remove stale blockers and identify what is truly still red.
2. Plan and execute Chapter 8 as a cluster: per-series-group scoping, shared scoring helpers, per-pass score handling, and 8.7 selection cleanup.
3. Run full `cdsi-engine` and full FITS before/after, with a progress-ledger entry.
4. Fix conditional skip context plus 7.6 validation as a single shared-evaluator repair.
5. Then fix Chapter 9 aggregation once upstream best-patient-series selection is more trustworthy.
6. Treat immunity/contraindication scoping as its own larger data-model round.

## Process Recommendation

For each cluster, do not ask "which single JUnit class can I make green?" Ask:

- what invariant is this subsystem supposed to enforce?
- which step tests express that invariant?
- which current FITS failures are plausibly downstream of it?
- which existing passes might be accidental?
- what before/after movement do we predict?
- did any changed case move for a reason we cannot explain?

The project is now in the uncomfortable but productive phase where spec-aligned fixes can temporarily lower FITS pass count. That is not automatically bad. What would be bad is continuing to make narrow fixes without a coherent subsystem model, because that will make later regressions harder to explain.

## Bottom Line

The next level is not one more isolated Role B round. The next level is a deliberate Chapter 8 rewrite, followed by conditional skip/validation and Chapter 9 aggregation. Those are the places where the current engine still has old architecture and new repairs coexisting awkwardly.

