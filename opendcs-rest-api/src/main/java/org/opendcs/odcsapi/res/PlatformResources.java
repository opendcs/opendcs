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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;

import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformList;
import decodes.db.PlatformSensor;
import decodes.db.PlatformStatus;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.db.Site;
import decodes.db.TransportMedium;
import decodes.db.ValueNotFoundException;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.ScheduleEntryDAI;
import org.opendcs.odcsapi.beans.ApiPlatform;
import org.opendcs.odcsapi.beans.ApiPlatformRef;
import org.opendcs.odcsapi.beans.ApiPlatformSensor;
import org.opendcs.odcsapi.beans.ApiPlatformStatus;
import org.opendcs.odcsapi.beans.ApiTransportMedium;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

@Path("/")
public final class PlatformResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("platformrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "The GET platformrefs method returns a list of platforms",
			description = "The GET platformrefs method returns a list of platforms, "
					+ "optionally with a given Transport Medium type.  \n\n"
					+ "It accepts the following arguments:  \n* **tmtype** – the transport medium type desired. "
					+ "If not provided, all types are returned. The method will return any platform that has a "
					+ "transport medium with the given type. The returned data structure will still contain all "
					+ "of the transport media in the platform, but the list of platforms will be filtered to only "
					+ "include platforms with a TM of the given type:\n  * **NOTE**: medium type 'goes' will match "
					+ "either goes-self-timed or goes-random.\n\nData Structure TBD but will include  "
					+ "\n* **name** – an index into the hashed set. Combination of site name and designator  "
					+ "\n* **agency** – The agency that owns and/or maintains this platform  "
					+ "\n* **configId** – Numeric surrogate key to the configuration record  \n* **description**  "
					+ "\n* **platformId** - Numeric surrogate key to the platform record  "
					+ "\n* **siteId** - Numeric surrogate key to the site record  "
					+ "\n* **transportMedia** – a list of tmtype/tm id pairs.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved platform references",
							content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiPlatformRef.class)),
									mediaType = MediaType.APPLICATION_JSON)),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - DECODES Platform Records"}
	)
	public Response getPlatformRefs(@Parameter(description = "Transport medium type",
			schema = @Schema(implementation = String.class, example = "goes"))
		@QueryParam("tmtype") String tmtype)
			throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		Map<String, ApiPlatformRef> ret = new HashMap<>();
		try
		{

			PlatformList platformList = new PlatformList();
			dbIo.readPlatformList(platformList, tmtype);
			List<ApiPlatformRef> platSpecs = map(platformList);
			for(ApiPlatformRef ps : platSpecs)
			{
				ret.put(ps.getName(), ps);
			}
			return Response.status(HttpServletResponse.SC_OK).entity(ret).build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException("Unable to retrieve platform list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static List<ApiPlatformRef> map(PlatformList platformList)
	{
		List<ApiPlatformRef> ret = new ArrayList<>();
		Iterator<Platform> platform = platformList.iterator();
		while (platform.hasNext())
		{
			ApiPlatformRef ref = new ApiPlatformRef();
			Platform plat = platform.next();
			ref.setName(plat.getDisplayName());
			if (plat.getId() != null)
			{
				ref.setPlatformId(plat.getId().getValue());
			}
			else
			{
				ref.setPlatformId(DbKey.NullKey.getValue());
			}
			ref.setAgency(plat.getAgency());
			ref.setConfig(plat.getConfigName());
			ref.setDescription(plat.getDescription());
			if (plat.getConfig() != null && plat.getConfig().getId() != null)
			{
				ref.setConfigId(plat.getConfig().getId().getValue());
			}
			else
			{
				ref.setConfigId(DbKey.NullKey.getValue());
			}
			if (plat.getSite() != null)
			{
				ref.setSiteId(plat.getSite().getId().getValue());
			}
			else
			{
				ref.setSiteId(DbKey.NullKey.getValue());
			}
			ref.setName(plat.getSiteName(false));
			ref.setTransportMedia(plat.getProperties());
			ref.setDesignator(plat.getPlatformDesignator());
			ret.add(ref);
		}
		return ret;
	}

	@GET
	@Path("platform")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "This method returns a JSON representation of a single, complete DECODES Platform record",
			description = "Fetches detailed information about a specific platform using its unique ID. "
					+ "Example: \n\n    http://localhost:8080/odcsapi/platform?platformid=5",
			operationId = "getPlatform",
			responses = {
					@ApiResponse(responseCode = "200", description = "Platform details retrieved successfully",
							content = @Content(schema = @Schema(implementation = ApiPlatform.class))),
					@ApiResponse(responseCode = "400", description = "Missing or invalid platform ID"),
					@ApiResponse(responseCode = "404", description = "Platform not found"),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"REST - DECODES Platform Records"}
	)
	public Response getPlatform(
			@Parameter(
					description = "Platform ID",
					required = true,
					schema = @Schema(implementation = Long.class, example = "5"))
			@QueryParam("platformid") Long platformId)
			throws WebAppException, DbException
	{
		if (platformId == null)
		{
			throw new MissingParameterException("Missing required platformid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			Platform platform = new Platform();
			platform.setId(DbKey.createDbKey(platformId));
			dbIo.readPlatform(platform);
			return Response.status(HttpServletResponse.SC_OK).entity(map(platform)).build();
		}
		catch(ValueNotFoundException ex)
		{
			throw new DatabaseItemNotFoundException("Platform with ID " + platformId + " not found.", ex);
		}
		catch (DatabaseException ex)
		{
			if (ex.getCause() instanceof ValueNotFoundException)
			{
				throw new DatabaseItemNotFoundException("Platform with ID " + platformId + " not found.", ex);
			}
			throw new DbException("Unable to retrieve platform", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ApiPlatform map(Platform platform)
	{
		ApiPlatform ret = new ApiPlatform();
		if (platform.getId() != null)
		{
			ret.setPlatformId(platform.getId().getValue());
		}
		else
		{
			ret.setPlatformId(DbKey.NullKey.getValue());
		}
		ret.setAgency(platform.getAgency());
		ret.setDescription(platform.getDescription());
		ret.setDesignator(platform.getPlatformDesignator());
		if (platform.getConfig() != null && platform.getConfig().getId() != null)
		{
			ret.setConfigId(platform.getConfig().getId().getValue());
		}
		else
		{
			ret.setConfigId(DbKey.NullKey.getValue());
		}
		ret.setProduction(platform.isProduction);
		if (platform.getSite() != null && platform.getSite().getId() != null)
		{
			ret.setSiteId(platform.getSite().getId().getValue());
		}
		else
		{
			ret.setSiteId(DbKey.NullKey.getValue());
		}
		ret.setProperties(platform.getProperties());
		ret.setName(platform.getSiteName(false));
		ret.setTransportMedia(map(platform.getTransportMedia()));
		return ret;
	}

	static ArrayList<ApiTransportMedium> map(Iterator<TransportMedium> transportMedium)
	{
		ArrayList<ApiTransportMedium> transportMedia = new ArrayList<>();
		while (transportMedium.hasNext())
		{
			TransportMedium tm = transportMedium.next();
			ApiTransportMedium apiTm = new ApiTransportMedium();
			apiTm.setMediumId(tm.getMediumId());
			apiTm.setMediumType(tm.getMediumType());
			apiTm.setBaud(tm.getBaud());
			apiTm.setAssignedTime(tm.assignedTime);
			apiTm.setChannelNum(tm.channelNum);
			apiTm.setDataBits(tm.getDataBits());
			apiTm.setTimezone(tm.getTimeZone());
			apiTm.setStopBits(tm.getStopBits());
			apiTm.setParity(String.valueOf(tm.getParity()));
			apiTm.setDoLogin(tm.isDoLogin());
			apiTm.setPassword(tm.getPassword());
			apiTm.setUsername(tm.getUsername());
			apiTm.setScriptName(tm.scriptName);
			apiTm.setTransportInterval(tm.transmitInterval);
			apiTm.setTransportWindow(tm.transmitWindow);
			apiTm.setTimeAdjustment(tm.getTimeAdjustment());
			apiTm.setLoggerType(tm.getLoggerType());

			transportMedia.add(apiTm);
		}
		return transportMedia;
	}

	@POST
	@Path("platform")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create or Overwrite Existing Decodes Platform",
			description = "The GET platform method takes a single DECODES Platform record in JSON format,"
					+ " as described above for GET.  \n\n"
					+ "For creating a new platform, leave platformId out of the passed data structure.  \n\n"
					+ "For overwriting an existing one, include the platformId that was previously returned. "
					+ "The platform in the database is replaced with the one sent.",
			requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiPlatform.class),
					mediaType = MediaType.APPLICATION_JSON,
					examples = {
							@ExampleObject(name = "Basic", value = ResourceExamples.PlatformExamples.BASIC),
							@ExampleObject(name = "New", value = ResourceExamples.PlatformExamples.NEW),
							@ExampleObject(name = "Update", value = ResourceExamples.PlatformExamples.UPDATE)
					}), required = true, description = "Decodes Platform Object"),
			responses = {
					@ApiResponse(responseCode = "201", description = "Platform created successfully"),
					@ApiResponse(responseCode = "500",
							description = "Internal server error occurred while storing the platform")
			},
			tags = {"REST - DECODES Platform Records"}
	)
	public Response postPlatform(ApiPlatform platform)
			throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			Platform plat = map(platform);
			dbIo.writePlatform(plat);
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(plat))
					.build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException(String.format("Unable to store platform with name: %s", platform.getName()), ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static Platform map(ApiPlatform platform) throws DatabaseException
	{
		Platform ret = new Platform();
		if (platform.getPlatformId() != null)
		{
			ret.setId(DbKey.createDbKey(platform.getPlatformId()));
		}
		else
		{
			ret.setId(DbKey.NullKey);
		}
		ret.setAgency(platform.getAgency());
		ret.setDescription(platform.getDescription());
		ret.setPlatformDesignator(platform.getDesignator());
		ret.lastModifyTime = new Date();
		ret.platformSensors = platMap(platform.getPlatformSensors());
		ret.platformSensors.forEach(p -> p.platform = ret);
		if (platform.getConfigId() != null)
		{
			PlatformConfig config = new PlatformConfig();
			config.setId(DbKey.createDbKey(platform.getConfigId()));
			ret.setConfig(config);
		}
		ret.isProduction = platform.isProduction();
		if (platform.getSiteId() != null)
		{
			Site site = new Site();
			site.setId(DbKey.createDbKey(platform.getSiteId()));
			site.setPublicName(platform.getName());
			ret.setSite(site);
		}
		Properties properties = platform.getProperties();
		if(properties != null)
		{
			for(Map.Entry<Object, Object> entry : properties.entrySet())
			{
				ret.setProperty(entry.getKey().toString(), entry.getValue().toString());
			}
		}
		ret.transportMedia = map(platform.getTransportMedia());
		return ret;
	}

	static Vector<PlatformSensor> platMap(List<ApiPlatformSensor> platformSensors) throws DatabaseException
	{
		Vector<PlatformSensor> ret = new Vector<>();
		for (ApiPlatformSensor sensor: platformSensors)
		{
			PlatformSensor ps = new PlatformSensor();
			ps.sensorNumber = sensor.getSensorNum();
			if (sensor.getActualSiteId() != null)
			{
				Site site = new Site();
				site.setId(DbKey.createDbKey(sensor.getActualSiteId()));
				ps.site = site;
			}
			Integer usgsDdno = sensor.getUsgsDdno();
			if(usgsDdno != null)
			{
				ps.setUsgsDdno(usgsDdno);
			}
			Properties props = sensor.getSensorProps();
			for (String name : props.stringPropertyNames())
			{
				ps.setProperty(name, props.getProperty(name));
			}
			ret.add(ps);
		}
		return ret;
	}

	static Vector<TransportMedium> map(List<ApiTransportMedium> transportMedium)
	{
		Vector<TransportMedium> ret = new Vector<>();
		for (ApiTransportMedium tm : transportMedium)
		{
			Platform platform = new Platform();
			TransportMedium t = new TransportMedium(platform);
			if (tm.getMediumId() != null)
			{
				t.setMediumId(tm.getMediumId());
			}
			t.setMediumType(tm.getMediumType());
			Integer baud = tm.getBaud();
			if(baud != null)
			{
				t.setBaud(baud);
			}
			if (tm.getAssignedTime() != null)
			{
				t.assignedTime = tm.getAssignedTime();
			}
			if (tm.getChannelNum() != null)
			{
				t.channelNum = tm.getChannelNum();
			}
			if (tm.getDataBits() != null)
			{
				t.setDataBits(tm.getDataBits());
			}
			t.setTimeZone(tm.getTimezone());
			if (tm.getStopBits() != null)
			{
				t.setStopBits(tm.getStopBits());
			}
			String parity = tm.getParity();
			if(parity != null)
			{
				t.setParity(parity.charAt(0));
			}
			Boolean doLogin = tm.getDoLogin();
			if(doLogin != null)
			{
				t.setDoLogin(doLogin);
			}
			t.setPassword(tm.getPassword());
			t.setUsername(tm.getUsername());
			t.scriptName = tm.getScriptName();
			if (tm.getTransportInterval() != null)
			{
				t.transmitInterval = tm.getTransportInterval();
			}
			if (tm.getTransportWindow() != null)
			{
				t.transmitWindow = tm.getTransportWindow();
			}
			if (tm.getChannelNum() != null)
			{
				t.channelNum = tm.getChannelNum();
			}
			ret.add(t);
		}
		return ret;
	}

	@DELETE
	@Path("platform")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Existing Decodes Platform",
			description = "Required argument platformid must be passed.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Platform deleted successfully"),
					@ApiResponse(responseCode = "400",
							description = "Missing or invalid platform ID"),
					@ApiResponse(responseCode = "500",
							description = "Database error occurred while deleting the platform")
			},
			tags = {"REST - DECODES Platform Records"}
	)
	public Response deletePlatform(@Parameter(description = "Platform ID", required = true,
			schema = @Schema(implementation = Long.class, example = "5"))
		@QueryParam("platformid") Long platformId)
			throws DbException, WebAppException
	{
		if (platformId == null)
		{
			throw new MissingParameterException("Missing required platformid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			Platform plat = new Platform();
			plat.setId(DbKey.createDbKey(platformId));
			dbIo.deletePlatform(plat);
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("Platform with ID " + platformId + " deleted")
					.build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException(String.format("Unable to delete platform with ID: %s", platformId), ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	@GET
	@Path("platformstat")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Returned structure contains information about recent activity on each platform",
			description = "Sample URL:\n  \n    http://localhost:8080/odcsapi/platformstat  \n  \n  \n"
					+ "Optional argument 'netlistid' can be passed to only return platforms that have a "
					+ "transport medium in the referenced network list.\n  \nThe returned data structure contains "
					+ "information about recent activity on each platform:\n  \n```\n  [\n    {\n      "
					+ "\"platformId\": 53,\n      \"platformName\": \"OKVI4\",\n      \"siteId\": 1,\n      "
					+ "\"lastContact\": \"2023-06-09T18:30:53.086Z[UTC]\",\n      \"lastMessage\": "
					+ "\"2023-06-09T18:30:53.086Z[UTC]\",\n      \"lastError\": null,\n      \"lastMsgQuality\": \"G\",\n"
					+ "      \"annotation\": null,\n      \"lastRoutingExecId\": 609,\n      \"routingSpecName\": "
					+ "\"periodic-10-minute\"\n    },\n    {\n      \"platformId\": 54,\n      \"platformName\": "
					+ "\"MROI4\",\n      \"siteId\": 6,\n      \"lastContact\": \"2023-06-09T18:30:53.102Z[UTC]\",\n"
					+ "      \"lastMessage\": \"2023-06-09T18:30:53.102Z[UTC]\",\n      \"lastError\": null,\n      "
					+ "\"lastMsgQuality\": \"G\",\n      \"annotation\": null,\n      \"lastRoutingExecId\": 609,\n      "
					+ "\"routingSpecName\": \"periodic-10-minute\"\n    },\n    {\n      \"platformId\": 55,\n      "
					+ "\"platformName\": \"ROWI4\",\n      \"siteId\": 2,\n      "
					+ "\"lastContact\": \"2023-06-09T18:30:53.013Z[UTC]\",\n      "
					+ "\"lastMessage\": \"2023-06-09T18:30:53.013Z[UTC]\",\n      \"lastError\": null,\n      "
					+ "\"lastMsgQuality\": \"G\",\n      \"annotation\": null,\n      \"lastRoutingExecId\": 609,\n      "
					+ "\"routingSpecName\": \"periodic-10-minute\"\n    }\n  ]\n\n```\n  "
					+ "\nThis may be used to populate a GUI similar to the Java 'Platform Monitor' GUI in OpenDCS. "
					+ "A pulldown list of network list IDs and names is recommended.\n  \nNote the following:\n  "
					+ "\n*  **lastContact** is the last time that any communication from this platform was received.\n  "
					+ "\n*  **lastMessage** is the time stamp that the last message from this platform was received.\n  "
					+ "\n*  **lastError** is the time that a decoding or communications error last occurred with "
					+ "this platform.\n*  **lastRoutingExecId** indicates the specific execution of a routing spec "
					+ "that was last used to process this platform. (See Routing Exec Status above.)\n  "
					+ "\n*  **annotation** is the text of the last error message generated from this platform. "
					+ "To retrieve a list of Data Acquisition events for a platform, you can use the GET dacqevent "
					+ "method described in section 5.2.3, passing the 'platformid' argument.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved platform status",
						content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiPlatformStatus.class)))),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"OpenDCS Process Monitor and Control (Routing)"}
	)
	public Response getPlatformStats(@Parameter(description = "Only return platforms that have a transport medium "
			+ "in the referenced network list.", schema = @Schema(implementation = Long.class, example = "1001"))
		@QueryParam("netlistid") Long netlistId)
			throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try (PlatformStatusDAI dao = dbIo.makePlatformStatusDAO())
		{
			List<PlatformStatus> statuses;
			if (netlistId != null)
			{
				statuses = dao.readPlatformStatusList(DbKey.createDbKey(netlistId));
			}
			else
			{
				statuses = dao.readPlatformStatusList(null);
			}
			return Response.status(HttpServletResponse.SC_OK).entity(statusListMap(dbIo, statuses)).build();
		}
		catch (DbIoException | DatabaseException ex)
		{
			throw new DbException(String.format("Unable to retrieve platform status with ID: %s", netlistId), ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static List<ApiPlatformStatus> statusListMap(DatabaseIO dbIo, List<PlatformStatus> statuses) throws DatabaseException
	{
		if(statuses == null)
		{
			return new ArrayList<>();
		}
		List<ApiPlatformStatus> ret = new ArrayList<>();
		for(PlatformStatus status : statuses)
		{
			ApiPlatformStatus ps = new ApiPlatformStatus();
			if(status.getPlatformId() != null)
			{
				ps.setPlatformId(status.getPlatformId().getValue());
			}
			else
			{
				ps.setPlatformId(DbKey.NullKey.getValue());
			}
			ps.setAnnotation(status.getAnnotation());
			ps.setLastContact(status.getLastContactTime());
			ps.setLastError(status.getLastErrorTime());
			ps.setLastMessage(status.getLastMessageTime());
			if (!status.getLastScheduleEntryStatusId().isNull()
					&& (status.getLastRoutingSpecName() == null || status.getLastRoutingSpecName().isEmpty()))
			{
				try (ScheduleEntryDAI dai = dbIo.makeScheduleEntryDAO())
				{
					ScheduleEntry scheduleEntry = dai.readScheduleEntryByStatusId(status.getLastScheduleEntryStatusId());
					if(scheduleEntry != null && scheduleEntry.getRoutingSpecId() != null)
					{
						long routingId = scheduleEntry.getRoutingSpecId().getValue();
						RoutingSpec rs = new RoutingSpec();
						rs.setId(DbKey.createDbKey(routingId));
						dbIo.readRoutingSpec(rs);
						ps.setRoutingSpecName(rs.getName());
					}
				}
				catch (DbIoException ex)
				{
					throw new DatabaseException("Unable to retrieve routing spec for platform status", ex);
				}
			}
			else
			{
				ps.setRoutingSpecName(status.getLastRoutingSpecName());
			}
			ps.setLastRoutingExecId(status.getLastScheduleEntryStatusId().getValue());
			if (status.getPlatformId() != null)
			{
				Platform pl = new Platform();
				pl.setId(status.getPlatformId());
				dbIo.readPlatform(pl);
				if (pl.getSite() != null && pl.getSite().getId() != null)
				{
					ps.setSiteId(pl.getSite().getId().getValue());
				}
				if (pl.getSite() != null && pl.getSite().getUniqueName() != null
						&& !pl.getSite().getUniqueName().isEmpty())
				{
					if (status.getDesignator() != null && !status.getDesignator().isEmpty())
					{
						ps.setPlatformName(pl.getSite().getUniqueName() + "-" + status.getDesignator());
					}
					else
					{
						ps.setPlatformName(pl.getSite().getUniqueName());
					}
				}
			}
			ret.add(ps);
		}
		return ret;
	}

}
