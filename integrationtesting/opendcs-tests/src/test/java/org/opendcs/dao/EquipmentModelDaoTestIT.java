package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.EquipmentModelDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.EquipmentModel;

@EnableIfTsDb
class EquipmentModelDaoTestIT  extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;


    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(EquipmentModelDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var emIn = new EquipmentModel("test_model");
            emIn.model = "555";
            emIn.description = "One of the simplest and most abused integrated circuits.";

            var emOut = dao.saveEquipmentModel(tx, emIn);
            assertEquals(emIn.name, emOut.name);
            assertEquals(emIn.model, emOut.model);
            assertEquals(emIn.description, emOut.description);
            assertNotEquals(emIn.getId(), emOut.getId());

            var emInWithProps = new EquipmentModel("IC_741");
            emInWithProps.model = "CMOS";
            emInWithProps.description = "Another common and abused integrated circuit.";
            emInWithProps.properties.setProperty("pin_count", "8");
            emInWithProps.properties.setProperty("vcc", "5");

            var emOutWithProps = dao.saveEquipmentModel(tx, emInWithProps);
            assertEquals(emInWithProps.name, emOutWithProps.name);
            assertEquals(emInWithProps.model, emOutWithProps.model);
            assertEquals(emInWithProps.description, emOutWithProps.description);
            assertNotEquals(emInWithProps.getId(), emOutWithProps.getId());

            assertEquals("5", emOutWithProps.properties.getProperty("vcc"));
        }  

    }
}
