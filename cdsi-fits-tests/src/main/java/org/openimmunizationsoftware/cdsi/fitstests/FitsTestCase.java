package org.openimmunizationsoftware.cdsi.fitstests;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.ForecastInput;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;

/**
 * A single FITS test case, frozen as plain data: a patient, an assessment
 * date, an immunization history, and the CDC-expected forecast(s) for one or
 * more vaccine groups. This is the JSON shape written by FitsDownloader and
 * read back by FitsFixtures/FitsFixtureTest - it has no dependency on the
 * NIST fits-client domain model, so running the tests never needs a live
 * NIST connection or those libraries on the classpath.
 *
 * All dates are ISO-8601 (yyyy-MM-dd) strings, already resolved (FITS
 * expresses many dates as rules relative to "today" or to another event;
 * FitsDownloader resolves those once, at download time, so the fixture is
 * deterministic on every subsequent run).
 */
public class FitsTestCase {

  private String uid;
  private String testPlanId;
  private String testPlanName;
  private String groupName;
  private String title;
  private String patientSex;
  private String birthDate;
  private String evalDate;
  private List<Vaccination> vaccinations = new ArrayList<>();
  private List<ExpectedForecast> expectedForecasts = new ArrayList<>();

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getTestPlanId() {
    return testPlanId;
  }

  public void setTestPlanId(String testPlanId) {
    this.testPlanId = testPlanId;
  }

  public String getTestPlanName() {
    return testPlanName;
  }

  public void setTestPlanName(String testPlanName) {
    this.testPlanName = testPlanName;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPatientSex() {
    return patientSex;
  }

  public void setPatientSex(String patientSex) {
    this.patientSex = patientSex;
  }

  public String getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }

  public String getEvalDate() {
    return evalDate;
  }

  public void setEvalDate(String evalDate) {
    this.evalDate = evalDate;
  }

  public List<Vaccination> getVaccinations() {
    return vaccinations;
  }

  public void setVaccinations(List<Vaccination> vaccinations) {
    this.vaccinations = vaccinations;
  }

  public List<ExpectedForecast> getExpectedForecasts() {
    return expectedForecasts;
  }

  public void setExpectedForecasts(List<ExpectedForecast> expectedForecasts) {
    this.expectedForecasts = expectedForecasts;
  }

  /** A short, human-readable label for test reports: "GROUP uid title". */
  public String displayName() {
    return groupName + " " + uid + " " + (title == null ? "" : title);
  }

  /** testPlanId-groupName-uid, not just groupName-uid: the same group name
   * (e.g. "HepA") and the same uid numbering scheme (e.g. "2013-0001")
   * both recur across different NIST test plans, so groupName+uid alone
   * collides for real - confirmed empirically (1053 of 4896 real fixtures
   * collide on groupName+uid). Without testPlanId, failure bundles and
   * allowlist entries for genuinely different cases would silently
   * collide. This is the one place this id is computed - diagnostics
   * (CaseRecord) and the regression allowlist (FitsFixtureTest) both call
   * this rather than each recomputing it, so they can never drift apart. */
  public String caseId() {
    String raw = nullToEmpty(testPlanId) + "-" + nullToEmpty(groupName) + "-" + nullToEmpty(uid);
    return raw.replaceAll("[^A-Za-z0-9._-]", "-");
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  @Override
  public String toString() {
    return displayName();
  }

  /** Adapts this fixture into the ForecastInput the engine actually consumes. */
  public ForecastInput toForecastInput() {
    ForecastInput input = new ForecastInput();
    input.setPatientDateOfBirth(FitsDates.parse(birthDate));
    input.setPatientSex(patientSex == null ? "F" : patientSex);
    input.setAssessmentDate(FitsDates.parse(evalDate));
    for (Vaccination v : vaccinations) {
      ForecastInput.VaccinationInput vaccination = input.addVaccination();
      vaccination.setDateAdministered(FitsDates.parse(v.getDate()));
      vaccination.setVaccineCvx(v.getCvx());
      vaccination.setVaccineMvx(v.getMvx());
      if (v.getDoseCondition() != null) {
        vaccination.setDoseCondition(DoseCondition.valueOf(v.getDoseCondition()));
      }
    }
    return input;
  }

  public static class Vaccination {
    private String date;
    private String cvx;
    private String mvx;
    private String doseCondition;

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }

    public String getCvx() {
      return cvx;
    }

    public void setCvx(String cvx) {
      this.cvx = cvx;
    }

    public String getMvx() {
      return mvx;
    }

    public void setMvx(String mvx) {
      this.mvx = mvx;
    }

    public String getDoseCondition() {
      return doseCondition;
    }

    public void setDoseCondition(String doseCondition) {
      this.doseCondition = doseCondition;
    }
  }

  /**
   * One expected vaccine-group outcome. vaccineCvx identifies which forecasted
   * vaccine group this expectation applies to (matched via the same
   * CVX-equivalence table FITS itself treats as interchangeable - see
   * {@link CvxEquivalence}). serieStatus is one of the engine's own
   * VaccineGroupStatus names (COMPLETE, NOT_COMPLETE, CONTRAINDICATED, IMMUNE,
   * NOT_RECOMMENDED, AGED_OUT) - FitsDownloader maps NIST's SerieStatus into
   * this at download time, so the running test never needs to know about
   * NIST's enum. earliestDate/recommendedDate are only meaningful (and only
   * checked) when serieStatus is NOT_COMPLETE, matching FITS' own semantics.
   */
  public static class ExpectedForecast {
    private String vaccineCvx;
    private String serieStatus;
    private String earliestDate;
    private String recommendedDate;

    public String getVaccineCvx() {
      return vaccineCvx;
    }

    public void setVaccineCvx(String vaccineCvx) {
      this.vaccineCvx = vaccineCvx;
    }

    public String getSerieStatus() {
      return serieStatus;
    }

    public void setSerieStatus(String serieStatus) {
      this.serieStatus = serieStatus;
    }

    public String getEarliestDate() {
      return earliestDate;
    }

    public void setEarliestDate(String earliestDate) {
      this.earliestDate = earliestDate;
    }

    public String getRecommendedDate() {
      return recommendedDate;
    }

    public void setRecommendedDate(String recommendedDate) {
      this.recommendedDate = recommendedDate;
    }
  }
}
