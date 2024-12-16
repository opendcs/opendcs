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

package org.opendcs.odcsapi.fixtures;

import javax.servlet.http.HttpServletResponse;

import io.restassured.RestAssured;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.PreconditionViolationException;
import org.opendcs.fixtures.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class DatabaseSetupExtension implements BeforeEachCallback
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSetupExtension.class);
	private final Configuration config;
	private TomcatServer tomcatServer;

	public DatabaseSetupExtension(Configuration config)
	{
		this.config = config;
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception
	{
		String warContext = System.getProperty("warContext", "odcsapi");
		if(tomcatServer == null)
		{
			tomcatServer = startTomcat(warContext);
		}
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = tomcatServer.getPort();
		RestAssured.basePath = warContext;
	}

	private TomcatServer startTomcat(String warContext) throws Exception
	{
		SystemExit exit = new SystemExit();
		EnvironmentVariables environment = new EnvironmentVariables();
		SystemProperties properties = new SystemProperties();
		config.start(exit, environment, properties);
		environment.getVariables().forEach(System::setProperty);
		TomcatServer tomcat = new TomcatServer("build/tomcat", 0, warContext);
		tomcat.start();
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = tomcat.getPort();
		RestAssured.basePath = warContext;
		healthCheck();
		return tomcat;
	}

	private static void healthCheck() throws InterruptedException
	{
		int attempts = 0;
		int maxAttempts = 15;
		for(; attempts < maxAttempts; attempts++)
		{
			try
			{
				given()
						.when()
						.delete("/logout")
						.then()
						.assertThat()
						.statusCode(is(HttpServletResponse.SC_NO_CONTENT));
				LOGGER.atInfo().log("Server is up!");
				break;
			}
			catch(Throwable e)
			{
				LOGGER.atInfo().log("Waiting for the server to start...");
				Thread.sleep(100);//NOSONAR
			}
		}
		if(attempts == maxAttempts)
		{
			throw new PreconditionViolationException("Server didn't start in time...");
		}
	}
}