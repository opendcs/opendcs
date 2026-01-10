package org.opendcs.regression_tests;

import decodes.dbimport.DbExport;
import decodes.dbimport.DbImport;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.helpers.TestResources;
import org.opendcs.spi.configuration.Configuration;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import uk.org.webcompere.systemstubs.SystemStubs;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DecodesConfigurationRequired({
        "DBImportExportTest/existing.xml"
})
public class DBImportExportTest extends AppTestBase
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static int prevPlats = 0;
    private static int prevRS = 0;
    // Since the order of DBExport results are non-determinate, we can't use a golden comparison.
    // If tests after this one start failing, the DBExport/Import functions may have been impacted.
    @BeforeAll
    public void beforeTests() throws Throwable
    {
        prevPlats = configuration.getDecodesDatabase().platformList.size();
        prevRS = configuration.getDecodesDatabase().routingSpecList.size();

        File logFile = new File(TestResources.getResource(configuration,"DBImportExportTest/ExportBefore.log"));

        String prevDb = SystemStubs.tapSystemOut(
                () -> exit.execute(() ->
                        DbExport.main(
                                args("-l", logFile.getAbsolutePath(), "-d3")
                        )
                )
        );
        assertExitNullOrZero();

        FileWriter exportXMLFile = new FileWriter(TestResources.getResource(configuration, "DBImportExportTest/prevexport.xml"));
        IOUtils.write(prevDb, exportXMLFile);
        IOUtils.closeQuietly(exportXMLFile);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, value = {
        "Name, Description, DBImport Arguments, Expected Platforms, Expected Routing Specs",
        "normal,DBImport with no extra flags,,3,2",
        "platOnly,DBImport with platform only flag,-p,3,1",
        "platOver,DBImport with platform and overwrite flag,-p -W -y,2,1",
    })
    public void test_normal(String type, String desc, String testArgs, int expectedPlats, int expectedRS) throws Throwable
    {
        Configuration config = this.configuration;
        File logFile = new File(config.getUserDir(),"/dbimport-" + type + ".log");

        // uses overwrite flags to reset database to completely known state.
        String clearDB = SystemStubs.tapSystemOut(
                () -> exit.execute(() ->
                        DbImport.main(
                                args("-l", logFile.getAbsolutePath(), "-W", "-y",
                                        TestResources.getResource(configuration,"DBImportExportTest/existing.xml"))
                        )
                )
        );
        assertExitNullOrZero();

        List<String> platArgs =  new ArrayList<>(Arrays.asList("-l", logFile.getAbsolutePath(), "-d3"));
        if (testArgs != null) {
            platArgs.addAll(Arrays.asList(testArgs.split(" ")));
        }

        String newFile = TestResources.getResource(configuration,"DBImportExportTest/new.xml");
        platArgs.add(newFile);

        log.info("Running Test: {}", desc);
        String importOut = SystemStubs.tapSystemOut(
                            () -> exit.execute(() ->
                                    DbImport.main(
                                            platArgs.toArray(new String[0])
                                    )
                            )
                        );
        assertExitNullOrZero();

        // would check the number of datasources here too, but that list is always length zero in the DB for some reason
        int existingPlats = configuration.getDecodesDatabase().platformList.size();
        int existingRS = configuration.getDecodesDatabase().routingSpecList.size();
        assertEquals(expectedPlats, existingPlats, "Expected number of platforms not found!");
        assertEquals(expectedRS, existingRS, "Expected number of data sources not found!");
    }

    @AfterAll
    public void afterTests() throws Throwable
    {
        File logFile = new File(TestResources.getResource(configuration,"DBImportExportTest/ImportAfter.log"));

        String restoreDB = SystemStubs.tapSystemOut(
                () -> exit.execute(() ->
                        DbImport.main(
                                args("-l", logFile.getAbsolutePath(), "-d3", "-W", "-y",
                                        TestResources.getResource(configuration, "DBImportExportTest/prevexport.xml"))
                        )
                )
        );
        assertExitNullOrZero();
        int newPlats = configuration.getDecodesDatabase().platformList.size();
        int newRs = configuration.getDecodesDatabase().routingSpecList.size();
        assertEquals(prevPlats, newPlats, "Expected number of platforms not found!");
        assertEquals(prevRS, newRs, "Expected number of data sources not found!");
    }
}