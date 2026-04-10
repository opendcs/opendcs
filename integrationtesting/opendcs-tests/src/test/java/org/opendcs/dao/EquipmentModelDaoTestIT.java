package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

            final var emOut = dao.saveEquipmentModel(tx, emIn);
            assertEquals(emIn.name, emOut.name);
            assertEquals(emIn.model, emOut.model);
            assertEquals(emIn.description, emOut.description);
            assertNotEquals(emIn.getId(), emOut.getId());

            final var emInWithProps = new EquipmentModel("IC_741");
            emInWithProps.model = "CMOS";
            emInWithProps.description = "Another common and abused integrated circuit.";
            emInWithProps.properties.setProperty("pin_count", "8");
            emInWithProps.properties.setProperty("vcc", "5");

            final var emOutWithProps = dao.saveEquipmentModel(tx, emInWithProps);
            assertEquals(emInWithProps.name, emOutWithProps.name);
            assertEquals(emInWithProps.model, emOutWithProps.model);
            assertEquals(emInWithProps.description, emOutWithProps.description);
            assertNotEquals(emInWithProps.getId(), emOutWithProps.getId());

            assertEquals("5", emOutWithProps.properties.getProperty("vcc"));

            dao.deleteEquipmentModel(tx, emOutWithProps.getId());
            var emShouldNotExist = dao.getEquipmentModel(tx, emOutWithProps.getId());
            assertFalse(emShouldNotExist.isPresent());


            final var emByName = dao.getEquipmentModel(tx, emIn.name)
                                    .orElseGet(() -> fail("unable to retrieve equipment model."));
            assertEquals(emIn.name, emByName.name);
            assertEquals(emIn.model, emByName.model);
            assertEquals(emIn.description, emByName.description);
            assertNotEquals(emIn.getId(), emByName.getId());
            assertEquals(emOut.getId(), emByName.getId());
        }
    }

    @Test
    void test_pagination() throws Exception
    {
        var dao = db.getDao(EquipmentModelDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {

            for (int i = 0; i < 30; i++)
            {
                final var em = new EquipmentModel("test_model_" + i);
                em.model = "model_" + i;
                em.description = "Test Model " + i;
                em.company = "OpenDCS";
                for (int j = 0; j < i; j++)
                {
                    em.properties.setProperty("prop" + i, "" + i);
                }

                dao.saveEquipmentModel(tx, em);
            }

            var ems = dao.getEquipmentModels(tx, -1, -1);
            assertTrue(ems.size() >= 30);

            var emsLimited = dao.getEquipmentModels(tx, 10, 0);
            assertEquals(10, emsLimited.size());
        }
    }
}
