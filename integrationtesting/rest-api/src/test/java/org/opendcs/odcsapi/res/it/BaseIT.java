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

import com.fasterxml.jackson.databind.ObjectMapper;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.PlatformStatus;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.Cookie;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.TimeSeriesDAI;
import org.apache.catalina.session.StandardSession;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.fixtures.DatabaseContextProvider;
import org.opendcs.odcsapi.fixtures.DatabaseSetupExtension;
import org.opendcs.odcsapi.fixtures.DbType;
import org.opendcs.odcsapi.fixtures.TomcatServer;
import org.opendcs.odcsapi.res.ObjectMapperContextResolver;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.sec.basicauth.Credentials;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opendcs.odcsapi.util.ApiConstants.ORGANIZATION_HEADER;


@ExtendWith(DatabaseContextProvider.class)
class BaseIT
{
	protected static String authHeader = null;
	protected static RequestSpecification authSpec = null;
	private static Cookie cookie;

	<T> T getDtoFromResource(String filename, Class<T> dtoType) throws Exception
	{
		String url = "org/opendcs/odcsapi/res/it/" + DatabaseSetupExtension.getCurrentDbType().getProvider() + "/" + filename;
		System.out.println("Trying " + url);
		try(InputStream inputStream = getClass().getClassLoader()
				.getResourceAsStream(url))
		{
			if(inputStream == null)
			{
				url = "org/opendcs/odcsapi/res/it/DEFAULT/" + filename;
				System.out.println("Trying " + url);
				try(InputStream defaultInputStream = getClass().getClassLoader().getResourceAsStream(url))
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
				.getResourceAsStream("org/opendcs/odcsapi/res/it/" + DatabaseSetupExtension.getCurrentDbType().getProvider() + "/" + filename))
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
				.getResource("org/opendcs/odcsapi/res/it/" + DatabaseSetupExtension.getCurrentDbType().getProvider() + "/" + filename);
		if(resource == null)
		{
			resource = getClass().getClassLoader()
					.getResource("org/opendcs/odcsapi/res/it/DEFAULT/" + filename);
		}
		return new JsonPath(resource);
	}

	void authenticate()
	{
		String COOKIE = "IntegrationTestAuthCookie";
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
		session.setAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE, mcup);
		
		cookie = new Cookie.Builder("JSESSIONID", COOKIE)
								  .setHttpOnly(true)
								  .setSecured(true)
								  .setMaxAge(-1)
								  .setPath("/odcsapi")
								  .build();
		String organization = DatabaseSetupExtension.getOrganization();
		authSpec = new RequestSpecBuilder()
				.addCookie(cookie)
				.addHeader(ORGANIZATION_HEADER, organization)
				.build();
		//Check while passing in cookie
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
		;
	}

	String getCookie()
	{
		if (cookie == null)
		{
			authenticate();
		}
		return String.format("%s=%s", cookie.getName(), cookie.getValue());
	}

	static void logout(SessionFilter sessionFilter)
	{
		if (DatabaseSetupExtension.getCurrentDbType() == DbType.OPENDCS_POSTGRES)
		{
			given()
				.log().ifValidationFails(LogDetail.ALL, true)
				.accept(MediaType.APPLICATION_JSON)
				.spec(authSpec)
			.when()
				.redirects().follow(true)
				.redirects().max(3)
				.delete("logout")
			.then()
				.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
				.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
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

	protected static TimeSeriesDb getTsdb() throws Throwable
	{
		return DatabaseSetupExtension.getCurrentConfig().getTsdb();
	}

	protected static DatabaseIO getDbIo() throws Throwable
	{
		return DatabaseSetupExtension.getCurrentConfig().getDecodesDatabase().getDbIo();
	}

	public static void storeScheduleEntryStatus(ScheduleEntryStatus status) throws DatabaseException
	{
		try (ScheduleEntryDAI dai = getDbIo().makeScheduleEntryDAO())
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
		try (ScheduleEntryDAI dai = getDbIo().makeScheduleEntryDAO())
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
		try (PlatformStatusDAI dai = getTsdb().makePlatformStatusDAO())
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
		try (PlatformStatusDAI dai = getTsdb().makePlatformStatusDAO())
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
		try (TimeSeriesDAI dai = getTsdb().makeTimeSeriesDAO())
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
		try (TimeSeriesDAI dai = getTsdb().makeTimeSeriesDAO())
		{
			dai.deleteTimeSeries(id);
		}
		catch (Throwable ex)
		{
			throw new DatabaseException("Error deleting time series", ex);
		}
	}

	Site map(ApiSite apiSite) throws DatabaseException
	{
		Site site = new Site();
		site.setPublicName(apiSite.getPublicName());
		site.setLocationType(apiSite.getLocationType());
		site.setElevation(apiSite.getElevation());
		site.setElevationUnits(apiSite.getElevUnits());
		site.latitude = apiSite.getLatitude();
		site.longitude = apiSite.getLongitude();
		if (apiSite.getSiteId() != null)
		{
			site.setId(DbKey.createDbKey(apiSite.getSiteId()));
		}
		site.setLastModifyTime(apiSite.getLastModified());
		site.setDescription(apiSite.getDescription());
		site.timeZoneAbbr = apiSite.getTimezone();
		site.nearestCity = apiSite.getNearestCity();
		site.state = apiSite.getState();
		site.country = apiSite.getCountry();
		site.region = apiSite.getRegion();
		site.setActive(apiSite.isActive());

		for (String props : apiSite.getProperties().stringPropertyNames())
		{
			site.setProperty(props, apiSite.getProperties().getProperty(props));
		}
		return site;
	}

	void tearDownSite(Long siteId)
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("siteid", siteId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("siteid", siteId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;
	}

	ApiSite storeSite(String jsonPath) throws Exception
	{
		assertNotNull(jsonPath);
		String siteJson = getJsonFromResource(jsonPath);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(siteJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
			;

		Long localSiteId = response.body().jsonPath().getLong("siteId");

		ObjectMapper mapper = new ObjectMapper();
		ApiSite retVal = mapper.readValue(siteJson, ApiSite.class);
		retVal.setSiteId(localSiteId);
		return retVal;
	}
}
