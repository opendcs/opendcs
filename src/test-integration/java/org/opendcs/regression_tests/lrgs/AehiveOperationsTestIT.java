package org.opendcs.regression_tests.lrgs;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Files;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;

public class AehiveOperationsTestIT
{

    private static LrgsTestInstance lrgs = null;

    @BeforeAll
    static void setup() throws Exception
    {
        File lrgsHome = Files.createTempDirectory("lrgshome").toFile();
        lrgsHome.mkdirs();
        lrgs = new LrgsTestInstance(lrgsHome);
    }

    @Test
    void test_save_and_read_back() throws Exception
    {
        assertNotNull(lrgs);   
    }
}
