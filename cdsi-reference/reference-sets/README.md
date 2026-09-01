# Reference Sets

Reviewed bindings that pin together an exact Logic Specification version, Supporting Data release, and `cdsi-fits-tests` fixture set, so a FITS run can record one identifier instead of three separately-drifting version numbers (Phase 16 of `StepIntoCDSi-Specification-Reference-Module-Plan.md`).

```bash
python -m cdsi_reference_tools reference-set create --logic-spec 4.6 --supporting-data 4.65
python -m cdsi_reference_tools reference-set list
python -m cdsi_reference_tools reference-set validate --id <id>
python -m cdsi_reference_tools reference-set export --id <id>
```

Each `<id>.yaml` here records its own checksums (the Logic Specification source PDF's, the Supporting Data release's bundle, and the FITS fixture set's) rather than just bare version numbers - `validate` re-derives all three from the current state of the repository and reports drift. The id itself (`acip-<logic-spec>-sd-<supporting-data>-fits-<fixture-checksum-prefix>`) is derived from those checksums, not chosen by hand.

`export` writes the subset of fields `cdsi-fits-tests`' Java code needs as a plain JSON file checked into that module (`cdsi-fits-tests/src/test/resources/reference-set.json`) - a one-way, reviewed export, not a live cross-module read. `ReferenceSetVerifier` (in `cdsi-fits-tests`) re-checks the Supporting Data and FITS-fixture-set checksums against what's actually bundled and on the classpath before every FITS run, and fails clearly if either has drifted since the reference set was created. The Logic Specification binding is recorded for traceability but not verified at Java runtime - `cdsi-engine` carries no marker of which specification version its code implements, so there is no runtime artifact to check it against.

One active reference set exists: `acip-4.6-sd-4.65-fits-8183b45d`, binding the current Logic Specification version (4.6), the latest Supporting Data release (4.65), and the 4896 FITS fixtures currently committed to `cdsi-fits-tests`.
