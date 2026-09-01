# StepIntoCDSI - Developer Guide

## Build Commands
- `mvn clean install` - Full reactor build (both modules) with tests
- `mvn test` - Run tests only
- `mvn package` - Build both modules' artifacts (`cdsi-engine.jar`, `cdsi-web/target/step.war`)
- `mvn -pl cdsi-engine -am install` / `mvn -pl cdsi-web -am install` - Build just one module (and whatever it depends on)

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
- `docs/` - Logic documentation and ACIP specification

## Running
- Tomcat: Deploy `cdsi-web/target/step.war` to Tomcat 10+
- Local deploy: `mvn -pl cdsi-web -am package -Plocal-tomcat-deploy -Dtomcat.webapps.dir=/path`

## Local Dependencies
- Some `cdsi-web` dependencies (NIST FITS client jars) come from the `repo/`
  folder (local Maven repo) at the project root

## Testing
- Uses JUnit 4
- `cdsi-engine` tests load supporting data from its own `src/main/resources/`
  (bundled zips and XML files)
- Tests verify logic table evaluation results
