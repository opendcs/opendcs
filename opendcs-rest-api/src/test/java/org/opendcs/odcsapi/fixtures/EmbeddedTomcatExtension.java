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
import io.restassured.filter.session.SessionFilter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public final class EmbeddedTomcatExtension implements BeforeAllCallback, AfterAllCallback
{
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(EmbeddedTomcatExtension.class);
	protected static TomcatServer opendcsInstance;
	protected SessionFilter sessionFilter;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception
	{

		String warContext = System.getProperty("warContext", "odcsapi");
		opendcsInstance = new TomcatServer("build/tomcat",0, warContext);
		opendcsInstance.start();
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = opendcsInstance.getPort();
		RestAssured.basePath = warContext;
		healthCheck();
		sessionFilter = new SessionFilter();
	}

	public static TomcatServer getOpendcsInstance()
	{
		return opendcsInstance;
	}

	private static void healthCheck() throws InterruptedException {
		int attempts = 0;
		int maxAttempts = 15;
		for (; attempts < maxAttempts; attempts++) {
			try {
				given()
						.when()
						.delete("/logout")
						.then()
						.assertThat()
						.statusCode(is(HttpServletResponse.SC_NO_CONTENT));
				LOGGER.atInfo().log("Server is up!");
				break;
			} catch (Throwable e) {
				LOGGER.atInfo().log("Waiting for the server to start...");
				Thread.sleep(100);
			}
		}
		if (attempts == maxAttempts) {
			throw new IllegalStateException("Server didn't start in time...");
		}
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception
	{
		opendcsInstance.stop();
	}
}
