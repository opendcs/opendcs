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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import decodes.db.Constants;
import decodes.db.DataTypeSet;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.EngineeringUnit;
import decodes.db.EngineeringUnitList;
import decodes.db.LinearConverter;
import decodes.db.NullConverter;
import decodes.db.Poly5Converter;
import decodes.db.UnitConverter;
import decodes.db.UnitConverterDb;
import decodes.db.UnitConverterSet;
import decodes.db.UsgsStdConverter;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dai.DataTypeDAI;
import org.opendcs.odcsapi.beans.ApiDataType;
import org.opendcs.odcsapi.beans.ApiUnit;
import org.opendcs.odcsapi.beans.ApiUnitConverter;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

/**
 * HTTP Resources relating to DataTypes, Engineering Units, and Conversions
 * @author mmaloney
 *
 */
@Path("/")
public final class DatatypeUnitResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("datatypelist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Retrieve Data Type List",
			description = "Examples:  \n\n    http://localhost:8080/odcsapi/datatypelist  \n\n    " 
					+ "http://localhost:8080/odcsapi/datatypelist?standard=cwms  \n\n\n" 
					+ "The returned data structure is an array of JSON data type objects:\n" 
					+ "```\n[\n  {\n    \"code\": \"72114\",\n    \"displayName\": \"CWMS:72114\",\n    "
					+ "\"id\": 367,\n    \"standard\": \"CWMS\"\n  },\n  {\n    \"code\": \"Address\",\n    " 
					+ "\"displayName\": \"CWMS:Address\",\n    \"id\": 368,\n    \"standard\": \"CWMS\"\n  }," 
					+ "\n  {\n    \"code\": \"Code-Channel\",\n    \"displayName\": \"CWMS:Code-Channel\",\n    "
					+ "\"id\": 382,\n    \"standard\": \"CWMS\"\n  },\n  {\n    \"code\": \"Code-DCPAddress\",\n    " 
					+ "\"displayName\": \"CWMS:Code-DCPAddress\",\n    \"id\": 372,\n    \"standard\": \"CWMS\"\n  }," 
					+ "\n  {\n    \"code\": \"Depth-Snow\",\n    \"id\": 72,\n    \"standard\": \"CWMS\"\n  }," 
					+ "\n…\n]\n```\n\nIf the optional ‘standard’ argument is supplied, then only data types with " 
					+ "the matching standard are returned. Otherwise all data types in the database are " 
					+ "returned sorted by (standard, code).",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved data type list.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiDataType.class)))),
					@ApiResponse(responseCode = "500", description = "Failed to retrieve data type list.")
			},
			tags = {"REST - Data Type Methods"}
	)
	public Response getDataTypeList(@Parameter(schema = @Schema(implementation = String.class), example = "cwms") 
		@QueryParam("standard") String std) 
			throws DbException
	{
		try (DataTypeDAI dai = getLegacyTimeseriesDB().makeDataTypeDAO())
		{
			DataTypeSet set = new DataTypeSet();
			dai.readDataTypeSet(set, std);
			return Response.status(HttpServletResponse.SC_OK).entity(map(set)).build();
		}
		catch(DbIoException ex)
		{
			throw new DbException("Unable to retrieve data type list", ex);
		}
	}

	static ArrayList<ApiDataType> map(DataTypeSet set)
	{
		ArrayList<ApiDataType> ret = new ArrayList<>();
		Iterator<decodes.db.DataType> it = set.iterator();
		while(it.hasNext())
		{
			decodes.db.DataType dt = it.next();
			ApiDataType adt = new ApiDataType();
			if (dt.getId() != null)
			{
				adt.setId(dt.getId().getValue());
			}
			else
			{
				adt.setId(DbKey.NullKey.getValue());
			}
			adt.setCode(dt.getCode());
			adt.setStandard(dt.getStandard());
			adt.setDisplayName(dt.getDisplayName());
			ret.add(adt);
		}
		return ret;
	}


	@GET
	@Path("unitlist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "REST - Engineering Unit Methods")
	@Operation(
			summary = "Returns an array of data structures representing all known Engineering Units",
			description = "Example: \n\n    http://localhost:8080/odcsapi/unitlist  \n  \n  "
					+ "An array of data structures representing all known Engineering Units will be returned as shown below.  "
					+ "\n  \n  ```\n    [\n      {\n        \"abbr\": \"$\",\n        \"family\": \"univ\",\n        "
					+ "\"measures\": \"Currency\",\n        \"name\": \"Dollars\"\n      },\n      {\n        "
					+ "\"abbr\": \"%\",\n        \"family\": \"univ\",\n        \"measures\": \"ratio\",\n        "
					+ "\"name\": \"percent\"\n      },\n      {\n        \"abbr\": \"1000 m2\",\n        "
					+ "\"family\": \"Metric\",\n        \"measures\": \"Area\",\n        "
					+ "\"name\": \"Thousands of square meters\"\n      },\n      {\n        "
					+ "\"abbr\": \"1000 m3\",\n        \"family\": \"Metric\",\n        "
					+ "\"measures\": \"Volume\",\n        \"name\": \"Thousands of cubic meters\"\n      },\n      "
					+ "{\n        \"abbr\": \"C\",\n        \"family\": \"Metric\",\n        "
					+ "\"measures\": \"Temperature\",\n        \"name\": \"Centigrade\"\n      },\n    …\n    ]\n\n  ```",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiUnit.class)))),
					@ApiResponse(responseCode = "500", description = "Internal server error")
			},
			tags = {"REST - Engineering Unit Methods"}
	)
	public Response getUnitList() throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			EngineeringUnitList euList = new EngineeringUnitList();
			dbIo.readEngineeringUnitList(euList);
			return Response.status(HttpServletResponse.SC_OK).entity(map(euList)).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to retrieve data type list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ArrayList<ApiUnit> map(EngineeringUnitList unitList)
	{
		ArrayList<ApiUnit> ret = new ArrayList<>();
		Iterator<EngineeringUnit> it = unitList.iterator();
		while(it.hasNext())
		{
			EngineeringUnit eu = it.next();
			ApiUnit apiUnit = new ApiUnit();
			apiUnit.setAbbr(eu.abbr);
			apiUnit.setName(eu.getName());
			apiUnit.setMeasures(eu.measures);
			apiUnit.setFamily(eu.family);
			ret.add(apiUnit);
		}
		return ret;

	}

	@POST
	@Path("eu")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "REST - Engineering Unit Methods")
	@Operation(
			summary = "Create a new, or update an existing Engineering Unit",
			description = "Example URL for POST:  \n\n    http://localhost:8080/odcsapi/eu\n\n\n"
					+ "The POST data should contain a single engineering unit as described above for unitlist.\n  "
					+ "For example, to create a new unit with abbreviation 'blob', the data could be:  \n  ```\n  {\n    "
					+ "\"abbr\": \"blob\",\n    \"family\": \"Metric\",\n    \"measures\": \"stuff\",\n    "
					+ "\"name\": \"A Blob of Stuff\"\n  }\n  ```",

			requestBody = @RequestBody(description = "Engineering unit",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiUnit.class))),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully stored the engineering unit.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiUnit.class))),
					@ApiResponse(responseCode = "500", description = "Failed to store the engineering unit.")
			},
			tags = {"REST - Engineering Unit Methods"}
	)
	public Response postEU(@Parameter(description = "The abbreviation of the engineering unit to replace, if updating.")
		@QueryParam("fromabbr") String fromabbr,
			@Parameter(description = "Engineering unit details.") ApiUnit eu)
		throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			EngineeringUnit unit = new EngineeringUnit(eu.getAbbr(), eu.getName(), eu.getFamily(), eu.getMeasures());
			EngineeringUnitList euList = new EngineeringUnitList();
			dbIo.readEngineeringUnitList(euList);
			if(fromabbr != null && !fromabbr.isEmpty())
			{
				EngineeringUnit engineeringUnit = euList.get(fromabbr);
				if(engineeringUnit != null)
				{
					euList.remove(engineeringUnit);
				}
			}
			euList.add(unit);
			dbIo.writeEngineeringUnitList(euList);
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(euList)).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to store Engineering Unit list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	@DELETE
	@Path("eu")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "REST - Engineering Unit Methods")
	@Operation(
			summary = "Delete an existing Engineering Unit",
			description = "Deletes an engineering unit record.\n\nExample URL:\n\n"
					+ "`http://localhost:8080/odcsapi/eu?abbr=blob`\n",
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted the engineering unit."),
					@ApiResponse(responseCode = "400", description = "Missing required abbreviation parameter."),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response deleteEU(@Parameter(description = "Engineering unit abbreviation", required = true,
			example = "blob", schema = @Schema(implementation = String.class))
		@QueryParam("abbr") String abbr)
			throws DbException, WebAppException
	{
		if(abbr == null)
		{
			throw new MissingParameterException("Missing required abbr parameter");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			EngineeringUnit unit = new EngineeringUnit(abbr, "", "", "");
			dbIo.deleteEngineeringUnit(unit);
			return Response.status(HttpServletResponse.SC_NO_CONTENT).entity("EU with abbr " + abbr + " deleted").build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to store Engineering Unit list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	@GET
	@Path("euconvlist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "REST - Engineering Unit Methods")
	@Operation(
			summary = "Returns a list of Engineering Unit Conversions defined in the database",
			description = "Returns a list of Engineering Unit Conversions defined in the database.  \n"
					+ "Example:  \n\n    http://localhost:8080/odcsapi/unitlist  \n\n"
					+ "An array of data structures representing all known conversions will be returned as shown below.  "
					+ "\n\n```\n  [\n    {\n      \"ucId\": 3689,\n      \"fromAbbr\": \"m^3/s\",\n      "
					+ "\"toAbbr\": \"cms\",\n      \"algorithm\": \"none\",\n      \"a\": 0,\n      \"b\": 0,\n      "
					+ "\"c\": 0,\n      \"d\": 0,\n      \"e\": 0,\n      \"f\": 0\n    },\n    {\n      "
					+ "\"ucId\": 3690,\n      \"fromAbbr\": \"ft\",\n      \"toAbbr\": \"in\",\n      "
					+ "\"algorithm\": \"linear\",\n      \"a\": 12,\n      \"b\": 0,\n      \"c\": 0,\n      "
					+ "\"d\": 0,\n      \"e\": 0,\n      \"f\": 0\n    },\n  . . .\n  ]\n```",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiUnitConverter.class)))),
					@ApiResponse(responseCode = "500", description = "Internal server error")
			},
			tags = {"REST - Engineering Unit Methods"}
	)
	public Response getUnitConvList() throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			UnitConverterSet unitConverterSet = new UnitConverterSet();
			dbIo.readUnitConverterSet(unitConverterSet);
			return Response.status(HttpServletResponse.SC_OK).entity(map(unitConverterSet)).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to retrieve Unit Converter list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static List<ApiUnitConverter> map(UnitConverterSet unitSet)
	{
		List<ApiUnitConverter> ret = new ArrayList<>();
		Iterator<UnitConverterDb> it = unitSet.iteratorDb();
		while (it.hasNext())
		{
			UnitConverterDb unitConv = it.next();
			ApiUnitConverter euc = map(unitConv);
			ret.add(euc);
		}
		return ret;
	}

	@POST
	@Path("euconv")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create a new, or update an existing Engineering Unit Conversion",
			description = "Example URL for POST:  \n\n    "
					+ "http://localhost:8080/odcsapi/euconv  \n\n\n"
					+ "The POST data should contain a single engineering unit conversion as described"
					+ " above for euconvlist.  \n\nFor example, to create a new conversion that declares "
					+ "'cms' to be a synonym of 'blob', the data could be:\n```\n  {\n    "
					+ "\"fromAbbr\": \"cms\",\n    \"toAbbr\": \"blob\",\n    \"algorithm\": \"none\",\n    "
					+ "\"a\": 0,\n    \"b\": 0,\n    \"c\": 0,\n    \"d\": 0,\n    \"e\": 0,\n    \"f\": 0\n  }\n  "
					+ "```\n\n**Note**: the 'none' algorithm means that no conversion is required and the "
					+ "coefficients A-F are ignored. It essentially means that the two units are synonyms.  \n\n"
					+ "**Note**: that we left off the 'ucId' member since we were creating a new conversion. "
					+ "To update an existing one, include 'ucId'.  \n\nThe returned data structure will be "
					+ "the same as the data passed, except that if this is a new conversion the "
					+ "ucId member will be added.",
			requestBody = @RequestBody(description = "Engineering unit conversion",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiUnitConverter.class))),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully added the unit converter.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiUnitConverter.class,
											description = "The returned data structure will be the same "
													+ "as the data passed, except that if this is a new conversion "
													+ "the ucId member will be added."))),
					@ApiResponse(responseCode = "500", description = "Internal server error")
			},
			tags = {"REST - Engineering Unit Methods"}
	)
	public Response postEUConv(ApiUnitConverter euc) throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			UnitConverterDb unitConverterDb = ucDbMap(euc);
			Long ucId = euc.getUcId();
			if(ucId != null)
			{
				dbIo.deleteUnitConverter(ucId);
				unitConverterDb.forceSetId(DbKey.NullKey);
			}
			dbIo.insertUnitConverter(unitConverterDb);
			return Response.status(HttpServletResponse.SC_CREATED).entity(map(unitConverterDb)).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to store Unit Converter list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ApiUnitConverter map(UnitConverterDb dbConv)
	{
		ApiUnitConverter euc = new ApiUnitConverter();
		if (dbConv.getId() != null)
		{
			euc.setUcId(dbConv.getId().getValue());
		}
		else
		{
			euc.setUcId(DbKey.NullKey.getValue());
		}
		euc.setFromAbbr(dbConv.fromAbbr);
		euc.setToAbbr(dbConv.toAbbr);
		euc.setAlgorithm(dbConv.algorithm);
		euc.setA(dbConv.coefficients[0] == 0.0 ? null : dbConv.coefficients[0]);
		euc.setB(dbConv.coefficients[1] == 0.0 ? null : dbConv.coefficients[1]);
		euc.setC(dbConv.coefficients[2] == 0.0 ? null : dbConv.coefficients[2]);
		euc.setD(dbConv.coefficients[3] == 0.0 ? null : dbConv.coefficients[3]);
		euc.setE(dbConv.coefficients[4] == 0.0 ? null : dbConv.coefficients[4]);
		euc.setF(dbConv.coefficients[5] == 0.0 ? null : dbConv.coefficients[5]);
		return euc;
	}

	static UnitConverterDb ucDbMap(ApiUnitConverter euc) throws DatabaseException, DbException
	{
		UnitConverterDb unitConverterDb = new UnitConverterDb(euc.getFromAbbr(), euc.getToAbbr());
		if (euc.getUcId() != null)
		{
			unitConverterDb.setId(DbKey.createDbKey(euc.getUcId()));
		}
		else
		{
			unitConverterDb.setId(DbKey.NullKey);
		}

		unitConverterDb.algorithm = euc.getAlgorithm();
		unitConverterDb.coefficients[0] = euc.getA() == null ? 0.0 : euc.getA();
		unitConverterDb.coefficients[1] = euc.getB() == null ? 0.0 : euc.getB();
		unitConverterDb.coefficients[2] = euc.getC() == null ? 0.0 : euc.getC();
		unitConverterDb.coefficients[3] = euc.getD() == null ? 0.0 : euc.getD();
		unitConverterDb.coefficients[4] = euc.getE() == null ? 0.0 : euc.getE();
		unitConverterDb.coefficients[5] = euc.getF() == null ? 0.0 : euc.getF();
		unitConverterDb.execConverter = ucMap(euc);
		return unitConverterDb;
	}

	static UnitConverter ucMap(ApiUnitConverter euc) throws DbException
	{
		UnitConverter unitConverter;
		EngineeringUnit fromEU = new EngineeringUnit(euc.getFromAbbr(), "", "", "");
		EngineeringUnit toEU = new EngineeringUnit(euc.getToAbbr(), "", "", "");
		if (euc.getAlgorithm().equalsIgnoreCase(Constants.eucvt_poly5))
		{
			unitConverter = new Poly5Converter(fromEU, toEU);
		}
		else if (euc.getAlgorithm().equalsIgnoreCase(Constants.eucvt_linear))
		{
			unitConverter = new LinearConverter(fromEU, toEU);
		}
		else if (euc.getAlgorithm().equalsIgnoreCase(Constants.eucvt_none))
		{
			unitConverter = new NullConverter(fromEU, toEU);
		}
		else if (euc.getAlgorithm().equalsIgnoreCase(Constants.eucvt_usgsstd))
		{
			unitConverter = new UsgsStdConverter(fromEU, toEU);
		}
		else
		{
			throw new DbException("Unknown algorithm: " + euc.getAlgorithm());
		}
		double[] coeffs = new double[6];
		coeffs[0] = euc.getA() == null ? 0.0 : euc.getA();
		coeffs[1] = euc.getB() == null ? 0.0 : euc.getB();
		coeffs[2] = euc.getC() == null ? 0.0 : euc.getC();
		coeffs[3] = euc.getD() == null ? 0.0 : euc.getD();
		coeffs[4] = euc.getE() == null ? 0.0 : euc.getE();
		coeffs[5] = euc.getF() == null ? 0.0 : euc.getF();
		unitConverter.setCoefficients(coeffs);
		return unitConverter;
	}

	@DELETE
	@Path("euconv")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "REST - Engineering Unit Methods")
	@Operation(
			summary = "Delete an existing Engineering Unit conversion record",
			description = "Example URL for DELETE:\n\n    "
					+ "http://localhost:8080/odcsapi/euconv\n\n\n"
					+ "This deletes the EU Conversion record with ID 1459.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted the unit converter."),
					@ApiResponse(responseCode = "400", description = "Missing required ID parameter."),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Engineering Unit Methods"}
	)
	public Response deleteEUConv(@Parameter(description = "EU Conversion Id", required = true,
			example = "1459", schema = @Schema(implementation = Long.class))
		@QueryParam("euconvid") Long id)
			throws DbException, WebAppException
	{
		if (id == null)
		{
			throw new MissingParameterException("Missing required euconvid parameter");
		}
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			dbIo.deleteUnitConverter(id);
			return Response.status(HttpServletResponse.SC_NO_CONTENT).entity("EUConv with id=" + id + " deleted").build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to delete Unit Converter", ex);
		}
		finally
		{
			dbIo.close();
		}
	}
}
