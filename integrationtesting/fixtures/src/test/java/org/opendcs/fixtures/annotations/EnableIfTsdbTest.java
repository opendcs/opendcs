package org.opendcs.fixtures.annotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class EnableIfTsdbOnMethodsTest
{
    @Test
    @EnableIfTsDb
    void test_no_annotation()
    {
        assertTrue(true, "test was not enabled");
    }


    @Test
    @EnableIfTsDb({"Not enabled"})
    void test_not_enabled()
    {
        fail("This should not have run");
    }

    @Test
    @EnableIfTsDb({"OpenDCS-Enabled", "Another impl"})
    void test_with_list()
    {
        assertTrue(true,"Test was not enabled");
    }

}
