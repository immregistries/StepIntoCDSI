package org.openimmunizationsoftware.cdsi.fitstests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.data.SupportingDataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;

/**
 * Phase 22: a shared, cached SupportingDataModel must come out of a batch of
 * real forecast runs exactly as it went in - a structural fingerprint taken
 * before and after a batch of real FITS cases must match. Not a full deep
 * equality of every domain object field (impractical and not what the
 * concrete risk here is); strong enough to catch the failure mode Phase 22's
 * own design note worried about - a shared graph accumulating or losing
 * entries across cases - which is exactly what CaseOrderIndependenceTest's
 * result comparison would not directly show if it happened.
 */
class SupportingDataModelUnmutatedAfterRunTest {

  private static final String FIXTURE_TEST_PLAN_ID = "5eeca0522cc4517a96b0f417";

  @Test
  void aSharedSupportingDataModelIsUnchangedAfterRunningABatchOfRealCases() throws Exception {
    String supportingDataSet = DefaultSupportingDataSet.resolve();

    DataModel before = DataModelLoader.createDataModel(supportingDataSet);
    SupportingDataModel supportingDataModel = before.getSupportingDataModel();
    String fingerprintBefore = fingerprint(supportingDataModel);

    List<FitsTestCase> batch = FitsFixtures.loadAll().stream()
        .filter(testCase -> FIXTURE_TEST_PLAN_ID.equals(testCase.getTestPlanId()))
        .collect(Collectors.toList());
    assertTrue(batch.size() > 1, "Expected more than one fixture under test plan " + FIXTURE_TEST_PLAN_ID
        + " to make this a real batch, found " + batch.size());

    for (FitsTestCase testCase : batch) {
      FitsEngineRunner.run(testCase, supportingDataSet);
    }

    String fingerprintAfter = fingerprint(supportingDataModel);
    assertEquals(fingerprintBefore, fingerprintAfter,
        "the shared SupportingDataModel changed shape after running " + batch.size() + " real cases through it");

    DataModel after = DataModelLoader.createDataModel(supportingDataSet);
    assertSame(supportingDataModel, after.getSupportingDataModel(),
        "the cache must still return the same SupportingDataModel instance after the batch, not a new one");
  }

  /**
   * Sizes plus sorted key/name snapshots of the 9 fields DataModelLoader
   * populates, plus one level of nested count (series doses per antigen
   * series) to catch a dose being added or removed inside an existing
   * series rather than only a whole series appearing or disappearing.
   */
  private static String fingerprint(SupportingDataModel supportingDataModel) {
    StringBuilder sb = new StringBuilder();
    sb.append("cvxMap=").append(sortedKeys(supportingDataModel.getCvxMap().keySet())).append('\n');
    sb.append("antigenMap=").append(sortedKeys(supportingDataModel.getAntigenMap().keySet())).append('\n');
    sb.append("antigenList.size=").append(supportingDataModel.getAntigenList().size()).append('\n');
    sb.append("vaccineGroupMap=").append(sortedKeys(supportingDataModel.getVaccineGroupMap().keySet())).append('\n');
    sb.append("vaccineGroupList.size=").append(supportingDataModel.getVaccineGroupList().size()).append('\n');

    List<String> scheduleNames = new ArrayList<>();
    for (Schedule schedule : supportingDataModel.getScheduleList()) {
      scheduleNames.add(schedule.getScheduleName());
    }
    Collections.sort(scheduleNames);
    sb.append("scheduleList=").append(scheduleNames).append('\n');

    int totalSeriesDoses = 0;
    List<String> seriesNames = new ArrayList<>();
    for (AntigenSeries antigenSeries : supportingDataModel.getAntigenSeriesList()) {
      seriesNames.add(antigenSeries.getSeriesName());
      totalSeriesDoses += antigenSeries.getSeriesDoseList().size();
    }
    Collections.sort(seriesNames);
    sb.append("antigenSeriesList=").append(seriesNames).append('\n');
    sb.append("totalSeriesDoses=").append(totalSeriesDoses).append('\n');

    sb.append("liveVirusConflictList.size=").append(supportingDataModel.getLiveVirusConflictList().size())
        .append('\n');
    sb.append("observationMap=").append(sortedKeys(supportingDataModel.getObservationMap().keySet())).append('\n');

    return sb.toString();
  }

  private static List<String> sortedKeys(java.util.Set<String> keys) {
    List<String> sorted = new ArrayList<>(keys);
    Collections.sort(sorted);
    return sorted;
  }
}
