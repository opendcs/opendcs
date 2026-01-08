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

package org.opendcs.odcsapi.sec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.specification.RequestSpecification;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.odcsapi.fixtures.DatabaseContextProvider;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(DatabaseContextProvider.class)
final class AuthorizationTestIT
{

	@TestTemplate
	void unauthorizedAccessShouldReturn401()
	{
		assertAll(getEndpoints().map(endpoint -> () ->
		{
			String path = endpoint.path;
			PathItem.HttpMethod method = endpoint.method;
			String acceptMediaType = endpoint.acceptMediaType;
			String contentMediaType = endpoint.contentMediaType;
			RequestSpecification spec = given()
					.accept(acceptMediaType)
					.contentType(contentMediaType)
					//ensures unauthorized session
					.filter(new SessionFilter())
					.when();
			var response = switch(method)
			{
				case GET -> spec.get(path);
				case POST -> spec.post(path);
				case PUT -> spec.put(path);
				case DELETE -> spec.delete(path);
				default -> throw new IllegalArgumentException("Unsupported method: " + method);
			};
			response.then()
					.log().ifValidationFails(LogDetail.ALL, true)
					.assertThat()
					.statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
		}));
	}


	private static Stream<Endpoint> getEndpoints()
	{
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		OpenAPI api = parser.read(RestAssured.baseURI + ":" + RestAssured.port + "/" + RestAssured.basePath + "/openapi.json");
		Paths paths = api.getPaths();
		return paths.entrySet().stream()
				.filter(e -> !e.getKey().equals("/credentials"))
				.filter(e -> !e.getKey().equals("/logout"))
				.filter(e -> !e.getKey().equals("/organizations"))
				.filter(e -> !e.getKey().startsWith("/health"))
				.flatMap(e ->
						e.getValue().readOperationsMap().entrySet().stream().map(opEntry ->
						{
							String contentType = jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
							RequestBody requestBody = opEntry.getValue().getRequestBody();
							if(requestBody != null)
							{
								contentType = requestBody.getContent().keySet().iterator().next();
							}
							List<ApiResponse> acceptTypes = new ArrayList<>(opEntry.getValue().getResponses().values());
							Content acceptContent = acceptTypes.get(0).getContent();
							String acceptType = jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
							if(acceptContent != null)
							{
								acceptType = acceptContent.keySet().iterator().next();
							}
							return new Endpoint(e.getKey(), opEntry.getKey(), contentType, acceptType);
						}));
	}

	private record Endpoint(String path, PathItem.HttpMethod method, String contentMediaType, String acceptMediaType)
	{

		@Override
		public String toString()
		{
			return path + " " + method;
		}
	}
}
