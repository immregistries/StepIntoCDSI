# A2: Cervarix given to males is called valid

Follow `.cursor/rules/fits-evaluation.mdc` and `cdsi-fits-tests/AGENTS.md`. Do not commit.
Do A1 first; it touches the same inadvertent-vaccine step.

## Goal

Thirteen doses of 118 (Cervarix, HPV bivalent) given to male patients. FITS says
NOT_VALID with reason J (inadvertent vaccine); the engine says VALID, sometimes with
reason GRACE_PERIOD.

Cases: HPV-2013-0423, 0424, 0425, 0426, 0430 (2 doses), 0437 (3), 0438 (3), 0483.

```bash
python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --group HPV --all-doses
```

## What we already know

- In Supporting Data 4.65, 118 is an inadvertent vaccine only in the male HPV series
  ("HPV male 2-dose series", "HPV male 3-dose ...", "HPV risk male ..."), whose
  `requiredGender` is `Male`. The female/unknown series (`Female`, `Unknown`) accept it.
- Until 2026-11-02 every FITS fixture said `patientSex: "F"` (a downloader bug). That is
  fixed and these fixtures now say `"M"`, yet the results did not change at all. So the
  engine does not pick a male series even when told the patient is male.

## Where to look first

1. How the patient's sex reaches the engine: `ForecastInput.patientSex` ("M"/"F") ->
   `GatherNecessaryData` -> `Patient.setGender`. Supporting Data spells it `Male`,
   `Female`, `Unknown`. Check whether anything converts between the two.
2. `SelectRelevantPatientSeries` (step 5.1, Tables 5-4/5-5): it compares the patient's
   gender with `AntigenSeries.getRequiredGenderList()`, and when that list is empty it
   assumes the series matches. Check where `AntigenSeries.requiredGenderList` is filled.
   `DataModelLoader` (around line 890) only adds `requiredGender` to `SeriesDose`, not to
   the series.
3. Read the 5.1 step package and the 4.6 spec text for how required gender is defined
   (series level vs. series dose level, and how "Unknown" works).
4. Once a male series is selected, confirm step 6.3 rejects 118 as inadvertent.

## Watch for

- This affects every gender-restricted series, not only HPV (e.g. the Pertussis risk
  1-dose series is Female/Unknown only, for pregnancy). Run the full suite and read every
  `statusChanged` and dose change, especially HPV, DTAP and RSV.
- The web app (`FitsServlet`, the forecast UI and the FHIR endpoints) passes "M"/"F" too.
  Any conversion belongs in the engine, so every caller gets it.

## Stop and ask if

- The fix makes any currently passing forecast fail.
- The spec is ambiguous about how a patient's sex is represented in input versus how
  Supporting Data names it. That is a finding to discuss, not a guess to code.
