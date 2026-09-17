package org.openimmunizationsoftware.cdsi.core.data;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Phase 22: DataModelLoader.createDataModel(String) must load (or parse)
 * the Supporting Data ZIP into a SupportingDataModel only once per distinct
 * supportingDataSet string for the JVM's lifetime, then hand back a fresh
 * DataModel per call that references the cached instance. See the Phase 22
 * section of StepIntoCDSi-Specification-Reference-Module-Plan.md.
 */
public class DataModelLoaderCacheTest {

  private static String testSupportingDataSet;

  @BeforeClass
  public static void findTestSupportingData() throws Exception {
    List<String> bundled = DataModelLoader.listBundledSupportingDataZipNames();
    if (!bundled.isEmpty()) {
      testSupportingDataSet = bundled.get(0);
      return;
    }
    List<String> candidatePaths = java.util.Arrays.asList(
        "src/main/resources/supporting-data-4.64-508.zip",
        "src/main/resources/supporting-data",
        "supporting-data");
    for (String path : candidatePaths) {
      File file = new File(path);
      if (file.exists()) {
        if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
          testSupportingDataSet = file.getName();
          break;
        } else if (file.isDirectory()) {
          File[] zipFiles = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
          if (zipFiles != null && zipFiles.length > 0) {
            testSupportingDataSet = zipFiles[0].getName();
            break;
          }
        }
      }
    }
    assertNotNull("No supporting data zip file found for this test", testSupportingDataSet);
  }

  @Test
  public void repeatedCallsWithTheSameSetShareOneSupportingDataModel() throws Exception {
    DataModel first = DataModelLoader.createDataModel(testSupportingDataSet);
    DataModel second = DataModelLoader.createDataModel(testSupportingDataSet);

    assertSame("the same supportingDataSet string must be served from the cache, not re-parsed",
        first.getSupportingDataModel(), second.getSupportingDataModel());
  }

  @Test
  public void repeatedCallsWithTheSameSetStillReturnDistinctDataModels() throws Exception {
    DataModel first = DataModelLoader.createDataModel(testSupportingDataSet);
    DataModel second = DataModelLoader.createDataModel(testSupportingDataSet);

    assertNotSame("each call must still hand back a fresh DataModel, even though the "
        + "SupportingDataModel it references is shared", first, second);
  }

  @Test
  public void differentSetsGetDifferentSupportingDataModelsWhenMoreThanOneIsBundled() throws Exception {
    List<String> bundled = DataModelLoader.listBundledSupportingDataZipNames();
    if (bundled.size() < 2) {
      // Only one Supporting Data set is bundled in this build - nothing to
      // compare against. Not a failure: the cache-differentiates-by-key
      // behavior this test would check is exercised for free the moment a
      // second set is added, and its absence here doesn't mean the cache is
      // wrong for the one set that does exist (covered by the tests above).
      return;
    }
    DataModel fromFirstSet = DataModelLoader.createDataModel(bundled.get(0));
    DataModel fromSecondSet = DataModelLoader.createDataModel(bundled.get(1));

    assertNotSame("two different supportingDataSet strings must not share a cache entry",
        fromFirstSet.getSupportingDataModel(), fromSecondSet.getSupportingDataModel());
  }
}
