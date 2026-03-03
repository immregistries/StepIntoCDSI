# FHIR Knowledge Base Integration - Implementation Summary

## Overview

This implementation replaces the legacy `supportingDataSet` parameter with the ImmDS-standard `knowledgeBase` and `knowledgeBaseVersion` parameters in the FHIR `$immds-forecast` operation. The changes align with the ImmDS proposal for standardized knowledge base selection and version management.

## Changes Made

### 1. VersionComparator Utility (NEW)

**File:** `src/main/java/org/openimmunizationsoftware/cdsi/servlet/VersionComparator.java`

A new utility class for comparing version strings using numeric segment comparison:

- **parseVersion(String)**: Parses dot-separated version strings into integer segments
- **compareVersions(String, String)**: Compares two versions numerically (returns -1, 0, or 1)
- **selectLatest(Collection<String>)**: Selects the latest version from a collection

**Key Features:**
- Numeric comparison: `4.10 > 4.9` (not alphabetical)
- Trailing zeros: `4.10 == 4.10.0`
- Leading zeros: `2025.02 == 2025.2`

**Test Coverage:** 8 unit tests in `VersionComparatorTest.java` covering edge cases and real-world scenarios.

---

### 2. ImmunizationRecommendationForecastProvider (UPDATED)

**File:** `src/main/java/org/openimmunizationsoftware/cdsi/servlet/fhir/ImmunizationRecommendationForecastProvider.java`

#### Parameter Changes

**REMOVED:**
- `@OperationParam` `supportingDataSet` (String, min=0, max=1)

**ADDED:**
- `@OperationParam` `knowledgeBase` (CodeableConcept, min=0, max=1)
- `@OperationParam` `knowledgeBaseVersion` (String, min=0, max=1)

#### Response Changes

**ADDED Output Parameters:**
- `knowledgeBaseUsed` (CodeableConcept, min=1, max=1)
  - System: `https://ivci.org/knowledge-base`
  - Code: `USA-CDC-CDSI` (hardcoded for now)
- `knowledgeBaseVersionUsed` (String, min=1, max=1)
  - Version extracted from resolved supporting data set (e.g., `4.64`)

#### New Helper Methods

1. **resolveSupportingDataSet(String requestedVersion)**
   - Maps requested knowledge base version to supporting data set ID
   - If version is null/empty → select latest using VersionComparator
   - If version specified → find exact match or return latest
   
2. **selectLatestSupportingDataSet(List<String> setIds)**
   - Extracts versions from set IDs
   - Uses VersionComparator to find latest
   - Falls back to alphabetical if no parseable versions
   
3. **extractVersionFromSetId(String setId)**
   - Parses version from set IDs like `"supporting-data-4.64-508"` → `"4.64"`
   - Removes `"supporting-data-"` prefix
   - Extracts numeric segments up to first separator (dash, underscore)

#### Validation

- Validates knowledge base ID against `USA-CDC-CDSI`
- Returns OperationOutcome with HTTP 400 for unsupported knowledge bases
- Severity: ERROR
- Issue type: NOTSUPPORTED

---

### 3. SandboxServlet (UPDATED)

**File:** `src/main/java/org/openimmunizationsoftware/cdsi/servlet/SandboxServlet.java`

#### Model Class Changes

Added two new fields to the `Model` class:
```java
public String knowledgeBase = "";
public String knowledgeBaseVersion = "";
```

Constructor updated to read these parameters from request:
```java
this.knowledgeBase = req.getParameter("knowledgeBase") != null ? req.getParameter("knowledgeBase") : "";
this.knowledgeBaseVersion = req.getParameter("knowledgeBaseVersion") != null ? req.getParameter("knowledgeBaseVersion") : "";
```

#### UI Changes

Added two new form fields after the encoding selection:

**Knowledge Base ID:**
```html
<input type="text" id="knowledgeBase" name="knowledgeBase" 
       value="..." placeholder="USA-CDC-CDSI">
```

**Knowledge Base Version:**
```html
<input type="text" id="knowledgeBaseVersion" name="knowledgeBaseVersion" 
       value="..." placeholder="Leave blank to use latest">
<small style="color: #666;">
  Leave blank to use latest available (computed using numeric segment comparison).
</small>
```

#### Request Building Changes

**Updated runForecast() signature:**
```java
private Result runForecast(String endpointBase, String encoding, 
    LocalDate assessmentDate, LocalDate patientDob, String patientSex, 
    List<ImmunizationRecord> immunizations,
    String knowledgeBase, String knowledgeBaseVersion)
```

**Added FHIR Parameters:**
```java
// Knowledge base parameter (CodeableConcept)
if (knowledgeBase != null && !knowledgeBase.trim().isEmpty()) {
    CodeableConcept kbConcept = new CodeableConcept();
    kbConcept.addCoding()
        .setSystem("https://ivci.org/knowledge-base")
        .setCode(knowledgeBase.trim())
        .setDisplay(knowledgeBase.trim());
    requestParams.addParameter().setName("knowledgeBase").setValue(kbConcept);
}

// Knowledge base version parameter (string)
if (knowledgeBaseVersion != null && !knowledgeBaseVersion.trim().isEmpty()) {
    requestParams.addParameter("knowledgeBaseVersion", 
        new StringType(knowledgeBaseVersion.trim()));
}
```

**Import Added:**
```java
import org.hl7.fhir.r4.model.StringType;
```

---

## Behavior Matrix

| Input KB ID | Input KB Version | Server Behavior | Output KB Version Used |
|-------------|-----------------|-----------------|----------------------|
| (empty) | (empty) | Select latest USA-CDC-CDSI | Latest (via numeric comparison) |
| USA-CDC-CDSI | (empty) | Select latest version | Latest (via numeric comparison) |
| USA-CDC-CDSI | 4.64 | Use exact match if exists | 4.64 (if found) or latest |
| OTHER-KB | (any) | Return OperationOutcome HTTP 400 | N/A |

## Version Comparison Examples

Given versions: `["4.9", "4.10", "4.64", "4.63"]`

- Latest selected: `4.64`
- Comparison: `4.10 > 4.9` (numeric, not alphabetical)
- Trailing zeros: `4.10 == 4.10.0`
- Leading zeros: `2025.02 == 2025.2`

## Testing

### Automated Tests

1. **VersionComparatorTest**: 8 tests covering:
   - Version parsing
   - Numeric comparison (4.10 > 4.9)
   - Trailing zeros (4.10 == 4.10.0)
   - Leading zeros (2025.02 == 2025.2)
   - Latest selection
   - Real-world version strings

2. **All existing tests pass**: 19 tests total, 0 failures

### Build Status

```
mvn clean package
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Manual Testing Checklist

To manually test the FHIR integration:

1. **Start Tomcat** (WAR deployed to `/webapps/step.war`)
2. **Navigate to `/step/sandbox`**
3. **Test Case 1: Default (latest version)**
   - Leave Knowledge Base ID empty (or enter "USA-CDC-CDSI")
   - Leave Knowledge Base Version empty
   - Submit forecast
   - Verify response includes `knowledgeBaseUsed` and `knowledgeBaseVersionUsed` with latest version
   
4. **Test Case 2: Specific version**
   - Enter "USA-CDC-CDSI" in Knowledge Base ID
   - Enter "4.64" in Knowledge Base Version
   - Submit forecast
   - Verify response uses version 4.64
   
5. **Test Case 3: Invalid knowledge base**
   - Enter "INVALID-KB" in Knowledge Base ID
   - Submit forecast
   - Verify OperationOutcome with HTTP 400 error
   
6. **Test Case 4: Check request/response formatting**
   - Inspect the "Request" and "Response" sections in sandbox UI
   - Verify CodeableConcept structure in request
   - Verify knowledgeBaseUsed/knowledgeBaseVersionUsed in response

---

## Migration Notes

### For Existing Clients

**Old Request:**
```json
{
  "resourceType": "Parameters",
  "parameter": [
    {"name": "supportingDataSet", "valueString": "supporting-data-4.64-508"},
    ...
  ]
}
```

**New Request:**
```json
{
  "resourceType": "Parameters",
  "parameter": [
    {
      "name": "knowledgeBase",
      "valueCodeableConcept": {
        "coding": [{
          "system": "https://ivci.org/knowledge-base",
          "code": "USA-CDC-CDSI"
        }]
      }
    },
    {"name": "knowledgeBaseVersion", "valueString": "4.64"},
    ...
  ]
}
```

**New Response:**
```json
{
  "resourceType": "Parameters",
  "parameter": [
    {"name": "recommendation", "resource": {...}},
    {
      "name": "knowledgeBaseUsed",
      "valueCodeableConcept": {
        "coding": [{
          "system": "https://ivci.org/knowledge-base",
          "code": "USA-CDC-CDSI",
          "display": "USA-CDC-CDSI"
        }]
      }
    },
    {"name": "knowledgeBaseVersionUsed", "valueString": "4.64"}
  ]
}
```

### Backward Compatibility

**REMOVED:** The `supportingDataSet` parameter is no longer supported. Clients using it will not receive an error but the parameter will be ignored. Clients should migrate to the new `knowledgeBase` and `knowledgeBaseVersion` parameters.

---

## Future Enhancements

1. **Multiple Knowledge Bases**: Currently hardcoded to `USA-CDC-CDSI`. Future versions could support:
   - State-specific schedules (e.g., `USA-NY-CDSI`)
   - International schedules (e.g., `CAN-NACI`, `AUS-ATAGI`)
   
2. **Version Validation**: Return OperationOutcome if requested version doesn't exist (currently falls back to latest)

3. **Version Metadata**: Expose available versions via a separate operation (e.g., `$list-knowledge-bases`)

4. **Suffix Handling**: Currently ignores suffixes like `-508` in `4.64-508`. Could be enhanced to treat these as sub-versions.

---

## Files Modified

1. **NEW**: `src/main/java/org/openimmunizationsoftware/cdsi/servlet/VersionComparator.java`
2. **NEW**: `src/test/java/org/openimmunizationsoftware/cdsi/servlet/VersionComparatorTest.java`
3. **UPDATED**: `src/main/java/org/openimmunizationsoftware/cdsi/servlet/fhir/ImmunizationRecommendationForecastProvider.java`
4. **UPDATED**: `src/main/java/org/openimmunizationsoftware/cdsi/servlet/SandboxServlet.java`

Total: 2 new files, 2 updated files

---

## Deployment

1. Build the project: `mvn clean package`
2. Deploy `target/step.war` to Tomcat `webapps/` directory
3. Restart Tomcat
4. Access the sandbox at `http://localhost:8080/step/sandbox`

---

## References

- **ImmDS Proposal**: Knowledge base and version selection specification
- **FHIR R4**: CodeableConcept, Parameters, OperationOutcome
- **IVCI Knowledge Base System**: `https://ivci.org/knowledge-base`
