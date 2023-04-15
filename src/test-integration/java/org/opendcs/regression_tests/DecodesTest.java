package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.io.StringWriter;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.OpenDCSAppTestCase;
import org.opendcs.spi.configuration.Configuration;
import decodes.dbimport.DbImport;
import decodes.routing.RoutingSpecThread;
import uk.org.webcompere.systemstubs.SystemStubs;

public class DecodesTest extends AppTestBase
{
    public DecodesTest(OpenDCSAppTestCase testCase) {
        super(testCase);
    }

    private static final Logger log = Logger.getLogger(DecodesTest.class.getName());
    
    public void test_SimpleDecodesTest(OpenDCSAppTestCase testCase) throws Exception
    {
        Configuration config = testCase.getConfiguration();
        String propertiesFile = config.getPropertiesFile().getAbsolutePath();
        String logFile = new File(config.getUserDir(),"/decodes.log").getAbsolutePath();
        log.info("Import site.");

        SystemStubs.tapSystemErrAndOut(() -> {
            DbImport.main(args("-l", logFile,
                               "-P", propertiesFile,
                               "-d3",
                           getResource("SimpleDecodesTest/site-OKVI4.xml")));
        });
        log.info("Loading platform, routing spec, etc.");
        SystemStubs.tapSystemErrAndOut(() -> {
            DbImport.main(args("-l",logFile,
                           "-P", propertiesFile,
                           "-d3",
                           getResource("SimpleDecodesTest/OKVI4-decodes.xml")));
        });
        
        
        String output = SystemStubs.tapSystemOut( 
                            () -> RoutingSpecThread.main(
                                    args("-l",logFile,"-d3","OKVI4-input")
                                )
                        );

        File goldenFile = new File(getResource("SimpleDecodesTest/golden"));
        String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");
        assertEquals(golden,output,"Output Doesn't match expected data.");
    }

    public void test_HydroJsonTest(OpenDCSAppTestCase testCase) throws Exception
    {
        SystemStubs.catchSystemExit(() -> {
            Configuration config = testCase.getConfiguration();
            String propertiesFile = config.getPropertiesFile().getAbsolutePath();
            String logFile = new File(config.getUserDir(),"/decodes-json.log").getAbsolutePath();
            log.info("Importing test db.");

            SystemStubs.tapSystemErrAndOut(() -> {
                DbImport.main(args("-l", logFile,
                                "-P", propertiesFile,
                                "-d3",
                                getResource("shared/test-sites.xml"),
                                getResource("shared/ROWI4.xml"),
                                getResource("shared/presgrp-regtest.xml"),
                                getResource("HydroJsonTest/HydroJSON-rs.xml")));
            });

            String output = SystemStubs.tapSystemOut(
                () -> RoutingSpecThread.main(
                        args("-l",logFile,"-d3","HydroJSON-Test")
                )
            );

            File goldenFile = new File(getResource("HydroJsonTest/golden"));
            String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");
            assertEquals(golden,output,"Output Doesn't match expected data.");
        });
    }

    @Override
    public DynamicNode tests() {
        return dynamicContainer("actual tests", Stream.of(
            dynamicTest("Decodes", () -> test_SimpleDecodesTest(testCase)),
            dynamicTest("HydroJSON", () -> test_HydroJsonTest(testCase))
        ));
    }
}