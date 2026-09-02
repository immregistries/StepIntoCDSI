# StepIntoCDSI - Developer Guide

## Build Commands
- `mvn clean install` - Full reactor build (all modules) with tests
- `mvn test` - Run tests only
- `mvn package` - Build all modules' artifacts (`cdsi-engine.jar`, `cdsi-web/target/step.war`, `cdsi-fits-tests.jar`)
- `mvn -pl <module> -am install` - Build just one module (and whatever it depends on)

## Requirements
- Java 17
- Maven 3.x

## Project Structure
Multi-module Maven reactor, split into an engine module with no servlet/web
dependency and a web module built on it:
- `cdsi-engine/` - The CDSi calculation engine (jar). Runs headlessly - no
  jakarta.servlet on its classpath at all.
  - `src/main/java/org/openimmunizationsoftware/cdsi/core/` - domain model
    (`core.domain`), supporting-data loading (`core.data`), and the CDSi
    processing-model steps (`core.logic`, plus `items`/`concepts`/`businessRules`)
  - `src/main/resources/` - bundled supporting-data zips and the raw
    AntigenSupportingData/ScheduleSupportingData XML/XSD files
  - `src/test/java/.../core/` - engine unit tests
- `cdsi-web/` - The web application (war), depends on `cdsi-engine`.
  - `src/main/java/org/openimmunizationsoftware/cdsi/servlet/` - web endpoints
    (forecast/step-through UI, FITS test-case runner, FHIR ImmDS+HALO
    operations, HTML rendering for the interactive step UI)
  - `src/main/java/org/openimmunizationsoftware/cdsi/auth/` - authentication
  - `src/main/webapp/` - web.xml, static assets, spec figures
- `cdsi-fits-tests/` - Runs the NIST FITS conformance suite against
  `cdsi-engine` directly, offline (no servlet container, no live NIST
  connection). This is the FITS regression check for local dev and for an
  AI agent debugging a failure: `mvn -pl cdsi-fits-tests test`.
  - `src/test/resources/fits/` - one JSON fixture per FITS test case (see the
    README.md there); a fresh checkout has none until `FitsDownloader` is run
  - `src/main/java/.../fitstests/download/FitsDownloader.java` - the
    dev-time tool that refreshes those fixtures from a live NIST FITS
    account (needs `NIST_FITS_URL`/`NIST_FITS_USERNAME`/`NIST_FITS_PASSWORD`
    env vars - see its class Javadoc)
- `cdsi-reference/` - Python tooling and versioned, agent-readable copies
  of the Logic Specification and Supporting Data, mapped to `cdsi-engine`
  classes/tests. NOT a Maven module and never a runtime dependency of the
  other three. See `cdsi-reference/README.md` and its own `AGENTS.md`
  before touching clinical logic anywhere in this repository - read the
  relevant step package first. Status and full 20-phase design:
  `StepIntoCDSi-Specification-Reference-Module-Plan.md` at the repository root.
- `docs/` - Logic documentation and ACIP specification

## Running
- Tomcat: Deploy `cdsi-web/target/step.war` to Tomcat 10+
- Local deploy: `mvn -pl cdsi-web -am package -Plocal-tomcat-deploy -Dtomcat.webapps.dir=/path`

## Local Dependencies
- Some `cdsi-web` dependencies (NIST FITS client jars) come from the `repo/`
  folder (local Maven repo) at the project root

## Testing
- `cdsi-engine`/`cdsi-web` use JUnit 4; `cdsi-fits-tests` uses JUnit 5 (its
  FITS suite is a `@TestFactory` generating one dynamic test per fixture)
- `cdsi-engine` tests load supporting data from its own `src/main/resources/`
  (bundled zips and XML files)
- Tests verify logic table evaluation results
