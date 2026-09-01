# cdsi-fits-tests

Runs the NIST FITS conformance suite against `cdsi-engine` directly and offline - no servlet container, no live NIST connection once fixtures are downloaded (`org.openimmunizationsoftware.cdsi.fitstests.download.FitsDownloader`, run once against a live NIST FITS account; see its class Javadoc).

```bash
mvn -pl cdsi-fits-tests test
```

`FitsFixtureTest` is a `@TestFactory` that turns every fixture under `src/test/resources/fits/` into a dynamic test. A fresh checkout with no fixtures downloaded yet produces zero dynamic tests - that's a no-op, not a failure.

## Which supporting data set the suite runs against

`cdsi-engine` can bundle more than one supporting-data zip under `src/main/resources/supporting-data/` - the standard CDC set (possibly at more than one published version, if an older one hasn't been removed yet) and, for the UI's benefit, alternative schedules such as a demo/preview set. `DefaultSupportingDataSet.resolve()` picks which one this suite forecasts against, and it is deliberately narrow:

- Only zips matching `supporting-data-<version>[-508].zip` are considered - anything else (a demo/preview set, an alternative schedule) is ignored. FITS conformance is only meaningful against the standard CDC set.
- Among those, it picks the **highest version number**, comparing dot-separated segments numerically (`4.10` > `4.9`), never the first one found or a lexicographic/alphabetical comparison (`"4.10"` sorts before `"4.9"` as a string).

This means adding a newly-published CDC supporting-data zip to `cdsi-engine/src/main/resources/supporting-data/` automatically makes the FITS suite start running against it, with no code change here required - which is the intended behavior: internal testing and verification should always run against the latest published supporting data, even though the web UI can still let a user explicitly select an older version or an alternative schedule (see `cdsi-web`'s `SupportingDataManager.resolveDefaultSupportingDataSet()`, which applies the identical "latest wins, unless explicitly overridden" rule to the UI's own default).

`DefaultSupportingDataSetTest` locks this in against cdsi-engine's real bundled resources - if it starts failing after adding or removing a zip, that's it correctly reporting an ambiguous or unexpected bundled set, not a flaky test.

Retiring an old CDC version's zip from `src/main/resources/supporting-data/` is a deliberate decision for whoever maintains this repository, not something either the FITS suite or the web UI's default forces - both are just consumers of whatever cdsi-engine bundles.
