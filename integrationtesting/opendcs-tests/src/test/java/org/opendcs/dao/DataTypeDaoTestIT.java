package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.DataTypeDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.DataType;
import decodes.util.DecodesSettings;

@EnableIfTsDb
class DataTypeDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;


    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(DataTypeDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var dtIn = new DataType("test_standard","test_code");
            
            final var dtOut = dao.save(tx, dtIn);
            assertEquals(dtIn.getStandard(), dtOut.getStandard());
            assertEquals(dtIn.getCode(), dtOut.getCode());
            assertNotEquals(dtIn.getId(), dtOut.getId());

            final var dtByName = dao.lookup(tx, dtIn.getCode())
                                    .orElseGet(() -> fail("unable to retrieve data type given options."));
            assertEquals(dtIn.getStandard(), dtByName.getStandard());
            assertEquals(dtIn.getCode(), dtByName.getCode());
            assertNotEquals(dtIn.getId(), dtByName.getId());


            final String dataTypePreference = tx.getContext()
                                                .getSettings(DecodesSettings.class)
                                                .orElseThrow()
                                                .dataTypeStdPreference;
            final var dtInSameCodeWithPrefStd = new DataType(dataTypePreference, "test_code");
            dao.save(tx, dtInSameCodeWithPrefStd);

            final var dtByCodeWithPref = dao.lookup(tx, "test_code")
                                            .orElseGet(() -> fail("No datatype at all returned?"));
            assertEquals(dataTypePreference, dtByCodeWithPref.getStandard());

            dao.delete(tx, dtOut.getId());
            var emShouldNotExist = dao.getById(tx, dtOut.getId());
            assertFalse(emShouldNotExist.isPresent());
        }
    }

    @Test
    void test_pagination() throws Exception
    {
        var dao = db.getDao(DataTypeDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {

            for (int i = 0; i < 30; i++)
            {
                final var dt = new DataType("test_standard", "test_code" + i);
                
                dao.save(tx, dt);
            }

            var dts = dao.getDataTypes(tx, -1, -1);
            assertTrue(dts.size() >= 30);

            var dtsLimited = dao.getDataTypes(tx, 10, 0);
            assertEquals(10, dtsLimited.size());
        }
    }
}
