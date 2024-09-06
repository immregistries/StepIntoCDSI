package org.openimmunizationsoftware.cdsi.core.data;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriodType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;


public class DataModelLoaderTest {

    @Test
    public void testLoadDataBasic() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();  
        assertEquals(19, dataModel.getVaccineGroupMap().size());
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
        assertEquals(361, dataModel.getLiveVirusConflictList().size());
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
        assertEquals(19, dataModel.getVaccineGroupList().size());
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
        assertEquals(19, dataModel.getVaccineGroupMap().size());
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
        assertEquals(141, dataModel.getCvxMap().size());
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
                assertEquals(2, currentSchedule.getAntigenSeriesList().size());
                assertEquals("Rabies risk 3-dose frequent series", currentSchedule.getAntigenSeriesList().get(1).getSeriesName());
                assertEquals("Rabies", currentSchedule.getAntigenSeriesList().get(1).getTargetDisease().getName());

                //testing selectSeries
                //getDefaultSeries, getProductPath, getSeriesPreference, and getMaxAgeToStart return null.
                //assertEquals(YesNo.NO, currentSchedule.getAntigenSeriesList().get(1).getSelectBestPatientSeries().getProductPath());
                //seriesGroupName, seriesGroup, seriesPriority and minAgeToStart all do not exist.
                
                //testing indication and observationCode.
                //the entire indication section and all children do not exist outside the supporting data.

                //testing seriesDose
                assertEquals(4, currentSchedule.getAntigenSeriesList().get(1).getSeriesDoseList().size());
                assertEquals("3",currentSchedule.getAntigenSeriesList().get(1).getSeriesDoseList().get(2).getDoseNumber());
                assertEquals(YesNo.NO,currentSchedule.getAntigenSeriesList().get(1).getSeriesDoseList().get(2).getRecurringDose().getValue());

                //testing age
                assertEquals(1,currentSchedule.getAntigenSeriesList().get(1).getSeriesDoseList().get(2).getAgeList().size());
                Age ageToCheck = currentSchedule.getAntigenSeriesList().get(1).getSeriesDoseList().get(2).getAgeList().get(0);
                assertNotNull(ageToCheck.getAbsoluteMinimumAge());
                assertEquals("0 days",ageToCheck.getAbsoluteMinimumAge().toString());
                assertNotNull(ageToCheck.getMinimumAge());
                assertEquals("0 days",ageToCheck.getMinimumAge().toString());
                assertNotNull(ageToCheck.getEarliestRecommendedAge());
                assertEquals("0 days",ageToCheck.getEarliestRecommendedAge().toString());
                assertNotNull(ageToCheck.getLatestRecommendedAge());
                assertEquals("0 days",ageToCheck.getLatestRecommendedAge().toString());
                assertNotNull(ageToCheck.getMaximumAge());
                assertEquals("0 days",ageToCheck.getMaximumAge().toString());
                //both effectiveDate and cessationDate do not exist outside the supporting data.

                //testing interval

            }
        }
        assertTrue(foundRabies);
    }

}
