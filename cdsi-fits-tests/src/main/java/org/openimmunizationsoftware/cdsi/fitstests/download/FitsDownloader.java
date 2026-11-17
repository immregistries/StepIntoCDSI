package org.openimmunizationsoftware.cdsi.fitstests.download;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.openimmunizationsoftware.cdsi.fitstests.FitsDates;
import org.openimmunizationsoftware.cdsi.fitstests.FitsTestCase;

import gov.nist.healthcare.cds.domain.Event;
import gov.nist.healthcare.cds.domain.ExpectedForecast;
import gov.nist.healthcare.cds.domain.FixedDate;
import gov.nist.healthcare.cds.domain.RelativeDate;
import gov.nist.healthcare.cds.domain.RelativeDateRule;
import gov.nist.healthcare.cds.domain.StaticDateReference;
import gov.nist.healthcare.cds.domain.TestCase;
import gov.nist.healthcare.cds.domain.TestCaseGroup;
import gov.nist.healthcare.cds.domain.TestPlan;
import gov.nist.healthcare.cds.domain.VaccinationEvent;
import gov.nist.healthcare.cds.domain.VaccineDateReference;
import gov.nist.hit.resources.deploy.client.SSLFITSClient;

/**
 * One-time (or occasional-refresh) dev tool: connects to a live NIST FITS
 * account, pulls every test plan/group/case, resolves each case's dates
 * (FITS expresses many as fixed dates or as rules relative to "today" or to
 * another event - this resolves them once, here, so the downstream JSON
 * fixture is a plain, deterministic snapshot), and writes one JSON file per
 * test case under src/test/resources/fits/&lt;testPlanId&gt;/&lt;groupName&gt;/&lt;uid&gt;.json.
 *
 * This is the ONLY class in this module that talks to NIST or needs
 * fits-client on the classpath - FitsFixtureTest runs entirely offline
 * against the JSON this writes.
 *
 * FITS test cases change rarely (see docs/16-fits-conformance-philosophy-vs-clinical-correctness.md
 * in the main project) - re-run this only when NIST publishes a new suite
 * revision, not as part of routine development.
 *
 * Usage (credentials via environment variables, never as command-line args -
 * those end up in shell history and process listings):
 * <pre>
 *   NIST_FITS_URL=https://fits.nist.gov/ \
 *   NIST_FITS_USERNAME=yourUsername \
 *   NIST_FITS_PASSWORD=yourPassword \
 *   mvn -pl cdsi-fits-tests exec:java \
 *     -Dexec.mainClass=org.openimmunizationsoftware.cdsi.fitstests.download.FitsDownloader
 * </pre>
 * or run this class's main() directly from an IDE with those three
 * environment variables set.
 */
public final class FitsDownloader {

  private static final SimpleDateFormat NIST_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

  private FitsDownloader() {
  }

  public static void main(String[] args) throws Exception {
    String url = requireEnv("NIST_FITS_URL");
    String username = requireEnv("NIST_FITS_USERNAME");
    String password = requireEnv("NIST_FITS_PASSWORD");
    File outputRoot = new File(args.length > 0 ? args[0] : defaultOutputDir());

    SSLFITSClient client = new SSLFITSClient(url, username, password);
    java.util.List<TestPlan> testPlans = client.getTestPlans().getBody();

    int written = 0;
    int skipped = 0;
    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    for (TestPlan testPlan : testPlans) {
      for (TestCaseGroup group : testPlan.getTestCaseGroups()) {
        for (TestCase testCase : group.getTestCases()) {
          try {
            FitsTestCase parsed = parse(testCase, testPlan, group);
            File dir = new File(outputRoot, sanitize(testPlan.getId()) + "/" + sanitize(group.getName()));
            dir.mkdirs();
            File file = new File(dir, sanitize(testCase.getUid()) + ".json");
            mapper.writeValue(file, parsed);
            written++;
          } catch (SkipTestCase skip) {
            System.err.println("Skipping " + group.getName() + " " + testCase.getUid() + ": " + skip.getMessage());
            skipped++;
          }
        }
      }
    }
    System.out.println("Wrote " + written + " fixtures to " + outputRoot.getAbsolutePath()
        + " (" + skipped + " test cases skipped - see stderr for reasons)");
  }

  /**
   * exec:java runs with the working directory of whatever process invoked
   * Maven, not the module's basedir - that's the root of the reactor when run
   * as `mvn -pl cdsi-fits-tests exec:java` from the project root (the
   * documented, normal way to run this), but plain `src/test/resources/fits`
   * from the module directory if run directly inside cdsi-fits-tests. Handle
   * both without requiring an explicit argument every time.
   */
  private static String defaultOutputDir() {
    File fromRoot = new File("cdsi-fits-tests/src/test/resources/fits");
    if (fromRoot.isDirectory()) {
      return fromRoot.getPath();
    }
    return "src/test/resources/fits";
  }

  private static String requireEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalStateException("Environment variable " + name + " is required");
    }
    return value;
  }

  private static String sanitize(String name) {
    return name == null ? "unknown" : name.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  /** Thrown for a test case that can't be resolved into usable fixture data (mirrors TestCaseRegistered's "problem" cases). */
  private static final class SkipTestCase extends Exception {
    SkipTestCase(String message) {
      super(message);
    }
  }

  private static FitsTestCase parse(TestCase testCase, TestPlan testPlan, TestCaseGroup group)
      throws SkipTestCase {
    if (testCase.getUid() == null || testCase.getUid().isEmpty()) {
      throw new SkipTestCase("test case id is not defined");
    }
    if (testCase.getPatient() == null || testCase.getPatient().getDob() == null) {
      throw new SkipTestCase("no patient date of birth is defined");
    }

    Date evalDate = new Date();
    if (testCase.getEvalDate() instanceof FixedDate) {
      FixedDate evalFixed = (FixedDate) testCase.getEvalDate();
      if (StringUtils.isNotBlank(evalFixed.getDateString())) {
        evalDate = parseNistDate(evalFixed.getDateString(), "eval date");
      }
    }

    Date birthDate;
    if (testCase.getPatient().getDob() instanceof FixedDate) {
      FixedDate dobFixed = (FixedDate) testCase.getPatient().getDob();
      if (StringUtils.isBlank(dobFixed.getDateString())) {
        throw new SkipTestCase("patient dob is blank");
      }
      birthDate = parseNistDate(dobFixed.getDateString(), "patient dob");
    } else if (testCase.getPatient().getDob() instanceof RelativeDate) {
      RelativeDate dobRelative = (RelativeDate) testCase.getPatient().getDob();
      if (dobRelative.getRules().isEmpty()) {
        throw new SkipTestCase("no relative rules for patient dob");
      }
      birthDate = resolveRelativeToEval(dobRelative, evalDate);
    } else {
      throw new SkipTestCase("patient dob is neither a fixed nor a relative date");
    }

    FitsTestCase fixture = new FitsTestCase();
    fixture.setUid(testCase.getUid());
    fixture.setTestPlanId(testPlan.getId());
    fixture.setTestPlanName(testPlan.getName());
    fixture.setGroupName(group.getName());
    fixture.setTitle(testCase.getName());
    fixture.setPatientSex("F");
    fixture.setBirthDate(FitsDates.format(birthDate));
    fixture.setEvalDate(FitsDates.format(evalDate));

    java.util.List<Date> vaccinationDates = new java.util.ArrayList<>();
    for (Event event : testCase.getEvents()) {
      if (!(event instanceof VaccinationEvent)) {
        continue;
      }
      VaccinationEvent vaccinationEvent = (VaccinationEvent) event;
      String cvx = vaccinationEvent.getAdministred().getCvx();
      Date vaccineDate = resolveEventDate(vaccinationEvent.getDate(), birthDate, vaccinationDates);
      if (cvx == null || vaccineDate == null) {
        continue; // matches TestCaseRegistered: an unresolved vaccination is dropped, not fatal to the case
      }
      vaccinationDates.add(vaccineDate);
      FitsTestCase.Vaccination vaccination = new FitsTestCase.Vaccination();
      vaccination.setDate(FitsDates.format(vaccineDate));
      vaccination.setCvx(cvx);
      vaccination.setMvx("");
      fixture.getVaccinations().add(vaccination);
    }

    if (testCase.getForecast().isEmpty()) {
      throw new SkipTestCase("no expected forecasts are defined");
    }
    for (ExpectedForecast expectedForecast : testCase.getForecast()) {
      if (expectedForecast.getSerieStatus() == null || expectedForecast.getTarget() == null) {
        continue;
      }
      FitsTestCase.ExpectedForecast expected = new FitsTestCase.ExpectedForecast();
      expected.setVaccineCvx(expectedForecast.getTarget().getCvx());
      expected.setSerieStatus(toEngineStatus(expectedForecast.getSerieStatus().name()));
      Date earliest = resolveEventDate(expectedForecast.getEarliest(), birthDate, vaccinationDates);
      Date recommended = resolveEventDate(expectedForecast.getRecommended(), birthDate, vaccinationDates);
      expected.setEarliestDate(FitsDates.format(earliest));
      expected.setRecommendedDate(FitsDates.format(recommended));
      fixture.getExpectedForecasts().add(expected);
    }
    if (fixture.getExpectedForecasts().isEmpty()) {
      throw new SkipTestCase("no usable expected forecasts (all missing serieStatus/target)");
    }
    return fixture;
  }

  /** NIST's SerieStatus enum (C/X/I/N/R/G) -> the engine's own VaccineGroupStatus name. */
  private static String toEngineStatus(String nistSerieStatusName) {
    switch (nistSerieStatusName) {
      case "C":
        return "COMPLETE";
      case "X":
        return "CONTRAINDICATED";
      case "I":
        return "IMMUNE";
      case "N":
        return "NOT_COMPLETE";
      case "R":
        return "NOT_RECOMMENDED";
      case "G":
        return "AGED_OUT";
      default:
        throw new IllegalArgumentException("Unrecognized NIST SerieStatus: " + nistSerieStatusName);
    }
  }

  private static Date parseNistDate(String dateString, String fieldForErrorMessage) throws SkipTestCase {
    try {
      return NIST_DATE_FORMAT.parse(dateString);
    } catch (ParseException e) {
      throw new SkipTestCase(fieldForErrorMessage + " could not be parsed: '" + dateString + "'");
    }
  }

  /** date field on an event/expectation - may be a FixedDate, a RelativeDate (to birth or another vaccination), or null/unset. */
  private static Date resolveEventDate(Object dateField, Date birthDate, java.util.List<Date> vaccinationDates) {
    if (dateField instanceof FixedDate) {
      FixedDate fixed = (FixedDate) dateField;
      if (StringUtils.isBlank(fixed.getDateString())) {
        return null;
      }
      try {
        return NIST_DATE_FORMAT.parse(fixed.getDateString());
      } catch (ParseException e) {
        return null;
      }
    }
    if (dateField instanceof RelativeDate) {
      RelativeDate relative = (RelativeDate) dateField;
      if (relative.getRules().isEmpty()) {
        return null;
      }
      return resolveRelativeFromBirthOrVaccination(relative, birthDate, vaccinationDates);
    }
    return null;
  }

  private static Date resolveRelativeToEval(RelativeDate relativeDate, Date evalDate) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(evalDate);
    for (RelativeDateRule rule : relativeDate.getRules()) {
      if (rule.getRelativeTo() instanceof StaticDateReference) {
        calendar.add(Calendar.DAY_OF_MONTH, -rule.getDay());
        calendar.add(Calendar.MONTH, -rule.getMonth());
        calendar.add(Calendar.DAY_OF_MONTH, -rule.getWeek() * 7);
        calendar.add(Calendar.YEAR, -rule.getYear());
      }
      // Any other "relative to" reference for a date-of-birth rule is not something
      // FITS actually uses; silently skipping a rule (rather than failing the whole
      // case) matches TestCaseRegistered's behavior for this same situation.
    }
    return calendar.getTime();
  }

  private static Date resolveRelativeFromBirthOrVaccination(RelativeDate relativeDate, Date birthDate,
      java.util.List<Date> vaccinationDates) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(birthDate);
    for (RelativeDateRule rule : relativeDate.getRules()) {
      if (rule.getRelativeTo() instanceof StaticDateReference) {
        calendar.add(Calendar.YEAR, rule.getYear());
        calendar.add(Calendar.MONTH, rule.getMonth());
        calendar.add(Calendar.DAY_OF_MONTH, rule.getWeek() * 7);
        calendar.add(Calendar.DAY_OF_MONTH, rule.getDay());
      } else if (rule.getRelativeTo() instanceof VaccineDateReference) {
        int vaccineIndex = ((VaccineDateReference) rule.getRelativeTo()).getId();
        if (vaccineIndex >= vaccinationDates.size()) {
          return null; // referenced vaccination hasn't been resolved (yet, or at all)
        }
        Date vaccineDate = vaccinationDates.get(vaccineIndex);
        if (vaccineDate == null) {
          return null;
        }
        calendar.setTime(vaccineDate);
        calendar.add(Calendar.YEAR, rule.getYear());
        calendar.add(Calendar.MONTH, rule.getMonth());
        calendar.add(Calendar.DAY_OF_MONTH, rule.getWeek() * 7);
        calendar.add(Calendar.DAY_OF_MONTH, rule.getDay());
      } else {
        return null;
      }
    }
    return calendar.getTime();
  }
}
