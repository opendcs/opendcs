package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml",
    "SimpleDecodesTest/OKVI4-decodes.xml"
})
class RoutingSpecDaoTestIT extends AppTestBase
{

    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_routing_spec_operations() throws Exception
    {
        var dao = db.getDao(RoutingSpecDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var spec = dao.getByName(tx, "OKVI4-input").orElseGet(() -> fail("could not find spec OKVI4-input"));
        }
    }    
}
