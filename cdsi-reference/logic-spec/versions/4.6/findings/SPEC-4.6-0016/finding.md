# SPEC-4.6-0016: Note 2a's age-based CVX-to-antigen association was entirely unimplemented

**Status:** confirmed (reviewed and merged by the project owner - see "Fix merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Note 2a (section 4.2): "The CVX to Antigen Supporting Data includes Association Begin Age and Association End Age attributes to properly associate the administered vaccine with the proper antigen based on the age of patient at the time of administration (e.g., a Zoster vaccine administered below 50 years should be associated with Varicella)."

The bundled Supporting Data's own `cvxToAntigenMap` entry for CVX 121 (Zoster live):

```xml
<cvxMap>
<cvx>121</cvx>
<shortDescription>Zoster live</shortDescription>
<association>
<antigen>Varicella</antigen>
<associationBeginAge>0 days</associationBeginAge>
<associationEndAge>50 years</associationEndAge>
</association>
<association>
<antigen>Zoster</antigen>
<associationBeginAge>50 years</associationBeginAge>
<associationEndAge/>
</association>
</cvxMap>
```

`DataModelLoader.readCvx`'s association branch read only the `<antigen>` child of each `<association>` element - it never read `associationBeginAge` or `associationEndAge` at all - and `VaccineType` held a plain `List<Antigen>` with nowhere to put them even if it had. `OrganizeImmunizationHistory` created an `AntigenAdministeredRecord` for every antigen in `vda.getVaccine().getVaccineType().getAntigenList()` unconditionally, so one CVX 121 dose always produced *both* a Varicella and a Zoster record, at any patient age.

## Interpretation

A complete implementation gap, not a subtle logic error: the relevant Supporting Data existed, but no code anywhere read it, and the domain model had nowhere to hold it even if something had.

`VaccineType.getAntigenList()` was confirmed (by exhaustive grep across the whole engine) to have exactly one caller - `OrganizeImmunizationHistory` itself - so adding an age filter there carries no risk of an unrelated caller expecting the old, unfiltered list.

Confirmed live by both of unit 4.2's red tests, and a new third test added this round pinning the exact boundary: a dose given on the patient's 50th birthday itself associates with Zoster alone, confirming begin-inclusive/end-exclusive (matching every other age-window convention in the specification, e.g. Table 6-15) rather than the reverse.

## Fix merged

Reviewed and approved by the project owner, merged to `develop`. Three files:

**`VaccineType.java`** - new `associationBeginAgeMap`/`associationEndAgeMap`, keyed by the same interned `Antigen` instances `getOrCreateAntigen` already shares:

```java
public TimePeriod getAssociationBeginAge(Antigen antigen) { return associationBeginAgeMap.get(antigen); }
public TimePeriod getAssociationEndAge(Antigen antigen) { return associationEndAgeMap.get(antigen); }
public void setAssociationAge(Antigen antigen, TimePeriod beginAge, TimePeriod endAge) { ... }
```

**`DataModelLoader.java`** - reads `associationBeginAge`/`associationEndAge` alongside the existing `antigen` read, and calls `cvx.setAssociationAge(antigen, beginAge, endAge)`.

**`OrganizeImmunizationHistory.java`** - a new `isAssociatedAtAdministration()` check before creating each record:

```java
private boolean isAssociatedAtAdministration(VaccineType vaccineType, Antigen antigen, Date dateAdministered) {
  Date dateOfBirth = dataModel.getPatient().getDateOfBirth();
  TimePeriod beginAge = vaccineType.getAssociationBeginAge(antigen);
  if (beginAge != null && beginAge.isValued()
      && dateAdministered.before(beginAge.getDateFrom(dateOfBirth))) {
    return false;
  }
  TimePeriod endAge = vaccineType.getAssociationEndAge(antigen);
  if (endAge != null && endAge.isValued()
      && !dateAdministered.before(endAge.getDateFrom(dateOfBirth))) {
    return false;
  }
  return true;
}
```

### Test verification

`OrganizeImmunizationHistoryTest` (12 tests, up from 11 - one new boundary test added this round): before, 9 green / 2 red; after, **12 green / 0 red** - unit 4.2 is now fully closed. Full `cdsi-engine` suite: 771 tests, 169 failures, 1 error - down from 171 failures before this round, a reduction of exactly 2. No other test in the suite changed status in either direction.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (4896 fixtures), verified by `ReferenceSetVerifier`. `mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run so `cdsi-fits-tests` resolved the freshly-built jar.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3378 | 3378 |
| Failed assertions | 1517 | 1517 |
| Execution errors | 1 | 1 |

Case-by-case comparison: **0 cases changed status, and not even a hidden `actualHash` change within an unchanged status** - a strict, complete no-op. None of the bundled fixtures happens to administer CVX 121 in a way this defect's double-counting would have affected the reported forecast for. This is a real, now-correct fix, just not one the current fixture set happens to exercise.

## Affected

- Spec sections: 4.2 (page 33)
- Code locations: `OrganizeImmunizationHistory`, `DataModelLoader`, `VaccineType`
- FITS cases: none - verified strict no-op on the current fixture set (see FITS verification above)
