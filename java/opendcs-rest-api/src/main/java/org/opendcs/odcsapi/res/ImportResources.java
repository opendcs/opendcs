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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.dbimport.DbImport;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.xml.DbXmlException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
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
 * HTTP resources for importing decodes and computations XML files into the database.
 */
@Path("/")
public final class ImportResources extends OpenDcsResource
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	static final String DECODES_IMPORT_TYPE = "decodes";
	static final String COMPUTATIONS_IMPORT_TYPE = "computations";

	private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY_PERMS =
			PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

	@FunctionalInterface
	private interface ImportStep
	{
		ApiImportResult apply(java.nio.file.Path file) throws DatabaseException, DbXmlException, DbException;
	}

	@POST
	@Path("import/decodes")
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN})
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Import a DECODES XML file into the database",
			description = """
					The request body must contain the raw contents of a DECODES XML file \
					(dbexport style). Runs the same import as the command line dbimport program.

					Set validateOnly=true to run in preview mode: conflicts are reported \
					but nothing is written to the database.""",
			tags = {"REST - Import"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Import (or validation) completed",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiImportResult.class))),
					@ApiResponse(responseCode = "400", description = "Missing or empty XML content, or import failed"),
					@ApiResponse(responseCode = "500", description = "Import failed")
			}
	)

	public Response importDecodes(
			@Parameter(description = "Validate only (preview): report conflicts but do not write to the database.")
			@QueryParam("validateOnly") boolean validateOnly,
			@Parameter(description = "Keep existing records when there is a conflict "
					+ "(default is to overwrite them with the imported records).")
			@QueryParam("keepOld") boolean keepOld,
			String xmlContent)
			throws DbException, WebAppException
	{
		requireContent(xmlContent);
		ApiImportResult result = stageAndImport(DECODES_IMPORT_TYPE, validateOnly, xmlContent, file ->
		{
			Database theDb = createDb().getLegacyDatabase(Database.class)
					.orElseThrow(() -> new DbException("No legacy DECODES database is available."));

			/*
			 * | Arg                   | CLI flag | Hardcoded meaning                                                       |
			 * |-----------------------|----------|-------------------------------------------------------------------------|
			 * | overwriteDb=false     | -W       | Never wipe the DB and replace with the imported image — too destructive |
			 * |                       |          | to expose over REST                                                     |
			 * | platformRelatedOnly=false | -p   | Import all object types in the XML, not just platform-related ones      |
			 * | allowHistorical=false | -H       | Ignore historical versions in the XML rather than import them           |
			 * | pdtFilePath=null      | -P       | No property-key translation dictionary file                             |
			 * | newDesignator=null    | -D       | No remapping of time-series designator IDs                              |
			 * | defaultAgency=null    | -A       | Keep agencies as written in the file                                    |
			 * | newPlatformOwner=null | -O       | Keep platform owners as written in the file                             |
			 */
			DbImport dbImport = new DbImport(theDb, validateOnly, keepOld, false, false, false,
					null, null, null, null, List.of(file.toString()));
			dbImport.importDatabase();

			ApiImportResult res = new ApiImportResult();
			res.setImportType(DECODES_IMPORT_TYPE);
			res.setValidateOnly(validateOnly);
			res.setSuccess(true);
			res.setMessage(validateOnly
					? "Validation completed without errors."
					: "Import completed successfully.");
			return res;
		});
		return Response.ok(result).build();
	}

	@POST
	@Path("import/computations")
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN})
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Import a computations XML file into the database",
			description = """
					The request body must contain the raw contents of a CompMetaData XML file. \
					Runs the same import as the command line compimport program \
					(time series groups, loading applications, computations and algorithms).

					Set validateOnly=true to parse and check the XML without writing anything \
					to the database. Note this is a syntax/structure check only \
					(compimport has no preview mode), not a conflict check.

					Individual records that fail to import are reported in the result message \
					(success=false); other records from the same file are still imported.""",
			tags = {"REST - Import"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Import (or validation) completed",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiImportResult.class))),
					@ApiResponse(responseCode = "400", description = "Missing or empty XML content, or unparsable file"),
					@ApiResponse(responseCode = "500", description = "Import failed")
			}
	)
	public Response importComputations(
			@Parameter(description = "Validate only (preview): parse and check the XML without writing to the database.")
			@QueryParam("validateOnly") boolean validateOnly,
			@Parameter(description = "Do not overwrite existing records with matching names "
					+ "(the compimport -o switch; default is to overwrite).")
			@QueryParam("keepOld") boolean keepOld,
			@Parameter(description = "Create time series as needed when resolving computation "
					+ "parameters (the compimport -C switch).")
			@QueryParam("createTimeSeries") boolean createTimeSeries,
			String xmlContent)
			throws DbException, WebAppException
	{
		requireContent(xmlContent);
		ApiImportResult result = stageAndImport(COMPUTATIONS_IMPORT_TYPE, validateOnly, xmlContent, file ->
		{
			TimeSeriesDb tsdb = getLegacyTimeseriesDB();

			ApiImportResult res = new ApiImportResult();
			res.setImportType(COMPUTATIONS_IMPORT_TYPE);
			res.setValidateOnly(validateOnly);
			if (validateOnly)
			{
				// Same parse the compimport program performs, without any database writes.
				new CompXio("ImportComp", tsdb).readFile(file.toString());
				res.setSuccess(true);
				res.setMessage("Validation completed without errors.");
				return res;
			}

			ImportComp importComp = new ImportComp(tsdb, createTimeSeries, keepOld, List.of(file.toString()));
			importComp.runApp();
			res.setSuccess(!importComp.hasErrors());
			res.setMessage(importComp.hasErrors()
					? ("Import completed with problems: " + String.join("; ", importComp.getErrors()))
					: "Import completed successfully.");
			return res;
		});
		return Response.ok(result).build();
	}

	private static void requireContent(String xmlContent) throws MissingParameterException
	{
		if (xmlContent == null || xmlContent.isBlank())
		{
			throw new MissingParameterException("Missing XML content in request body.");
		}
	}

	/** Stages the uploaded content to a temp file, runs the import, and cleans up. */
	private ApiImportResult stageAndImport(String importType, boolean validateOnly, String xmlContent,
			ImportStep step)
			throws DbException, WebAppException
	{
		java.nio.file.Path tempFile = null;
		try
		{
			tempFile = createStagingFile();
			Files.writeString(tempFile, xmlContent);
			return step.apply(tempFile);
		}
		catch (IOException ex)
		{
			throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
					"Unable to stage the uploaded file for import.", ex);
		}
		catch (DatabaseException | DbXmlException | IllegalStateException ex)
		{
			log.error("{} import failed (validateOnly={})", importType, validateOnly, ex);
			throw new DbException("Import failed: " + ex.getMessage(), ex);
		}
		finally
		{
			if (tempFile != null)
			{
				try
				{
					Files.deleteIfExists(tempFile);
				}
				catch (IOException ex)
				{
					log.warn("Unable to delete temporary import file {}", tempFile, ex);
				}
			}
		}
	}

	/** Creates the staging file for the uploaded XML, owner-only (rw-------) where POSIX permissions are supported. */
	private static java.nio.file.Path createStagingFile() throws IOException
	{
		java.nio.file.Path tempDir = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"));
		if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
		{
			return Files.createTempFile(tempDir, "odcs-import-", ".xml", OWNER_ONLY_PERMS);
		}
		return Files.createTempFile(tempDir, "odcs-import-", ".xml");
	}
}
