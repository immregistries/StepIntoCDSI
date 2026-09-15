package org.openimmunizationsoftware.cdsi.core.data;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;

/**
 * Sanity check tests for DataModelLoader with zip-based supporting data.
 * These tests verify that at least one zip file exists and can be loaded
 * successfully.
 * They do NOT check specific counts or structure details to avoid breaking on
 * CDC updates.
 */
public class DataModelLoaderTest {

    private static String testSupportingDataSet;

    @BeforeClass
    public static void findTestSupportingData() throws Exception {
        // Look for zip files in common locations
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

        assertNotNull(
                "No supporting data zip file found. Place a CDC supporting data zip in src/main/resources/ or supporting-data/",
                testSupportingDataSet);
    }

    @Test
    public void testZipFileCanBeLoaded() throws Exception {
        assertNotNull("Test supporting data set not initialized", testSupportingDataSet);
        DataModel dataModel = DataModelLoader.createDataModel(testSupportingDataSet);
        assertNotNull("DataModel should not be null", dataModel);
    }

    @Test
    public void testHasVaccineGroups() throws Exception {
        DataModel dataModel = DataModelLoader.createDataModel(testSupportingDataSet);
        assertNotNull("VaccineGroupMap should not be null", dataModel.getVaccineGroupMap());
        assertTrue("Should have at least some vaccine groups", dataModel.getVaccineGroupMap().size() > 0);
        assertNotNull("VaccineGroupList should not be null", dataModel.getVaccineGroupList());
        assertTrue("VaccineGroupList should not be empty", dataModel.getVaccineGroupList().size() > 0);
    }

    @Test
    public void testHasCvxMappings() throws Exception {
        DataModel dataModel = DataModelLoader.createDataModel(testSupportingDataSet);
        assertNotNull("CvxMap should not be null", dataModel.getCvxMap());
        assertTrue("Should have CVX to antigen mappings", dataModel.getCvxMap().size() > 0);
    }

    @Test
    public void testHasSchedules() throws Exception {
        DataModel dataModel = DataModelLoader.createDataModel(testSupportingDataSet);
        assertNotNull("ScheduleList should not be null", dataModel.getScheduleList());
        assertTrue("Should have at least some schedules/antigens", dataModel.getScheduleList().size() > 0);

        // Verify schedules have basic structure
        for (Schedule schedule : dataModel.getScheduleList()) {
            assertNotNull("Schedule name should not be null", schedule.getScheduleName());
            assertNotEquals("Schedule name should not be empty", "", schedule.getScheduleName());
        }
    }

    @Test
    public void testHasLiveVirusConflicts() throws Exception {
        DataModel dataModel = DataModelLoader.createDataModel(testSupportingDataSet);
        assertNotNull("LiveVirusConflictList should not be null", dataModel.getLiveVirusConflictList());
        assertTrue("Should have live virus conflicts defined", dataModel.getLiveVirusConflictList().size() > 0);
    }

    @Test
    public void testNullSupportingDataSetThrowsException() {
        try {
            DataModelLoader.createDataModel(null);
            fail("Should throw IllegalArgumentException for null supporting data set");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention parameter requirement",
                    e.getMessage().contains("cannot be null"));
        } catch (Exception e) {
            fail("Should throw IllegalArgumentException, not " + e.getClass().getName());
        }
    }

    @Test
    public void testEmptySupportingDataSetThrowsException() {
        try {
            DataModelLoader.createDataModel("");
            fail("Should throw IllegalArgumentException for empty supporting data set");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention parameter requirement",
                    e.getMessage().contains("cannot be null or empty"));
        } catch (Exception e) {
            fail("Should throw IllegalArgumentException, not " + e.getClass().getName());
        }
    }

    @Test
    public void testNoArgCreateDataModelThrowsException() {
        try {
            DataModelLoader.createDataModel();
            fail("Should throw IllegalArgumentException when no supporting data set specified");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention requirement",
                    e.getMessage().contains("required"));
        } catch (Exception e) {
            fail("Should throw IllegalArgumentException, not " + e.getClass().getName());
        }
    }

    /**
     * readAntigenSeries's <seasonalRecommendation> branch used to create a
     * SeasonalRecommendation record and add it to the series dose's list
     * without ever reading its <startDate>/<endDate> children, so
     * getSeasonalRecommendationStartDate()/EndDate() were always null - inert
     * for every antigen that declares one (e.g. RSV's main "RSV 1-dose
     * series", which anchors its earliest/recommended forecast date to the
     * start of RSV season). Not checking the exact dates here, in keeping
     * with this class's own stated intent to avoid breaking on CDC Supporting
     * Data updates - only that a declared seasonal recommendation actually
     * carries real, ordered dates rather than nulls.
     */
    @Test
    public void testSeasonalRecommendationDatesAreParsed() throws Exception {
        DataModel dataModel = DataModelLoader.createDataModel("supporting-data-4.65-508.zip");

        org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries rsvOneDoseSeries = null;
        for (org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries antigenSeries : dataModel
                .getAntigenSeriesList()) {
            if ("RSV 1-dose series".equals(antigenSeries.getSeriesName())) {
                rsvOneDoseSeries = antigenSeries;
                break;
            }
        }
        assertNotNull("Supporting Data must still declare \"RSV 1-dose series\"", rsvOneDoseSeries);
        assertFalse("\"RSV 1-dose series\" must declare at least one series dose",
                rsvOneDoseSeries.getSeriesDoseList().isEmpty());

        org.openimmunizationsoftware.cdsi.core.domain.SeriesDose seriesDose = rsvOneDoseSeries.getSeriesDoseList()
                .get(0);
        assertFalse("its dose must declare at least one seasonal recommendation",
                seriesDose.getSeasonalRecommendationList().isEmpty());

        org.openimmunizationsoftware.cdsi.core.domain.SeasonalRecommendation seasonalRecommendation = seriesDose
                .getSeasonalRecommendationList().get(0);
        assertNotNull("the seasonal recommendation's start date must be parsed, not left null",
                seasonalRecommendation.getSeasonalRecommendationStartDate());
        assertNotNull("the seasonal recommendation's end date must be parsed, not left null",
                seasonalRecommendation.getSeasonalRecommendationEndDate());
        assertTrue("the season must start before it ends",
                seasonalRecommendation.getSeasonalRecommendationStartDate()
                        .before(seasonalRecommendation.getSeasonalRecommendationEndDate()));
    }

    /**
     * SPEC-4.6-0053: CDC writes conditional-skip vaccineTypes as
     * {@code 15; 16; 88; ...} (space after {@code ;}). Loading without trim left
     * only CVX 15 resolved, so Influenza season-completion skips never matched
     * administered CVX 88/333 doses.
     */
    @Test
    public void testConditionalSkipVaccineTypesTrimCvxTokens() throws Exception {
        DataModel dataModel = DataModelLoader.createDataModel("supporting-data-4.65-508.zip");

        org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries influenzaSeries = null;
        for (org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries antigenSeries : dataModel
                .getAntigenSeriesList()) {
            if ("Influenza standard series".equals(antigenSeries.getSeriesName())) {
                influenzaSeries = antigenSeries;
                break;
            }
        }
        assertNotNull("Supporting Data must still declare \"Influenza standard series\"", influenzaSeries);
        assertTrue("Influenza standard series must have at least two doses",
                influenzaSeries.getSeriesDoseList().size() >= 2);

        org.openimmunizationsoftware.cdsi.core.domain.SeriesDose doseTwo = influenzaSeries.getSeriesDoseList().get(1);
        assertNotNull(doseTwo.getConditionalSkip());
        assertFalse(doseTwo.getConditionalSkip().getConditionalSkipSetList().isEmpty());

        org.openimmunizationsoftware.cdsi.core.domain.VaccineType cvx88 = dataModel.getCvxMap().get("88");
        assertNotNull(cvx88);
        boolean foundCvx88 = false;
        for (org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipSet set : doseTwo.getConditionalSkip()
                .getConditionalSkipSetList()) {
            for (org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition condition : set
                    .getConditionList()) {
                if (condition.getVaccineTypeSet().contains(cvx88)) {
                    foundCvx88 = true;
                    break;
                }
            }
        }
        assertTrue("Dose 2 season-completion skips must resolve CVX 88 despite '; ' separators", foundCvx88);
    }
}
