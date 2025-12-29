package org.opendcs.regression_tests;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @ParameterizedTest
    @CsvSource({
        "normal,DBImportExportTest/goldennormal${impl}.xml",
        "platonly,DBImportExportTest/goldenplatonly${impl}.xml",
        "platover,DBImportExportTest/goldenplatover${impl}.xml",
    })
    public void test_normal(String type, String expectedResultFile) throws Exception
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

        String cleanedOut = cleanString(exportOut);

        File goldenFile = new File(TestResources.getResource(configuration,expectedResultFile));
        String golden = cleanString(IOUtils.toString(goldenFile.toURI().toURL().openStream(), StandardCharsets.UTF_8));
        // code to generate golden results
        //FileWriter exportXMLFile = new FileWriter(TestResources.getResource(configuration,type + "export.xml"));
        //IOUtils.write(exportOut, exportXMLFile);
        //IOUtils.closeQuietly(exportXMLFile);
        assertEquals(golden, cleanedOut, "Output Doesn't match expected data."); //how to handle modified times?!?
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