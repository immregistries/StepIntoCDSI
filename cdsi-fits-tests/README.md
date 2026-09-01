# cdsi-fits-tests

Runs the NIST FITS conformance suite against `cdsi-engine` directly and offline - no servlet container, no live NIST connection once fixtures are downloaded (`org.openimmunizationsoftware.cdsi.fitstests.download.FitsDownloader`, run once against a live NIST FITS account; see its class Javadoc).

```bash
mvn -pl cdsi-fits-tests test
```

`FitsFixtureTest` is a `@TestFactory` that turns every fixture under `src/test/resources/fits/` into a dynamic test. The fixtures themselves are committed, so a normal checkout already has all 4896 of them - `FitsDownloader` is only for refreshing them from a live NIST FITS account, not a prerequisite for a first run.

## Which supporting data set the suite runs against

`cdsi-engine` can bundle more than one supporting-data zip under `src/main/resources/supporting-data/` - the standard CDC set (possibly at more than one published version, if an older one hasn't been removed yet) and, for the UI's benefit, alternative schedules such as a demo/preview set. `DefaultSupportingDataSet.resolve()` picks which one this suite forecasts against, and it is deliberately narrow:

- Only zips matching `supporting-data-<version>[-508].zip` are considered - anything else (a demo/preview set, an alternative schedule) is ignored. FITS conformance is only meaningful against the standard CDC set.
- Among those, it picks the **highest version number**, comparing dot-separated segments numerically (`4.10` > `4.9`), never the first one found or a lexicographic/alphabetical comparison (`"4.10"` sorts before `"4.9"` as a string).

This means adding a newly-published CDC supporting-data zip to `cdsi-engine/src/main/resources/supporting-data/` automatically makes the FITS suite start running against it, with no code change here required - which is the intended behavior: internal testing and verification should always run against the latest published supporting data, even though the web UI can still let a user explicitly select an older version or an alternative schedule (see `cdsi-web`'s `SupportingDataManager.resolveDefaultSupportingDataSet()`, which applies the identical "latest wins, unless explicitly overridden" rule to the UI's own default).

`DefaultSupportingDataSetTest` locks this in against cdsi-engine's real bundled resources - if it starts failing after adding or removing a zip, that's it correctly reporting an ambiguous or unexpected bundled set, not a flaky test.

Retiring an old CDC version's zip from `src/main/resources/supporting-data/` is a deliberate decision for whoever maintains this repository, not something either the FITS suite or the web UI's default forces - both are just consumers of whatever cdsi-engine bundles.

## Reference sets and fixture-set integrity

Before building the dynamic test list, `FitsFixtureTest` calls `ReferenceSetVerifier.loadAndVerify()`, which reads `src/test/resources/reference-set.json` (a plain JSON snapshot exported from `cdsi-reference` - see its `reference-sets/README.md`, Phase 16 of the reference-module plan) and re-derives two checksums:

- The bundled Supporting Data zip named in the reference set (read straight off the classpath, the same way `DefaultSupportingDataSet` finds it) must still hash to what the reference set recorded.
- Every fixture file actually under `src/test/resources/fits/` must still hash, combined, to what the reference set recorded (`ReferenceSetVerifier.computeFixtureSet()` - the identical algorithm, byte-for-byte, as `cdsi-reference`'s Python `compute_fixture_set`; `ReferenceSetVerifierTest` locks in that the two independently-computed values actually agree).

If either has drifted - a newer Supporting Data release replaced the one the reference set was created against, or the fixture files changed - `fitsFixtures()` throws `IllegalStateException` with a message naming exactly what changed, and JUnit reports the whole test class as failed rather than silently running against something the reference set doesn't describe. When that happens, the fix is to create and export a new reference set from `cdsi-reference` (`reference-set create` + `reference-set export`), not to edit this module's copy by hand.

The Logic Specification version recorded in the reference set is not checked here - there's no runtime artifact in a compiled `cdsi-engine` build to compare it against.
