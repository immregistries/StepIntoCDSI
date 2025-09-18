package org.openimmunizationsoftware.cdsi.core.data;

import static org.junit.Assert.*;


import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriodType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;


public class DataModelLoaderTest {

    @Test
    public void testLoadDataBasic() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();  
        assertEquals(25, dataModel.getVaccineGroupMap().size());
        boolean foundCholera = false;
        boolean foundDtap = false;
        for (String name : dataModel.getVaccineGroupMap().keySet()) 
        {
            VaccineGroup vaccineGroup = dataModel.getVaccineGroupMap().get(name);
            if (name.equals("Cholera"))
            {
                foundCholera = true;
                
            }
            else if (name.equals("DTaP/Tdap/Td"))
            {
                foundDtap = true;
                assertEquals(YesNo.NO, vaccineGroup.getAdministerFullVaccineGroup());
            }
        }
        assertTrue(foundCholera);
        assertTrue(foundDtap);
    }

    @Test 
    public void testLoadDataLiveVirusConflict() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();  
        assertEquals(529, dataModel.getLiveVirusConflictList().size());
        boolean foundWhatweWereLookingFor = false;
        for (LiveVirusConflict liveVirusConflict : dataModel.getLiveVirusConflictList())
        {
            assertNotNull(liveVirusConflict.getPreviousVaccineType());
            assertNotNull(liveVirusConflict.getCurrentVaccineType());
            assertNotNull(liveVirusConflict.getConflictBeginInterval());
            assertNotNull(liveVirusConflict.getConflictEndInterval());
            assertNotNull(liveVirusConflict.getMinimalConflictEndInterval());
            assertTrue(liveVirusConflict.getConflictBeginInterval().isValued());
            assertTrue(liveVirusConflict.getConflictEndInterval().isValued());
            assertTrue(liveVirusConflict.getMinimalConflictEndInterval().isValued());
            if (liveVirusConflict.getPreviousVaccineType().getCvxCode().equals("03") 
              && liveVirusConflict.getCurrentVaccineType().getCvxCode().equals("111"))
            {
                foundWhatweWereLookingFor = true;
                assertNotNull(liveVirusConflict.getPreviousVaccineType().getShortDescription());
                assertNotNull(liveVirusConflict.getCurrentVaccineType().getShortDescription());
                assertNotEquals("", liveVirusConflict.getPreviousVaccineType().getShortDescription());
                assertNotEquals("", liveVirusConflict.getCurrentVaccineType().getShortDescription());
                assertEquals(1, liveVirusConflict.getConflictBeginInterval().getAmount());
                assertEquals(TimePeriodType.DAY, liveVirusConflict.getConflictBeginInterval().getType());
                assertEquals(28, liveVirusConflict.getConflictEndInterval().getAmount());
                assertEquals(TimePeriodType.DAY, liveVirusConflict.getConflictEndInterval().getType());
                assertEquals(28, liveVirusConflict.getMinimalConflictEndInterval().getAmount());
                assertEquals(TimePeriodType.DAY, liveVirusConflict.getMinimalConflictEndInterval().getType());
            }
        }
        assertTrue(foundWhatweWereLookingFor);
    }

    @Test 
    public void testLoadDataVaccineGroups() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();
        boolean foundVaccineGroupMMR = false;
        assertEquals(25, dataModel.getVaccineGroupList().size());
        for (VaccineGroup vaccineGroup : dataModel.getVaccineGroupList()) {
            assertNotNull(vaccineGroup.getName());
            assertNotEquals("", vaccineGroup.getName());
            if(vaccineGroup.getAdministerFullVaccineGroup() != null) {
                assertNotEquals("", vaccineGroup.getAdministerFullVaccineGroup());
            }
            if(vaccineGroup.getName().equals("MMR")) {
                foundVaccineGroupMMR = true;
                assertEquals(YesNo.YES, vaccineGroup.getAdministerFullVaccineGroup());
            }
        }
        assertTrue(foundVaccineGroupMMR);
    }

    @Test 
    public void testLoadDataVaccineGroupToAntigenMap() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();
        boolean foundDTap = false;
        assertEquals(25, dataModel.getVaccineGroupMap().size());
        for (String vaccineGroupKey : dataModel.getVaccineGroupMap().keySet()) {
            if(vaccineGroupKey.equals("DTaP/Tdap/Td")) {
                foundDTap = true;
                assertEquals(3,dataModel.getVaccineGroupMap().get(vaccineGroupKey).getAntigenList().size());
                assertEquals("Pertussis",dataModel.getVaccineGroupMap().get(vaccineGroupKey).getAntigenList().get(1).toString());
            }
        }
        assertTrue(foundDTap);
    }

    //observations does not exist. "DataModelLoader.java" line 72 needs implemented
    @Test 
    public void testLoadObservation() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();
        assertEquals(0, dataModel.getClinicalGuidelineObservationMap().size());
    }

    @Test 
    public void testLoadDataCvxToAntigenMap() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();
        boolean foundPolio = false;
        boolean foundDtp = false;
        assertEquals(211, dataModel.getCvxMap().size());
        for (String vaccineCvx : dataModel.getCvxMap().keySet()) {
            assertNotNull(dataModel.getCvxMap().get(vaccineCvx).getCvxCode());
            if(vaccineCvx.equals("02")) {
                foundPolio = true;
                assertEquals("OPV",dataModel.getCvxMap().get(vaccineCvx).getShortDescription());
                assertNotNull(dataModel.getCvxMap().get(vaccineCvx).getAntigenList().get(0));
                assertEquals("Polio",dataModel.getCvxMap().get(vaccineCvx).getAntigenList().get(0).getName());
            }
            if(vaccineCvx.equals("22")) {
                foundDtp = true;
                assertEquals("DTP-Hib",dataModel.getCvxMap().get(vaccineCvx).getShortDescription());
                assertNotNull(dataModel.getCvxMap().get(vaccineCvx).getAntigenList().get(0));
                assertEquals("Diphtheria",dataModel.getCvxMap().get(vaccineCvx).getAntigenList().get(0).getName());
                assertEquals("Tetanus",dataModel.getCvxMap().get(vaccineCvx).getAntigenList().get(1).getName());
                assertEquals("Hib",dataModel.getCvxMap().get(vaccineCvx).getAntigenList().get(3).getName());
            }
        }
        assertTrue(foundPolio);
        assertTrue(foundDtp);
    }

    @Test 
    public void testLoadDataAntigenImmunity() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();

        assertEquals(23, dataModel.getScheduleList().size());
        boolean foundMeasles = false;
        for (Schedule currentSchedule : dataModel.getScheduleList()) {
            if(currentSchedule.getScheduleName().equals("Measles")) {
                foundMeasles = true;

                //assert for clinicalHistory
                assertEquals(1,currentSchedule.getImmunity().getClinicalHistoryList().size());
                assertEquals("020",currentSchedule.getImmunity().getClinicalHistoryList().get(0).getImmunityGuidelineCode());

                //assert for dateOfBirth and Exclusion
                assertEquals(1,currentSchedule.getImmunity().getBirthDateImmunityList().size());
                assertEquals("Tue Jan 01 00:00:00 MST 1957",currentSchedule.getImmunity().getBirthDateImmunityList().get(0).getImmunityBirthDate().toString());
                assertEquals(1, currentSchedule.getImmunity().getBirthDateImmunityList().get(0).getExclusionList().size());
                assertEquals("055", currentSchedule.getImmunity().getBirthDateImmunityList().get(0).getExclusionList().get(0).getExclusionCode());
            }
        }
        assertTrue(foundMeasles);
    }

    //contraindications do not exist yet.
    @Test 
    public void testLoadContraindications() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();
        assertEquals(0, dataModel.getContraindicationList().size());
    }

    @Test 
    public void testLoadDataAntigenSeries() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();

        boolean foundRabies = false;
        for (Schedule currentSchedule : dataModel.getScheduleList()) {
            if(currentSchedule.getScheduleName().equals("Rabies")) {
                foundRabies = true;
                //testing series
                assertEquals(3, currentSchedule.getAntigenSeriesList().size());
                assertEquals("Rabies risk frequent exposure series", currentSchedule.getAntigenSeriesList().get(1).getSeriesName());
                assertEquals("Rabies", currentSchedule.getAntigenSeriesList().get(1).getTargetDisease().getName());

                //testing selectSeries
                //getDefaultSeries, getProductPath, getSeriesPreference, and getMaxAgeToStart return null.
                //assertEquals(YesNo.NO, currentSchedule.getAntigenSeriesList().get(1).getSelectBestPatientSeries().getProductPath());
                //seriesGroupName, seriesGroup, seriesPriority and minAgeToStart all do not exist.
                
                //testing indication and observationCode.
                //the entire indication section and all children do not exist.

                //testing seriesDose
                assertEquals(4, currentSchedule.getAntigenSeriesList().get(1).getSeriesDoseList().size());

                SeriesDose currentSeriesDose = currentSchedule.getAntigenSeriesList().get(1).getSeriesDoseList().get(2);
                assertEquals("3",currentSeriesDose.getDoseNumber());
                assertEquals(YesNo.NO,currentSeriesDose.getRecurringDose().getValue());

                //testing age
                assertEquals(1,currentSeriesDose.getAgeList().size());
                Age ageToCheck = currentSeriesDose.getAgeList().get(0);
                assertNotNull(ageToCheck.getAbsoluteMinimumAge());
                assertEquals("n/a",ageToCheck.getAbsoluteMinimumAge().toString());
                assertNotNull(ageToCheck.getMinimumAge());
                assertEquals("n/a",ageToCheck.getMinimumAge().toString());
                assertNotNull(ageToCheck.getEarliestRecommendedAge());
                assertEquals("n/a",ageToCheck.getEarliestRecommendedAge().toString());
                assertNotNull(ageToCheck.getLatestRecommendedAge());
                assertEquals("n/a",ageToCheck.getLatestRecommendedAge().toString());
                assertNotNull(ageToCheck.getMaximumAge());
                assertEquals("n/a",ageToCheck.getMaximumAge().toString());
                //both effectiveDate and cessationDate do not exist.

                //testing interval
                assertEquals(1,currentSeriesDose.getIntervalList().size());
                assertEquals(YesNo.YES,currentSeriesDose.getIntervalList().get(0).getFromImmediatePreviousDoseAdministered());
                assertEquals("",currentSeriesDose.getIntervalList().get(0).getFromTargetDoseNumberInSeries());
                //both fromMostRecent and fromRelevantObs do not exist.
                assertEquals("14 days",currentSeriesDose.getIntervalList().get(0).getAbsoluteMinimumInterval().toString());
                assertEquals("14 days",currentSeriesDose.getIntervalList().get(0).getMinimumInterval().toString());
                assertEquals("14 days",currentSeriesDose.getIntervalList().get(0).getEarliestRecommendedInterval().toString());
                assertEquals("21 days",currentSeriesDose.getIntervalList().get(0).getLatestRecommendedInterval().toString());
                assertNull(currentSeriesDose.getIntervalList().get(0).getIntervalPriority());
                //both effectiveDate and cessationDate do not exist.

                //testing allowableInterval
                assertEquals(0,currentSeriesDose.getAllowableintervalList().size());

                //testing preferableVaccine
                assertEquals(2,currentSeriesDose.getPreferrableVaccineList().size());
                assertEquals("Rabies - IM fibroblast culture (176)",currentSeriesDose.getPreferrableVaccineList().get(1).getVaccineType().toString());
                assertEquals("176",currentSeriesDose.getPreferrableVaccineList().get(1).getVaccineType().getCvxCode());
                assertEquals("0 days",currentSeriesDose.getPreferrableVaccineList().get(1).getVaccineTypeBeginAge().toString());
                assertEquals("n/a",currentSeriesDose.getPreferrableVaccineList().get(1).getVaccineTypeEndAge().toString());
                assertEquals("",currentSeriesDose.getPreferrableVaccineList().get(1).getTradeName());
                //mvx does not exist except for in the supporting data.
                assertEquals("1.0",currentSeriesDose.getPreferrableVaccineList().get(1).getVolume());
                assertEquals(YesNo.NO,currentSeriesDose.getPreferrableVaccineList().get(1).getForecastVaccineType());

                //testing allowableVaccine
                assertEquals(4,currentSeriesDose.getAllowableVaccineList().size());
                assertEquals("90",currentSeriesDose.getAllowableVaccineList().get(1).getVaccineType().getCvxCode());
                assertEquals("0 days",currentSeriesDose.getAllowableVaccineList().get(1).getVaccineTypeBeginAge().toString());
                assertEquals("n/a",currentSeriesDose.getAllowableVaccineList().get(1).getVaccineTypeEndAge().toString());

                //testing inadvertentVaccine
                //inadvertentVaccine does not exist.

                //testing seasonalRecommendation
                assertEquals(1,currentSeriesDose.getSeasonalRecommendationList().size());
            }
        }
        assertTrue(foundRabies);
    }

}
