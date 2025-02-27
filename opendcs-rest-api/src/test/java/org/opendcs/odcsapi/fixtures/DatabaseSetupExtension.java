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

package org.opendcs.odcsapi.fixtures;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;

import io.restassured.RestAssured;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.PreconditionViolationException;
import org.opendcs.fixtures.configuration.Configuration;
import org.opendcs.odcsapi.hydrojson.DbInterface;
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
	private static DbType currentDbType;
	private static Configuration currentConfig;
	private static TomcatServer currentTomcat;
	private final Configuration config;
	private final DbType dbType;
	private TomcatServer tomcatServer;

	public DatabaseSetupExtension(Configuration config, DbType dbType)
	{
		this.config = config;
		this.dbType = dbType;
		currentConfig = config;
	}

	public static DbType getCurrentDbType()
	{
		return currentDbType;
	}

	public static Configuration getCurrentConfig()
	{
		return currentConfig;
	}

	public static TomcatServer getCurrentTomcat()
	{
		return currentTomcat;
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
		currentDbType = dbType;
		currentTomcat = tomcatServer;
	}

	private TomcatServer startTomcat(String warContext) throws Exception
	{
		SystemExit exit = new SystemExit();
		EnvironmentVariables environment = new EnvironmentVariables();
		SystemProperties properties = new SystemProperties();
		try
		{
			config.start(exit, environment, properties);
		}
		catch(Exception ex)
		{
			if(System.getProperty("testcontainer.cwms.bypass.url") == null)
			{
				throw ex;
			}
		}
		environment.getVariables().forEach(System::setProperty);
		if(dbType == DbType.CWMS)
		{
			DbInterface.isCwms = true;
			System.setProperty("DB_DRIVER_CLASS", "oracle.jdbc.driver.OracleDriver");
			System.setProperty("DB_DATASOURCE_CLASS", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			String dbOffice = System.getProperty("DB_OFFICE");
			String initScript = String.format("BEGIN cwms_ccp_vpd.set_ccp_session_ctx(cwms_util.get_office_code('%s'), 2, '%s' ); END;", dbOffice, dbOffice);
			System.setProperty("DB_CONNECTION_INIT", initScript);
			DbInterface.decodesProperties.setProperty("CwmsOfficeId", dbOffice);
			DbInterface.setDatabaseType("cwms");
		}
		else
		{
			DbInterface.isCwms = false;
			System.setProperty("DB_DRIVER_CLASS", "org.postgresql.Driver");
			System.setProperty("DB_DATASOURCE_CLASS", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			System.setProperty("DB_CONNECTION_INIT", "SELECT 1");
		}
		setupClientUser();
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

	public static void loadXMLDataIntoDb(String[] files) throws Exception
	{
		String[] filePaths = new String[files.length];
		for (int i = 0; i < files.length; i++)
		{
			filePaths[i] = String.format("%s%s%s",
					System.getProperty("user.dir"),
					"/src/test/resources/org/opendcs/odcsapi/res/it/",
					files[i]);
		}
		currentConfig.loadXMLData(filePaths, new SystemExit(), new SystemProperties());
	}

	private void setupClientUser()
	{
		if(dbType == DbType.CWMS)
		{
			String userPermissions = "begin execute immediate 'grant web_user to " + System.getProperty("DB_USERNAME") + "'; end;";
			String dbOffice = System.getProperty("DB_OFFICE");
			String setWebUserPermissions = "begin\n" +
					"   cwms_sec.add_user_to_group(?, 'CWMS User Admins',?) ;\n" +
					"   commit;\n" +
					"end;";
			try(Connection connection = DriverManager.getConnection(System.getProperty("DB_URL"), "CWMS_20",
					System.getProperty("DB_PASSWORD"));
				PreparedStatement stmt1 = connection.prepareStatement(userPermissions);
				PreparedStatement stmt2 = connection.prepareStatement(setWebUserPermissions))
			{
				stmt1.executeQuery();
				stmt2.setString(1, System.getProperty("DB_USERNAME"));
				stmt2.setString(2, dbOffice);
				stmt2.executeQuery();
			}
			catch(SQLException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}