/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import decodes.sql.DbKey;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ScriptType;
import decodes.tsdb.TsdbException;
import decodes.tsdb.compedit.AlgorithmInList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import opendcs.dai.AlgorithmDAI;
import org.opendcs.odcsapi.beans.ApiAlgoParm;
import org.opendcs.odcsapi.beans.ApiAlgorithm;
import org.opendcs.odcsapi.beans.ApiAlgorithmRef;
import org.opendcs.odcsapi.beans.ApiAlgorithmScript;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Path("/")
public final class AlgorithmResources extends OpenDcsResource
{

	@GET
	@Path("algorithmrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Retrieve all algorithm references",
			description = "Example: \n\n    http://localhost:8080/odcsapi/algorithmrefs",
			operationId = "getalgorithmrefs",
			tags = {"REST - Algorithm Methods"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiAlgorithmRef.class)))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response getAlgorithmRefs() throws DbIoException
	{
		try(AlgorithmDAI dai = getLegacyTimeseriesDB().makeAlgorithmDAO())
		{
			List<ApiAlgorithmRef> algorithmRefs = dai.listAlgorithmsForGui()
					.stream()
					.map(AlgorithmResources::map)
					.sorted(comparing(ApiAlgorithmRef::getAlgorithmId))
					.collect(toList());
			return Response.status(HttpServletResponse.SC_OK).entity(algorithmRefs)
					.build();
		}
	}

	static ApiAlgorithmRef map(AlgorithmInList algorithm)
	{
		ApiAlgorithmRef retval = new ApiAlgorithmRef();
		retval.setAlgorithmId(algorithm.getAlgorithmId().getValue());
		retval.setDescription(algorithm.getDescription());
		retval.setAlgorithmName(algorithm.getAlgorithmName());
		retval.setExecClass(algorithm.getExecClass());
		retval.setNumCompsUsing(algorithm.getNumCompsUsing());
		return retval;
	}

	@GET
	@Path("algorithm")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Retrieve an algorithm by its ID",
			description = "Example: \n\n    http://localhost:8080/odcsapi/algorithm?algorithmid=4",
			operationId = "getalgorithm",
			tags = {"REST - Algorithm Methods"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiAlgorithm.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request - Missing required parameter"),
					@ApiResponse(responseCode = "404", description = "Not Found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response getAlgorithm(@Parameter(description = "Unique Algorithm ID", required = true,
			schema = @Schema(implementation = Long.class, example = "4"))
		@QueryParam("algorithmid") Long algoId)
			throws WebAppException, DbIoException
	{
		if(algoId == null)
		{
			throw new MissingParameterException("Missing required algorithmid parameter.");
		}
		try(AlgorithmDAI dai = getLegacyTimeseriesDB().makeAlgorithmDAO())
		{
			ApiAlgorithm apiAlgorithm = map(dai.getAlgorithmById(DbKey.createDbKey(algoId)));
			return Response.status(HttpServletResponse.SC_OK)
					.entity(apiAlgorithm)
					.build();
		}
		catch(NoSuchObjectException ex)
		{
			throw new DatabaseItemNotFoundException(String.format("No Computation Algorithm with id: %d", algoId), ex);
		}
	}

	static ApiAlgorithm map(DbCompAlgorithm algorithm)
	{
		ApiAlgorithm retval = new ApiAlgorithm();
		retval.setAlgorithmId(algorithm.getId().getValue());
		retval.setName(algorithm.getName());
		retval.setExecClass(algorithm.getExecClass());
		retval.setDescription(algorithm.getComment());
		retval.setNumCompsUsing(algorithm.getNumCompsUsing());
		retval.setProps(algorithm.getProperties());
		List<ApiAlgoParm> parameters = new ArrayList<>();
		Iterator<DbAlgoParm> params = algorithm.getParms();
		while(params.hasNext())
		{
			parameters.add(map(params.next()));
		}
		retval.setParms(parameters);
		retval.setAlgoScripts(algorithm.getScripts().stream()
				.map(AlgorithmResources::map)
				.collect(toList()));
		return retval;
	}

	private static ApiAlgorithmScript map(DbCompAlgorithmScript script)
	{
		ApiAlgorithmScript retval = new ApiAlgorithmScript();
		retval.setText(script.getText());
		retval.setScriptType(script.getScriptType().getDbChar());
		return retval;
	}

	private static ApiAlgoParm map(DbAlgoParm parameter)
	{
		ApiAlgoParm retval = new ApiAlgoParm();
		retval.setParmType(parameter.getParmType());
		retval.setRoleName(parameter.getRoleName());
		return retval;
	}


	@POST
	@Path("algorithm")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create or Overwrite Existing OpenDCS Algorithm",
			description = "It takes a single OpenDCS Algorithm Record in JSON format, as described above for GET.   \n\n"
					+ "For creating a new record, leave algorithmId out of the passed data structure.  \n\n"
					+ "For overwriting an existing one, include the algorithmId that was previously returned. "
					+ "The algorithm in the database is replaced with the one sent.",
			operationId = "postAlgorithm",
			tags = {"REST - Algorithm Methods"},
			requestBody = @RequestBody(description = "OpenDcs Algorithm",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiAlgorithm.class),
					examples = {
							@ExampleObject(name = "Basic", value = ResourceExamples.AlgorithmExamples.BASIC),
							@ExampleObject(name = "New", value = ResourceExamples.AlgorithmExamples.NEW),
							@ExampleObject(name = "Update", value = ResourceExamples.AlgorithmExamples.UPDATE)
					})),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully Created",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiAlgorithm.class))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response postAlgorithm(ApiAlgorithm algo) throws DbIoException
	{
		try(AlgorithmDAI dai = getLegacyTimeseriesDB().makeAlgorithmDAO())
		{
			DbCompAlgorithm map = map(algo);
			dai.writeAlgorithm(map);
			algo.setAlgorithmId(map.getId().getValue());
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(map))
					.build();
		}
	}

	static DbCompAlgorithm map(ApiAlgorithm algo)
	{
		Long algorithmId = algo.getAlgorithmId();
		DbKey dbKey = DbKey.NullKey;
		if(algorithmId != null)
		{
			dbKey = DbKey.createDbKey(algorithmId);
		}
		DbCompAlgorithm retval = new DbCompAlgorithm(dbKey, algo.getName(), algo.getExecClass(), algo.getDescription());
		retval.setNumCompsUsing(algo.getNumCompsUsing());
		algo.getProps()
				.forEach((key, value) -> retval.setProperty(String.valueOf(key), String.valueOf(value)));
		algo.getParms().forEach(p -> retval.addParm(new DbAlgoParm(p.getRoleName(), p.getParmType())));
		algo.getAlgoScripts().forEach(s -> retval.putScript(map(s, retval)));
		return retval;
	}

	private static DbCompAlgorithmScript map(ApiAlgorithmScript script, DbCompAlgorithm parent)
	{
		ScriptType scriptType = ScriptType.fromDbChar(script.getScriptType());
		DbCompAlgorithmScript retval = new DbCompAlgorithmScript(parent, scriptType);
		retval.addToText(script.getText());
		return retval;
	}

	@DELETE
	@Path("algorithm")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Existing OpenDCS Algorithm",
			description = "Required argument algorithmid must be passed in the URL.  \n\n"
					+ "This operation will fail if the algorithm is currently being used by any computation records.",
			operationId = "deleteAlgorithm",
			tags = {"REST - Algorithm Methods"},
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted algorithm"),
					@ApiResponse(responseCode = "400", description = "Bad Request - Missing required parameter"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response deleteAlgorithm(@Parameter(description = "ID of the algorithm to delete", required = true,
			schema = @Schema(implementation = Long.class, example = "4"))
		@QueryParam("algorithmid") Long algorithmId)
			throws TsdbException, MissingParameterException
	{
		if (algorithmId == null)
		{
			throw new MissingParameterException("Missing required algorithmid parameter.");
		}
		try(AlgorithmDAI dai = getLegacyTimeseriesDB().makeAlgorithmDAO())
		{
			dai.deleteAlgorithm(DbKey.createDbKey(algorithmId));
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.build();
		}
	}
}
