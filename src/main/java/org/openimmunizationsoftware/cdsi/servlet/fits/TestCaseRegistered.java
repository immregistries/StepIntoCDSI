package org.openimmunizationsoftware.cdsi.servlet.fits;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

import gov.nist.healthcare.cds.domain.Event;
import gov.nist.healthcare.cds.domain.ExpectedForecast;
import gov.nist.healthcare.cds.domain.VaccinationEvent;
import gov.nist.healthcare.cds.domain.VaccineDateReference;
import gov.nist.healthcare.cds.enumeration.SerieStatus;
import gov.nist.healthcare.cds.domain.FixedDate;
import gov.nist.healthcare.cds.domain.RelativeDate;
import gov.nist.healthcare.cds.domain.RelativeDateRule;
import gov.nist.healthcare.cds.domain.StaticDateReference;
import gov.nist.healthcare.cds.domain.TestCase;
import gov.nist.healthcare.cds.domain.TestCaseGroup;

public class TestCaseRegistered {
  private TestCase testCase;
  private String problemReason = null;
  private int age = -1;
  private TestCaseGroup testCaseGroup;
  private Date evalDate = null;
  private Date birthDate = null;
  private Exception exception = null;

  public void setBirthDate(Date birthDate) {
    this.birthDate = birthDate;
  }

  public void setEvalDate(Date evalDate) {
    this.evalDate = evalDate;
  }

  public Exception getException() {
    return exception;
  }

  public void setException(Exception exception) {
    this.exception = exception;
  }

  public boolean hasException() {
    return exception != null;
  }

  public class Vaccination {
    private Date vaccineDate = null;
    private String vaccineCvx = "";
    private String vaccineMvx = "";

    public void setVaccineDate(Date vaccineDate) {
      this.vaccineDate = vaccineDate;
    }

    public void setVaccineCvx(String vaccineCvx) {
      this.vaccineCvx = vaccineCvx;
    }

    public void setVaccineMvx(String vaccineMvx) {
      this.vaccineMvx = vaccineMvx;
    }

    public Date getVaccineDate() {
      return vaccineDate;
    }

    public String getVaccineCvx() {
      return vaccineCvx;
    }

    public String getVaccineMvx() {
      return vaccineMvx;
    }
  }

  public Vaccination addVaccination() {
    Vaccination vaccination = new Vaccination();
    vaccinationList.add(vaccination);
    return vaccination;
  }

  private List<Vaccination> vaccinationList = new ArrayList<>();

  public List<Vaccination> getVaccinationList() {
    return vaccinationList;
  }

  public Date getBirthDate() {
    return birthDate;
  }

  public Date getEvalDate() {
    return evalDate;
  };

  public TestCaseGroup getTestCaseGroup() {
    return testCaseGroup;
  }

  public boolean isGood() {
    return problemReason == null;
  }

  public TestCase getTestCase() {
    return testCase;
  }

  public String getProblemReason() {
    return problemReason;
  }

  public int getAge() {
    return age;
  }

  public TestCaseRegistered() {
    // dfeault constructor
  }

  public TestCaseRegistered(TestCase testCase, TestCaseGroup testCaseGroup) {
    this.testCase = testCase;
    this.testCaseGroup = testCaseGroup;
    if (testCase.getPatient() == null) {
      recordProblem("No patient is defined (testCase.getPatient() == null)");
    } else if (testCase.getPatient().getDob() == null) {
      recordProblem("No patient dob is defined (testCase.getPatient().getDob() == null)");
    } else {
      evalDate = new Date();
      if (testCase.getEvalDate() instanceof FixedDate) {
        FixedDate evalFixed = (FixedDate) testCase.getEvalDate();
        if (StringUtils.isNotBlank(evalFixed.getDateString())) {
          try {
            evalDate = getDateFormatter().parse(evalFixed.getDateString());
          } catch (ParseException e) {
            recordProblem("Eval date could not be parsed: " + evalFixed.getDateString());
          }
        }
      }
      birthDate = null;
      if (testCase.getPatient().getDob() instanceof FixedDate) {
        FixedDate dobFixed = (FixedDate) testCase.getPatient().getDob();
        if (StringUtils.isBlank(dobFixed.getDateString())) {
          recordProblem("No patient dob is defined (dobFixed.getDate() == null)");
        } else {
          try {
            birthDate = getDateFormatter().parse(dobFixed.getDateString());
          } catch (ParseException e) {
            birthDate = null;
            recordProblem("Patient birth date could not be parsed: " + dobFixed.getDateString());
          }
        }
      } else if (testCase.getPatient().getDob() instanceof RelativeDate) {
        RelativeDate dobRelative = (RelativeDate) testCase.getPatient().getDob();
        if (dobRelative.getRules().size() == 0) {
          recordProblem("No relative rules for patient dob (dobRelative.getRules().size() == 0)");
        } else {
          Calendar calendar = resolveDateRelativeToEval(dobRelative);
          if (problemReason == null) {
            birthDate = calendar.getTime();
          }
        }
      }

      for (Event event : testCase.getEvents()) {
        if (event instanceof VaccinationEvent) {
          VaccinationEvent vaccinationEvent = (VaccinationEvent) event;
          Vaccination vaccination = new Vaccination();
          vaccination.vaccineCvx = vaccinationEvent.getAdministred().getCvx();
          vaccination.vaccineMvx = ""; // TODO
          if (vaccinationEvent.getDate() instanceof FixedDate) {
            FixedDate vaccineFixed = (FixedDate) vaccinationEvent.getDate();
            if (StringUtils.isNotBlank(vaccineFixed.getDateString())) {
              try {
                vaccination.vaccineDate = getDateFormatter().parse(vaccineFixed.getDateString());
              } catch (ParseException e) {
                vaccination.vaccineDate = null;
              }
            }
          } else if (vaccinationEvent.getDate() instanceof RelativeDate) {
            RelativeDate vaccineRelative = (RelativeDate) vaccinationEvent.getDate();
            if (vaccineRelative.getRules().size() != 0) {
              Calendar calendar = resolveDateRelativeFromBirthOrVaccination(vaccineRelative);
              vaccination.vaccineDate = calendar.getTime();
            }
          }
          if (vaccination.vaccineCvx == null) {
            recordProblem("Vaccine CVX is not defined (vaccinationEvent.getAdministred().getCvx() == null)");
          } else if (vaccination.vaccineDate == null) {
            recordProblem("Vaccine date is not defined (vaccinationEvent.getDate() == null)");
          } else {
            vaccinationList.add(vaccination);
          }
        }
      }

      for (ExpectedForecast expectedForecast : testCase.getForecast()) {
        Forecast forecast = new Forecast(expectedForecast);
        forecastList.add(forecast);
      }
      if (forecastList.size() == 0) {
        recordProblem("No forecasts are defined (testCase.getForecast().size() == 0)");
      }

    }
    if (testCase.getUid() == null) {
      recordProblem("Test case id is not defined (testCase.getUid() == null)");
    } else if ("".equals(testCase.getUid())) {
      recordProblem("Test case id is not defined (testCase.getUid().equals(\"\")");
    }
  }

  private void recordProblem(String problem) {
    if (problemReason == null) {
      problemReason = problem;
    }
  }

  private List<Forecast> forecastList = new ArrayList<>();

  public List<Forecast> getForecastList() {
    return forecastList;
  }

  public class Forecast {

    public Forecast(ExpectedForecast expectedForecast) {
      if (expectedForecast.getSerieStatus() == null) {
        problemReason = "Series status is not defined (expectedForecast.getSerieStatus() == null)";
        return;
      }
      this.serieStatusExp = expectedForecast.getSerieStatus();
      if (expectedForecast.getTarget() == null) {
        problemReason = "Target is not defined (expectedForecast.getTarget() == null)";
        return;
      }
      vaccineCvxExp = expectedForecast.getTarget().getCvx();
      // read earliest expected date, if it is not null
      if (expectedForecast.getEarliest() != null) {
        if (expectedForecast.getEarliest() instanceof FixedDate) {
          FixedDate earliestFixed = (FixedDate) expectedForecast.getEarliest();
          if (StringUtils.isNotBlank(earliestFixed.getDateString())) {
            try {
              earliestExp = getDateFormatter().parse(earliestFixed.getDateString());
            } catch (ParseException e) {
              earliestExp = null;
            }
          }
        } else if (expectedForecast.getEarliest() instanceof RelativeDate) {
          RelativeDate earliestRelative = (RelativeDate) expectedForecast.getEarliest();
          if (earliestRelative.getRules().size() != 0) {
            Calendar calendar = resolveDateRelativeFromBirthOrVaccination(earliestRelative);
            earliestExp = calendar.getTime();
          }
        }
      }
      // read recommended expected date, if it is not null
      if (expectedForecast.getRecommended() != null) {
        if (expectedForecast.getRecommended() instanceof FixedDate) {
          FixedDate recommendedFixed = (FixedDate) expectedForecast.getRecommended();
          if (StringUtils.isNotBlank(recommendedFixed.getDateString())) {
            try {
              recommendedExp = getDateFormatter().parse(recommendedFixed.getDateString());
            } catch (ParseException e) {
              recommendedExp = null;
            }
          }
        } else if (expectedForecast.getRecommended() instanceof RelativeDate) {
          RelativeDate recommendedRelative = (RelativeDate) expectedForecast.getRecommended();
          if (recommendedRelative.getRules().size() != 0) {
            Calendar calendar = resolveDateRelativeFromBirthOrVaccination(recommendedRelative);
            recommendedExp = calendar.getTime();
          }
        }
      }
    }

    private Date earliestExp = null;
    private Date recommendedExp = null;
    private String vaccineCvxExp = null;
    private SerieStatus serieStatusExp = null;
    private Date earliestAct = null;
    private Date recommendedAct = null;
    private String vaccineCvxAct = null;
    private SerieStatus serieStatusAct = null;

    public Date getEarliestAct() {
      return earliestAct;
    }

    public Date getRecommendedAct() {
      return recommendedAct;
    }

    public SerieStatus getSerieStatusAct() {
      return serieStatusAct;
    }

    public String getVaccineCvxAct() {
      return vaccineCvxAct;
    }

    public void setEarliestAct(Date earliestAct) {
      this.earliestAct = earliestAct;
    }

    public void setEarliestExp(Date earliestExp) {
      this.earliestExp = earliestExp;
    }

    public void setRecommendedAct(Date recommendedAct) {
      this.recommendedAct = recommendedAct;
    }

    public void setRecommendedExp(Date recommendedExp) {
      this.recommendedExp = recommendedExp;
    }

    public void setSerieStatusAct(SerieStatus serieStatusAct) {
      this.serieStatusAct = serieStatusAct;
    }

    public void setSerieStatusExp(SerieStatus serieStatusExp) {
      this.serieStatusExp = serieStatusExp;
    }

    public void setVaccineCvxAct(String vaccineCvxAct) {
      this.vaccineCvxAct = vaccineCvxAct;
    }

    public void setVaccineCvxExp(String vaccineCvxExp) {
      this.vaccineCvxExp = vaccineCvxExp;
    }

    public Date getEarliestExp() {
      return earliestExp;
    }

    public Date getRecommendedExp() {
      return recommendedExp;
    }

    public String getVaccineCvxExp() {
      return vaccineCvxExp;
    }

    public SerieStatus getSerieStatusExp() {
      return serieStatusExp;
    }
  }

  private Calendar resolveDateRelativeToEval(RelativeDate dobRelative) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(evalDate);
    for (RelativeDateRule relativeDateRule : dobRelative.getRules()) {
      if (relativeDateRule.getRelativeTo() instanceof StaticDateReference) {
        calendar.add(Calendar.DAY_OF_MONTH, -relativeDateRule.getDay());
        calendar.add(Calendar.MONTH, -relativeDateRule.getMonth());
        calendar.add(Calendar.DAY_OF_MONTH, -relativeDateRule.getWeek() * 7);
        calendar.add(Calendar.YEAR, -relativeDateRule.getYear());
      } else {
        recordProblem("Relative date rule to is not supported (relativeDateRule.getRelativeTo() instanceof "
            + relativeDateRule.getRelativeTo().getClass().getName() + ")");
      }
    }
    return calendar;
  }

  private Calendar resolveDateRelativeFromBirthOrVaccination(RelativeDate dobRelative) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(birthDate);
    for (RelativeDateRule relativeDateRule : dobRelative.getRules()) {
      if (relativeDateRule.getRelativeTo() instanceof StaticDateReference) {
        calendar.add(Calendar.YEAR, relativeDateRule.getYear());
        calendar.add(Calendar.MONTH, relativeDateRule.getMonth());
        calendar.add(Calendar.DAY_OF_MONTH, relativeDateRule.getWeek() * 7);
        calendar.add(Calendar.DAY_OF_MONTH, relativeDateRule.getDay());
      } else if (relativeDateRule.getRelativeTo() instanceof VaccineDateReference) {
        VaccineDateReference vaccineDateReference = (VaccineDateReference) relativeDateRule.getRelativeTo();
        int vaccineIndex = vaccineDateReference.getId();
        if (vaccineIndex >= vaccinationList.size()) {
          recordProblem("Vaccine index is out of range (vaccineIndex >= vaccineList.size)");
        } else {
          Date vaccineDate = vaccinationList.get(vaccineIndex).getVaccineDate();
          if (vaccineDate == null) {
            recordProblem("Vaccine date is not defined (vaccineDate == null)");
          } else {
            calendar.setTime(vaccineDate);
            calendar.add(Calendar.YEAR, relativeDateRule.getYear());
            calendar.add(Calendar.MONTH, relativeDateRule.getMonth());
            calendar.add(Calendar.DAY_OF_MONTH, relativeDateRule.getWeek() * 7);
            calendar.add(Calendar.DAY_OF_MONTH, relativeDateRule.getDay());
          }
        }
      } else {
        if (relativeDateRule.getRelativeTo() == null) {
          recordProblem("Relative date rule from is not defined (relativeDateRule.getRelativeTo() == null)");
        } else {
          recordProblem("Relative date rule from is not supported (relativeDateRule.getRelativeTo() instanceof "
              + relativeDateRule.getRelativeTo().getClass().getName() + ")");
        }
      }
    }
    return calendar;

  }

  private SimpleDateFormat getDateFormatter() {
    return new SimpleDateFormat("MM/dd/yyyy");
  }

  public static LocalDate asLocalDate(Date date) {
    if (date == null) {
      return null;
    }

    return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
