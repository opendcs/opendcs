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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.ImportComp;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsImporter;
import decodes.tsdb.TsdbException;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;
import ilex.util.FileUtil;
import ilex.var.TimedVariable;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;
import opendcs.dai.ComputationDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.Programs;
import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.odcsapi.beans.ApiAlgorithm;
import org.opendcs.odcsapi.beans.ApiCompParm;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.fixtures.DatabaseSetupExtension;
import org.opendcs.odcsapi.res.ComputationResources;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.utils.FailableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

final class ComputationResourcesIT extends BaseApiIT
{
	private static final Logger log = LoggerFactory.getLogger(ComputationResources.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TimeSeriesIdentifier tsId;
	private TimeSeriesIdentifier tsId2;
	private Long compId;
	private Long siteId;
	private Long appId;
	private Long algId;
	private Long tsCompId;
	private final EnvironmentVariables environment = new EnvironmentVariables();
	private final SystemExit exit = new SystemExit();
	List<CTimeSeries> expectedTsList = new ArrayList<>();

	@BeforeEach
	void init() throws Exception
	{
		setUpCreds();
		authenticate();

		ApiComputation comp = getDtoFromResource("computation_insert_data.json", ApiComputation.class);

		ApiSite site = getDtoFromResource("computation_site_data.json", ApiSite.class);
		Site tsSite = map(site);

		ApiLoadingApp app = getDtoFromResource("computation_app_data.json", ApiLoadingApp.class);
		String appJson = MAPPER.writeValueAsString(app);

		// Insert the app
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(appJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		appId = response.body().jsonPath().getLong("appId");

		// Insert Algorithm
		ApiAlgorithm alg = getDtoFromResource("computation_algorithm_data.json", ApiAlgorithm.class);

		String algJson = MAPPER.writeValueAsString(alg);

		// Insert the algorithm
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(algJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("algorithm")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		algId = response.body().jsonPath().getLong("algorithmId");

		String siteJson = MAPPER.writeValueAsString(site);

		// Insert the site
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
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

		siteId = response.body().jsonPath().getLong("siteId");

		// Create an active time series
		CwmsTsId identifier = new CwmsTsId();
		identifier.setUniqueString(String.format("%s.%s.%s.%s.%s.%s", tsSite.getDisplayName(),
				"Stor-FilledCon", "Inst", "~6Hours", "0", "CENWP-COMPUTED-FCST"));
		identifier.setSite(tsSite);
		identifier.setStorageUnits("ac-ft");
		identifier.setActive(true);
		identifier.setInterval("~6Hours");
		identifier.setDuration("0");
		identifier.setDescription("Storage at TS test site");
		CTimeSeries ts = new CTimeSeries(identifier);
		tsId = identifier;
		ts.setUnitsAbbr("ac-ft");

		TimedVariable tv = new TimedVariable(Date.from(Instant.parse("2012-01-01T00:00:00Z")), 0.01, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2012-01-01T01:00:00Z")), 0.01, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2012-01-01T02:00:00Z")), 0.02, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2012-01-01T03:00:00Z")), 0.03, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2012-01-01T04:00:00Z")), 0.05, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2012-01-01T05:00:00Z")), 0.08, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2012-01-01T06:00:00Z")), 0.13, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);

		storeTimeSeries(ts);

		// Create an active time series
		CwmsTsId identifier2 = new CwmsTsId();
		identifier2.setUniqueString(String.format("%s.%s.%s.%s.%s.%s", tsSite.getDisplayName(),
				"Stor-AuthorizedCon", "Inst", "~6Hours", "0", "CENWP-COMPUTED-FCST"));
		identifier2.setSite(tsSite);
		identifier2.setStorageUnits("ac-ft");
		identifier2.setActive(true);
		identifier2.setInterval("~6Hours");
		identifier2.setDuration("0");
		identifier2.setDescription("Storage at TS test site");
		CTimeSeries ts2 = new CTimeSeries(identifier2);
		tsId2 = identifier2;
		ts2.setUnitsAbbr("ac-ft");

		TimedVariable tv2 = new TimedVariable(Date.from(Instant.parse("2012-01-01T00:00:00Z")), 0.01, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2012-01-01T00:00:00Z")), 0.01, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2012-01-01T01:00:00Z")), 0.02, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2012-01-01T02:00:00Z")), 0.03, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2012-01-01T03:00:00Z")), 0.05, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2012-01-01T04:00:00Z")), 0.08, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2012-01-01T05:00:00Z")), 0.13, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2012-01-01T06:00:00Z")), 0.21, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);

		storeTimeSeries(ts2);

		// get TS DbKeys and save to the identifiers
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
			;

		JsonPath actual = response.body().jsonPath();
		List<Map<String, Object>> actualList = actual.getList("");
		assertFalse(actualList.isEmpty());
		boolean found = false;
		boolean found2 = false;
		for (Map<String, Object> actualMap : actualList)
		{
			if (actualMap.get("uniqueString").equals(identifier.getUniqueString()))
			{
				tsId.setKey(DbKey.createDbKey(((Integer) actualMap.get("key")).longValue()));
				found = true;
			}
			else if (actualMap.get("uniqueString").equals(identifier2.getUniqueString()))
			{
				tsId2.setKey(DbKey.createDbKey(((Integer) actualMap.get("key")).longValue()));
				found2 = true;
			}
		}
		assertTrue(found);
		assertTrue(found2);

		for (int i = 0; i < 2; i++)
		{
			comp.getParmList().get(i).setSiteName(site.getPublicName());
			comp.getParmList().get(i).setSiteId(siteId);
		}
		comp.setApplicationName(app.getAppName());
		comp.setAppId(appId);
		comp.setAlgorithmName(alg.getName());
		comp.setAlgorithmId(algId);

		String compJson = MAPPER.writeValueAsString(comp);

		// Insert the computation
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(compJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		compId = response.body().jsonPath().getLong("computationId");
	}

	@AfterEach
	void cleanup() throws Exception
	{
		// Delete the computation
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", compId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Delete the app
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("appid", appId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Delete the algorithm
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("algorithmid", algId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("algorithm")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		deleteTimeSeries(tsId);
		deleteTimeSeries(tsId2);
		tearDownSite(siteId);

		logout();
	}

	@Test
	void testGetComputationRefs()
	{
		JsonPath expectedJson = getJsonPathFromResource("computation_refs_expected.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;
		final var actualItem = response.body()
									   .jsonPath()
									   .setRootPath("find { it -> it.name == '" + expectedJson.getString("name") + "'}");

		assertNotNull(actualItem);
		assertEquals(expectedJson.getString("name"), actualItem.getString("name"));
		assertEquals(expectedJson.getString("description"), actualItem.getString("description"), () -> "Return was: " + response.asPrettyString());
		assertEquals(expectedJson.getBoolean("enabled"), actualItem.getBoolean("enabled"));
		assertEquals(expectedJson.getString("groupName"), actualItem.getString("groupName"));
		assertEquals(expectedJson.getString("algorithmName"), actualItem.getString("algorithmName"));
		assertEquals(expectedJson.getString("processName"), actualItem.getString("processName"));
	}

	@Test
	void testGetComputationRefsWithFilters() throws Exception
	{
		JsonPath expected = getJsonPathFromResource("computation_refs_expected.json");

		ApiComputation comp = getDtoFromResource("computation_insert_data.json", ApiComputation.class);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("site", siteId)
			.queryParam("datatype", comp.getParmList().get(0).getDataTypeId())
			.queryParam("group", comp.getGroupId())
			.queryParam("algorithm", algId)
			.queryParam("process", appId)
			.queryParam("enabled", comp.isEnabled())
			.queryParam("interval", comp.getParmList().get(0).getInterval())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		List<Map<String, Object>> actualList = response.body().jsonPath().getList("");
		assertFalse(actualList.isEmpty());
		Map<String, Object> actualItem = actualList.get(0);
		assertEquals(expected.getString("name"), actualItem.get("name"));
		assertEquals(expected.getString("description"), actualItem.get("description"));
		assertEquals(expected.getBoolean("enabled"), actualItem.get("enabled"));
		assertEquals(expected.getString("groupName"), actualItem.get("groupName"));
		assertEquals(expected.getString("algorithmName"), actualItem.get("algorithmName"));
		assertEquals(expected.getString("processName"), actualItem.get("processName"));
	}

	@Test
	void testGetComputationRefsWithNoMatchingFilters() throws Exception
	{
		ApiComputation comp = getDtoFromResource("computation_insert_data.json", ApiComputation.class);

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("site", comp.getParmList().get(0).getSiteId())
			.queryParam("datatype", comp.getParmList().get(0).getDataTypeId())
			.queryParam("group", "test group")
			.queryParam("algorithm", comp.getAlgorithmId())
			.queryParam("process", comp.getAppId())
			.queryParam("enabled", false)
			.queryParam("interval", comp.getParmList().get(0).getInterval())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("site", comp.getParmList().get(0).getSiteId())
			.queryParam("datatype", comp.getParmList().get(0).getDataTypeId())
			.queryParam("group", comp.getGroupId())
			.queryParam("algorithm", comp.getAlgorithmId())
			.queryParam("process", comp.getAppId())
			.queryParam("enabled", comp.isEnabled())
			.queryParam("interval", "bi-annual")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("size()", is(0))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("site", comp.getParmList().get(0).getSiteId())
			.queryParam("datatype", comp.getParmList().get(0).getDataTypeId())
			.queryParam("group", comp.getGroupId())
			.queryParam("algorithm", "25")
			.queryParam("process", comp.getAppId())
			.queryParam("enabled", comp.isEnabled())
			.queryParam("interval", comp.getParmList().get(0).getInterval())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
		;
	}

	@Test
	void testGetComputation()
	{
		JsonPath expected = getJsonPathFromResource("computation_insert_data.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", compId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertEquals(expected.getString("name"), actual.getString("name"));
		assertEquals(expected.getString("description"), actual.getString("description"));
		assertEquals(expected.getString("enabled"), actual.getString("enabled"));
		assertEquals(expected.getString("groupName"), actual.getString("groupName"));
		assertEquals(expected.getString("applicationName"), actual.getString("applicationName"));
		assertEquals(expected.getString("algorithmName"), actual.getString("algorithmName"));
		assertEquals(algId, actual.getLong("algorithmId"));
		assertEquals(appId, actual.getLong("appId"));
		for (int i = 0; i < 2; i++)
		{
			assertEquals(siteId, actual.getLong("parmList[" + i + "].siteId"));
			String dataType = actual.getString("parmList[" + i + "].dataType");
			String[] dataTypeParts = dataType != null ? dataType.split(":") : null;
			assertEquals(expected.getString("parmList[" + i + "].dataType"),
					dataTypeParts != null ? String.format("%s:%s", dataTypeParts[0], dataTypeParts[1]): null);
			assertEquals(expected.getString("parmList[" + i + "].interval"),
					actual.getString("parmList[" + i + "].interval"));
		}
		assertEquals(expected.getString("comment"), actual.getString("comment"));
	}

	@Test
	void testPostAndDeleteComputation() throws Exception
	{
		ApiComputation comp = getDtoFromResource("computation_post_delete_insert_data.json",
				ApiComputation.class);

		String compJson = MAPPER.writeValueAsString(comp);

		// Store the new computation
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(compJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		Long newCompId = response.body().jsonPath().getLong("computationId");

		// Get the new computation and assert it matches the expected data
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", newCompId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertEquals(comp.getName(), actual.getString("name"));
		assertEquals(comp.isEnabled(), actual.getBoolean("enabled"));
		assertEquals(comp.getGroupName(), actual.getString("groupName"));
		assertEquals(comp.getApplicationName(), actual.getString("applicationName"));
		assertEquals(comp.getAlgorithmName(), actual.getString("algorithmName"));
		assertEquals(comp.getComment(), actual.getString("comment"));
		List<Map<String, Object>> actualParmList = actual.getList("parmList");
		int i = 0;
		for (ApiCompParm parm : comp.getParmList())
		{
			Map<String, Object> actualItem = actualParmList.get(i);
			assertEquals(parm.getSiteName(), actualItem.get("siteName"));
			assertEquals(parm.getDataType(), actualItem.get("dataType"));
			assertEquals(parm.getInterval(), actualItem.get("interval"));
			i++;
		}

		// Delete the new computation
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", newCompId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Get the new computation and assert it is not found
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", newCompId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;
	}

	@TestTemplate
	void testExecuteComputation() throws Exception
	{
		String organization = DatabaseSetupExtension.getOrganization();

		importData(Optional.of(Path.of("ResEvap", "Test1")));
		assertFalse(expectedTsList.isEmpty(), "No time series found imported");

		ClientRequestFilter auth = ctx ->
		{
			ctx.getHeaders().putSingle(ApiConstants.ORGANIZATION_HEADER, organization);
			ctx.getHeaders().putSingle("Cookie", getCookie());
		};

		URI baseURI = URI.create(String.format("%s:%d/%s", RestAssured.baseURI, RestAssured.port, RestAssured.basePath));

		WebTarget target;
		try (Client client = ClientBuilder.newBuilder()
				.register(SseEventSource.class)
				.register(auth)
				.build())
		{
			target = client.target(baseURI)
					.path("runcomputation")
					.queryParam("computationid", tsCompId);

			List<InboundSseEvent> events = new CopyOnWriteArrayList<>();
			CountDownLatch done = new CountDownLatch(1);
			CountDownLatch firstEvent = new CountDownLatch(1);

			try(SseEventSource source = SseEventSource.target(target).build())
			{
				source.register(
					(InboundSseEvent event) ->
					{
						events.add(event);
						firstEvent.countDown();

						if (event.getName().equalsIgnoreCase("Results"))
						{
							done.countDown();
						}
						else
						{
							String data = event.readData(String.class, MediaType.TEXT_PLAIN_TYPE);
							String name = event.getName();
							log.atInfo().log(String.format("Received SSE event: %s, %s", name, data));
						}
					},
					(Throwable error) -> {
						throw new AssertionError("SSE Error: " + error.getMessage());
					}
				);
				source.open();
				boolean received = firstEvent.await(10, TimeUnit.SECONDS);
				boolean completed = done.await(100, TimeUnit.SECONDS);
				assertTrue(received, "Timed out waiting for SSE events");
				assertTrue(completed, "Timed out waiting for SSE completion");

				assertFalse(events.isEmpty(), "SSE did not receive any events");
				InboundSseEvent event = events.getFirst();

				String data = event.readData(String.class, MediaType.TEXT_PLAIN_TYPE);
				assertNotNull(data);
				assertFalse(data.isBlank());

				boolean found = false;
				for (InboundSseEvent sseEvent : events)
				{
					if (sseEvent.getName().equalsIgnoreCase("Results"))
					{
						found = true;
						break;
					}
				}
				assertTrue(found);
			}
		}
	}

	private void importData(Optional<Path> endPath)
	{
		String workingDirectoryPath = Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), "opendcs-tests").toString();
		String currentDirectory = buildFilePath(workingDirectoryPath, "src", "test", "resources", "data", "Comps");
		File directory = new File(currentDirectory);

		try
		{
			TimeSeriesDb tsDb = getTsdb();
			try(TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO();
				SiteDAI siteDAO = tsDb.makeSiteDAO())
			{
				TsImporter importer = buildTsImporter(tsDb, tsDao, siteDAO);
				if(directory.exists() && directory.isDirectory())
				{
					File[] comps = directory.listFiles();
					if(comps != null)
					{
						for(File comp : comps)
						{
							if(comp.isDirectory())
							{
								for (File comp_data : comp.listFiles())
								{
									if (comp_data.isDirectory() && (!endPath.isPresent() || comp_data.toPath().endsWith(endPath.get())))
									{
										doImport(tsDb, comp_data, comp, importer, getConfig());
									}
								}
							}
						}
					}
				}
				else
				{
					log.error("Invalid directory: " + currentDirectory);
					throw new RuntimeException("Invalid directory: " + currentDirectory);
				}
			}
		}
		catch(Throwable e)
		{
			log.atError().setCause(e).log("Error getting TsImporter");
			throw new RuntimeException(e);
		}
	}

	private void doImport(TimeSeriesDb tsDb, File test, File comp, TsImporter importer, Configuration configuration)
	{
		for (File comp_data : test.listFiles())
		{
			// Process comp xml
			String name = comp_data.getName();
			if (name.contains("Comp.xml"))
			{
				log.info("Comps: " + comp_data.getAbsolutePath());
				String compstr = comp_data.getAbsolutePath();
				List<String> compxml =  Arrays.asList(compstr);
				ImportComp ic = new ImportComp(tsDb, true, false, compxml);
				ic.runApp();

				DbComputation testComp = null;
				try (ComputationDAI compdao = tsDb.makeComputationDAO())
				{
					testComp = compdao.getComputationByName(test.getName()+comp.getName());
					tsCompId = testComp.getId().getValue();
				}
				catch (NoSuchObjectException | DbIoException ex)
				{
					log.atError().setCause(ex).log("Error getting Computation: " + test.getName()+comp.getName());
					throw new RuntimeException(ex);
				}
			}
			else if (name.contains(".config"))
			{
				log.info("Has config: " + comp_data.getAbsolutePath());
				File configFile = new File(comp_data.getAbsolutePath());
				try (InputStream configStream = new FileInputStream(configFile)) {
					String firstLine = new BufferedReader(new InputStreamReader(configStream)).readLine();
					String keyword = "EnableOn:";
					if (firstLine != null && firstLine.contains(keyword)) {
						String substring = firstLine.substring(firstLine.indexOf(keyword) + keyword.length()).trim();
						final String testEngine = System.getProperty("opendcs.test.engine", "").trim();
						assumeFalse(!substring.equals(testEngine), "Test is disabled by config file for: " + substring);
					}
				}
				catch (IOException ex)
				{
					log.atError().setCause(ex).log("Error reading config file: " + comp_data.getAbsolutePath());
					throw new RuntimeException(ex);
				}
			}
			else if (name.endsWith(".sql"))
			{
				log.info("Found SQL file: " + comp_data.getAbsolutePath());
				try
				{
					executeSqlFile(configuration.getOpenDcsDatabase(), tsDb, comp_data);
				}
				catch (Throwable ex)
				{
					log.atError().setCause(ex).log("Error executing SQL file: " + comp_data.getAbsolutePath());
					throw new RuntimeException(ex);
				}
			}
		}

		try
		{
			List<CTimeSeries> inputTS = loadTSimport(tsDb, buildFilePath(test.getAbsolutePath(), "timeseries", "inputs"), importer);
			Collection<CTimeSeries> outputTS = loadTSimport(tsDb, buildFilePath(test.getAbsolutePath(), "timeseries", "outputs"), importer);
			expectedTsList = loadTSimport(tsDb, buildFilePath(test.getAbsolutePath(), "timeseries", "expectedOutputs"), importer);

			loadRatingimport(tsDb, buildFilePath(test.getAbsolutePath(), "rating"));
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Error loading TS/Rating/Screenings");
			throw new RuntimeException(ex);
		}
	}

	private TsImporter buildTsImporter(TimeSeriesDb tsDb, TimeSeriesDAI tsDao, SiteDAI siteDAO)
	{
		DecodesSettings settings = DecodesSettings.instance();
		return new TsImporter(TimeZone.getTimeZone("UTC"), settings.siteNameTypePreference, (tsIdStr) ->
		{
			try
			{
				return tsDao.getTimeSeriesIdentifier(tsIdStr);
			}
			catch(Exception ex)
			{
				log.warn("No existing time series. Will attempt to create.");

				try
				{
					TimeSeriesIdentifier tsId = tsDb.makeEmptyTsId();
					tsId.setUniqueString(tsIdStr);
					Site site = tsDb.getSiteById(siteDAO.lookupSiteID(tsId.getSiteName()));
					if(site == null)
					{
						site = new Site();
						site.addName(new SiteName(site, Constants.snt_CWMS, tsId.getSiteName()));
						siteDAO.writeSite(site);
					}
					tsId.setSite(site);

					log.info("Calling createTimeSeries");
					tsDao.createTimeSeries(tsId);
					log.info("After createTimeSeries, ts key = {}", tsId.getKey());
					return tsId;
				}
				catch(Exception ex2)
				{
					throw new DbIoException(String.format("No such time series and cannot create for '%s'", tsIdStr), ex);
				}
			}
		});
	}

	private static String buildFilePath(String... parts) {
		// Start with the first part
		Path path = Paths.get(parts[0]);

		// Append all the other parts using resolve() so it's platform independent
		for (int i = 1; i < parts.length; i++) {
			path = path.resolve(parts[i]);
		}

		// Return the platform-specific path as a string
		return path.toString();
	}

	private void loadScreenings(Configuration configuration, String screeningsFile) throws Exception
	{
		File folderTS = new File(screeningsFile);
		if (!folderTS.exists())
		{
			return;
		}
		File log = new File(configuration.getUserDir().getParentFile(), "screenings-import-"+ folderTS.getName()+".log");
		Programs.ImportScreenings(log, configuration.getPropertiesFile(), environment, exit, screeningsFile);
	}

	private ArrayList<CTimeSeries> loadTSimport(TimeSeriesDb tsDb, String folderTSstr, TsImporter importer)
			throws Exception
	{
		File folderTS = new File(folderTSstr);
		ArrayList<CTimeSeries> fullTs = new ArrayList<CTimeSeries>();
		if (!folderTS.exists()){
			return fullTs;
		}
		for (File tsfiles : folderTS.listFiles())
		{
			try(TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO())
			{
				Collection<CTimeSeries> allTs = importer.readTimeSeriesFile(tsfiles.getAbsolutePath());
				for (CTimeSeries tsIn: allTs)
				{
					log.info("load: " + tsIn.getDisplayName());
					log.info("Saving {}", tsIn.getTimeSeriesIdentifier());

					tsDao.saveTimeSeries(tsIn);
					FailableResult<TimeSeriesIdentifier, TsdbException> tsIdSavedResult = tsDao.findTimeSeriesIdentifier(tsIn.getTimeSeriesIdentifier().getUniqueString());
					assertTrue(tsIdSavedResult.isSuccess(), "Time series was not correctly saved.");
				}
				fullTs.addAll(allTs);
			}
		}
		return fullTs;
	}

	private void loadRatingimport(TimeSeriesDb tsDb, String folderRatingStr) throws Exception
	{
		File folderTS = new File(folderRatingStr);
		if (!folderTS.exists())
			return;
		try (CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsDb)){
			crd.setUseReference(false);
			for (File tsfiles : folderTS.listFiles())
			{
				try
				{
					String xml = FileUtil.getFileContents(tsfiles);
					crd.importXmlToDatabase(xml);
				}
				catch(Exception ex)
				{
					log.error(ex.getMessage(), ex);
					throw new RuntimeException("Error importing rating file: " + tsfiles.getAbsolutePath(), ex);
				}
			}
		}
	}

	/**
	 * Execute SQL file in the database for test setup
	 * @param sqlFile The SQL file to execute
	 */
	private void executeSqlFile(OpenDcsDatabase db, TimeSeriesDb tsDb, File sqlFile)
	{
		if (db == null)
		{
			log.warn("OpenDcsDatabase not available, skipping SQL file execution: " + sqlFile.getName());
			return;
		}

		try
		{
			// Read the SQL file content
			String sqlContent = new String(Files.readAllBytes(sqlFile.toPath()), StandardCharsets.UTF_8);

			// Replace office ID placeholder if this is a CWMS database
			if (tsDb instanceof CwmsTimeSeriesDb)
			{
				CwmsTimeSeriesDb cwmsDb = (CwmsTimeSeriesDb) tsDb;
				String officeId = cwmsDb.getDbOfficeId();
				sqlContent = sqlContent.replace("DEFAULT_OFFICE", officeId);
			}

			// Execute the SQL using a transaction
			try (DataTransaction tx = db.newTransaction())
			{
				Connection conn = tx.connection(Connection.class)
						.orElseThrow(() -> new RuntimeException("JDBC Connection not available in this transaction."));

				// Use CallableStatement to execute the SQL (handles PL/SQL blocks properly)
				try (CallableStatement stmt = conn.prepareCall(sqlContent))
				{
					log.info("Executing SQL from file: " + sqlFile.getName());
					log.debug("SQL Content: " + sqlContent.substring(0, Math.min(200, sqlContent.length())) + "...");
					stmt.execute();
				}
				log.info("Successfully executed SQL file: " + sqlFile.getName());
			}
		}
		catch (Exception ex)
		{
			log.error("Failed to execute SQL file " + sqlFile.getName() + ": " + ex.getMessage(), ex);
			// Don't fail the test, just log the error
		}
	}
}
