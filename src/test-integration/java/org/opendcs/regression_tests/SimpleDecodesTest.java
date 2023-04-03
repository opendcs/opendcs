package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.OpenDCSAppTestCase;
import org.opendcs.spi.configuration.Configuration;
import decodes.dbimport.DbImport;
import decodes.routing.RoutingSpecThread;
import uk.org.webcompere.systemstubs.SystemStubs;
import uk.org.webcompere.systemstubs.stream.output.Output;

public class SimpleDecodesTest extends AppTestBase
{
    private static final Logger log = Logger.getLogger(SimpleDecodesTest.class.getName());


    @TestTemplate
    @Order(1)
    public void the_test(OpenDCSAppTestCase testCase) throws Exception
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

    
}