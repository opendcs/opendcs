/*
 *  Copyright 2026 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.res;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.dbimport.DbImport;
import decodes.tsdb.DbIoException;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsImporter;
import decodes.tsdb.xml.DbXmlException;
import decodes.util.DecodesSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opendcs.odcsapi.beans.ApiImportResult;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;
import org.slf4j.Logger;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;

/**
 * Imports configuration, computations, and time series using content detection.
 */
@Path("/")
public final class ImportResources extends OpenDcsResource
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    static final int MAX_IMPORT_BYTES = 10 * 1024 * 1024;

    static final String DECODES_IMPORT_TYPE = "decodes";
    static final String COMPUTATIONS_IMPORT_TYPE = "computations";
    static final String TIMESERIES_IMPORT_TYPE = "timeseries";
    private static final Set<String> DECODES_ROOTS = Set.of("Database", "EnumList", "Platform",
            "DataTypeEquivalenceList", "EngineeringUnitList", "RoutingSpec", "NetworkList",
            "PresentationGroup", "PlatformConfig", "EquipmentModel", "DataSource", "Site",
            "PlatformList", "IntervalList", "ScheduleEntry", "PlatformStatus");

    @Context
    HttpServletRequest httpRequest;

    @POST
    @Path("import/preview")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN})
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Inspect an import file without database writes",
            description = """
                    Upload raw UTF-8 file contents, at most 10 MiB. Returns the detected type, importer,
                    file statistics, and an upload ID for confirmation in the same session.
                    """,
            tags = {"REST - Import"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "File inspected",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ApiImportResult.class))),
                    @ApiResponse(responseCode = "400", description = "Missing, malformed, or unsupported content"),
                    @ApiResponse(responseCode = "413", description = "Import file exceeds 10 MiB")
            })
    public Response previewUpload(InputStream content) throws WebAppException
    {
        String text = readImportContent(content);
        ApiImportResult result = ImportPreviewParser.inspect(text);
        HttpSession session = httpRequest.getSession(false);
        if (session == null)
        {
            throw new WebAppException(401, "An authenticated session is required.");
        }
        result.setUploadId(ImportUploads.stage(session, organizationId, text, result));
        return Response.ok(result).build();
    }

    @POST
    @Path("import/{uploadId}/confirm")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Import DECODES configuration, computations, or time series data",
            description = """
                    Import the previewed file using its upload ID in the same session.
                    The upload is removed after processing or when the session ends.
                    """,
            tags = {"REST - Import"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Import completed",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ApiImportResult.class))),
                    @ApiResponse(responseCode = "404", description = "Upload unavailable in this session or organization"),
                    @ApiResponse(responseCode = "500", description = "Import failed")
            })
    public Response confirmImport(
            @PathParam("uploadId") String uploadId,
            @Parameter(description = "Keep existing records in XML imports.")
            @QueryParam("keepOld") boolean keepOld,
            @Parameter(description = "Create missing time series for computation parameters.")
            @QueryParam("createTimeSeries") boolean createTimeSeries) throws DbException, WebAppException
    {
        try (ImportUploads.Upload upload = ImportUploads.take(httpRequest.getSession(false), uploadId, organizationId))
        {
            ApiImportResult result = switch (upload.preview().getImportType())
            {
                case DECODES_IMPORT_TYPE -> importDecodes(keepOld, upload.file());
                case COMPUTATIONS_IMPORT_TYPE -> importComputations(keepOld, createTimeSeries, upload.file());
                default -> importTimeSeries(upload.file());
            };
            result.setUploadId(uploadId);
            result.setImporter(upload.preview().getImporter());
            result.setSizeBytes(upload.preview().getSizeBytes());
            return Response.ok(result).build();
        }
        catch (DatabaseException | DbXmlException | DbIoException | IllegalStateException ex)
        {
            log.error("Import failed for upload {}", uploadId, ex);
            throw new DbException("Import failed: " + ex.getMessage(), ex);
        }
        catch (IOException ex)
        {
            throw new WebAppException(500, "Unable to read the staged import file.", ex);
        }
    }

    static String readImportContent(InputStream content) throws WebAppException
    {
        if (content == null)
        {
            throw new MissingParameterException("Missing import content in request body.");
        }
        try
        {
            byte[] bytes = content.readNBytes(MAX_IMPORT_BYTES + 1);
            if (bytes.length > MAX_IMPORT_BYTES)
            {
                throw new WebAppException(413, "Import file exceeds the maximum size of 10 MiB (10,485,760 bytes).");
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        }
        catch (IOException ex)
        {
            throw new WebAppException(400, "Unable to read import content.", ex);
        }
    }

    private ApiImportResult importDecodes(boolean keepOld, java.nio.file.Path file) throws DatabaseException, DbException
    {
        Database db = createDb().getLegacyDatabase(Database.class)
                .orElseThrow(() -> new DbException("No legacy DECODES database is available."));
        DbImport dbImport = new DbImport(db, false, keepOld, false, false, false,
                null, null, null, null, List.of(file.toString()));
        dbImport.importDatabase();
        return importResult(DECODES_IMPORT_TYPE, true, "Import completed successfully.");
    }

    private ApiImportResult importComputations(boolean keepOld, boolean createTimeSeries, java.nio.file.Path file)
            throws DbXmlException
    {
        ImportComp importComp = new ImportComp(getLegacyTimeseriesDB(), createTimeSeries, keepOld, List.of(file.toString()));
        importComp.runApp();
        return importResult(COMPUTATIONS_IMPORT_TYPE, !importComp.hasErrors(), importComp.hasErrors()
                ? "Import completed with problems: " + String.join("; ", importComp.getErrors())
                : "Import completed successfully.");
    }

    private static ApiImportResult importResult(String type, boolean success, String message)
    {
        ApiImportResult result = new ApiImportResult();
        result.setImportType(type);
        result.setSuccess(success);
        result.setMessage(message);
        return result;
    }

    static String detectImportType(String content) throws WebAppException
    {
        requireContent(content);
        String first = content.lines().map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).findFirst().orElse("");
        if (!first.startsWith("<"))
        {
            if (first.regionMatches(true, 0, "TSID:", 0, 5)
                    || first.regionMatches(true, 0, "SET:", 0, 4))
            {
                return TIMESERIES_IMPORT_TYPE;
            }
            throw new WebAppException(400, "Unsupported import content. Expected DECODES XML, CompMetaData XML, or TSID:/SET: records.");
        }
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try
        {
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(content));
            try
            {
                String root = null;
                while (reader.hasNext())
                {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT && root == null)
                    {
                        root = reader.getLocalName();
                    }
                }
                if ("CompMetaData".equalsIgnoreCase(root) || "LoadingApplication".equalsIgnoreCase(root))
                {
                    return COMPUTATIONS_IMPORT_TYPE;
                }
                if (root != null && DECODES_ROOTS.stream().anyMatch(root::equalsIgnoreCase))
                {
                    return DECODES_IMPORT_TYPE;
                }
                throw new WebAppException(400, "Unsupported XML root element: " + root);
            }
            finally
            {
                reader.close();
            }
        }
        catch (XMLStreamException ex)
        {
            throw new WebAppException(400, "Malformed XML import content.", ex);
        }
    }

    private ApiImportResult importTimeSeries(java.nio.file.Path file) throws IOException, DbIoException
    {
        TimeSeriesDb tsdb = getLegacyTimeseriesDB();
        DecodesSettings settings = DecodesSettings.instance();
        int expected = TsImporter.scanForTsIds(file.toString()).size();
        int count = TsImporter.importTimeSeriesFile(tsdb, file.toString(), null,
                TimeZone.getTimeZone(settings.sqlTimeZone), settings.siteNameTypePreference);
        return importResult(TIMESERIES_IMPORT_TYPE, count == expected,
                "Imported " + count + " of " + expected + " time series.");
    }

    private static void requireContent(String xmlContent) throws MissingParameterException
    {
        if (xmlContent == null || xmlContent.isBlank())
        {
            throw new MissingParameterException("Missing import content in request body.");
        }
    }
}
