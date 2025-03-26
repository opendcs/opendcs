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

import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;

import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.beans.ApiDecodedMessage;
import org.opendcs.odcsapi.beans.ApiPropSpec;
import org.opendcs.odcsapi.beans.DecodeRequest;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.opendcs_dep.PropSpecHelper;
import org.opendcs.odcsapi.opendcs_dep.TestDecoder;
import org.opendcs.odcsapi.util.ApiConstants;


@Path("/")
public final class OdcsapiResource extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("tsdb_properties")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "Get TSDB Properties",
			description = "Example:  \n\n    http://localhost:8080/odcsapi/tsdb_properties  \n    \n    \n"
					+ "The tsdb_properties table in the database will be returned in a structure as follows:  \n  \n"
					+ "```\n  {\n    \"offsetErrorAction\": \"ROUND\",\n    \"storagePresentationGroup\": \"hydrodcs\","
					+ "\n    \"api.datasource\": \"Cove-LRGS\",\n    \"allowDstOffsetVariation\": \"true\"\n  }\n\n```",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = Properties.class),
									examples = {@ExampleObject(name = "Properties",
										value = "{\n  \"offsetErrorAction\": \"ROUND\",\n  "
											+ "\"storagePresentationGroup\": \"hydrodcs\",\n  "
											+ "\"api.datasource\": \"Cove-LRGS\",\n  "
											+ "\"allowDstOffsetVariation\": \"true\"\n}"),
							})),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - TSDB Properties Methods"}
	)
	public Response getTsdbProperties() throws DbException
	{
		try
		{
			TimeSeriesDb tsdb = getLegacyTimeseriesDB();
			tsdb.readTsdbProperties(tsdb.getConnection());

			Properties props = new Properties();
			for (Object keyObj : Collections.list(tsdb.getPropertyNames()))
			{
				String key = (String) keyObj;
				props.setProperty(key, tsdb.getProperty(key));
			}
			return Response.status(HttpServletResponse.SC_OK).entity(props).build();
		}
		catch(SQLException e)
		{
			throw new DbException("Error reading timeseries properties", e);
		}
	}

	@POST
	@Path("tsdb_properties")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Create or Update TSDB Properties",
			description = "The POST tsdb_properties method takes one or more properties in a structure"
					+ " as defined above for the GET method.  \n\n"
					+ "*\tAny property with the same name as one supplied will be overwritten by the passed value.\n"
					+ "*\tIf there is no property in the database with a matching name, a new property is added.\n"
					+ "*\tTo delete a property from the database, pass an empty string as the value.",
			requestBody = @RequestBody(
					required = true,
					description = "Properties to be stored in the database.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = Properties.class),
							examples = {@ExampleObject(name = "Properties",
									value = "{\n  \"offsetErrorAction\": \"ROUND\",\n  "
											+ "\"storagePresentationGroup\": \"hydrodcs\",\n  "
											+ "\"api.datasource\": \"Cove-LRGS\",\n  "
											+ "\"allowDstOffsetVariation\": \"true\"\n}"),
							})),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully stored properties",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = Properties.class),
									examples = {@ExampleObject(name = "Properties",
											value = "{\n  \"offsetErrorAction\": \"ROUND\",\n  "
													+ "\"storagePresentationGroup\": \"hydrodcs\",\n  "
													+ "\"api.datasource\": \"Cove-LRGS\",\n  "
													+ "\"allowDstOffsetVariation\": \"true\"\n}"),
									})),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - TSDB Properties Methods"}
	)
	public Response postTsdbProperties(Properties props) throws DbException
	{
		try
		{
			TimeSeriesDb tsdb = getLegacyTimeseriesDB();
			tsdb.writeTsdbProperties(props);
		}
		catch (DbIoException e)
		{
			throw new DbException("Error writing timeseries properties", e);
		}
		return Response.status(HttpServletResponse.SC_OK).entity(props).build();
	}

	@GET
	@Path("propspecs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Tag(name = "REST - Retrieving Property Specs", description = "Many of the Java classes within OpenDCS maintain a "
			+ "set of properties that can alter the object’s behavior. "
			+ "This method allows the caller to get a list of acceptable properties for a given class.")
	@Operation(
			summary = "Retrieve Property Specifications",
			description = "Many of the Java classes within OpenDCS maintain a set of properties that "
					+ "can alter the object’s behavior. This method allows the caller to get a list of acceptable "
					+ "properties for a given class.  \n\n"
					+ "\n\nExample:\n\n\thttp://localhost:8080/odcsapi/propspecs?class=decodes.db.Platform\n"
					+ "For example, here are the properties accepted by the class decodes.db.Platform\n```\n[\n  {\n    "
					+ "\"description\": \"(default=0) Set to 1, 2, 3 for increasing levels of debug information "
					+ "when this platform is decoded.\",\n    \"dynamic\": false,\n    \"name\": \"debugLevel\",\n    "
					+ "\"type\": \"i\"\n  },\n  {\n    "
					+ "\"description\": \"Set to have this platform ignored during a specified season.\",\n    "
					+ "\"dynamic\": false,\n    \"name\": \"ignoreSeason\",\n    \"type\": \"e:Season\"\n  },\n  {\n    "
					+ "\"description\": \"Set to have this platform only processed during a specified season.\",\n    "
					+ "\"dynamic\": false,\n    \"name\": \"processSeason\",\n    \"type\": \"e:Season\"\n  },\n  {\n    "
					+ "\"description\": \"(default=3) For polled stations, this determines the order in which they "
					+ "will be polled (1 = highest priority = polled first)\",\n    \"dynamic\": false,\n    "
					+ "\"name\": \"pollPriority\",\n    \"type\": \"i\"\n  }\n]\n```\nAn array of PropertySpec objects "
					+ "is returned in JSON format:\n*\t'name' is the property name\n"
					+ "*\t'description' is a description displayed to the user as a tooltip "
					+ "or in a dialog to set the property.\n"
					+ "*\t'type' gives the code information about acceptable values for this property.\n"
					+ "*\t'dynamic' is not used and should be ignored.  \n\n"
					+ "The 'type' variable can be used by the code to offer a pull-down list or to "
					+ "limit the range of acceptable values. 'type' will be one of the following: \n\n"
					+ "| value            | Description |\n| ---------------- | ------------|\n"
					+ "| **i** | Integer – Value can only be a whole number.     |\n"
					+ "| **n** | Number – may be integer or floating point |\n"
					+ "| **b** | Boolean – Value can be 'true' or 'false' |\n| **f** | Filename|\n"
					+ "| **d** | Directory Name|\n| **s** | Free-form string|\n"
					+ "| **t** | Time Zone Abbreviation – See the Java TimeZone doc for a list "
					+ "of acceptable abbreviations.|\n|**e:RefListName** |\t"
					+ "A pulldown list of values from a DECODES Reference List should be offered to the user "
					+ "(with a blank space at the top meaning ‘no selection’). See section 4.3 above for info "
					+ "on how to retrieve reference lists.|\n| **h**|\tA host name or IP address|\n| **l**\t"
					+ "| (That’s lower case L) indicates a long string that should be displayed in a "
					+ "multi-line text area with an optional scroll bar. But be aware that most of the "
					+ "property tables in the database limit a property value to 240 chars.|\n\n"
					+ "The following class names currently support property specs. More may be added in the future:  \n\n"
					+ "*\tdecodes.comp.AreaRatingCompResolver\n*\tdecodes.comp.RdbRatingCompResolver\n"
					+ "*\tdecodes.comp.StationExcludeCompResolver\n*\tdecodes.comp.TabRatingCompResolver\n"
					+ "*\tdecodes.consumer.DirectoryConsumer\n*\tdecodes.consumer.FileAppendConsumer\n"
					+ "*\tdecodes.consumer.FileConsumer\n*\tdecodes.consumer.PipeConsumer\n"
					+ "*\tdecodes.consumer.StringBufferConsumer\n*\tdecodes.consumer.TcpClientConsumer\n"
					+ "*\tdecodes.consumer.AlbertaLoaderFormatter\n*\tcovesw.azul.consumer.CsvFormatter\n"
					+ "*\tdecodes.consumer.EmitAsciiFormatter\n*\tdecodes.consumer.EmitOracleFormatter\n"
					+ "*\tdecodes.consumer.HeaderFormatter\n*\tdecodes.consumer.HtmlFormatter\n"
					+ "*\tdecodes.consumer.HumanReadableFormatter\n*\tdecodes.consumer.HydroJSONFormatter\n"
					+ "*\tdecodes.consumer.KHydstraFormatter\n*\tdecodes.consumer.KistersFormatter\n"
					+ "*\tdecodes.consumer.NullFormatter\n*\tdecodes.consumer.RawFormatter\n"
					+ "*\tdecodes.consumer.ShefFormatter\n*\tdecodes.consumer.ShefitFormatter\n"
					+ "*\tdecodes.consumer.TransmitMonitorFormatter\n*\tdecodes.consumer.TsImportFormatter\n"
					+ "*\tdecodes.cwms.CwmsConsumer\n*\tdecodes.datasource.DirectoryDataSource\n"
					+ "*\tdecodes.datasource.FtpDataSource\n*\tdecodes.datasource.HotBackupGroup\n"
					+ "*\tdecodes.datasource.LrgsDataSource\n*\tdecodes.datasource.RoundRobinGroup\n"
					+ "*\tdecodes.datasource.ScpDataSource\n*\tdecodes.datasource.SocketStreamDataSource\n"
					+ "*\tdecodes.datasource.UsgsWebDataSource\n*\tdecodes.datasource.WebAbstractDataSource\n"
					+ "*\tdecodes.datasource.WebDataSource\n*\tdecodes.datasource.WebDirectoryDataSource\n"
					+ "*\tdecodes.polling.PollingDataSource\n*\tdecodes.db.Platform\n*\tdecodes.db.PlatformSensor\n"
					+ "*\tdecodes.db.ConfigSensor\n*\tdecodes.routing.RoutingScheduler\n*\tdecodes.tsdb.CompAppInfo\n"
					+ "*\tdecodes.tsdb.algo.HdbRating\n*\tdecodes.tsdb.algo.RunningAverageAlgorithm\n"
					+ "*\tdecodes.tsdb.algo.AverageAlgorithm\n*\tdecodes.tsdb.algo.BigAdder\n"
					+ "*\tdecodes.tsdb.algo.Multiplication\n*\tdecodes.tsdb.algo.SumOverTimeAlgorithm\n"
					+ "*\tdecodes.tsdb.algo.ExpressionParserAlgorithm\n*\tdecodes.tsdb.algo.FillForward\n"
					+ "*\tdecodes.tsdb.algo.HdbReservoirMassBalance\n*\tdecodes.tsdb.algo.AddToPrevious\n"
					+ "*\tdecodes.tsdb.algo.Stat\n*\tdecodes.tsdb.algo.HdbEvaporation\n"
					+ "*\tdecodes.tsdb.algo.EstimatedInflow\n*\tdecodes.tsdb.algo.GroupAdder\n"
					+ "*\tdecodes.tsdb.algo.ReservoirFull\n*\tdecodes.tsdb.algo.ScalerAdder\n"
					+ "*\tdecodes.tsdb.algo.HdbACAPSRating\n*\tdecodes.tsdb.algo.ChooseOne\n"
					+ "*\tdecodes.tsdb.algo.BridgeClearance\n*\tdecodes.tsdb.algo.UsgsEquation\n"
					+ "*\tdecodes.tsdb.algo.IncrementalPrecip\n*\tdecodes.tsdb.algo.FlowResIn\n"
					+ "*\tdecodes.tsdb.algo.CopyAlgorithm\n*\tdecodes.tsdb.algo.DisAggregate\n"
					+ "*\tdecodes.tsdb.algo.WeightedWaterTemperature\n*\tdecodes.tsdb.algo.PythonAlgorithm\n"
					+ "*\tdecodes.tsdb.algo.Resample\n*\tdecodes.tsdb.algo.ShowAlgoProps\n"
					+ "*\tdecodes.tsdb.algo.PeriodToDate\n*\tdecodes.tsdb.algo.CopyNoOverwrite\n"
					+ "*\tdecodes.tsdb.algo.SubSample\n*\tdecodes.tsdb.algo.CentralRunningAverageAlgorithm\n"
					+ "*\tdecodes.tsdb.algo.TabRating\n*\tdecodes.tsdb.algo.RdbRating\n"
					+ "*\tdecodes.tsdb.algo.Division\n*\tdecodes.tsdb.algo.VirtualGage\n"
					+ "*\tdecodes.tsdb.alarm.AlarmScreeningAlgorithm\n*\tdecodes.hdb.algo.InflowAdvancedAlg\n*"
					+ "\tdecodes.hdb.algo.EquationSolverAlg\n*\tdecodes.hdb.algo.EstGLDAInflow\n"
					+ "*\tdecodes.hdb.algo.BeginofPeriodAlg\n*\tdecodes.hdb.algo.DynamicAggregatesAlg\n"
					+ "*\tdecodes.hdb.algo.HdbShiftRating\n*\tdecodes.hdb.algo.HdbLookupTimeShiftRating\n"
					+ "*\tdecodes.hdb.algo.SideInflowAlg\n*\tdecodes.hdb.algo.VolumeToFlowAlg\n"
					+ "*\tdecodes.hdb.algo.NVRNUnreg\n*\tdecodes.hdb.algo.PowerToEnergyAlg\n"
					+ "*\tdecodes.hdb.algo.GLDAUnreg\n*\tdecodes.hdb.algo.CallProcAlg\n"
					+ "*\tdecodes.hdb.algo.EndofPeriodAlg\n*\tdecodes.hdb.algo.GLDAEvap\n"
					+ "*\tdecodes.hdb.algo.BMDCUnreg\n*\tdecodes.hdb.algo.CRRCUnreg\n"
					+ "*\tdecodes.hdb.algo.ParshallFlume\n*\tdecodes.hdb.algo.FlowToVolumeAlg\n"
					+ "*\tdecodes.hdb.algo.SimpleDisaggAlg\n*\tdecodes.hdb.algo.EOPInterpAlg\n"
					+ "*\tdecodes.hdb.algo.TimeWeightedAverageAlg\n*\tdecodes.hdb.algo.InflowBasicAlg\n"
					+ "*\tdecodes.hdb.algo.MPRCUnreg\n*\tdecodes.hdb.algo.GlenDeltaBSMBAlg\n"
					+ "*\tdecodes.hdb.algo.FLGUUnreg\n*\tdecodes.cwms.rating.CwmsRatingSingleIndep\n"
					+ "*\tdecodes.cwms.rating.CwmsRatingMultIndep\n*\tdecodes.cwms.validation.ScreeningAlgorithm\n"
					+ "*\tdecodes.util.DecodesSettings\n*\tlrgs.lrgsmain.LrgsConfig\n"
					+ "*\topendcs.opentsdb.OpenTsdbSettings",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success. Property specifications retrieved.",
						content = @Content(mediaType = MediaType.APPLICATION_JSON,
							array = @ArraySchema(schema = @Schema(implementation = ApiPropSpec.class)))),
					@ApiResponse(responseCode = "400", description = "Missing or invalid parameter."),
					@ApiResponse(responseCode = "409", description = "Unable to retrieve property spec for class."),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Retrieving Property Specs"}
	)
	public Response getPropSpecs(@Parameter(description = "Fully Qualified Class Name", required = true,
			example = "decodes.db.Platform", schema = @Schema(implementation = String.class))
		@QueryParam("class") String className)
			throws WebAppException
	{
		if (className == null)
		{
			throw new MissingParameterException("Missing required class argument.");
		}

		return Response.status(HttpServletResponse.SC_OK).entity(PropSpecHelper.getPropSpecs(className)).build();
	}

	@POST
	@Path("decode")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Decode a Message",
			description = "Example URL for HTTP POST method:  \n"
					+ "```http://localhost:8080/odcsapi/decode?script=ST```\n\n"
					+ "Arguments required:\n* The script name to use in decoding the message "
					+ "(if omitted, the first script in the config will be used).\n\n"
					+ "The POST body must be in the following structure:\n```\n{\n\t\"config\": "
					+ "{ config as returned by GET config described above },\n"
					+ "\t\"rawmsg\": { Raw Message as returned by GET message described above }\n}\n```\n"
					+ "The raw data will be decoded according to the instructions in the passed config.\n"
					+ "The returned data will include log messages generated to trace the script execution, "
					+ "and the decoded data from the message.\n"
					+ "Note that for each decoded value, the position within the raw message is given.",
			requestBody = @RequestBody(required = true, description = "Decodes Request",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = DecodeRequest.class),
					examples = {
							@ExampleObject(name = "Basic", value = ResourceExamples.DecodeExamples.BASIC),
							@ExampleObject(name = "New", value = ResourceExamples.DecodeExamples.VERBOSE)
					})),
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiDecodedMessage.class))),
					@ApiResponse(responseCode = "400", description = "Missing or invalid script name."),
					@ApiResponse(responseCode = "500", description = "Error executing decoding script.")
			},
			tags = {"REST - Test Decoding"}
	)
	public Response postDecode(@Parameter(name = "script", description = "The script name to use in decoding.",
			example = "ST", schema = @Schema(type = "string"))
		@QueryParam("script") String scriptName,
			DecodeRequest request) throws WebAppException, DbException
	{
		if (scriptName == null)
		{
			throw new MissingParameterException("Missing required script argument.");
		}
		OpenDcsDatabase db = createDb();

		return Response.status(HttpServletResponse.SC_OK).entity(TestDecoder.decodeMessage(request.getRawmsg(),
				request.getConfig(), scriptName, db)).build();
	}
}