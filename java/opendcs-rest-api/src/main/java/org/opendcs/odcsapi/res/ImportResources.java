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
import java.nio.file.Files;
import java.util.List;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.dbimport.DbImport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
 * HTTP Resources for importing XML files into the database.
 * The DECODES import runs the same code as the command line dbimport program.
 */
@Path("/")
public final class ImportResources extends OpenDcsResource
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	static final String DECODES_IMPORT_TYPE = "decodes";

	@POST
	@Path("import/{importType}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN})
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Import an XML file into the database",
			description = """
					The request body must contain the raw contents of the XML file to import. \
					The importType path parameter selects the importer to run. \
					Currently only 'decodes' (the same import performed by the command line \
					dbimport program) is supported; a 'computations' importer is planned.

					Set validateOnly=true to run in preview mode: conflicts are reported \
					but nothing is written to the database.""",
			tags = {"REST - Import"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Import (or validation) completed",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiImportResult.class))),
					@ApiResponse(responseCode = "400", description = "Missing or empty XML content"),
					@ApiResponse(responseCode = "501", description = "Unsupported import type"),
					@ApiResponse(responseCode = "500", description = "Import failed")
			}
	)
	@SuppressWarnings("deprecation") // DbImport is legacy code and needs the legacy Database handle.
	public Response importFile(
			@Parameter(required = true, description = "Type of import to perform (currently only 'decodes').")
			@PathParam("importType") String importType,
			@Parameter(description = "Validate only (preview): report conflicts but do not write to the database.")
			@QueryParam("validateOnly") boolean validateOnly,
			@Parameter(description = "Keep existing records when there is a conflict "
					+ "(default is to overwrite them with the imported records).")
			@QueryParam("keepOld") boolean keepOld,
			String xmlContent)
			throws DbException, WebAppException
	{
		if (xmlContent == null || xmlContent.isBlank())
		{
			throw new MissingParameterException("Missing XML content in request body.");
		}
		if (!DECODES_IMPORT_TYPE.equalsIgnoreCase(importType))
		{
			throw new WebAppException(Response.Status.NOT_IMPLEMENTED.getStatusCode(),
					String.format("Unsupported import type '%s'. Supported types: %s.",
							importType, DECODES_IMPORT_TYPE));
		}

		java.nio.file.Path tempFile = null;
		try
		{
			tempFile = Files.createTempFile("odcs-import-", ".xml");
			Files.writeString(tempFile, xmlContent);

			Database theDb = createDb().getLegacyDatabase(Database.class)
					.orElseThrow(() -> new DbException("No legacy DECODES database is available."));

			DbImport dbImport = new DbImport(theDb, validateOnly, keepOld, false, false, false,
					null, null, null, null, List.of(tempFile.toString()));
			dbImport.importDatabase();

			ApiImportResult result = new ApiImportResult();
			result.setImportType(DECODES_IMPORT_TYPE);
			result.setValidateOnly(validateOnly);
			result.setSuccess(true);
			result.setMessage(validateOnly
					? "Validation completed without errors."
					: "Import completed successfully.");
			return Response.ok(result).build();
		}
		catch (IOException ex)
		{
			throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
					"Unable to stage the uploaded file for import.", ex);
		}
		catch (DatabaseException | IllegalStateException ex)
		{
			log.error("Decodes import failed (validateOnly={})", validateOnly, ex);
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
}
