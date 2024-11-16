package org.openimmunizationsoftware.cdsi.servlet.fits;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import gov.nist.healthcare.cds.domain.FixedDate;
import gov.nist.healthcare.cds.domain.RelativeDate;
import gov.nist.healthcare.cds.domain.RelativeDateRule;
import gov.nist.healthcare.cds.domain.StaticDateReference;
import gov.nist.healthcare.cds.domain.TestCase;
import gov.nist.healthcare.cds.domain.TestCaseGroup;
import gov.nist.healthcare.cds.domain.TestPlan;
import gov.nist.healthcare.cds.domain.VaccineDateReference;
import gov.nist.hit.resources.deploy.api.FITSClient;
import gov.nist.hit.resources.deploy.client.SSLFITSClient;
import gov.nist.hit.resources.deploy.model.ClientSoftwareConfig;

public class FitsManager {
  protected FITSClient client;
  protected List<TestPlan> testPlanList;
  protected List<TestPlan> testPlanListDisabled;
  protected List<TestPlan> testPlanListNotSetup;
  protected Set<Integer> fitsTestPlanIdsSetNotInFits;
  protected Map<String, Map<String, TestCaseRegistered>> testCaseMapGood;
  protected Map<String, Map<String, Map<String, TestCaseRegistered>>> testCaseMapProblem;
  protected Map<String, TestCaseRegistered> testCaseMap;
  protected List<String> groupNames;
  protected String username = "";
  private String url = "";
  private String password = "";
  private boolean setupProblem = false;

  public void setUrl(String url) {
    this.url = url;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public Map<String, Map<String, TestCaseRegistered>> getTestCaseMapGood() {
    return testCaseMapGood;
  }

  public Map<String, Map<String, Map<String, TestCaseRegistered>>> getTestCaseMapProblem() {
    return testCaseMapProblem;
  }

  public List<String> getGroupNames() {
    return groupNames;
  }

  public List<TestCaseRegistered> getTestCaseListGood(String groupName) {
    List<TestCaseRegistered> testCaseListGood;
    if (testCaseMapGood.get(groupName) == null) {
      testCaseListGood = new ArrayList<>();
    } else {
      testCaseListGood = new ArrayList<>(testCaseMapGood.get(groupName).values());
      Collections.sort(testCaseListGood, new Comparator<TestCaseRegistered>() {
        @Override
        public int compare(TestCaseRegistered tc1, TestCaseRegistered tc2) {
          String tc1Uid = tc1.getTestCase().getUid();
          if (tc1Uid == null) {
            tc1Uid = "";
          }
          String tc2Uid = tc1.getTestCase().getUid();
          if (tc2Uid == null) {
            tc2Uid = "";
          }
          return tc1Uid.compareTo(tc2Uid);
        }
      });
    }
    return testCaseListGood;
  }

  public TestCaseRegistered getTestCase(String fitsTestId) {
    return testCaseMap.get(fitsTestId);
  }

  public TestCaseRegistered getTestCaseGood(String groupName, String fitsDisplayedIdentifer) {
    if (testCaseMapGood.get(groupName) == null) {
      return null;
    } else {
      return testCaseMapGood.get(groupName).get(fitsDisplayedIdentifer);
    }
  }

  public List<String> getTestCaseProblemList(String groupName) {
    List<String> testCaseProblemList;
    if (testCaseMapProblem.get(groupName) == null) {
      testCaseProblemList = new ArrayList<>();
    } else {
      testCaseProblemList = new ArrayList<>(testCaseMapProblem.get(groupName).keySet());
      Collections.sort(testCaseProblemList);
    }
    return testCaseProblemList;
  }

  public TestCaseRegistered getTestCaseProblem(String groupName, String problem,
      String fitsDisplayedIdentifer) {
    if (testCaseMapProblem.get(groupName) == null) {
      return null;
    } else if (testCaseMapProblem.get(groupName).get(problem) == null) {
      return null;
    } else {
      return testCaseMapProblem.get(groupName).get(problem).get(fitsDisplayedIdentifer);
    }
  }

  public List<TestPlan> getTestPlanList() {
    return testPlanList;
  }

  public FitsManager() {
    testCaseMap = new HashMap<>();
  }

  public void init() throws Exception {
    synchronized (this) {
      client = new SSLFITSClient(url, username, password);

      testPlanList = client.getTestPlans().getBody();

      client = new SSLFITSClient(url, username, password);

      groupNames = new ArrayList<>();
      testCaseMap = new HashMap<>();
      testCaseMapGood = new HashMap<>();
      testCaseMapProblem = new HashMap<>();
      for (TestPlan testPlan : testPlanList) {

        for (TestCaseGroup testCaseGroup : testPlan.getTestCaseGroups()) {
          String groupName = testCaseGroup.getName();
          if (!groupNames.contains(groupName)) {
            groupNames.add(groupName);
          }
          Map<String, TestCaseRegistered> gMap = testCaseMapGood.get(groupName);
          if (gMap == null) {
            gMap = new HashMap<>();
            testCaseMapGood.put(groupName, gMap);
          }
          Map<String, Map<String, TestCaseRegistered>> pMap = getTestCaseMapProblemMap(groupName);
          for (TestCase testCase : testCaseGroup.getTestCases()) {
            TestCaseRegistered testCaseRegistered = new TestCaseRegistered(testCase, testCaseGroup, this);
            String problemReason = testCaseRegistered.getProblemReason();
            if (problemReason == null) {
              gMap.put(testCase.getUid(), testCaseRegistered);
            } else {
              registerProblemReason(pMap, testCase, testCaseRegistered, problemReason);
            }
          }
        }
      }
      Collections.sort(groupNames);
    }

  }

  protected void registerProblemReason(Map<String, Map<String, TestCaseRegistered>> pMap,
      TestCase testCase, TestCaseRegistered testCaseRegistered, String problemReason) {
    Map<String, TestCaseRegistered> m = pMap.get(problemReason);
    if (m == null) {
      m = new HashMap<>();
      pMap.put(problemReason, m);
    }
    m.put(testCase.getUid(), testCaseRegistered);
  }

  protected Map<String, Map<String, TestCaseRegistered>> getTestCaseMapProblemMap(
      String groupName) {
    Map<String, Map<String, TestCaseRegistered>> pMap = testCaseMapProblem.get(groupName);
    if (pMap == null) {
      pMap = new HashMap<>();
      testCaseMapProblem.put(groupName, pMap);
    }
    return pMap;
  }

  public FITSClient getFITSClient() {
    return client;
  }

  public static Date convertToDate(gov.nist.healthcare.cds.domain.Date nistDate) {
    if (nistDate instanceof FixedDate) {
      FixedDate fixed = (FixedDate) nistDate;
      try {
        if (StringUtils.isBlank(fixed.getDateString())) {
          return null;
        } else {
          return new SimpleDateFormat("MM/dd/yyyy").parse(fixed.getDateString());
        }
      } catch (ParseException nfe) {
        return null;
      }
    } else if (nistDate instanceof RelativeDate) {
      RelativeDate relative = (RelativeDate) nistDate;
      if (relative.getRules().size() == 0) {
        return null;
      }
      Calendar calendar = Calendar.getInstance();
      for (RelativeDateRule relativeDateRule : relative.getRules()) {
        if (relativeDateRule.getRelativeTo() instanceof StaticDateReference) {
          calendar.add(Calendar.DAY_OF_MONTH, -relativeDateRule.getDay());
          calendar.add(Calendar.MONTH, -relativeDateRule.getMonth());
          calendar.add(Calendar.DAY_OF_MONTH, -relativeDateRule.getWeek() * 7);
          calendar.add(Calendar.YEAR, -relativeDateRule.getYear());
        } else if (relativeDateRule.getRelativeTo() instanceof VaccineDateReference) {
          // ? what should do at this point?
        }
      }
      return calendar.getTime();
    }
    return null;
  }

  public boolean isSetupProblem() {
    return setupProblem;
  }

  public void setSetupProblem(boolean setupProblem) {
    this.setupProblem = setupProblem;
  }

  public List<TestPlan> getTestPlanListDisabled() {
    return testPlanListDisabled;
  }

  public void setTestPlanListDisabled(List<TestPlan> testPlanListDisabled) {
    this.testPlanListDisabled = testPlanListDisabled;
  }

  public List<TestPlan> getTestPlanListNotSetup() {
    return testPlanListNotSetup;
  }

  public void setTestPlanListNotSetup(List<TestPlan> testPlanListNotSetup) {
    this.testPlanListNotSetup = testPlanListNotSetup;
  }

  public Set<Integer> getFitsTestPlanIdsSetNotInFits() {
    return fitsTestPlanIdsSetNotInFits;
  }

  public void setFitsTestPlanIdsSetNotInFits(Set<Integer> fitsTestPlanIdsSetNotInFits) {
    this.fitsTestPlanIdsSetNotInFits = fitsTestPlanIdsSetNotInFits;
  }
}
