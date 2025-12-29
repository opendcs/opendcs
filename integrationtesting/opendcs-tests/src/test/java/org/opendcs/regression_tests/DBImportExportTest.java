package org.opendcs.regression_tests;

import decodes.dbimport.DbExport;
import decodes.dbimport.DbImport;
import org.apache.commons.io.IOUtils;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DecodesConfigurationRequired({
        "DBImportExportTest/existing.xml"
})
public class DBImportExportTest extends AppTestBase
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    //TODO: try DBExport @Before this complete test, and DBImport @After, so other tests aren't impacted by the clean imports.
    //TODO: remove golden comparisons since they have indeterminate orders and won't compare cleanly crossplatform
    //TODO: change name of class since DBExport tests will not be included
    //TODO: move try handling to assertdoesnotthrow per PR review comments

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, value = { // would check datasources here too, but they're always zero in the DB for some reason
        "Name, Expected DBExport File, Expected Platforms, Expected Routing Specs",
        "normal,DBImportExportTest/goldennormal${impl}.xml,3,2",
        "platonly,DBImportExportTest/goldenplatonly${impl}.xml,3,1",
        "platover,DBImportExportTest/goldenplatover${impl}.xml,2,1",
    })
    public void test_normal(String type, String expectedResultFile, int plats, int rs) throws Exception
    {
        Configuration config = this.configuration;
        File logFile = new File(config.getUserDir(),"/dbimport-" + type + ".log");

        assertExitNullOrZero();

        String clearDB = SystemStubs.tapSystemOut(
                () -> exit.execute(() ->
                        DbImport.main(
                                args("-l", logFile.getAbsolutePath(), "-W", "-y",
                                        TestResources.getResource(configuration,"DBImportExportTest/existing.xml"))
                        )
                )
        );
        assertExitNullOrZero();

        List<String> platargs =  new ArrayList<>(Arrays.asList("-l", logFile.getAbsolutePath(), "-d3"));
        switch(type) {
            case "normal":
                log.info("Loading platform, routing spec, etc with no extra flags.");
                break;
            case "platonly":
                log.info("Loading platforms with platform only flag.");
                platargs.add("-p");
                break;
            case "platover":
                log.info("Loading platforms with platform only and overwrite flags.");
                platargs.addAll(Arrays.asList("-W", "-y", "-p"));
                break;
        }
        String newFile = TestResources.getResource(configuration,"DBImportExportTest/new.xml");
        platargs.add(newFile);

        String importOut = SystemStubs.tapSystemOut(
                            () -> exit.execute(() ->
                                    DbImport.main(
                                            platargs.toArray(new String[0])
                                    )
                            )
                        );

        assertExitNullOrZero();

        String exportOut = SystemStubs.tapSystemOut(
                () -> exit.execute(() ->
                        DbExport.main(
                                args("-l", logFile.getAbsolutePath(), "-d3")
                        )
                )
        );
        assertExitNullOrZero();

        //String cleanedOut = cleanString(exportOut);

        //File goldenFile = new File(TestResources.getResource(configuration,expectedResultFile));
        //String golden = cleanString(IOUtils.toString(goldenFile.toURI().toURL().openStream(), StandardCharsets.UTF_8));
        // code to generate golden results
        //FileWriter exportXMLFile = new FileWriter(TestResources.getResource(configuration,type + "export.xml"));
        //IOUtils.write(exportOut, exportXMLFile);
        //IOUtils.closeQuietly(exportXMLFile);
        //assertEquals(golden, cleanedOut, "Output Doesn't match expected data."); //how to handle modified times?!?

        try
        {
            int existingplats = configuration.getDecodesDatabase().platformList.size();
            int existingrs = configuration.getDecodesDatabase().routingSpecList.size();
            assertEquals(plats, existingplats, "Expected number of platforms not found!");
            assertEquals(rs, existingrs, "Expected number of data sources not found!");
        } catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    private String cleanString(String exportOut)
    {
        List<String> exportLines = new ArrayList<String>(Arrays.asList(exportOut.split("\\R")));
        return exportLines.stream().filter(line -> !line.contains("LastModifyTime"))
                .filter(line -> !line.contains("PlatformId"))
                .filter(line -> !line.contains("password"))
                .collect(Collectors.joining(System.lineSeparator()));
    }


}