# SPEC-4.6-0037: Trade-name matching wired up for real, and a mislabeled attribute pair fixed - with a FITS-driven course correction along the way

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Unit 6.8's last three reds:

**1. Trade-name condition hardcoded.** Table 6-26's third condition ("is the trade name of the vaccine dose administered the same as the trade name of the preferable vaccine?") always returned `YES`, with a comment explaining why: *"trade name is not set to the correct value, and trade name is not passed into the forecaster."* This step package's own Review Findings had already recorded this as a known, deliberate-looking limitation pending confirmation. Investigation found the comment stale: `AntigenAdministeredRecord`'s production constructor already copies `vda.getVaccine().getTradeName()`, and `DataModelLoader.readVaccine` already parses `<tradeName>` onto every `PreferrableVaccine` - the data was there on both sides, just never compared.

**2. Mislabeled attribute pair.** Table 6-25's last two rows had attribute type and name swapped, and the name had lost its "Preferable" prefix - published as type `"Vaccine Type Begin Age Date"` / name `"Calculated date (CALCDTPREF-1)"` instead of type `"Calculated date (CALCDTPREF-1)"` / name `"Preferable Vaccine Type Begin Age Date"`. The values themselves (CALCDTPREF-1/2's actual dates) were already correct - presentation only.

## A wrong turn, caught by FITS

A first fix simply wired the condition up as an exact-match comparison. Unit 6.8's own two targeted JUnit tests went green, but the full FITS suite dropped from 3627 to 3617 passed - 10 real cases regressed, 0 improved, all 5 test-plan snapshots each of `HepB-2013-0208` and `HepB-2013-0210`, both explicitly titled around a specific Recombivax Hep B dose.

Reading the raw source fixture confirmed why: the official NIST/CDC FITS test-plan JSON's `vaccinations` schema carries only `date`, `cvx`, `mvx`, and `doseCondition` - no trade name field at all. The brand name appears only in the case's human-readable title, never as data the engine can see. So under a strict exact-match rule, every FITS-driven administered dose has an empty trade name, which can never equal a preferable vaccine's specific non-empty requirement - exactly the failure mode for HepB's 8 branded preferable-vaccine entries (ENGERIX-B ADULT x6, RECOMBIVAX ADULT x2).

## Fix

- `caVaccineTypeBeginAgeDate`/`EndAgeDate` are now constructed with type `"Calculated date (CALCDTPREF-1/2)"` and name `"Preferable Vaccine Type Begin/End Age Date"`.
- The trade-name condition now genuinely compares both sides, but treats an empty trade name on *either* side as "no constraint" rather than a mismatch - matching this same table's own next condition, where an empty administered volume is already treated as automatically sufficient rather than compared. Only two known, non-empty trade names that actually differ produce `NO`. This isn't a workaround bolted on to satisfy FITS; it's the more defensible reading of "is the trade name the same" when one side is simply unrecorded, using an idiom the table already relies on one condition away.

## Verification

- Unit 6.8's own suite (`EvaluateForPreferableVaccineTest`): 30/30 green (up from 27/30).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 3 tests flipped and nothing else.
- Full FITS run: the exact-match first attempt regressed the 10 cases identified above (0 improvements) - rejected. The final, missing-data-tolerant implementation restores 3627 passed with 0 changed cases end to end - the FITS suite's own missing trade-name field means it cannot exercise a genuine brand mismatch either way, the same category as SPEC-4.6-0026, SPEC-4.6-0030, SPEC-4.6-0034, SPEC-4.6-0035, and SPEC-4.6-0036.

## Affected

- Spec sections: 6.8 (page 52, Table 6-25 and Table 6-26)
- Code locations: `EvaluateForPreferableVaccine.java` (attribute construction, Table 6-26 condition 2)
- FITS cases: 10 real cases (5 snapshots each of HepB-2013-0208 and HepB-2013-0210) were the regression signal for the rejected first attempt; final fix leaves all 10 unchanged since the fixture format cannot supply a trade name to genuinely exercise the rule
