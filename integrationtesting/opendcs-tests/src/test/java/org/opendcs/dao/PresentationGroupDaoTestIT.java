package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.PresentationGroup;

@EnableIfTsDb
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml",
    "SimpleDecodesTest/OKVI4-decodes.xml",
    "presentationgroup/parent.xml",
    "presentationgroup/child.xml"
})
class PresentationGroupDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;


    @Test
    void test_retrieve_existing() throws Exception
    {
        final var dao = db.getDao(PresentationGroupDao.class).orElseThrow();


        try (var tx = db.newTransaction())
        {
            var group = dao.getByName(tx, "CWMS-English")
                           .orElseGet(() -> fail("Group was not retrieved"));

            assertFalse(group.dataPresentations.isEmpty());
        }
    }

    @Test
    void test_retrieve_existing_with_parent_child_relation() throws Exception
    {
        final var dao = db.getDao(PresentationGroupDao.class).orElseThrow();


        try (var tx = db.newTransaction())
        {
            var parentGroup = dao.getByName(tx, "parent")
                           .orElseGet(() -> fail("Group was not retrieved"));

            assertFalse(parentGroup.dataPresentations.isEmpty());
            assertNull(parentGroup.parent);
            

            var childGroup = dao.getByName(tx, "child")
                           .orElseGet(() -> fail("Group was not retrieved"));

            assertFalse(childGroup.dataPresentations.isEmpty());
            assertNotNull(childGroup.parent);
        }
    }


    @Test
    void test_basic_operations() throws Exception
    {
        final var dao = db.getDao(PresentationGroupDao.class).orElseThrow();

        final var parentDataPresentation1 = new DataPresentation();
        parentDataPresentation1.setDataType(new DataType("CWMS", "Stage"));
        parentDataPresentation1.setMaxDecimals(3);
        parentDataPresentation1.setUnitsAbbr("ft");

        final var parentDataPresentation2 = new DataPresentation();
        parentDataPresentation2.setDataType(new DataType("CWMS", "Flow"));
        parentDataPresentation2.setMaxDecimals(1);
        parentDataPresentation2.setUnitsAbbr("cfs");

        try (var tx = db.newTransaction())
        {

            final var parentGroupIn = new PresentationGroup("newParent");
            parentGroupIn.isProduction = true;
            parentGroupIn.addDataPresentation(parentDataPresentation1);
            parentGroupIn.addDataPresentation(parentDataPresentation2);

            final var parentGroupOut = dao.save(tx, parentGroupIn);

            assertEquals(parentGroupOut.groupName, parentGroupIn.groupName);
            assertEquals(2, parentGroupOut.dataPresentations.size());


        }
    }
    
}
