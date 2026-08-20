package org.opendcs.dao.cwms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.opendcs.cwms.data.CwmsOffice;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.OrganizationDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb("CWMS-Oracle")
class CwmsOrganizationDaoTestIT extends AppTestBase
{
  
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_get_orgs() throws Exception
    {
        var dao = db.getDao(OrganizationDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var allOffices = dao.getAll(tx, -1, -1);
            assertFalse(allOffices.isEmpty());
            assertInstanceOf(CwmsOffice.class, allOffices.getFirst());

            var first10 = dao.getAll(tx, 10, 0);
            var second10 = dao.getAll(tx, 10, 10);

            assertEquals(allOffices.get(0).getDisplayName(), first10.get(0).getDisplayName());
            assertEquals(allOffices.get(9).getDisplayName(), first10.getLast().getDisplayName());

            assertEquals(allOffices.get(10).getDisplayName(), second10.get(0).getDisplayName());
            assertEquals(allOffices.get(19).getDisplayName(), second10.getLast().getDisplayName());

            var spk = allOffices.stream().filter(office -> "SPK".equals(office.getName())).findFirst().orElseThrow();
            var spd = allOffices.stream().filter(office -> "SPD".equals(office.getName())).findFirst().orElseThrow();
            assertEquals(spd, spk.getReportsToOffice());
        }
    }
}
