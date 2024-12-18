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

package org.opendcs.odcsapi.res.it;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;
import org.opendcs.odcsapi.fixtures.DatabaseSetupExtension;
import org.opendcs.odcsapi.fixtures.DbType;
import org.opendcs.odcsapi.res.ObjectMapperContextResolver;
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
		if(DatabaseSetupExtension.getCurrentDbType() == DbType.OPEN_TSDB)
		{
			//TODO - create a test user and use its credentials
			Credentials credentials = new Credentials();
			credentials.setUsername(System.getProperty("DB_USERNAME"));
			credentials.setPassword(System.getProperty("DB_PASSWORD"));
			given()
				.log().ifValidationFails(LogDetail.ALL, true)
				.accept("application/json")
				.contentType("application/json")
				.body(credentials)
				.filter(sessionFilter)
			.when()
				.redirects().follow(true)
				.redirects().max(3)
				.post("credentials")
			.then()
				.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
				.statusCode(is(HttpServletResponse.SC_OK))
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
}
