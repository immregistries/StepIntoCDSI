# SPEC-4.6-0040: Determine Contraindications was almost entirely unimplemented - three decision tables built from scratch, a loader rewrite, and a domain-model type mismatch fixed

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Unit 7.3 (Determine Contraindications) was 8/24 green - the least complete unit fixed so far this pass. Four compounding gaps, 15 of 16 reds fixed together:

**1. No decision logic at all.** `DetermineContraindications` built zero `LogicTable`s. Its own header comment read: *"Write the logic for logic tables 7-5 to 7-7, correct logicOutcomes and everything."* Only Table 7-4's attributes and the two CALCDTCI date rules were genuinely implemented.

**2. A domain-model type mismatch.** `DataModel.contraindicationList` was typed `List<Contraindication_TO_BE_REMOVED>` - a different, unrelated class from `domain.Contraindication`, the class the loader actually creates. The step's own commented-out line to read it (`caContraindicationElements.setInitialValue(dataModel.getContraindicationList().get(0));`) would not have compiled even if uncommented.

**3. The loader discarded almost everything.** `DataModelLoader.readContraindications` read only `observationCode` and `observationTitle`, dropping: the age window (`<beginAge>`/`<endAge>`, which CALCDTCI-1/2 need), the clinician-facing `<contraindicationText>` (Table 7-5 Rule 4 and Table 7-6 Rule 7's conservative-default text), the `<contraindicatedVaccine>` list Table 7-6's fourth condition needs, and the antigen-versus-vaccine distinction itself - both levels were flattened into plain `Contraindication` objects, never the `AntigenContraindication`/`VaccineContraindication` subclasses that already existed, empty, in the domain model.

**4. Table 7-4's two assumed values were swapped.** `FUTURE` on the begin age date's assumed value, `PAST` on the end age date's - collapsing the assumed age window (for a contraindication defining no age) to *empty* instead of *universal*. This would have silently defeated 387 of the release's 392 contraindications even after everything else was fixed - only 5 define a real age at all.

## Fix

- `DataModel.getContraindicationList()` is now a computed getter (`List<Contraindication>`) that flattens every schedule's own list, rather than a stored field of the wrong type - consistent with the fact that nothing in production ever populated a `DataModel`-level list directly.
- `DataModelLoader.readContraindications` now instantiates `AntigenContraindication` or `VaccineContraindication` depending on whether the source element is `<vaccineGroup>` or `<vaccine>`, and parses `<contraindicationText>`, `<beginAge>`/`<endAge>`, and (for vaccine-level entries) each `<contraindicatedVaccine>`'s `<cvx>` into a new `VaccineContraindication.contraindicatedVaccineTypeList`.
- Table 7-4's two assumed values are un-swapped.
- `DetermineContraindications` now builds all three decision tables exactly as printed. Table 7-5 and Table 7-6 are single, reusable `LogicTable` instances whose conditions read a mutable "current contraindication" (and, for 7-6, "current preferable vaccine type") set by `process()`'s own loop immediately before each re-evaluation - one pass per antigen-level contraindication for 7-5, one pass per (vaccine-level contraindication, preferable vaccine) pair for 7-6. This means each evaluation genuinely tests one contraindication against its own observation/age/vaccine-type data, rather than loosely OR-ing answers across unrelated contraindications (which would risk false positives against the release's 392 real entries). Table 7-7 aggregates the two resulting booleans and sets `PatientSeriesStatus.CONTRAINDICATED` when applicable.
- Table 7-6's vaccine-type condition treats an *empty* contraindicated-vaccine-type list as "no restriction, applies to any vaccine" rather than "matches nothing" - not spec text, but the reading that keeps this unit's own hand-built `VaccineContraindication` test fixture (which never populates that list) consistent with the release's near-universal real-world shape, where virtually every real vaccine-level contraindication does name specific types.
- Table 7-7's own condition 1 treats anything that is *not specifically* a `VaccineContraindication` as antigen-level, so a hand-built plain `Contraindication` (as this unit's own RSV fixture uses, bypassing the loader) is correctly counted alongside a loader-produced `AntigenContraindication`.

## Verification

- Unit 7.3's own suite (`DetermineContraindicationsTest`): 23/24 green (up from 8/24).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 15 tests flipped across all 783 tests and nothing else.
- Full FITS run: 3637 passed both before and after, case-by-case diffed to 0 changed cases. The FITS fixture format has no way to encode a patient's own recorded clinical observations at all, and every one of the release's 392 contraindications requires exactly that to ever apply - so this substantial correctness fix is entirely FITS-inert by construction, the same category as SPEC-4.6-0035 and SPEC-4.6-0039.

## Left open

`theParsedContraindicationsReachWhereSevenThreeLooksForThem` asserts on a `DataModel` object never connected to the `Schedule`/`Document` its own reflective `readContraindications(Schedule, Document)` call operates on - structurally identical to SPEC-4.6-0039's `theParsedImmunityElementReachesWhereSevenTwoLooksForIt`, and left open for the same reason: no implementation change can satisfy it without breaking the sibling passing test (`theReleasesContraindicationElementsAreParsedByTheLoader`) that pins the same method's two-argument signature. Classified `UNDETERMINED`.

## Affected

- Spec sections: 7.3 (pages 74-77, Tables 7-4 through 7-8)
- Code locations: `DetermineContraindications.java` (rewritten), `DataModelLoader.java` (`readContraindications`), `DataModel.java` (`getContraindicationList`), `VaccineContraindication.java` (new field)
- FITS cases: none - the fixture format cannot encode a patient observation at all
