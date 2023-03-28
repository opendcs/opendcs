package org.opendcs.regression_tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.OpenDCSAppTestCase;
import org.opendcs.spi.configuration.Configuration;
import decodes.dbimport.DbImport;

public class SimpleDecodesTest extends AppTestBase
{
    @Test
    @TestTemplate
    public void the_test(OpenDCSAppTestCase testCase) throws Exception
    {
        Configuration config = testCase.getConfiguration();

        DbImport.main(args("-l","/dev/stdout","-d3","OKVI4-decodes"));
    }

    public static String[] args(String... arg)
    {
        return arg;
    }
}