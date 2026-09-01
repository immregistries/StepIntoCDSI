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

## Refreshing from NIST

Real fixtures are produced by `FitsDownloader`
(`org.openimmunizationsoftware.cdsi.fitstests.download.FitsDownloader`),
run by hand against a live NIST FITS account:

```
NIST_FITS_URL=https://fits.nist.gov/ \
NIST_FITS_USERNAME=yourUsername \
NIST_FITS_PASSWORD=yourPassword \
mvn -pl cdsi-fits-tests exec:java \
  -Dexec.mainClass=org.openimmunizationsoftware.cdsi.fitstests.download.FitsDownloader
```

It writes one file per test case under `<testPlanId>/<groupName>/<uid>.json`,
overwriting whatever was there before. Review the diff before committing a
refresh - FITS test cases change rarely, so most of the time the diff should
be empty or very small (see `docs/16-fits-conformance-philosophy-vs-clinical-correctness.md`
in the main project for why FITS is treated as the operational conformance
oracle here).
