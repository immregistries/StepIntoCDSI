# FITS fixtures

Each `*.json` file under this directory is one FITS test case, in the shape
`org.openimmunizationsoftware.cdsi.fitstests.FitsTestCase` deserializes.
`FitsFixtureTest` runs every one of them against `cdsi-engine` directly.

## `sample/`

`SAMPLE-001.json` is **not** an official NIST FITS test case - it's a
hand-built fixture whose expected values were taken directly from the
engine's own output for that input, kept here to demonstrate and
regression-check the fixture-loading/engine-running/comparison mechanism
itself. Real conformance fixtures come only from `FitsDownloader`.

## Refreshing from FITS

FITS moved from `fits.nist.gov` to <https://tools.valitheus.com/fits/> in
2026 (same API, new host). Real fixtures are produced by `FitsDownloader`
(`org.openimmunizationsoftware.cdsi.fitstests.download.FitsDownloader`),
run by hand against a live FITS account:

```
FITS_USERNAME=yourUsername \
FITS_PASSWORD=yourPassword \
mvn -pl cdsi-fits-tests exec:java \
  -Dexec.mainClass=org.openimmunizationsoftware.cdsi.fitstests.download.FitsDownloader
```

(`FITS_URL` optionally overrides the default `https://tools.valitheus.com/`;
the older `NIST_FITS_*` variable names are still accepted.)

### Which plans are in scope

Only the test plans listed in `cdsi-fits-tests/fits-plans.txt` are
downloaded: the current CDSi plan, AIRA Test Cases for CDS v3, and the AIRA
Extra Test Cases. FITS keeps every earlier CDSi revision as a "HISTORICAL"
plan, and most cases carry forward from one revision to the next, so
including them would run the same case several times. A refresh removes
every plan folder here (except `sample/`) and rewrites only the listed
plans, so what's on disk always matches that file exactly. When NIST
publishes a new CDSi plan, swap its id into `fits-plans.txt`.

It writes one file per test case under `<testPlanId>/<groupName>/<uid>.json`,
always with LF line endings (the files are checksummed byte-for-byte - see
`reference-set.json`). Each vaccination carries FITS' `expectedEvaluations`
(the expected VALID / NOT_VALID / EXTRANEOUS / SUB_STANDARD verdict for that
dose against one vaccine group) alongside the case's `expectedForecasts`.

Review the diff before committing a refresh. Expect every date in the
relative-dated cases (e.g. the AIRA ones) to shift, because FITS resolves
them against the download day; apart from that, FITS test cases change
rarely (see `docs/16-fits-conformance-philosophy-vs-clinical-correctness.md`
in the main project for why FITS is treated as the operational conformance
oracle here). A refresh changes the fixture checksum, so create and export a
new reference set from `cdsi-reference` afterwards.
