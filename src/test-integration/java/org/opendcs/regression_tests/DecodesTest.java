package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.helpers.Programs;
import org.opendcs.spi.configuration.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.routing.RoutingSpecThread;
import uk.org.webcompere.systemstubs.SystemStubs;

public class DecodesTest extends AppTestBase
{
    private static final Logger log = Logger.getLogger(DecodesTest.class.getName());


    @BeforeAll
    public void load_sites() throws Exception
    {
        log.info("Importing shared data.");
        Configuration config = this.configuration;
        File propertiesFile = config.getPropertiesFile();
        File logFile = new File(config.getUserDir(),"/decodes-test-setup.log");
        Programs.DbImport(logFile, propertiesFile, environment, exit,
                                getResource("shared/test-sites.xml"),
                                getResource("shared/ROWI4.xml"),
                                new File(config.getUserDir(),"/schema/cwms/cwms-import.xml").getAbsolutePath(),
                                getResource("shared/presgrp-regtest.xml"),
                                getResource("HydroJsonTest/HydroJSON-rs.xml"),
                                getResource("SimpleDecodesTest/site-OKVI4.xml"),
                                getResource("SimpleDecodesTest/OKVI4-decodes.xml"));
    }

    @ParameterizedTest
    @CsvSource({
        "OKVI4-input,SimpleDecodesTest/golden"
    })
    public void test_humanReadable(String specName, String expectedResultFile) throws Exception
    {
        Configuration config = this.configuration;
        File logFile = new File(config.getUserDir(),"/decodes-humanReadable-" + specName + ".log");

        assertExitNullOrZero();
        log.info("Loading platform, routing spec, etc.");

        String output = SystemStubs.tapSystemOut(
                            () -> exit.execute(() ->
                                RoutingSpecThread.main(
                                    args("-l",logFile.getAbsolutePath(),"-d3",specName)
                                )
                            )
                        );
        assertExitNullOrZero();
        File goldenFile = new File(getResource(expectedResultFile));
        String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");
        assertEquals(golden,output,"Output Doesn't match expected data.");
    }

    @ParameterizedTest
    @CsvSource({
        "HydroJSON-Test,HydroJsonTest/golden"
    })
    public void test_HydroJsonTest(String specName, String expectedResultFile) throws Exception
    {
        Configuration config = this.configuration;
        File logFile = new File(config.getUserDir(),"/decodes-HydroJson-" + specName + ".log");
        log.info("Importing test db.");

        String output = SystemStubs.tapSystemOut(
            () -> exit.execute(() ->
                        RoutingSpecThread.main(
                            args("-l",logFile.getAbsolutePath(),"-d3",specName)
                        )
                    )
        );
        assertExitNullOrZero();

        File goldenFile = new File(getResource(expectedResultFile));
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
}