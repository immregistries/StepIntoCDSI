# SPEC-4.6-0029: Unit 5.1's active-observation, Rule 3, and gender-attribute gaps fixed; series-level gender restriction confirmed real but deliberately deferred after measuring its regression

**Status:** confirmed, partially merged (3 of 4 gaps fixed; the 4th deferred with evidence)
**Category:** IMPLEMENTATION_MISMATCH

## Context

Second unit tackled under the project owner's "work forward through failing JUnits" strategy (after 4.4/SPEC-4.6-0028). `status.yaml`'s pre-existing notes for 5.1 had already diagnosed all four of its red-test-driving gaps in detail.

## Fixed

**1. Table 5-4 condition 0 was a stub.** "Does the indication describe any active patient observations?" unconditionally returned `NO` under a `// logic condition not yet implemented` comment - `caActivePatientObservations` was populated from the patient's medical history but never read. Now it scans `MedicalHistory.getPatientObservationList()` for any `PatientObservation` whose `ObservationCode.getCode()` matches the indication's own.

**2. Table 5-4 Rule 3 was encoded wrong.** Its first condition was `ANY` where the specification says `Unknown` - since the condition (fixed above) only ever answers `YES`/`NO`, encoding Rule 3 as `ANY` made it a strictly weaker copy of Rule 2, so an ordinary "does not apply" indication matched both columns and wrongly raised the "flag for clinician" notification meant only for indications that genuinely couldn't be resolved. Changed to `UNKNOWN`, which the condition never returns - correctly making Rule 3 unreachable via this path, matching `status.yaml`'s own observation that "the only honest test is the structural one."

**3. Table 5-5's own gender attributes were never populated.** Condition 0 ("is the patient gender one of the required genders of the antigen series?") reads `caGender`/`caRequiredGender` off `LT55` itself - but the constructor only ever populated those fields on each `LT54` (Table 5-4) child it built, never on the `LT55` (Table 5-5) instance that actually asks the question. Both stayed `null`, so the condition always took its "no required genders supplied" early return (`YES`), regardless of the series' actual required genders. Now `LT55` gets its own copies of both attributes, populated identically to `LT54`'s.

A fourth, smaller item from the same notes - Table 5-2's "Series type" attribute being populated from `getSeriesName()` instead of the series' actual type - was already covered by an existing, passing test (it only checks attribute type/name, not value) but was fixed anyway for correctness: `caSeriesType.setInitialValue(...)` now uses `antigenSeries.getSeriesType().toString()`.

## Confirmed but deferred: series-level gender restriction is still a no-op in production

Fix 3 makes `LT55`'s condition 0 read the right attribute - but `AntigenSeries.getRequiredGenderList()` (the `List<String>` it reads) is never populated by `DataModelLoader` at all. The bundled Supporting Data's series-level `<requiredGender>` - a direct child of `<series>`, e.g. HPV's "HPV male 2-dose series" declaring `<requiredGender>Male</requiredGender>` - has no parsing branch in `readAntigenSeries`'s `<series>` child loop. A *different* `<requiredGender>` element exists too, nested under `<seriesDose>`, parsed by `readSeriesDose` into `RequiredGender` objects on `SeriesDose` - an unrelated, dose-level concept that happens to share the element name.

Adding the missing series-level parsing branch was implemented and measured directly rather than assumed safe. Result: **the full FITS suite dropped from 3511 to 3123 passed - 388 regressions, 0 improvements, 100% concentrated in the HPV group** (the only antigen in the bundled release with any series-level gender restriction - all 4 of its series declare one). Root cause: `Patient.getGender()` carries `ForecastInput`'s raw `"F"`/`"M"` vocabulary straight through (`GatherNecessaryData.java`: `patient.setGender(input.getPatientSex())`, no normalization anywhere), while Supporting Data's `<requiredGender>` values are `"Female"`/`"Male"`/`"Unknown"`. `LT55`'s exact string-equality comparison (`"F".equals("Male")`) never matches, so *every* gender-restricted series flips to "not relevant" for every patient - including the correct gender, which is worse than the current always-relevant behavior for that demographic.

This is exactly the vocabulary mismatch 5.1's own JUnit suite already flagged as deliberately out of scope: "the tests use the Supporting Data vocabulary so they stay independent of where that normalization should live." The `DataModelLoader` change was reverted in full; a fresh FITS run confirmed the revert restores the 5.1-fix-only baseline exactly (3511 passed, 0 changed cases).

## Why this wasn't bundled into the same round

Fixing this safely requires a second, independent design decision - where "F"/"M" ↔ "Female"/"Male"/"Unknown" normalization belongs (the `ForecastInput`/`GatherNecessaryData` boundary, `Patient` itself, or each comparison site individually). That decision has its own blast radius separate from 5.1's own gaps: `EvaluateGender` (4.10) has a third, independent gender comparison over yet another type (`List<RequiredGender>` on `SeriesDose`, via a coincidentally-working `String.contains()` trick) - but `EvaluateGender` is already separately flagged (see `mappings/spec-to-code.yaml`) as probable dead code, unconditionally returning `YES` regardless of its own comparison, so it may not even be the right home for a fix despite looking topically related. This is a cross-cutting question deserving its own bounded round, not a rider on 5.1's.

## Verification

- Unit 5.1's own suite (`SelectRelevantPatientSeriesTest`): 23/23 green (up from 16/23).
- Full `cdsi-engine` suite: 127 failures/1 error (down from 134/1) - sorted diff confirms exactly the 7 targeted tests flipped and nothing else.
- Full FITS run for the 3 fixes actually shipped: 3511 passed both before and after - 0 changed cases. The FITS fixture format never carries `PatientObservation`/`MedicalHistory` data at all (confirmed: no reference to either in `FitsTestCase.java`), so fix 1 is genuinely inert against the current fixture set, the same way SPEC-4.6-0026's CALCDT-5 fix was; fix 2 only changes which log line fires for an already-correctly-decided outcome; fix 3 is inert only because of the deferred gap above.
- The deferred gap's regression (388 cases, 100% HPV, 0 improvements) was measured directly via a full FITS run before being reverted, not estimated.

## Affected

- Spec sections: 5.1 (pages 41-44, Tables 5-2, 5-4, 5-5)
- Code locations: `SelectRelevantPatientSeries.java` (fixed); `DataModelLoader.java` (`readAntigenSeries`, gap confirmed, fix reverted pending the normalization decision); `EvaluateGender.java` (flagged as a related, separately-tracked concern)
- FITS cases: none changed by what was shipped; 388 HPV cases (all currently passing) would regress if the `DataModelLoader` gap is closed without also normalizing gender vocabulary
