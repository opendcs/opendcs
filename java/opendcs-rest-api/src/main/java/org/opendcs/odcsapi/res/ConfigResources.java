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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.DataTypeSet;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.DecodesScript;
import decodes.db.DecodesScriptException;
import decodes.db.EngineeringUnit;
import decodes.db.FormatStatement;
import decodes.db.LinearConverter;
import decodes.db.NullConverter;
import decodes.db.PlatformConfig;
import decodes.db.PlatformConfigList;
import decodes.db.Poly5Converter;
import decodes.db.ScriptSensor;
import decodes.db.UnitConverter;
import decodes.db.UnitConverterDb;
import decodes.db.UsgsStdConverter;
import decodes.db.ValueNotFoundException;
import decodes.sql.DbKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opendcs.odcsapi.beans.ApiConfigRef;
import org.opendcs.odcsapi.beans.ApiConfigScript;
import org.opendcs.odcsapi.beans.ApiConfigScriptSensor;
import org.opendcs.odcsapi.beans.ApiConfigSensor;
import org.opendcs.odcsapi.beans.ApiPlatformConfig;
import org.opendcs.odcsapi.beans.ApiScriptFormatStatement;
import org.opendcs.odcsapi.beans.ApiUnitConverter;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

@Path("/")
public final class ConfigResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("configrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(
			summary = "This method returns a JSON list of DECODES Config records suitable for displaying in a table or pick-list",
			description = "Example:\n\n    http://localhost:8080/odcsapi/configrefs\n\n\n"
					+ "This method returns a JSON list of DECODES Config records suitable for displaying "
					+ "in a table or pick-list. The returned structure contains an array in the following format:\n\n  "
					+ "**Note**:The numeric ID may be used in subsequent calls to the 'config' method.\n\n```\n[\n  "
					+ "{\n    \"configId\": 1,\n    \"description\": \"WSC SHEF - 2 sensors - HG, VB\",\n    "
					+ "\"name\": \"Shef-WSC-Hydro-RCOYCHER\",\n    \"numPlatforms\": 1\n  },\n  {\n    "
					+ "\"configId\": 2,\n    \"description\": \"WSC SHEF - 2 sensors - QR=QF, HG\",\n    "
					+ "\"name\": \"Shef-WSC-Hydro-RBRDDDVH\",\n    \"numPlatforms\": 1\n  },\n  {\n    "
					+ "\"configId\": 3,\n    \"description\": \"WSC SHEF - 2 sensors - HG, VB\",\n    "
					+ "\"name\": \"Shef-WSC-Hydro-RBLOCLEE\",\n    \"numPlatforms\": 1\n  },\n  {\n    "
					+ "\"configId\": 4,\n    \"description\": \"AE SHEF - 4 sensors - PC, TA, SW, YB=VB\",\n    "
					+ "\"name\": \"Shef-AE-Met-SESK\",\n    \"numPlatforms\": 1\n  },\n  {\n    \"configId\": 5,\n    "
					+ "\"description\": \"WSC SHEF - 2 sensors - HG, VB\",\n    "
					+ "\"name\": \"Shef-WSC-Hydro-RBULLRES\",\n    \"numPlatforms\": 1\n  },\n  {\n    "
					+ "\"configId\": 6,\n    \"description\": \"WSC SHEF - 2 sensors - HG, VB\",\n    "
					+ "\"name\": \"Shef-WSC-Hydro-RREDBIN\",\n    \"numPlatforms\": 1\n  }\n]\n```",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
							array = @ArraySchema(schema = @Schema(implementation = ApiConfigRef.class)))),
					@ApiResponse(responseCode = "500",
							description = "Database error occurred while retrieving the configuration references")
			},
			tags = {"REST - DECODES Platform Configurations"}
	)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	public Response getConfigRefs() throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			PlatformConfigList configList = new PlatformConfigList();
			dbIo.readConfigList(configList);
			return Response.ok().entity(map(configList)).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Error reading config list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static List<ApiConfigRef> map(PlatformConfigList configList)
	{
		List<ApiConfigRef> configRefs = new ArrayList<>();
		for (PlatformConfig config : configList.values())
		{
			ApiConfigRef configRef = new ApiConfigRef();
			if (config.getId() != null)
			{
				configRef.setConfigId(config.getId().getValue());
			}
			else
			{
				configRef.setConfigId(DbKey.NullKey.getValue());
			}
			configRef.setName(config.getName());
			configRef.setNumPlatforms(config.numPlatformsUsing);
			configRef.setDescription(config.description);
			configRefs.add(configRef);
		}
		return configRefs;
	}

	@GET
	@Path("config")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "This method returns a JSON representation of a single, complete DECODES Config record",
			description = "Example:  \n\n    http://localhost:8080/odcsapi/config?configid=12\n\n\n"
					+ "This method returns a JSON representation of a single, complete DECODES Config record. ",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved configuration details",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiPlatformConfig.class))),
					@ApiResponse(responseCode = "400", description = "Missing or invalid configid parameter"),
					@ApiResponse(responseCode = "404", description = "Configuration not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - DECODES Platform Configurations"}
	)
	public Response getConfig(@Parameter(schema = @Schema(implementation = Long.class, example = "12"), required = true)
		@QueryParam("configid") Long configId) throws WebAppException, DbException
	{
		if (configId == null)
		{
			throw new MissingParameterException("Missing required configid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			PlatformConfig config = new PlatformConfig();
			config.setId(DbKey.createDbKey(configId));
			dbIo.readConfig(config);
			return Response.ok().entity(map(config)).build();
		}
		catch (ValueNotFoundException ex)
		{
			throw new DatabaseItemNotFoundException("Config with ID " + configId + " not found", ex);
		}
		catch (DatabaseException ex)
		{
			if (ex.getCause() instanceof ValueNotFoundException)
			{
				throw new DatabaseItemNotFoundException("Config with ID " + configId + " not found", ex);
			}
			throw new DbException("Error reading config", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ApiPlatformConfig map(PlatformConfig config)
	{
		ApiPlatformConfig apiConfig = new ApiPlatformConfig();
		if (config.getId() != null)
		{
			apiConfig.setConfigId(config.getId().getValue());
		}
		else
		{
			apiConfig.setConfigId(DbKey.NullKey.getValue());
		}
		apiConfig.setName(config.getName());
		apiConfig.setNumPlatforms(config.numPlatformsUsing);
		apiConfig.setDescription(config.description);
		List<ApiConfigSensor> sensors = new ArrayList<>();
		config.getSensors().forEachRemaining(sensor -> {
			ApiConfigSensor apiSensor = new ApiConfigSensor();
			apiSensor.setSensorNumber(sensor.sensorNumber);
			apiSensor.setSensorName(sensor.sensorName);
			apiSensor.setProperties(sensor.getProperties());
			apiSensor.setAbsoluteMax(sensor.absoluteMax == Constants.undefinedDouble ? null : sensor.absoluteMax);
			apiSensor.setAbsoluteMin(sensor.absoluteMin == Constants.undefinedDouble ? null : sensor.absoluteMin);
			apiSensor.setRecordingInterval(sensor.recordingInterval);
			apiSensor.setTimeOfFirstSample(sensor.timeOfFirstSample);
			apiSensor.setUsgsStatCode(sensor.getUsgsStatCode());
			apiSensor.setRecordingMode(ApiConfigSensor.RecordingMode.fromChar(sensor.recordingMode));
			Map<String, String> dataTypes = new HashMap<>();
			sensor.getDataTypes()
				.forEachRemaining(entry ->
						dataTypes.put(entry.getStandard(), entry.getCode()));
			apiSensor.setDataTypes(dataTypes);
			sensors.add(apiSensor);
		});
		apiConfig.setConfigSensors(sensors);
		List<ApiConfigScript> scripts = new ArrayList<>();
		config.getScripts().forEachRemaining(script -> {
			ApiConfigScript apiScript = new ApiConfigScript();
			apiScript.setName(script.scriptName);
			List<ApiConfigScriptSensor> scriptSensors = new ArrayList<>();
			script.scriptSensors.forEach(sensor -> {
				ApiConfigScriptSensor apiSensor = new ApiConfigScriptSensor();
				apiSensor.setSensorNumber(sensor.sensorNumber);
				ApiUnitConverter uc = new ApiUnitConverter();
				UnitConverterDb rawConverter = sensor.rawConverter;
				if(rawConverter != null)
				{
					uc.setFromAbbr(rawConverter.fromAbbr);
					uc.setToAbbr(rawConverter.toAbbr);
					uc.setAlgorithm(rawConverter.algorithm);
					uc.setA(rawConverter.coefficients[0]);
					uc.setB(rawConverter.coefficients[1]);
					uc.setC(rawConverter.coefficients[2]);
					uc.setD(rawConverter.coefficients[3]);
					uc.setE(rawConverter.coefficients[4]);
					uc.setF(rawConverter.coefficients[5]);
				}
				uc.setUcId(sensor.getUnitConverterId().getValue());
				apiSensor.setUnitConverter(uc);
				scriptSensors.add(apiSensor);
			});
			apiScript.setScriptSensors(scriptSensors);
			apiScript.setFormatStatements(map(script.getFormatStatements()));
			apiScript.setHeaderType(script.scriptType);
			switch(String.valueOf(script.getDataOrder()).toLowerCase())
			{
				case "d":
					apiScript.setDataOrder(ApiConfigScript.DataOrder.DESCENDING);
					break;
				case "a":
					apiScript.setDataOrder(ApiConfigScript.DataOrder.ASCENDING);
					break;
				default:
					apiScript.setDataOrder(ApiConfigScript.DataOrder.UNDEFINED);
					break;
			}
			scripts.add(apiScript);
		});
		apiConfig.setScripts(scripts);
		return apiConfig;
	}

	static List<ApiScriptFormatStatement> map(Vector<FormatStatement> formatStatements)
	{
		List<ApiScriptFormatStatement> apiFormatStatements = new ArrayList<>();
		for (FormatStatement fs : formatStatements)
		{
			ApiScriptFormatStatement apiFs = new ApiScriptFormatStatement();
			apiFs.setFormat(fs.format);
			apiFs.setLabel(fs.label);
			apiFs.setSequenceNum(fs.sequenceNum);
			apiFormatStatements.add(apiFs);
		}
		return apiFormatStatements;
	}

	@POST
	@Path("config")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create or Overwrite Existing Config",
			description = "The POST config method takes a single DECODES Platform Configuration record in JSON format, "
					+ "as described above for GET config.   \n\n"
					+ "For creating a new config, leave configId out of the passed data structure.  \n\n"
					+ "For overwriting an existing one, include the configId that was previously returned. "
					+ "The configuration in the database is replaced with the one sent.",
			requestBody = @RequestBody(
					description = "The configuration object to be created or updated",
					required = true,
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiPlatformConfig.class),
							examples = {
									@ExampleObject(name = "Basic", value = ResourceExamples.ConfigExamples.BASIC),
									@ExampleObject(name = "New", value = ResourceExamples.ConfigExamples.NEW),
									@ExampleObject(name = "Update", value = ResourceExamples.ConfigExamples.UPDATE)
							}
					)
			),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully created or updated the configuration",
							content = @Content(schema = @Schema(implementation = ApiPlatformConfig.class))),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"REST - DECODES Platform Configurations"}
	)
	public Response postConfig(ApiPlatformConfig config) throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			DataTypeSet dataTypeSet = new DataTypeSet();
			dbIo.readDataTypeSet(dataTypeSet);
			PlatformConfig pc = map(config, dataTypeSet);
			dbIo.writeConfig(pc);
			return Response.status(Response.Status.CREATED)
					.entity(map(pc))
					.build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException("Error saving config", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static PlatformConfig map(ApiPlatformConfig config, DataTypeSet dataTypeSet) throws DbException
	{
		try
		{
			PlatformConfig pc = new PlatformConfig(config.getName());
			if (config.getConfigId() != null)
			{
				pc.setId(DbKey.createDbKey(config.getConfigId()));
			}
			else
			{
				pc.setId(DbKey.NullKey);
			}
			pc.description = config.getDescription();
			pc.numPlatformsUsing = config.getNumPlatforms();

			pc.configName = config.getName();
			pc.decodesScripts = map(config.getScripts(), pc);
			for (ApiConfigSensor sensor : config.getConfigSensors())
			{
				ConfigSensor configSensor = new ConfigSensor(null, sensor.getSensorNumber());
				configSensor.sensorName = sensor.getSensorName();
				configSensor.platformConfig = pc;
				Double absoluteMax = sensor.getAbsoluteMax();
				if(absoluteMax != null)
				{
					configSensor.absoluteMax = absoluteMax;
				}
				Double absoluteMin = sensor.getAbsoluteMin();
				if(absoluteMin != null)
				{
					configSensor.absoluteMin = absoluteMin;
				}
				configSensor.recordingInterval = sensor.getRecordingInterval();
				configSensor.timeOfFirstSample = sensor.getTimeOfFirstSample();
				configSensor.recordingMode = sensor.getRecordingMode().getCode();
				configSensor.setUsgsStatCode(sensor.getUsgsStatCode());
				for (Map.Entry<String, String> entry : sensor.getDataTypes().entrySet())
				{
					DataType dt = dataTypeSet.get(entry.getKey(), entry.getValue());
					if(dt == null )
					{
						dt = new DataType(entry.getKey(), entry.getValue());
					}
					configSensor.addDataType(dt);
				}
				for (String name : sensor.getProperties().stringPropertyNames())
				{
					configSensor.setProperty(name, sensor.getProperties().getProperty(name));
				}
				pc.addSensor(configSensor);
			}

			return pc;
		}
		catch (DatabaseException ex)
		{
			throw new DbException("Error mapping platform config", ex);
		}
	}

	static Vector<DecodesScript> map(List<ApiConfigScript> scripts, PlatformConfig config) throws DbException
	{
		if (scripts == null)
		{
			return new Vector<>();
		}

		try
		{
			Vector<DecodesScript> decodesScripts = new Vector<>();
			for(ApiConfigScript script : scripts)
			{
				DecodesScript.DecodesScriptBuilder dsb = DecodesScript.empty();
				dsb.platformConfig(config);
				dsb.scriptName(script.getName());
				DecodesScript ds = dsb.build();
				String headerType = script.getHeaderType();
				if(headerType != null)
				{
					ds.scriptType = headerType;
				}
				for (ApiConfigScriptSensor sensor : script.getScriptSensors())
				{
					ds.addScriptSensor(map(sensor));
				}
				decodesScripts.add(ds);
			}
			return decodesScripts;
		}
		catch(DecodesScriptException | IOException | DatabaseException ex)
		{
			throw new DbException("Error mapping scripts", ex);
		}
	}

	static ScriptSensor map(ApiConfigScriptSensor sensor) throws DatabaseException
	{
		ScriptSensor scriptSensor = new ScriptSensor(null, sensor.getSensorNumber());
		scriptSensor.execConverter = map(sensor.getUnitConverter());
		UnitConverterDb rawConv = new UnitConverterDb(sensor.getUnitConverter().getFromAbbr(),
				sensor.getUnitConverter().getToAbbr());
		rawConv.algorithm = sensor.getUnitConverter().getAlgorithm();
		if (sensor.getUnitConverter().getUcId() != null)
		{
			rawConv.setId(DbKey.createDbKey(sensor.getUnitConverter().getUcId()));
		}
		else
		{
			rawConv.setId(DbKey.NullKey);
		}
		ApiUnitConverter uc = sensor.getUnitConverter();
		rawConv.coefficients = coefficientMap(uc);
		scriptSensor.rawConverter = rawConv;
		if (sensor.getUnitConverter().getUcId() != null)
		{
			scriptSensor.setUnitConverterId(DbKey.createDbKey(sensor.getUnitConverter().getUcId()));
		}
		else
		{
			scriptSensor.setUnitConverterId(DbKey.NullKey);
		}
		return scriptSensor;
	}

	static UnitConverter map(ApiUnitConverter unitConverter)
	{
		EngineeringUnit from = EngineeringUnit.getEngineeringUnit(unitConverter.getFromAbbr());
		EngineeringUnit to = EngineeringUnit.getEngineeringUnit(unitConverter.getToAbbr());
		double[] coeffs = coefficientMap(unitConverter);
		if (unitConverter.getAlgorithm().equalsIgnoreCase("None"))
		{
			NullConverter nc = new NullConverter(from, to);
			nc.setCoefficients(coeffs);
			return nc;
		}
		else if (unitConverter.getAlgorithm().equalsIgnoreCase(Constants.eucvt_poly5))
		{
			Poly5Converter pc = new Poly5Converter(from, to);
			pc.setCoefficients(coeffs);
			return pc;
		}

		else if (unitConverter.getAlgorithm().equalsIgnoreCase(Constants.eucvt_linear))
		{
			LinearConverter lc = new LinearConverter(from, to);
			lc.setCoefficients(coeffs);
			return lc;
		}
		else if (unitConverter.getAlgorithm().equalsIgnoreCase(Constants.eucvt_usgsstd))
		{
			UsgsStdConverter uc = new UsgsStdConverter(from, to);
			uc.setCoefficients(coeffs);
			return uc;
		}
		else
		{
			throw new IllegalArgumentException("Unsupported algorithm: " + unitConverter.getAlgorithm());
		}
	}

	static double[] coefficientMap(ApiUnitConverter unitConverter)
	{
		double[] coeffs = new double[6];
		coeffs[0] = unitConverter.getA() != null ? unitConverter.getA() : 0.0;
		coeffs[1] = unitConverter.getB() != null ? unitConverter.getB() : 0.0;
		coeffs[2] = unitConverter.getC() != null ? unitConverter.getC() : 0.0;
		coeffs[3] = unitConverter.getD() != null ? unitConverter.getD() : 0.0;
		coeffs[4] = unitConverter.getE() != null ? unitConverter.getE() : 0.0;
		coeffs[5] = unitConverter.getF() != null ? unitConverter.getF() : 0.0;
		return coeffs;
	}


	@DELETE
	@Path("config")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Existing Config",
			description = "Required argument configid must be passed.  \n\n"
					+ "Error 405 will be returned if the referenced configuration is used by one "
					+ "or more platforms and cannot be deleted.",
			parameters = {
					@Parameter(name = "configid", description = "The unique ID of the configuration to delete",
							required = true, schema = @Schema(type = "integer"))
			},
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted the configuration"),
					@ApiResponse(responseCode = "400", description = "Missing or invalid configid parameter"),
					@ApiResponse(responseCode = "405", description = "Configuration is in use and cannot be deleted"),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"REST - DECODES Platform Configurations"}
	)
	public Response deleteConfig(@Parameter(description = "The unique ID of the configuration to delete",
			required = true, schema = @Schema(implementation = Long.class))
		@QueryParam("configid") Long configId)
			throws DbException, WebAppException
	{
		if (configId == null)
		{
			throw new MissingParameterException("Missing required configid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			PlatformConfig pc = new PlatformConfig();
			pc.setId(DbKey.createDbKey(configId));
			dbIo.readConfig(pc);

			if (pc.numPlatformsUsing > 0)
			{
				return Response.status(Response.Status.METHOD_NOT_ALLOWED)
						.entity(" Cannot delete config with ID "
								+ configId + " because it is used by one or more platforms.")
						.build();
			}

			dbIo.deleteConfig(pc);
			return Response.noContent()
					.entity("Config with ID " + configId + " deleted")
					.build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException("Error deleting config", ex);
		}
		finally
		{
			dbIo.close();
		}
	}
}
