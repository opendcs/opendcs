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

package org.opendcs.odcsapi.res.it;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.EnumSet;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import decodes.db.DatabaseException;
import decodes.db.PlatformStatus;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesIdentifier;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.TimeSeriesDAI;
import org.apache.catalina.session.StandardSession;
import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.odcsapi.fixtures.DatabaseSetupExtension;
import org.opendcs.odcsapi.fixtures.DbType;
import org.opendcs.odcsapi.fixtures.TomcatServer;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.res.ObjectMapperContextResolver;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.sec.basicauth.Credentials;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.is;

class BaseIT
{
	protected static String authHeader = null;

	<T> T getDtoFromResource(String filename, Class<T> dtoType) throws Exception
	{
		try(InputStream inputStream = getClass().getClassLoader()
				.getResourceAsStream("org/opendcs/odcsapi/res/it/" + DatabaseSetupExtension.getCurrentDbType().name() + "/" + filename))
		{
			if(inputStream == null)
			{
				try(InputStream defaultInputStream = getClass().getClassLoader()
						.getResourceAsStream("org/opendcs/odcsapi/res/it/DEFAULT/" + filename))
				{
					ObjectMapper mapper = new ObjectMapperContextResolver().getContext(dtoType);
					return mapper.readValue(defaultInputStream, dtoType);
				}
			}
			else
			{
				ObjectMapper mapper = new ObjectMapperContextResolver().getContext(dtoType);
				return mapper.readValue(inputStream, dtoType);
			}
		}
	}

	String getJsonFromResource(String filename) throws Exception
	{
		try(InputStream implInputStream = getClass().getClassLoader()
				.getResourceAsStream("org/opendcs/odcsapi/res/it/" + DatabaseSetupExtension.getCurrentDbType().name() + "/" + filename))
		{
			if(implInputStream == null)
			{
				try(InputStream defaultInputStream = getClass().getClassLoader()
						.getResourceAsStream("org/opendcs/odcsapi/res/it/DEFAULT/" + filename);
				InputStreamReader isr = new InputStreamReader(defaultInputStream);
				BufferedReader reader = new BufferedReader(isr))
				{
					return reader.lines().collect(joining(System.lineSeparator()));
				}
			}
			else
			{
				try(InputStreamReader isr = new InputStreamReader(implInputStream);
					BufferedReader reader = new BufferedReader(isr))
				{
					return reader.lines().collect(joining(System.lineSeparator()));
				}
			}
		}
	}

	JsonPath getJsonPathFromResource(String filename)
	{
		URL resource = getClass().getClassLoader()
				.getResource("org/opendcs/odcsapi/res/it/" + DatabaseSetupExtension.getCurrentDbType().name() + "/" + filename);
		if(resource == null)
		{
			resource = getClass().getClassLoader()
					.getResource("org/opendcs/odcsapi/res/it/DEFAULT/" + filename);
		}
		return new JsonPath(resource);
	}

	void authenticate(SessionFilter sessionFilter)
	{
		String COOKIE = "IntegrationTestAuthCookie";
		String parameterKey = "opendcs.rest.api.authorization.type";
		DbInterface.decodesProperties.setProperty(parameterKey, "sso");
		String username = System.getProperty("DB_USERNAME");
		TomcatServer tomcat = DatabaseSetupExtension.getCurrentTomcat();
		StandardSession session = (StandardSession) tomcat.getTestSessionManager()
				.createSession(COOKIE);
		if(session == null) {
			throw new RuntimeException("Test Session Manager is unusable.");
		}
		OpenDcsPrincipal mcup = new OpenDcsPrincipal(username, EnumSet.allOf(OpenDcsApiRoles.class));
		session.setAuthType("CLIENT-CERT");
		session.setPrincipal(mcup);
		session.activate();
		tomcat.getSsoValve()
				.wrappedRegister(COOKIE, mcup, "CLIENT-CERT", null,null);


		//Check while passing in cookie
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.filter(sessionFilter)
			.cookie("JSESSIONIDSSO", COOKIE)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
		;
	}

	static void logout(SessionFilter sessionFilter)
	{
		if (DatabaseSetupExtension.getCurrentDbType() == DbType.OPEN_TSDB)
		{
			given()
				.log().ifValidationFails(LogDetail.ALL, true)
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", authHeader)
				.filter(sessionFilter)
			.when()
				.redirects().follow(true)
				.redirects().max(3)
				.delete("logout")
			.then()
				.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
				.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
			;
		}
	}

	public static void setUpCreds()
	{
		String authHeaderPrefix = "Basic ";
		Credentials adminCreds = new Credentials();
		adminCreds.setPassword(System.getProperty("DB_PASSWORD"));
		adminCreds.setUsername(System.getProperty("DB_USERNAME"));
		String credentialsJson = Base64.getEncoder()
				.encodeToString(String.format("%s:%s", adminCreds.getUsername(), adminCreds.getPassword()).getBytes());
		authHeader = authHeaderPrefix + credentialsJson;
	}

	public static void storeScheduleEntryStatus(ScheduleEntryStatus status) throws DatabaseException
	{
		Configuration currentConfig = DatabaseSetupExtension.getCurrentConfig();
		try (ScheduleEntryDAI dai = currentConfig.getDecodesDatabase().getDbIo().makeScheduleEntryDAO())
		{
			dai.writeScheduleStatus(status);
		}
		catch (Throwable ex)
		{
			throw new DatabaseException("Error storing schedule entry status", ex);
		}
	}

	public static void deleteScheduleEntryStatus(DbKey statusId) throws DatabaseException
	{
		Configuration currentConfig = DatabaseSetupExtension.getCurrentConfig();
		try (ScheduleEntryDAI dai = currentConfig.getDecodesDatabase().getDbIo().makeScheduleEntryDAO())
		{
			ScheduleEntry entry = new ScheduleEntry(statusId);
			dai.deleteScheduleStatusFor(entry);
		}
		catch (Throwable ex)
		{
			throw new DatabaseException("Error deleting schedule entry status", ex);
		}
	}

	public static void storePlatformStatus(PlatformStatus status) throws DatabaseException
	{
		Configuration currentConfig = DatabaseSetupExtension.getCurrentConfig();
		try (PlatformStatusDAI dai = currentConfig.getTsdb().makePlatformStatusDAO())
		{
			dai.writePlatformStatus(status);
		}
		catch (Throwable ex)
		{
			throw new DatabaseException("Error storing platform status", ex);
		}
	}

	public static void deletePlatformStatus(DbKey statusId) throws DatabaseException
	{
		Configuration currentConfig = DatabaseSetupExtension.getCurrentConfig();
		try (PlatformStatusDAI dai = currentConfig.getTsdb().makePlatformStatusDAO())
		{
			dai.deletePlatformStatus(statusId);
		}
		catch (Throwable ex)
		{
			throw new DatabaseException("Error deleting platform status", ex);
		}
	}

	public static void storeTimeSeries(CTimeSeries ts) throws Exception
	{
		Configuration currentConfig = DatabaseSetupExtension.getCurrentConfig();
		try (TimeSeriesDAI dai = currentConfig.getTsdb().makeTimeSeriesDAO())
		{
			dai.saveTimeSeries(ts);
		}
		catch (Throwable ex)
		{
			throw new DatabaseException("Error storing time series", ex);
		}
	}

	public static void deleteTimeSeries(TimeSeriesIdentifier id) throws Exception
	{
		Configuration currentConfig = DatabaseSetupExtension.getCurrentConfig();
		try (TimeSeriesDAI dai = currentConfig.getTsdb().makeTimeSeriesDAO())
		{
			dai.deleteTimeSeries(id);
		}
		catch (Throwable ex)
		{
			throw new DatabaseException("Error deleting time series", ex);
		}
	}
}
