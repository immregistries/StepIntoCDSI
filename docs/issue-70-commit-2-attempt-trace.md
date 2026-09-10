# Issue 70 Commit 2 Attempt Trace

Attempt branch: `copilot/chapter-8-commit-2-attempt`

The coordinated null-status change was tested against three `PASS -> FAIL`
cases from the FITS diff:

- `5eeca0522cc4517a96b0f417-HIB-2013-0281`
- `5eeca0522cc4517a96b0f417-HPV-2013-0416`
- `5eeca0522cc4517a96b0f417-PCV-2013-0576`

At `PreFilterPatientSeries` (8.1), the HPV and PCV traces both contained
`COVID-19 start at 2 years+ shared clinical decision-making series` with
`status=null`. The HIB trace had no null HIB status, but contained that same
null-status COVID series in the list passed to 8.1. Therefore the HIB
regression is cross-antigen contamination, not a missing HIB status.

`DetermineForecastNeed` (7.4) is the normal writer of patient-series status;
the trace shows these series genuinely reach 8.1 without a status after the
upstream flow. The attempted null-tolerance fix admits them, but the existing
Chapter 8 scope defect still gives 8.1 a list containing unrelated antigens.
That makes the narrowly coordinated fix unsafe until the scope defect is
addressed together with it.

The full case-level FITS diff is committed beside this file as
`issue-70-commit-2-attempt-changed-cases.json`.