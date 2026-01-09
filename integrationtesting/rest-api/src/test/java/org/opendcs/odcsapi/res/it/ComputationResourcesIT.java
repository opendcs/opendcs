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

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import decodes.cwms.CwmsTsId;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.VarFlags;
import ilex.var.TimedVariable;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.odcsapi.beans.ApiAlgorithm;
import org.opendcs.odcsapi.beans.ApiCompParm;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.fixtures.DatabaseSetupExtension;
import org.opendcs.odcsapi.util.ApiConstants;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputationResourcesIT extends BaseIT
{
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static SessionFilter sessionFilter;
	private TimeSeriesIdentifier tsId;
	private TimeSeriesIdentifier tsId2;
	private Long compId;
	private Long siteId;
	private Long appId;
	private Long algId;

	@BeforeEach
	void init() throws Exception
	{
		setUpCreds();
		sessionFilter = new SessionFilter();
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

		logout(sessionFilter);
	}

	@TestTemplate
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
		List<Map<String, Object>> actualList = response.body().jsonPath().getList("");
		assertFalse(actualList.isEmpty());
		Map<String, Object> actualItem = actualList.get(0);
		assertEquals(expectedJson.getString("name"), actualItem.get("name"));
		assertEquals(expectedJson.getString("description"), actualItem.get("description"));
		assertEquals(expectedJson.getBoolean("enabled"), actualItem.get("enabled"));
		assertEquals(expectedJson.getString("groupName"), actualItem.get("groupName"));
		assertEquals(expectedJson.getString("algorithmName"), actualItem.get("algorithmName"));
		assertEquals(expectedJson.getString("processName"), actualItem.get("processName"));
	}

	@TestTemplate
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

	@TestTemplate
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

	@TestTemplate
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

	@TestTemplate
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
		String tsids = "Test Site.Stor-AuthorizedCon.Inst.~6Hours.0.CENWP-COMPUTED-FCST,"
				+ "Test Site.Stor-FilledCon.Inst.~6Hours.0.CENWP-COMPUTED-FCST";

		String organization = DatabaseSetupExtension.getOrganization();

		ClientRequestFilter auth = ctx ->
		{
			ctx.getHeaders().putSingle(ApiConstants.ORGANIZATION_HEADER, organization);
			ctx.getHeaders().putSingle("Cookie", getCookie());
		};

		WebTarget target;
		try (Client client = ClientBuilder.newBuilder()
				.register(SseEventSource.class)
				.register(auth)
				.build())
		{
			URI baseURI = URI.create(String.format("%s:%d/%s", RestAssured.baseURI, RestAssured.port, RestAssured.basePath));
			target = client.target(baseURI)
					.path("runcomputation")
					.queryParam("computationid", compId)
					.queryParam("tsids", tsids);

			List<InboundSseEvent> events = new CopyOnWriteArrayList<>();
			CountDownLatch latch = new CountDownLatch(5);

			try(SseEventSource source = SseEventSource.target(target).build())
			{
				source.register(
					(InboundSseEvent event) ->
					{
						events.add(event);
						latch.countDown();
					},
					(Throwable error) -> {
						throw new AssertionError("SSE Error: " + error.getMessage());
					},
					() -> System.out.println("SSE completed")
				);
				source.open();
				assertTrue(source.isOpen());
				boolean received = latch.await(60, TimeUnit.SECONDS);
				assertTrue(received, "Timed out waiting for SSE events");
				source.close();
				assertFalse(source.isOpen());

				assertFalse(events.isEmpty(), "SSE did not receive any events");
				InboundSseEvent event = events.getFirst();

				String data = event.readData(String.class, MediaType.TEXT_PLAIN_TYPE);
				assertNotNull(data);
				assertFalse(data.isBlank());
			}
		}
	}
}
