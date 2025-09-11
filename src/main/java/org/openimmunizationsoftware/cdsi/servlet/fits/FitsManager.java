package org.openimmunizationsoftware.cdsi.servlet.fits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import gov.nist.healthcare.cds.domain.TestCase;
import gov.nist.healthcare.cds.domain.TestCaseGroup;
import gov.nist.healthcare.cds.domain.TestPlan;
import gov.nist.hit.resources.deploy.api.FITSClient;
import gov.nist.hit.resources.deploy.client.SSLFITSClient;

public class FitsManager {
  protected FITSClient client;
  protected List<TestPlan> testPlanList;
  protected Map<String, Map<String, Map<String, TestCaseRegistered>>> testPlanGroupTestCaseMap;
  protected List<String> groupNames;
  protected String username = "";
  private String url = "";
  private String password = "";
  private boolean setupProblem = false;

  public Map<String, Map<String, Map<String, TestCaseRegistered>>> getTestPlanGroupTestCaseMap() {
    return testPlanGroupTestCaseMap;
  }

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

  public List<String> getGroupNames() {
    return groupNames;
  }

  public List<TestPlan> getTestPlanList() {
    return testPlanList;
  }

  public TestCaseRegistered getTestCaseRegistered(String testPlanId, String groupName, String uid) {
    Map<String, Map<String, TestCaseRegistered>> groupTestCaseMap = testPlanGroupTestCaseMap.get(testPlanId);
    if (groupTestCaseMap != null) {
      Map<String, TestCaseRegistered> testCaseMap = groupTestCaseMap.get(groupName);
      if (testCaseMap != null) {
        return testCaseMap.get(uid);
      }
    }
    return null;
  }

  public Map<String, Map<String, TestCaseRegistered>> getGroupTestCaseMap(String testPlanId) {
    return testPlanGroupTestCaseMap.get(testPlanId);
  }

  public Map<String, TestCaseRegistered> getTestCaseMap(String testPlanId, String groupName) {
    Map<String, Map<String, TestCaseRegistered>> groupTestCaseMap = testPlanGroupTestCaseMap.get(testPlanId);
    if (groupTestCaseMap != null) {
      return groupTestCaseMap.get(groupName);
    }
    return null;
  }

  public void init() throws Exception {
    synchronized (this) {
      client = new SSLFITSClient(url, username, password);
      testPlanList = client.getTestPlans().getBody();
      groupNames = new ArrayList<>();
      testPlanGroupTestCaseMap = new HashMap<>();

      for (TestPlan testPlan : testPlanList) {
        HashMap<String, Map<String, TestCaseRegistered>> groupTestCaseMap = new HashMap<>();
        testPlanGroupTestCaseMap.put(testPlan.getId(), groupTestCaseMap);
        for (TestCaseGroup testCaseGroup : testPlan.getTestCaseGroups()) {
          String groupName = testCaseGroup.getName();
          HashMap<String, TestCaseRegistered> testCaseMap = new HashMap<>();
          groupTestCaseMap.put(groupName, testCaseMap);
          if (!groupNames.contains(groupName)) {
            groupNames.add(groupName);
          }
          for (TestCase testCase : testCaseGroup.getTestCases()) {
            TestCaseRegistered testCaseRegistered = new TestCaseRegistered(testCase, testCaseGroup);
            testCaseMap.put(testCase.getUid(), testCaseRegistered);
          }
        }
      }
      Collections.sort(groupNames);
    }

  }

  public FITSClient getFITSClient() {
    return client;
  }

  public boolean isSetupProblem() {
    return setupProblem;
  }

  public void setSetupProblem(boolean setupProblem) {
    this.setupProblem = setupProblem;
  }

}
