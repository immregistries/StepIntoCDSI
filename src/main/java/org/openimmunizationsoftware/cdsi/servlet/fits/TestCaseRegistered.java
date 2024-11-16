package org.openimmunizationsoftware.cdsi.servlet.fits;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import gov.nist.healthcare.cds.domain.Event;
import gov.nist.healthcare.cds.domain.VaccinationEvent;
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

  public class Vaccination {
    private Date vaccineDate = null;
    private String vaccineCvx = "";
    private String vaccineMvx = "";

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

  public TestCaseRegistered(TestCase testCase, TestCaseGroup testCaseGroup,
      FitsManager fitsManager) {
    this.testCase = testCase;
    this.testCaseGroup = testCaseGroup;
    if (testCase.getPatient() == null) {
      problemReason = "No patient is defined (testCase.getPatient() == null)";
    } else if (testCase.getPatient().getDob() == null) {
      problemReason = "No patient dob is defined (testCase.getPatient().getDob() == null)";
    } else {
      evalDate = new Date();
      birthDate = null;
      if (testCase.getPatient().getDob() instanceof FixedDate) {
        FixedDate dobFixed = (FixedDate) testCase.getPatient().getDob();
        if (StringUtils.isBlank(dobFixed.getDateString())) {
          problemReason = "No patient dob is defined (dobFixed.getDate() == null)";
        } else {
          try {
            birthDate = getDateFormatter().parse(dobFixed.getDateString());
          } catch (ParseException e) {
            birthDate = null;
            problemReason = "Patient birt date could not be parsed: " + dobFixed.getDateString();
          }
        }
      } else if (testCase.getPatient().getDob() instanceof RelativeDate) {
        RelativeDate dobRelative = (RelativeDate) testCase.getPatient().getDob();
        if (dobRelative.getRules().size() == 0) {
          problemReason = "No relative rules for patient dob (dobRelative.getRules().size() == 0)";
        } else {
          Calendar calendar = resolveRelativeDate(dobRelative);
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
              Calendar calendar = resolveRelativeDate(vaccineRelative);
              vaccination.vaccineDate = calendar.getTime();
            }
          }
          if (vaccination.vaccineCvx == null) {
            problemReason = "Vaccine CVX is not defined (vaccinationEvent.getAdministred().getCvx() == null)";
          } else if (vaccination.vaccineDate == null) {
            problemReason = "Vaccine date is not defined (vaccinationEvent.getDate() == null)";
          } else {
            vaccinationList.add(vaccination);
          }
        }
      }

      if (testCase.getEvalDate() instanceof FixedDate) {
        FixedDate evalFixed = (FixedDate) testCase.getEvalDate();
        if (StringUtils.isNotBlank(evalFixed.getDateString())) {
          try {
            evalDate = getDateFormatter().parse(evalFixed.getDateString());
          } catch (ParseException e) {
            evalDate = null;
          }
        }
      } else if (testCase.getEvalDate() instanceof RelativeDate) {
        RelativeDate evalRelative = (RelativeDate) testCase.getEvalDate();
        if (evalRelative.getRules().size() != 0) {
          Calendar calendar = resolveRelativeDate(evalRelative);
          evalDate = calendar.getTime();
        }
      }
      if (birthDate == null && problemReason == null) {
        problemReason = "Unable to determine birth date (birthDate == null)";
      } else {
        age = Period.between(asLocalDate(birthDate), asLocalDate(evalDate)).getYears();
      }
    }
    if (testCase.getUid() == null) {
      problemReason = "Test case id is not defined (testCase.getUid() == null)";
    } else if ("".equals(testCase.getUid())) {
      problemReason = "Test case id is not defined (testCase.getUid().equals(\"\")";
    } else if (fitsManager.testCaseMap.containsKey(testCase.getUid())) {
      problemReason = "Test case id is already defined (duplicate)";
      // remove the other one from good map and put on bad map
      for (Map<String, TestCaseRegistered> map : fitsManager.testCaseMapGood.values()) {
        TestCaseRegistered otherTCR = map.get(testCase.getUid());
        if (otherTCR != null) {
          map.remove(testCase.getUid());
        }
        Map<String, Map<String, TestCaseRegistered>> pMap = fitsManager
            .getTestCaseMapProblemMap(testCaseGroup.getName());
        fitsManager.registerProblemReason(pMap, testCase, otherTCR, problemReason);
      }
    } else {
      fitsManager.testCaseMap.put(testCase.getUid(), this);
    }
  }

  private Calendar resolveRelativeDate(RelativeDate dobRelative) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(evalDate);
    for (RelativeDateRule relativeDateRule : dobRelative.getRules()) {
      if (!(relativeDateRule.getRelativeTo() instanceof StaticDateReference)) {
        problemReason = "Relative rule is not defined as static (!(relativeDateRule.getRelativeTo() instanceof StaticDateReference))";
      } else {
        calendar.add(Calendar.DAY_OF_MONTH, -relativeDateRule.getDay());
        calendar.add(Calendar.MONTH, -relativeDateRule.getMonth());
        calendar.add(Calendar.DAY_OF_MONTH, -relativeDateRule.getWeek() * 7);
        calendar.add(Calendar.YEAR, -relativeDateRule.getYear());
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
