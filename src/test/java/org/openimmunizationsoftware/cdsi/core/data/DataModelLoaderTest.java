package org.openimmunizationsoftware.cdsi.core.data;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
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


}
