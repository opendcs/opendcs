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
public class DatatypeUnitResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("datatypelist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getDataTypeList(@QueryParam("standard") String std) throws DbException
	{
		try (DataTypeDAI dai = getLegacyTimeseriesDB().makeDataTypeDAO())
		{
			DataTypeSet set = new DataTypeSet();
			dai.readDataTypeSet(set, std);
			return Response.status(HttpServletResponse.SC_OK).entity(map(set)).build();
		}
		catch(DbIoException e)
		{
			throw new DbException("Unable to retrieve data type list", e);
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
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getUnitList() throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			EngineeringUnitList euList = new EngineeringUnitList();
			dbIo.readEngineeringUnitList(euList);
			return Response.status(HttpServletResponse.SC_OK).entity(map(euList)).build();
		}
		catch(DatabaseException e)
		{
			throw new DbException("Unable to retrieve data type list", e);
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postEU(@QueryParam("fromabbr") String fromabbr, ApiUnit eu)
			throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			EngineeringUnit unit = new EngineeringUnit(fromabbr, eu.getName(), eu.getFamily(), eu.getMeasures());
			EngineeringUnitList euList = new EngineeringUnitList();
			dbIo.readEngineeringUnitList(euList);
			euList.add(unit);
			dbIo.writeEngineeringUnitList(euList);
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(euList)).build();
		}
		catch(DatabaseException e)
		{
			throw new DbException("Unable to store Engineering Unit list", e);
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deleteEU(@QueryParam("abbr") String abbr) throws DbException, WebAppException
	{
		if (abbr == null)
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
		catch(DatabaseException e)
		{
			throw new DbException("Unable to store Engineering Unit list", e);
		}
		finally
		{
			dbIo.close();
		}
	}

	@GET
	@Path("euconvlist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getUnitConvList() throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			UnitConverterSet unitConverterSet = new UnitConverterSet();
			dbIo.readUnitConverterSet(unitConverterSet);
			return Response.status(HttpServletResponse.SC_OK).entity(map(unitConverterSet)).build();
		}
		catch(DatabaseException e)
		{
			throw new DbException("Unable to retrieve Unit Converter list", e);
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postEUConv(ApiUnitConverter euc) throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			UnitConverterDb unitConverterDb = ucDbMap(euc);
			dbIo.insertUnitConverter(unitConverterDb);
			return Response.status(HttpServletResponse.SC_CREATED).entity(map(unitConverterDb)).build();
		}
		catch(DatabaseException e)
		{
			throw new DbException("Unable to store Unit Converter list", e);
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deleteEUConv(@QueryParam("euconvid") Long id) throws DbException, WebAppException
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
		catch(DatabaseException e)
		{
			throw new DbException("Unable to delete Unit Converter", e);
		}
		finally
		{
			dbIo.close();
		}
	}
}
