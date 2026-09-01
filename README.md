# StepIntoCDSi

A transparent reference implementation of the CDC Clinical Decision Support for Immunization (CDSi) Logic Specification. See `StepIntoCDSi-Project-Overview.md` (one directory up) for the project's purpose and audience.

## Modules

- **`cdsi-engine`** - the CDSi calculation engine (jar). Domain model, supporting-data loading, and the CDSi processing-model steps. No servlet/web dependency - runs headlessly.
- **`cdsi-web`** - the web application (war), depends on `cdsi-engine`. Forecast/step-through UI, FITS test-case browser, FHIR ImmDS+HALO operations, authentication.
- **`cdsi-fits-tests`** - runs the NIST FITS conformance suite against `cdsi-engine` directly and offline (no servlet container, no live NIST connection once fixtures are downloaded). `mvn -pl cdsi-fits-tests test` is the FITS regression suite.
- **`cdsi-reference`** - versioned, agent-readable copies of the Logic Specification and Supporting Data, deterministically extracted and mapped to `cdsi-engine`'s classes and tests. A development/documentation asset (Python tooling, not part of the Maven reactor) - supports specification-to-code comparison, Supporting Data comparison, and reproducible interpretation of FITS results. See `StepIntoCDSi-Specification-Reference-Module-Plan.md` (repository root) for its full design and `cdsi-reference/README.md` for what's built so far.

## Build

```bash
mvn clean install     # builds cdsi-engine, cdsi-web, cdsi-fits-tests
```

See each module's own README/AGENTS.md for details specific to it, and `AGENTS.md` at this level for the overall developer guide.
