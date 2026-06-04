# StepIntoCDSI - Developer Guide

## Build Commands
- `mvn clean install` - Full build with tests
- `mvn test` - Run tests only
- `mvn package` - Create WAR file

## Requirements
- Java 17
- Maven 3.x

## Project Structure
- `src/main/java/org/openimmunizationsoftware/cdsi/` - Main source
  - `core/` - CDSI logic and domain model
  - `servlet/` - Web endpoints
  - `auth/` - Authentication
- `src/test/` - JUnit 4 tests
- `_source/` - XML test data (immunization schedules)
- `docs/` - Logic documentation and ACIP specification

## Running
- Direct: Run `org.openimmunizationsoftware.cdsi.Start` main class
- Tomcat: Deploy `target/step.war` to Tomcat 10+
- Local deploy: `mvn package -Plocal-tomcat-deploy -Dtomcat.webapps.dir=/path`

## Local Dependencies
- Some dependencies in `repo/` folder (local Maven repo)

## Testing
- Uses JUnit 4
- Test data loaded from `_source/` XML files
- Tests verify logic table evaluation results
