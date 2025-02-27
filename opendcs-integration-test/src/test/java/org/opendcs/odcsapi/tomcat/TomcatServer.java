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

package org.opendcs.odcsapi.tomcat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.junit.platform.commons.PreconditionViolationException;
import org.opendcs.fixtures.configuration.Configuration;
import org.opendcs.fixtures.configuration.ConfigurationProvider;
import org.opendcs.fixtures.configuration.cwms.CwmsOracleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;


/**
 * Tomcat server for ./gradle run and
 * Integration tests
 */
public final class TomcatServer implements AutoCloseable
{
	private static final Logger logger = LoggerFactory.getLogger(TomcatServer.class);
	private final Tomcat tomcatInstance;

	/**
	 * Setups the baseline for tomcat to run.
	 *
	 * @param baseDir set to the CATALINA_BASE directory the build has set up
	 * @param port    Network port to listen on
	 */
	public TomcatServer(String baseDir, String port, String restWar, String guiWar) throws IOException
	{
		tomcatInstance = new Tomcat();
		tomcatInstance.setBaseDir(baseDir);
		Connector connector = new Connector();
		connector.setSecure(true);
		connector.setScheme("https");
		connector.setPort(Integer.parseInt(port));
		tomcatInstance.setConnector(connector);
		Host host = tomcatInstance.getHost();
		host.setAppBase("webapps");
		String catalinaBase = tomcatInstance.getServer().getCatalinaBase().toString();
		Files.createDirectories(Paths.get(catalinaBase, "temp"));
		Files.createDirectories(Paths.get(catalinaBase, "webapps"));
		tomcatInstance.setPort(Integer.parseInt(port));
		tomcatInstance.setSilent(false);
		tomcatInstance.enableNaming();
		StandardContext restApiContext = (StandardContext) tomcatInstance.addWebapp("/odcsapi", restWar);
		restApiContext.setDelegate(true);
		restApiContext.setParentClassLoader(TomcatServer.class.getClassLoader());
		restApiContext.getPipeline().addValve(new TestSingleSignOn());
		restApiContext.setReloadable(true);
		restApiContext.setPrivileged(true);
		if(System.getProperty("DB_OFFICE") != null)
		{
			restApiContext.removeParameter("opendcs.rest.api.cwms.office");
			restApiContext.addParameter("opendcs.rest.api.cwms.office", System.getProperty("DB_OFFICE"));
		}

		StandardContext guiContext = (StandardContext) tomcatInstance.addWebapp("/", guiWar);
		guiContext.setDelegate(true);
		guiContext.setParentClassLoader(TomcatServer.class.getClassLoader());
		guiContext.setReloadable(true);
		guiContext.setPrivileged(true);

		Context context = tomcatInstance.addContext("/sso", new File(".").getAbsolutePath());
		Tomcat.addServlet(context, "sso", new SsoServlet());
		context.addServletMappingDecoded("/*", "sso");
	}

	public void start(String dbType) throws Exception
	{
		tomcatInstance.start();
		if(CwmsOracleConfiguration.NAME.equals(dbType))
		{
			FileUtils.copyFile(Paths.get("src/test/resources/rest-api/conf/cwms-web.xml").toFile(),
					Paths.get("build/tomcat/webapps/odcsapi/WEB-INF/web.xml").toFile(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			FileUtils.copyFile(Paths.get("src/test/resources/web-client/conf/cwms-web.xml").toFile(),
					Paths.get("build/tomcat/webapps/ROOT/WEB-INF/web.xml").toFile(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		}
		else
		{
			FileUtils.copyFile(Paths.get("src/test/resources/rest-api/conf/opentsdb-web.xml").toFile(),
					Paths.get("build/tomcat/webapps/odcsapi/WEB-INF/web.xml").toFile(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			FileUtils.copyFile(Paths.get("src/test/resources/web-client/conf/opentsdb-web.xml").toFile(),
					Paths.get("build/tomcat/webapps/ROOT/WEB-INF/web.xml").toFile(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		}
		logger.info("Tomcat listening at http://localhost:{}", tomcatInstance.getConnector().getLocalPort());
	}

	public int getPort()
	{
		return tomcatInstance.getConnector().getLocalPort();
	}

	/**
	 * Used for the ./gradlew run command.
	 * Unit tests only need to call start and move on.
	 */
	public void await()
	{
		tomcatInstance.getServer().await();
	}

	/**
	 * Stops the instance of tomcat, including destroying the JNDI context.
	 *
	 * @throws LifecycleException any error in the stop sequence
	 */
	@Override
	public void close() throws LifecycleException
	{
		tomcatInstance.stop();
	}

	/**
	 * arg[0] - the CATALINA_BASE directory you've setup
	 * arg[1] - full path to the war file generated by this build script
	 * arg[2] - name to use for this instance. See constructor for guidance
	 *
	 * @param args standard argument list
	 */
	public static void main(String[] args)
	{
		try
		{
			SLF4JBridgeHandler.removeHandlersForRootLogger();
			SLF4JBridgeHandler.install();
			String baseDir = args[0];
			String port = args[1];
			String restWar = args[2];
			String guiWar = args[3];
			String dbType = args[4];
			setupDb(dbType);
			TomcatServer tomcat = new TomcatServer(baseDir, port, restWar, guiWar);
			tomcat.start(dbType);
			tomcat.await();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void setupDb(String dbType)
	{
		ConfigurationProvider provider = getProvider(dbType);
		try
		{
			File tmp = Files.createTempDirectory("configs-" + provider.getImplementation()).toFile();
			Configuration config = provider.getConfig(tmp);
			if(isBypass())
			{
				setupDbForBypass(dbType);
			}
			else
			{
				startDbContainer(config, dbType);
			}
		}
		catch(Exception ex)
		{
			throw new PreconditionViolationException("Error creating configuration for db: " + dbType, ex);
		}
	}

	private static void setupDbForBypass(String dbType)
	{
		if(CwmsOracleConfiguration.NAME.equals(dbType))
		{
			System.setProperty("DB_DRIVER_CLASS", "oracle.jdbc.driver.OracleDriver");
			System.setProperty("DB_DATASOURCE_CLASS", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			String dbOffice = System.getProperty("testcontainer.cwms.bypass.office.id");
			String initScript = String.format("BEGIN cwms_ccp_vpd.set_ccp_session_ctx(cwms_util.get_office_code('%s'), 2, '%s' ); END;", dbOffice, dbOffice);
			System.setProperty("DB_OFFICE", dbOffice);
			System.setProperty("DB_CONNECTION_INIT", initScript);
			System.setProperty("DB_VALIDATION_QUERY", "SELECT 1 FROM dual");
			System.setProperty("DB_URL", System.getProperty("testcontainer.cwms.bypass.url"));
			System.setProperty("DB_USERNAME", System.getProperty("testcontainer.cwms.bypass.office.eroc") + "WEBTEST");
			System.setProperty("DB_PASSWORD", System.getProperty("testcontainer.cwms.bypass.cwms.pass"));
		}
		else
		{

			System.setProperty("DB_DRIVER_CLASS", "org.postgresql.Driver");
			System.setProperty("DB_DATASOURCE_CLASS", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			System.setProperty("DB_CONNECTION_INIT", "SELECT 1");
			System.setProperty("DB_VALIDATION_QUERY", "SELECT 1");
			System.setProperty("DB_URL", System.getProperty("testcontainer.opentsdb.bypass.url"));
			System.setProperty("DB_USERNAME", System.getProperty("testcontainer.opentsdb.bypass.username"));
			System.setProperty("DB_PASSWORD", System.getProperty("testcontainer.opentsdb.bypass.pass"));
		}
	}

	private static boolean isBypass()
	{
		return System.getProperty("testcontainer.cwms.bypass.url") != null
				|| System.getProperty("testcontainer.opentsdb.bypass.url") != null;
	}

	private static ConfigurationProvider getProvider(String dbType)
	{
		ServiceLoader<ConfigurationProvider> loader = ServiceLoader.load(ConfigurationProvider.class);
		Iterator<ConfigurationProvider> configs = loader.iterator();

		ConfigurationProvider configProvider = null;
		while(configs.hasNext())
		{
			ConfigurationProvider configProviderTmp = configs.next();
			if(configProviderTmp.getImplementation().equals(dbType))
			{
				configProvider = configProviderTmp;
			}
		}
		if(configProvider == null)
		{
			throw new PreconditionViolationException("Invalid dbtype: " + dbType);
		}
		return configProvider;
	}

	private static void startDbContainer(Configuration config, String dbType) throws Exception
	{
		SystemExit exit = new SystemExit();
		EnvironmentVariables environment = new EnvironmentVariables();
		SystemProperties properties = new SystemProperties();
		config.start(exit, environment, properties);
		environment.getVariables().forEach(System::setProperty);
		if(CwmsOracleConfiguration.NAME.equals(dbType))
		{
			System.setProperty("DB_DRIVER_CLASS", "oracle.jdbc.driver.OracleDriver");
			System.setProperty("DB_DATASOURCE_CLASS", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			String dbOffice = System.getProperty("DB_OFFICE");
			String initScript = String.format("BEGIN cwms_ccp_vpd.set_ccp_session_ctx(cwms_util.get_office_code('%s'), 2, '%s' ); END;", dbOffice, dbOffice);
			System.setProperty("DB_CONNECTION_INIT", initScript);
			System.setProperty("DB_VALIDATION_QUERY", "SELECT 1 FROM dual");
		}
		else
		{
			System.setProperty("DB_DRIVER_CLASS", "org.postgresql.Driver");
			System.setProperty("DB_DATASOURCE_CLASS", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			System.setProperty("DB_CONNECTION_INIT", "SELECT 1");
			System.setProperty("DB_VALIDATION_QUERY", "SELECT 1");
		}
		setupClientUser(dbType);
	}


	private static void setupClientUser(String dbType) throws Exception
	{
		if(CwmsOracleConfiguration.NAME.equals(dbType))
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
				//No-op
			}
		}
	}

}
