package org.opendcs.odcsapi.res;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opendcs.odcsapi.beans.ApiImportResult;
import org.opendcs.odcsapi.errorhandling.WebAppException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImportResourcesTest
{
    @Test
    void rejectsOversizedUploadAndBoundsReading()
    {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[ImportResources.MAX_IMPORT_BYTES + 100]);
        WebAppException error = assertThrows(WebAppException.class,
                () -> ImportResources.readImportContent(stream));
        assertEquals(413, error.getStatus());
        assertEquals(99, stream.available()); // Of 100 extra bytes, only one is read to detect the size limit.
    }

    @ParameterizedTest
    @MethodSource("importTypeCases")
    void detectsImportType(String expectedType, String content) throws Exception
    {
        assertEquals(expectedType, ImportResources.detectImportType(content));
    }

    private static Stream<Arguments> importTypeCases()
    {
        return Stream.of(
                Arguments.of("decodes", """
                        <?xml version="1.0"?>
                        <!-- CompMetaData -->
                        <Database><Site/></Database>
                        """),
                Arguments.of("computations", """
                        <?xml version="1.0"?>
                        <CompMetaData><Site/></CompMetaData>
                        """),
                Arguments.of("decodes", "<Platform/>"),
                Arguments.of("decodes", """
                        <!DOCTYPE Database SYSTEM "file:///does-not-exist.dtd">
                        <Database/>
                        """),
                Arguments.of("timeseries", """
                        # time series export

                        TSID:TEST.Flow.Inst.1Hour.0.test
                        """),
                Arguments.of("timeseries", """
                        set:TZ=UTC
                        tsid:TEST.Flow.Inst.1Hour.0.test
                        """));
    }

    @Test
    void previewCountsNestedDecodesObjects() throws Exception
    {
        String xml = "<Database><Platform><Site/><PlatformConfig/></Platform><Site/></Database>";
        ApiImportResult result = ImportPreviewParser.inspect(xml);
        assertEquals("decodes", result.getImportType());
        assertEquals("dbimport", result.getImporter());
        assertEquals(Map.of("Platform", 1L, "Site", 2L, "PlatformConfig", 1L), result.getCounts());
        assertEquals(xml.getBytes(StandardCharsets.UTF_8).length, result.getSizeBytes());
        assertTrue(result.isValidateOnly());
        assertTrue(result.isSuccess());
    }

    @ParameterizedTest
    @MethodSource("timeSeriesPreviewCases")
    void previewsTimeSeries(String content, long series, long samples, String firstTime,
            String lastTime, int warnings) throws Exception
    {
        ApiImportResult result = ImportPreviewParser.inspect(content);
        assertEquals("timeseries", result.getImportType());
        assertEquals("tsimport", result.getImporter());
        assertEquals(Map.of("timeSeries", series, "samples", samples), result.getCounts());
        assertEquals(firstTime, result.getFirstSampleTime());
        assertEquals(lastTime, result.getLastSampleTime());
        assertTrue(result.isSuccess());
        assertEquals(warnings, result.getWarnings().size());
    }

    private static Stream<Arguments> timeSeriesPreviewCases()
    {
        return Stream.of(
                Arguments.of("""
                        # Export
                        SET:TZ=GMT-06:00
                        TSID:A.Flow.Inst.1Hour.0.test
                        SET:UNITS=cfs
                        2026/01/02-00:00:00,1,0x01
                        TSID:B.Flow.Inst.1Hour.0.test
                        2026/01/01-00:00:00,m,0b10
                        TSID:A.Flow.Inst.1Hour.0.test
                        2026/01/03-00:00:00,2,0
                        """, 2L, 3L, "2026-01-01T06:00:00Z", "2026-01-03T06:00:00Z", 0),
                Arguments.of("""
                        SET:TZ=UTC
                        SET:DATEFORMAT=yyyy-MM-dd HH:mm
                        TSID:A
                        2026-01-01 00:00,3,0
                        2026-01-02 00:00,invalid,0
                        2026-01-03 00:00,4,invalid
                        """, 1L, 1L, "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", 2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<Unknown/>", "<Database>", "random text",
            "<!-- <Database/> --><Unknown/>", "# only a comment"})
    void rejectsUnsupportedAndMalformedContent(String content)
    {
        WebAppException error = assertThrows(WebAppException.class,
                () -> ImportPreviewParser.inspect(content));
        assertEquals(400, error.getStatus());
    }
}
