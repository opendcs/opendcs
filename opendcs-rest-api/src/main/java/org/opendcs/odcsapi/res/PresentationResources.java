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
import java.util.List;
import java.util.Vector;
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

import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.PresentationGroup;
import decodes.db.PresentationGroupList;
import decodes.db.RoutingSpec;
import decodes.db.ValueNotFoundException;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import opendcs.dai.DataTypeDAI;
import org.opendcs.odcsapi.beans.ApiPresentationElement;
import org.opendcs.odcsapi.beans.ApiPresentationGroup;
import org.opendcs.odcsapi.beans.ApiPresentationRef;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

@Path("/")
public final class PresentationResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("presentationrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
 	public Response getPresentationRefs() throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			PresentationGroupList groupList = new PresentationGroupList();
			dbIo.readPresentationGroupList(groupList);
			return Response.status(HttpServletResponse.SC_OK).entity(map(groupList)).build();
		}
		catch (DatabaseException e)
		{
			throw new DbException("Unable to retrieve presentation groups", e);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ArrayList<ApiPresentationRef> map(PresentationGroupList groupList)
	{
		ArrayList<ApiPresentationRef> ret = new ArrayList<>();
		for (PresentationGroup group : groupList.getVector())
		{
			ApiPresentationRef presRef = new ApiPresentationRef();
			if (group.getId() != null)
			{
				presRef.setGroupId(group.getId().getValue());
			}
			else
			{
				presRef.setGroupId(DbKey.NullKey.getValue());
			}
			presRef.setName(group.groupName);
			presRef.setInheritsFrom(group.inheritsFrom);
			presRef.setProduction(group.isProduction);
			if (group.inheritsFrom != null && !group.inheritsFrom.isEmpty())
			{
				for (PresentationGroup pg : groupList.getVector())
				{
					if (pg.groupName.equalsIgnoreCase(group.inheritsFrom))
					{
						presRef.setInheritsFromId(pg.getId().getValue());
						break;
					}
				}
			}
			else
			{
				presRef.setInheritsFromId(DbKey.NullKey.getValue());
			}
			presRef.setLastModified(group.lastModifyTime);
			ret.add(presRef);
		}
		return ret;
	}

	@GET
	@Path("presentation")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getPresentation(@QueryParam("groupid") Long groupId)
			throws WebAppException, DbException
	{
		if (groupId == null)
		{
			throw new MissingParameterException("Missing required groupid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			PresentationGroup group = new PresentationGroup();
			group.setId(DbKey.createDbKey(groupId));
			dbIo.readPresentationGroup(group);
			return Response.status(HttpServletResponse.SC_OK).entity(map(group)).build();
		}
		catch (ValueNotFoundException e)
		{
			throw new DatabaseItemNotFoundException(String.format("Presentation group with ID %s not found", groupId), e);
		}
		catch (DatabaseException e)
		{
			if (e.getCause() instanceof ValueNotFoundException)
			{
				throw new DatabaseItemNotFoundException(String.format("Presentation group with ID %s not found", groupId), e);
			}
			throw new DbException(String.format("Unable to retrieve presentation group with ID: %s", groupId), e);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ApiPresentationGroup map(PresentationGroup group)
	{
		ApiPresentationGroup presGrp = new ApiPresentationGroup();
		presGrp.setLastModified(group.lastModifyTime);
		presGrp.setName(group.groupName);
		presGrp.setProduction(group.isProduction);
		if (group.parent != null && group.parent.groupName != null && !group.parent.groupName.isEmpty())
		{
			presGrp.setInheritsFrom(group.parent.groupName);
			presGrp.setInheritsFromId(group.parent.getId().getValue());
		}
		if (group.getId() != null)
		{
			presGrp.setGroupId(group.getId().getValue());
		}
		else
		{
			presGrp.setGroupId(DbKey.NullKey.getValue());
		}
		presGrp.setElements(map(group.dataPresentations));
		return presGrp;
	}

	static List<ApiPresentationElement> map(List<DataPresentation> dataPresentations)
	{
		List<ApiPresentationElement> ret = new ArrayList<>();
		for(DataPresentation dp : dataPresentations)
		{
			ApiPresentationElement ape = new ApiPresentationElement();
			ape.setDataTypeCode(dp.getDataType().getCode());
			ape.setDataTypeStd(dp.getDataType().getStandard());
			ape.setFractionalDigits(dp.getMaxDecimals());
			ape.setMax(dp.getMaxValue());
			ape.setMin(dp.getMinValue());
			ape.setUnits(dp.getUnitsAbbr());
			ret.add(ape);
		}
		return ret;
	}

	@POST
	@Path("presentation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postPresentation(ApiPresentationGroup presGrp) throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try (DataTypeDAI dai = getLegacyTimeseriesDB().makeDataTypeDAO())
		{
			PresentationGroup group = map(dai, presGrp);
			dbIo.writePresentationGroup(group);
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(group))
					.build();
		}
		catch (DatabaseException e)
		{
			throw new DbException("Unable to store presentation group", e);
		}
		finally
		{
			dbIo.close();
		}
	}

	static PresentationGroup map(DataTypeDAI dai, ApiPresentationGroup presGrp) throws DatabaseException
	{
		// The inheritsFromId is not present in the target data object, so it is not mapped
		// The inheritsFromId is found internally using the inheritsFrom group name
		PresentationGroup group = new PresentationGroup();
		group.lastModifyTime = presGrp.getLastModified();
		group.groupName = presGrp.getName();
		if (presGrp.getGroupId() != null)
		{
			group.setId(DbKey.createDbKey(presGrp.getGroupId()));
		}
		else
		{
			group.setId(DbKey.NullKey);
		}
		group.isProduction = presGrp.isProduction();
		group.inheritsFrom = presGrp.getInheritsFrom();
		if (presGrp.getInheritsFromId() != null)
		{
			PresentationGroup parentGroup = new PresentationGroup();
			parentGroup.groupName = presGrp.getInheritsFrom();
			parentGroup.setId(DbKey.createDbKey(presGrp.getInheritsFromId()));
			group.parent = parentGroup;
		}
		group.dataPresentations = map(dai, presGrp.getElements(), group);

		return group;
	}

	static Vector<DataPresentation> map(DataTypeDAI dai, List<ApiPresentationElement> elements, PresentationGroup group)
			throws DatabaseException
	{
		Vector<DataPresentation> ret = new Vector<>();

		for (ApiPresentationElement ape : elements)
		{
			DataPresentation dataPres = new DataPresentation();
			dataPres.setUnitsAbbr(ape.getUnits());
			DataType dt = new DataType(ape.getDataTypeStd(), ape.getDataTypeCode());

			// Perform a lookup to see if the data type exists in the database
			// If it does, use the existing data type, otherwise create a new one by setting the ID to null
			if (dai != null)
			{
				try
				{
					DataType retDt = dai.lookupDataType(ape.getDataTypeCode());
					if(retDt != null)
					{
						dt = retDt;
					}
				}
				catch (DbIoException | NoSuchObjectException e)
				{
					dt.setId(DbKey.NullKey);
				}
			}
			else
			{
				dt.setId(DbKey.NullKey);
			}
			dataPres.setDataType(dt);
			dataPres.setMaxDecimals(ape.getFractionalDigits());
			dataPres.setMinValue(ape.getMin());
			dataPres.setMaxValue(ape.getMax());
			dataPres.setGroup(group);
			ret.add(dataPres);
		}
		return ret;
	}

	@DELETE
	@Path("presentation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deletePresentation(@QueryParam("groupid") Long groupId) throws DbException, WebAppException
	{
		if (groupId == null)
		{
			throw new MissingParameterException("Missing required groupid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			PresentationGroup group = new PresentationGroup();
			group.setId(DbKey.createDbKey(groupId));

			List<RoutingSpec> routeList = dbIo.routeSpecsUsing(groupId);
			if (!routeList.isEmpty())
			{
				StringBuilder sb = new StringBuilder();
				for (RoutingSpec rs : routeList)
				{
					sb.append(String.format("%s:%s, ", rs.getId(), rs.getName()));
				}
				String routeSpecs = sb.toString();
				if (routeSpecs.endsWith(", "))
				{
					routeSpecs = routeSpecs.substring(0, routeSpecs.length() - 2);
				}
				return Response.status(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
						.entity(String.format("Cannot delete presentation group %s " +
								"because it is used by the following routing specs: %s", groupId, routeSpecs)).build();
			}
			dbIo.deletePresentationGroup(group);
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("Presentation Group with ID " + groupId + " deleted")
					.build();
		}
		catch (DatabaseException e)
		{
			throw new DbException("Unable to delete presentation group", e);
		}
		finally
		{
			dbIo.close();
		}
	}
}