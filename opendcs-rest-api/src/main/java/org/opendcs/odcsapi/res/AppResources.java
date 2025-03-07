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
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
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

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbCompLock;
import opendcs.dai.LoadingAppDAI;
import org.opendcs.odcsapi.beans.ApiAppRef;
import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

/**
 * Resources for editing, monitoring, stopping, and starting processes.
 */
@Path("/")
public final class AppResources extends OpenDcsResource
{
	private static final String NO_APP_FOUND = "No such app with ID: %s";

	@Context private HttpServletRequest request;
	@Context private HttpHeaders httpHeaders;

	@GET
	@Path("apprefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getAppRefs() throws DbException
	{
		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			List<ApiAppRef> ret = dai.listComputationApps(false)
					.stream()
					.map(AppResources::map)
					.collect(Collectors.toList());
			return Response.status(HttpServletResponse.SC_OK)
					.entity(ret).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve apps", ex);
		}
	}

	static ApiAppRef map(CompAppInfo app)
	{
		ApiAppRef ret = new ApiAppRef();
		ret.setAppId(app.getAppId().getValue());
		ret.setAppName(app.getAppName());
		ret.setAppType(app.getAppType());
		ret.setComment(app.getComment());
		ret.setLastModified(app.getLastModified());
		return ret;
	}

	@GET
	@Path("app")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getApp(@QueryParam("appid") Long appId)
			throws WebAppException, DbException
	{
		if (appId == null)
		{
			throw new MissingParameterException("Missing required appid parameter.");
		}
		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			return Response.status(HttpServletResponse.SC_OK)
					.entity(mapLoading(dai.getComputationApp(DbKey.createDbKey(appId)))).build();
		}
		catch (NoSuchObjectException e)
		{
			throw new DatabaseItemNotFoundException(String.format(NO_APP_FOUND, appId), e);
		}
		catch (DbIoException ex)
		{
			throw new DbException(String.format("Unable to retrieve requested app with id: %d", appId), ex);
		}
	}

	@POST
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postApp(ApiLoadingApp app)
			throws DbException
	{
		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			CompAppInfo compApp = map(app);
			dai.writeComputationApp(compApp);
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(compApp))
					.build();
		}
		catch(DbIoException ex)
		{
			throw new DbException("Unable to store app", ex);
		}
	}

	static CompAppInfo map(ApiLoadingApp app)
	{
		CompAppInfo ret = new CompAppInfo();
		if (app.getAppId() != null)
		{
			ret.setAppId(DbKey.createDbKey(app.getAppId()));
		}
		else
		{
			ret.setAppId(DbKey.NullKey);
		}
		ret.setAppName(app.getAppName());
		ret.setComment(app.getComment());
		ret.setLastModified(app.getLastModified());
		ret.setProperties(app.getProperties());
		ret.setManualEditApp(app.isManualEditingApp());
		String appType = app.getProperties().getProperty("appType");
		if (appType == null)
		{
			ret.setProperty("appType", app.getAppType());
		}
		return ret;
	}

	@DELETE
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deleteApp(@QueryParam("appid") Long appId)
			throws DbException, WebAppException
	{
		if (appId == null)
		{
			throw new MissingParameterException("Missing required appid parameter.");
		}

		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			CompAppInfo app = dai.getComputationApp(DbKey.createDbKey(appId));
			if (app == null)
			{
				throw new DbException(String.format(NO_APP_FOUND, appId));
			}
			dai.deleteComputationApp(app);
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("appId with ID " + appId + " deleted").build();
		}
		catch (NoSuchObjectException e)
		{
			throw new DatabaseItemNotFoundException(String.format(NO_APP_FOUND, appId), e);
		}
		catch (DbIoException | ConstraintException ex)
		{
			throw new DbException(String.format(NO_APP_FOUND, appId), ex);
		}
	}

	@GET
	@Path("appstat")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response getAppStatus() throws DbException
	{
		List<ApiAppStatus> ret = new ArrayList<>();
		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			for (TsdbCompLock lock : dai.getAllCompProcLocks())
			{
				ret.add(map(dai, lock));
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(ret).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve app status", ex);
		}
	}

	static ApiAppStatus map(LoadingAppDAI dai, TsdbCompLock lock) throws DbIoException
	{
		ApiAppStatus ret = new ApiAppStatus();
		ret.setAppId(lock.getAppId().getValue());
		ret.setAppName(lock.getAppName());
		ret.setHostname(lock.getHost());
		ret.setPid((long) lock.getPID());
		ret.setHeartbeat(lock.getHeartbeat());
		ret.setStatus(lock.getStatus());
		if (dai != null)
		{
			try {
				ApiLoadingApp app = mapLoading(dai.getComputationApp(lock.getAppId()));
				if (app.getProperties() != null && app.getProperties().getProperty("EventPort") != null)
				{
					ret.setEventPort(Integer.parseInt(app.getProperties().getProperty("EventPort")));
				}
				ret.setAppType(app.getAppType());
			}
			catch (DbIoException | NoSuchObjectException | NumberFormatException ex)
			{
				throw new DbIoException("Error mapping app status", ex);
			}
		}
		return ret;
	}

	static ApiLoadingApp mapLoading(CompAppInfo app)
	{
		ApiLoadingApp ret = new ApiLoadingApp();
		ret.setAppId(app.getAppId().getValue());
		ret.setAppName(app.getAppName());
		ret.setComment(app.getComment());
		ret.setLastModified(app.getLastModified());
		ret.setManualEditingApp(app.getManualEditApp());
		ret.setAppType(app.getAppType());
		ret.setProperties(app.getProperties());
		return ret;
	}

}
