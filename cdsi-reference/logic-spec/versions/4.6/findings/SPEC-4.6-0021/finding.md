# SPEC-4.6-0021: minAgeToStart/maxAgeToStart were never enforced - an age-inappropriate series group could become a "best patient series" once Chapter 8 is scoped per series group

**Status:** confirmed, merged - fixed in `SelectBestPatientSeries` (4.5)'s series-group enumeration
**Category:** IMPLEMENTATION_MISMATCH

## Correction to the original write-up

The original version of this finding claimed `SelectPatientSeries` didn't declare `minAgeToStart` "at all - the field doesn't exist in the domain model." That was wrong: `git log` on `SelectPatientSeries.java` shows the field, its getter, and its setter have existed unchanged since the module-split commit `c1abaa1` (2026-08-31). What was actually missing was narrower: `DataModelLoader` never parsed `<minAgeToStart>` out of the `<selectSeries>` XML (so the field, though declared, was always left `null`), and **no code anywhere read either `minAgeToStart` or `maxAgeToStart`** - `maxAgeToStart` was parsed but had zero read sites, exactly as the original evidence section said.

## Evidence

Grepping all of `cdsi-engine`/`cdsi-web` for `maxAgeToStart` before this fix: exactly two hits - the getter/setter declaration and the one loader write site. **Zero read sites anywhere.** `minAgeToStart` had the getter/setter but no loader write site and no read sites either.

The bundled Supporting Data declares both on many series - e.g. `AntigenSupportingData- Pneumococcal-508.xml`'s "Pneumococcal 50+ 1-dose PCV series":

```xml
<seriesName>Pneumococcal 50+ 1-dose PCV series</seriesName>
...
<seriesType>Standard</seriesType>
<selectSeries>
<defaultSeries>Yes</defaultSeries>
<seriesGroupName>Standard 50+</seriesGroupName>
<seriesGroup>3</seriesGroup>
<minAgeToStart>50 years</minAgeToStart>
<maxAgeToStart/>
</selectSeries>
```

Table 8-2's only age-to-start check, SELECTSCORE-2's second bullet, is narrower than a general applicability gate:

> "The earliest vaccine dose administered with an evaluation status of 'Valid' associated with the relevant patient series has a date administered before the maximum age to start date."

This only fires for a Standard series that **already has** at least one administered dose. It has no power to exclude a series the patient has never received any dose toward - which is exactly the "Pneumococcal 50+" case for a young patient. A full-text search of the specification confirms "minimum age to start" / "minAgeToStart" appears **nowhere** in the spec's prose - only "maximum age to start" appears, once, in SELECTSCORE-2 itself (Table 8-2, page 87). There is no spec text describing a general age-applicability gate at all.

## Interpretation

Traced directly from SPEC-4.6-0020's FITS regression: case `5eeca0522cc4517a96b0f417-PCV-2013-0575`, a patient under 50, was forecast for "Pneumococcal 50+ 1-dose PCV series."

**Mechanism.** Before SPEC-4.6-0020's fix, Chapter 8 ran as one undivided competition across every series group of an antigen - an age-inappropriate series with no real administered doses simply lost the scoring comparison to the antigen's genuine winner, so this gap was invisible. Once Chapter 8 is correctly scoped per series group (matching the specification's own "repeated for each series group" text), a solo-series group with an unenforced age gate reached `PreFilterPatientSeries`'s default-series fallback with a trivially-zero *within-group* valid-dose count, was admitted as the group's default, and - being alone in its group - proceeded straight through Table 8-3 Rule 2 (a single candidate needs no scoring) into `bestPatientSeriesList`, regardless of the patient's actual age.

**Two fix attempts inside `PreFilterPatientSeries` (8.1) were tried and reverted.** The first gated the default-series fallback on the series' own age window, which correctly excluded "Pneumococcal 50+" but left HPV's zero-dose adult patients with *no* scorable series at all, since HPV splits one logical default pathway across an age boundary into two series *within the same series group* ("2-dose" default, age <15; "3-dose" non-default, age 15+) rather than into a separate group. The second attempt tried promoting the age-appropriate alternate within the group when the literal default wasn't age-appropriate; this fixed PCV/MEN but produced the wrong dates for HPV. Direct inspection of FITS fixture `675c41f8e4b089d2bdc0c483-HPV-2013-0480` (a 27-year-old, zero HPV doses) shows the reference implementation's actual behavior is to keep reporting the literal default ("2-dose") series's own historical dates, **not** switch to the age-appropriate alternate - an invented rule with no spec basis, contradicted by FITS's own reference answer.

**Where the fix actually belongs.** The PCV/RSV/MEN shape and the HPV shape are structurally different, and Chapter 8's business rules (Table 8-2 and friends) have no textual hook for an age gate at all - so the fix does not belong inside any single Chapter 8 step's per-series scoring. It belongs one level up, in `SelectBestPatientSeries` (4.5)'s new per-antigen series-group enumeration (added by SPEC-4.6-0020): a series group is excluded from the series-group stepper entirely - before Chapter 8 ever runs for it - when **none** of its member series has an age-to-start window that includes the patient's current age, **and** the patient has no already-satisfied target dose toward any series in that group. This leaves the *within-group* HPV default-series behavior completely untouched (matching FITS's own reference answer), while correctly removing an entirely separate, age-inappropriate group (PCV/RSV/MEN's "Standard 50+"/"75+" shape) from consideration before it can be admitted as a solo-candidate default.

**Materiality.** At least three of the bundled release's antigens define a solo-series, age-gated series group of this exact shape:

- Pneumococcal - "Standard 50+" (group 3), `minAgeToStart` 50 years
- RSV - "Standard 75+" (group 3), `minAgeToStart` 50 years (the series dose's own age window narrows this further to 75)
- At least one Meningococcal series group

Together these accounted for the great majority of SPEC-4.6-0020's 175-case FITS regression (PCV 143, RSV 20, MEN 12).

## Fix and verification

`SelectBestPatientSeries.java` gained `isSeriesGroupApplicable(...)`/`isWithinAgeToStartWindow(...)`, evaluated once per antigen when building the series-group stepper (see also SPEC-4.6-0020). `DataModelLoader.readAntigenSeries` now also parses `<minAgeToStart>` (mirroring the existing `<maxAgeToStart>` branch), which it never did before.

Verified via the standing Role B pipeline: `SelectBestPatientSeriesTest` (5 new tests: solo age-inappropriate group excluded, same group included once age-appropriate, included regardless of age when the patient already has a satisfied dose in it, included when no patient/DOB data is available at all) plus the full `cdsi-engine` suite (139 pre-existing, unrelated failures - identical count with and without this change, confirmed via `git stash` A/B) plus a full FITS run, case-by-case diffed against the pre-SPEC-4.6-0021 baseline (`2026-09-08T195714-671589Z-18ba253-...`, 3253 passed): **+143 improvements, 0 regressions, all 143 improvements in the PCV group.** (One incidental NPE was found and fixed during FITS verification: `isSeriesGroupApplicable`'s "does the patient have a satisfied dose in this group" check did not guard against `PatientSeries.getTargetDoseList()` returning `null`, which real pipeline data does for some series - e.g. COVID-19 case `2023-0071` - unlike every hand-built JUnit fixture, which always populated a list.)

## Remaining, distinct gap - not addressed by this fix

RSV defines a genuinely separate second series group ("RSV 75 years+ 1-dose series", group 3, `minAgeToStart` 50 years) that is **not** declared `equivalentSeriesGroups` to the antigen's main group 1 ("RSV 1-dose series", no age bounds at all). For a 75+ patient who has already completed group 1's series, group 3 is independently age-eligible and produces a second, competing "best patient series" with status `AGED_OUT` instead of the correct `COMPLETE`. SPEC-4.6-0020's 8.8 equivalence-suppression (Table 8-14) cannot reconcile these because the Supporting Data doesn't declare them equivalent - unlike, e.g., HepA/HPV/Hib/Pneumococcal/Meningococcal B/Zoster/COVID-19, which do declare `equivalentSeriesGroups`. This looks like a Supporting Data gap or a Chapter 8 equivalence-fallback question, not fixable from inside `SelectBestPatientSeries`, and needs its own investigation.

## Affected

- Spec sections: 8.1 (page 87, Table 8-2) - the layer where this was exposed, not where it was fixed; actually fixed in 4.5 (page 39)
- Code locations: `SelectBestPatientSeries.java` (fix), `DataModelLoader.java` (`minAgeToStart` parsing, previously missing)
- FITS cases: 143 fixed (all PCV), 0 regressed
