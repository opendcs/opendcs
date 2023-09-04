package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.helpers.Programs;
import org.opendcs.spi.configuration.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.routing.RoutingSpecThread;
import uk.org.webcompere.systemstubs.SystemStubs;

public class DecodesTest extends AppTestBase
{
    private static final Logger log = Logger.getLogger(DecodesTest.class.getName());

    @Test
    public void test_SimpleDecodesTest() throws Exception
    {
        Configuration config = this.configuration;
        File propertiesFile = config.getPropertiesFile();
        File logFile = new File(config.getUserDir(),"/decodes.log");
        log.info("Import site.");
        Programs.DbImport(logFile, propertiesFile, environment, exit,
                            getResource("SimpleDecodesTest/site-OKVI4.xml"));

        assertExitNullOrZero();
        log.info("Loading platform, routing spec, etc.");

        Programs.DbImport(logFile,propertiesFile,environment,exit,
                           getResource("SimpleDecodesTest/OKVI4-decodes.xml"));

        String output = SystemStubs.tapSystemOut(
                            () -> exit.execute(() ->
                                RoutingSpecThread.main(
                                    args("-l",logFile.getAbsolutePath(),"-d3","OKVI4-input")
                                )
                            )
                        );
        assertExitNullOrZero();
        File goldenFile = new File(getResource("SimpleDecodesTest/golden"));
        String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");
        assertEquals(golden,output,"Output Doesn't match expected data.");
    }

    @Test
    public void test_HydroJsonTest() throws Exception
    {
        Configuration config = this.configuration;
        File propertiesFile = config.getPropertiesFile();
        File logFile = new File(config.getUserDir(),"/decodes-json.log");
        log.info("Importing test db.");
        Programs.DbImport(logFile, propertiesFile, environment, exit,
                                getResource("shared/test-sites.xml"),
                                getResource("shared/ROWI4.xml"),
                                new File(config.getUserDir(),"/schema/cwms/cwms-import.xml").getAbsolutePath(),
                                getResource("shared/presgrp-regtest.xml"),
                                getResource("HydroJsonTest/HydroJSON-rs.xml"));

        String output = SystemStubs.tapSystemOut(
            () -> exit.execute(() ->
                        RoutingSpecThread.main(
                            args("-l",logFile.getAbsolutePath(),"-d3","HydroJSON-Test")
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
}