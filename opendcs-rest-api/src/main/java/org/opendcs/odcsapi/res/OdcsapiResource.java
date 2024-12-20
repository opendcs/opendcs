/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
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
import java.util.ArrayList;
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

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.ConfigSensor;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.PropertySpec;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.beans.ApiDecodedMessage;
import org.opendcs.odcsapi.beans.ApiDecodesTimeSeries;
import org.opendcs.odcsapi.beans.ApiPropSpec;
import org.opendcs.odcsapi.beans.ApiRawMessage;
import org.opendcs.odcsapi.beans.DecodeRequest;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.opendcs_dep.PropSpecHelper;
import org.opendcs.odcsapi.opendcs_dep.TestDecoder;
import org.opendcs.odcsapi.sec.AuthorizationCheck;


@Path("/")
public class OdcsapiResource extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("tsdb_properties")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getTsdbProperties() throws DbException
	{
		try
		{
			TimeSeriesDb tsdb = getLegacyTimeseriesDB();
			tsdb.readTsdbProperties(tsdb.getConnection());

			Properties props = new Properties();
			while (tsdb.getPropertyNames().hasMoreElements())
			{
				String key = (String) tsdb.getPropertyNames().nextElement();
				props.setProperty(key, tsdb.getProperty(key));
			}

			return Response.status(HttpServletResponse.SC_OK).entity(props).build();
		}
		catch(SQLException e)
		{
			throw new DbException("Error reading timeseries properties: " + e);
		}
	}

	@POST
	@Path("tsdb_properties")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postTsdbProperties(Properties props)
	{
		TimeSeriesDb tsdb = getLegacyTimeseriesDB();
		for (String key : props.stringPropertyNames())
		{
			tsdb.setProperty(key, props.getProperty(key));
		}
		return Response.status(HttpServletResponse.SC_OK).entity(props).build();
	}

	@GET
	@Path("propspecs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getPropSpecs(@QueryParam("class") String className)
			throws WebAppException
	{
		if (className == null)
		{
			throw new WebAppException(HttpServletResponse.SC_BAD_REQUEST, "Missing required class argument.");
		}

		return Response.status(HttpServletResponse.SC_OK).entity(map(PropSpecHelper.getPropSpecs(className))).build();
	}

	static PropertySpec[] map(ApiPropSpec[] specs)
	{
		PropertySpec[] ret = new PropertySpec[specs.length];

		for (int i = 0; i < specs.length; i++)
		{
			ret[i] = new PropertySpec(specs[i].getName(), specs[i].getType(), specs[i].getDescription());
		}

		return ret;
	}

	@POST
	@Path("decode")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postDecode(@QueryParam("script") String scriptName, DecodeRequest request)
			throws WebAppException, DbException
	{
		if (scriptName == null)
		{
			throw new WebAppException(HttpServletResponse.SC_BAD_REQUEST, "Missing required script argument.");
		}
		OpenDcsDatabase db = createDb();
		try
		{
			DecodedMessage message = map(TestDecoder.decodeMessage(request.getRawmsg(),
					request.getConfig(), scriptName, db), request.getRawmsg());
			return Response.status(HttpServletResponse.SC_OK).entity(message).build();
		}
		catch (UnknownPlatformException | DatabaseException e)
		{
			throw new DbException("Unknown platform: " + e.getMessage());
		}
	}

	static DecodedMessage map(ApiDecodedMessage message, ApiRawMessage rawMessage)
			throws UnknownPlatformException, DatabaseException, DbException
	{
		RawMessage raw = map(rawMessage);
		DecodedMessage ret = new DecodedMessage(raw, false);
		ret.setMessageTime(message.getMessageTime());
		ArrayList<TimeSeries> timeSeries = map(message.getTimeSeries());
		for (TimeSeries ts : timeSeries)
		{
			ret.addTimeSeries(ts);
		}
		return ret;
	}

	static RawMessage map(ApiRawMessage message) throws DatabaseException, UnknownPlatformException
	{
		byte[] data = message.getBase64().getBytes();
		RawMessage retMessage = new RawMessage(data, data.length);
		if (message.getPlatformId() != null)
		{
			Platform platform = new Platform();
			platform.setId(DbKey.createDbKey(Long.parseLong(message.getPlatformId())));
			retMessage.setPlatform(platform);
			TransportMedium tm = new TransportMedium(platform);
			retMessage.setTransportMedium(tm);
		}
		retMessage.setTimeStamp(message.getXmitTime());
		if (retMessage.getTransportMedium() == null)
		{
			Platform platform = new Platform();
			TransportMedium tm = new TransportMedium(platform);
			retMessage.setTransportMedium(tm);
		}
		return retMessage;
	}

	static ArrayList<TimeSeries> map(ArrayList<ApiDecodesTimeSeries> timeSeries) throws DbException
	{
		ArrayList<TimeSeries> ret = new ArrayList<>();
		for (ApiDecodesTimeSeries ts : timeSeries)
		{
			TimeSeries tsVal = new TimeSeries(ts.getSensorNum());
			tsVal.setUnits(ts.getUnits());
			PlatformConfig platformConfig = new PlatformConfig();
			ConfigSensor configSensor = new ConfigSensor(platformConfig, ts.getSensorNum());
			configSensor.sensorName = ts.getSensorName();
			Sensor sensor = new Sensor(configSensor, null, null, null);
			tsVal.setSensor(sensor);
			for (int i = 0; i < ts.getValues().size(); i++)
			{
				try
				{
					TimedVariable tv = new TimedVariable();
					tv.setTime(ts.getValues().get(i).getTime());
					tv.setValue(new Variable(ts.getValues().get(i).getValue()));
					tsVal.addSample(tv);
				}
				catch (NoConversionException e)
				{
					throw new DbException("Error converting value: ", e);
				}
			}
			ret.add(tsVal);
		}
		return ret;
	}
}