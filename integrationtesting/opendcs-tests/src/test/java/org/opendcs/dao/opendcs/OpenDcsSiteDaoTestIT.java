package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
@DecodesConfigurationRequired({
    "shared/test-sites.xml",        
    "SimpleDecodesTest/site-OKVI4.xml"
})
class OpenDcsSiteDaoTestIT extends AppTestBase
{
    @ConfiguredField
    private OpenDcsDatabase db;
  
    @Test
    void test_pagination() throws Exception
    {
        var dao = db.getDao(SiteDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var sites =  dao.getAll(tx, -1, -1);
            assertFalse(sites.isEmpty());
        }
       
    }
}
