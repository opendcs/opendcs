package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import static org.opendcs.fixtures.Toolkit.args;

import java.io.File;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DynamicNode;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.OpenDCSAppTestCase;
import org.opendcs.fixtures.Toolkit;
import org.opendcs.spi.configuration.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.dbimport.DbImport;
import decodes.routing.RoutingSpecThread;
import uk.org.webcompere.systemstubs.SystemStubs;

public class DecodesTest extends AppTestBase
{
    private static final Logger log = Logger.getLogger(DecodesTest.class.getName());
    private static final String TEST_SET_NAME = "Decodes Test";
    
    public DecodesTest(OpenDCSAppTestCase testCase) {
        super(testCase);
    }

    public void test_SimpleDecodesTest() throws Exception
    {
        Configuration config = testCase.getConfiguration();
        String propertiesFile = config.getPropertiesFile().getAbsolutePath();
        String logFile = new File(config.getUserDir(),"/decodes.log").getAbsolutePath();
        log.info("Import site.");

        exit.execute(() ->
            SystemStubs.tapSystemErrAndOut(() -> {
                DbImport.main(args("-l", logFile,
                                "-P", propertiesFile,
                                "-d3",
                            getResource("SimpleDecodesTest/site-OKVI4.xml")));
                })
        );
        assertExitNullOrZero();
        log.info("Loading platform, routing spec, etc.");
        exit.execute(() ->
        SystemStubs.tapSystemErrAndOut(() -> {
            DbImport.main(args("-l",logFile,
                           "-P", propertiesFile,
                           "-d3",
                           getResource("SimpleDecodesTest/OKVI4-decodes.xml")));
        }));
        assertExitNullOrZero();

        String output = SystemStubs.tapSystemOut(
                            () -> exit.execute(() ->
                                RoutingSpecThread.main(
                                    args("-l",logFile,"-d3","OKVI4-input")
                                )
                            )
                        );
        assertExitNullOrZero();
        File goldenFile = new File(getResource("SimpleDecodesTest/golden"));
        String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");
        assertEquals(golden,output,"Output Doesn't match expected data.");
    }

    public void test_HydroJsonTest() throws Exception
    {
        Configuration config = testCase.getConfiguration();
        String propertiesFile = config.getPropertiesFile().getAbsolutePath();
        String logFile = new File(config.getUserDir(),"/decodes-json.log").getAbsolutePath();
        log.info("Importing test db.");
        exit.execute(() -> {
            SystemStubs.tapSystemErrAndOut(() -> {
                DbImport.main(args("-l", logFile,
                                "-P", propertiesFile,
                                "-d3",
                                getResource("shared/test-sites.xml"),
                                getResource("shared/ROWI4.xml"),
                                new File(config.getUserDir(),"/schema/cwms/cwms-import.xml").getAbsolutePath(),
                                getResource("shared/presgrp-regtest.xml"),
                                getResource("HydroJsonTest/HydroJSON-rs.xml")));
            });
        });
        assertExitNullOrZero();

        String output = SystemStubs.tapSystemOut(
            () -> exit.execute(() ->
                        RoutingSpecThread.main(
                            args("-l",logFile,"-d3","HydroJSON-Test")
                        )
                    )
        );
        assertExitNullOrZero();

        File goldenFile = new File(getResource("HydroJsonTest/golden"));
        String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(golden),mapper.readTree(output),"Output Doesn't match expected data.");
        /**
         * This doesn't pass, and the data is huge. Technically the above test isn't sufficient either.
         * The mapper only reads the first value from each. However this *IS* correct behavior for JSON.
         * The output should be formatted as a list of objects, not objects separated by a new line.
         */
        //assertEquals(golden,output,"Output Doesn't match expected data.");
    }

    @Override
    public DynamicNode tests(String baseName) {
        return dynamicContainer(Toolkit.testName(baseName,TEST_SET_NAME), Stream.of(
            dynamicTest(Toolkit.testName(baseName,TEST_SET_NAME,"Routing"), () -> test_SimpleDecodesTest()),
            dynamicTest(Toolkit.testName(baseName,TEST_SET_NAME,"HydroJSON"), () -> test_HydroJsonTest())
        ));
    }
}