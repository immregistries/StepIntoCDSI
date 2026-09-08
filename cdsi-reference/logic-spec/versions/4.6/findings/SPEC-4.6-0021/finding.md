# SPEC-4.6-0021: minAgeToStart/maxAgeToStart are never enforced - an age-inappropriate series can become a "best patient series" once Chapter 8 is scoped per series group

**Status:** open (not fixed - discovered while merging SPEC-4.6-0020, whose regression it causes)
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`SelectPatientSeries` declares `maxAgeToStart` (a `TimePeriod`, parsed by `DataModelLoader.readAntigenSeries`'s `<selectSeries>` branch) but not `minAgeToStart` at all - the field doesn't exist in the domain model. Grepping all of `cdsi-engine`/`cdsi-web` for `maxAgeToStart`: exactly two hits - the getter/setter declaration and the one loader write site. **Zero read sites anywhere.**

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

This only fires for a Standard series that **already has** at least one administered dose. It has no power to exclude a series the patient has never received any dose toward - which is exactly the "Pneumococcal 50+" case for a young patient.

`PreFilterPatientSeries`'s own end-of-method "no default series fallback" - already recorded as a `SPECIFICATION_AMBIGUITY` in this unit's `status.yaml` notes, since SELECTSCORE-2 never documents how a group's default series becomes scorable - has no age check of any kind either.

## Interpretation

Traced directly from SPEC-4.6-0020's FITS regression: case `5eeca0522cc4517a96b0f417-PCV-2013-0575`, a patient under 50, was forecast **2076-09-01** for "Pneumococcal 50+ 1-dose PCV series."

**Mechanism.** Before SPEC-4.6-0020's fix, Chapter 8 ran as one undivided competition across every series group of an antigen - an age-inappropriate series with no real administered doses simply lost the scoring comparison to the antigen's genuine winner, so this gap was invisible. Once Chapter 8 is correctly scoped per series group (matching the specification's own "repeated for each series group" text), a solo-series group with an unenforced age gate reaches `PreFilterPatientSeries`'s default-series fallback with a trivially-zero *within-group* valid-dose count, is admitted as the group's default, and - being alone in its group - proceeds straight through Table 8-3 Rule 2 (a single candidate needs no scoring) into `bestPatientSeriesList`, regardless of the patient's actual age.

**Where this doesn't belong.** This is not a defect in `PreFilterPatientSeries` or any other single Chapter 8 step's own business rules - Table 8-2 does not describe an age-applicability gate at all, narrow or general, so there's no "wrong" Chapter 8 table to fix. Chapter 8 is the wrong layer entirely.

**Where this probably belongs.** The most likely correct home is earlier in the pipeline, where "relevant patient series" is first decided - 4.3 Create Relevant Patient Series or 5.1 Select Relevant Patient Series. A patient who has not reached `minAgeToStart`, or who has passed `maxAgeToStart` with nothing administered toward the series, arguably should never have that series appear as a *relevant* patient series at all - upstream of every question Chapter 8 asks. Confirming that placement requires reading 4.3/5.1's own business rules and Supporting Data usage in detail, which this finding does not attempt. Recording the defect and its confirmed mechanism is this round's contribution; placement is a separate Role A/B decision, and per the standing cross-step rule, not one to make unilaterally from inside a Chapter 8 round.

**Materiality.** At least three of the bundled release's antigens define a solo-series, age-gated series group of this exact shape:

- Pneumococcal - "Standard 50+" (group 3), `minAgeToStart` 50 years
- RSV - "Standard 75+" (group 3), `minAgeToStart` 50 years (the series dose's own age window narrows this further to 75)
- At least one Meningococcal series group

Together these account for the great majority of SPEC-4.6-0020's 175-case FITS regression (PCV 143, RSV 20, MEN 12), and the shape is likely to recur wherever the bundled release defines an age-split "alternate" series group for a single antigen.

## Affected

- Spec sections: 8.1 (page 87, Table 8-2) - the layer where this is exposed, not necessarily where it should be fixed
- Code locations: `SelectPatientSeries` (missing `minAgeToStart` field), `DataModelLoader` (parses `maxAgeToStart`, not `minAgeToStart`), `PreFilterPatientSeries` (where the unguarded fallback lives) - likely also 4.3 `CreateRelevantPatientSeries` / 5.1 `SelectRelevantPatientSeries`, not yet investigated
- FITS cases: at least 175, the bulk of SPEC-4.6-0020's regression (PCV 143, RSV 20, MEN 12)
