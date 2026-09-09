# SPEC-4.6-0039: Evidence of immunity was completely unreachable in production - a loader gap, a wrong read source, a placeholder condition, and two smaller defects, all compounding

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Six gaps in unit 7.2 (Determine Evidence of Immunity), five fixed together, one left open and documented:

**1. Never reached `DataModel`.** `DataModelLoader.readImmunity` correctly parsed `<immunity>` but only ever stored it on a transient `Schedule` object - never on the `Antigen` it belongs to, and never on `DataModel`. Table 7-3's conditions read `dataModel.getImmunityList()` - the only field anything in production ever populates - so every real forecast run saw an empty list. This alone made the whole immunity determination inert for all six immunity-bearing antigens in the bundled release (HepA, HepB, Measles, Mumps, Rubella, Varicella).

**2. Wrong read source.** Even with data present, all three implemented conditions indexed `dataModel.getImmunityList().get(0)` rather than the per-antigen attribute Table 7-2 actually declares and the constructor actually builds (`caImmunityElements`) - masked by gap 1, since neither path was populated, but a distinct defect.

**3. A placeholder condition.** Condition 1 ("does the patient have an immunity guideline in their history?") always returned `NO`, carrying its own comment sketching the intended traversal. The entire clinical-history half of the section's stated purpose - "immune due to their clinical history or ... born before a defined date" - was unreachable.

**4. Asked the wrong party.** Condition 3 ("does the patient have an immunity exclusion condition?") asked whether the Supporting Data defined *any* exclusion at all, never checking the patient's own history - so a patient on the birth-date path could be wrongly disqualified purely because the immunity element happened to define an exclusion, regardless of that patient.

**5. Null check too late.** Condition 4 (country of birth) called `.toString()` on its final value before its own null guard ran, throwing `NullPointerException` instead of answering `No` for a patient with no recorded country of birth.

**6. `tableSevenTwoRegistersEveryAttributeItPrints`**: `caImmunityElements` was built and given an initial value but never added to `conditionAttributesList`, publishing three of Table 7-2's four attributes.

**Left open - `theParsedImmunityElementReachesWhereSevenTwoLooksForIt`.** This test asserts on a `DataModel` fixture object never connected, directly or indirectly, to the `Schedule`/`Document` its own `readImmunity(Schedule, Document)` reflective call operates on. No change to `readImmunity`, `DataModel`, or `Antigen` can make an unrelated object's field non-empty, and `readImmunity`'s two-argument signature is independently pinned by the sibling (passing) test `theReleasesImmunityElementIsParsedByTheLoader`'s own reflective lookup - changing it would break that test instead. Classified `UNDETERMINED` per `cdsi-engine/AGENTS.md` step 7 rather than forced with a workaround.

## Fix

- `DataModelLoader`'s per-antigen loop now adds the parsed `Immunity` onto its own target-disease `Antigen`'s `immunityList` (resolved via the schedule's own name - the same identity `<targetDisease>` independently uses for the same antigen), immediately after calling `readImmunity`. `readImmunity`'s own signature and its `Schedule`-storing behavior are untouched.
- `DetermineEvidenceOfImmunity`'s constructor now also copies that antigen list into `dataModel.getImmunityList()` whenever it is non-empty - the sole production writer of that field, so in every real run this is simply "always." This reconciles the unit's own test fixtures too: some tests set `dataModel.setImmunityList(...)` directly as a testing convenience, one sets the antigen's own list to verify per-antigen sourcing specifically - both keep working since Table 7-3's conditions were left reading `dataModel.getImmunityList()` unchanged.
- Condition 1 now matches the patient's own `PatientObservation` history against each `Immunity`'s `ClinicalHistory` guideline codes.
- Condition 3 now matches the patient's own history against each `BirthDateImmunity`'s `Exclusion` codes, instead of asking whether the Supporting Data defines one at all.
- Condition 4's null check now runs before the dereference it used to guard too late.
- `caImmunityElements` is now added to `conditionAttributesList`.
- A latent typo surfaced by fixing condition 1: Rule 1's forecast reason capitalized "Evidence" where Rule 3 already correctly used lowercase - corrected to match.

## Verification

- Unit 7.2's own suite (`DetermineEvidenceOfImmunityTest`): 15/16 green (up from 10/16; the 16th is the documented `UNDETERMINED` case).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 5 tests flipped and nothing else.
- Full FITS run: 3637 passed (up from 3632) - 5 real cases improved, all 5 test-plan snapshots of `MMR-2015-0024` ("Patient is born before 01/01/1957" with no vaccinations administered - the birth-date immunity path this defect made completely unreachable in production), 0 regressions. The second genuinely broad fix of this Role B pass to move real FITS cases, after SPEC-4.6-0038.

## Affected

- Spec sections: 7.2 (page 73, Table 7-2 and Table 7-3)
- Code locations: `DetermineEvidenceOfImmunity.java`, `DataModelLoader.java` (the per-antigen loading loop)
- FITS cases: 5 real cases improved (MMR-2015-0024, all 5 snapshots)
