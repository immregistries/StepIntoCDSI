package org.openimmunizationsoftware.cdsi.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;


public class DataModelLoaderTest {

    @Test 
    public void testLoadData() throws Exception
    {
        DataModel dataModel = DataModelLoader.createDataModel();  
        assertEquals(21, dataModel.getVaccineGroupMap().size());
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

}
