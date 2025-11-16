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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.jdbc.pool.DataSourceFactory;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.fixtures.configurations.cwms.CwmsOracleConfiguration;
import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.fixtures.spi.ConfigurationProvider;
import org.slf4j.Logger;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;


/**
 * Tomcat server for ./gradle run and
 * Integration tests
 */
public final class TomcatServer implements AutoCloseable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String DB_OFFICE = "DB_OFFICE";
	public static final String DB_DRIVER_CLASS = "DB_DRIVER_CLASS";
	public static final String DB_DATASOURCE_CLASS = "DB_DATASOURCE_CLASS";
	public static final String DB_CONNECTION_INIT = "DB_CONNECTION_INIT";
	public static final String DB_VALIDATION_QUERY = "DB_VALIDATION_QUERY";
	public static final String DB_URL = "DB_URL";
	public static final String DB_USERNAME = "DB_USERNAME";
	public static final String DB_PASSWORD = "DB_PASSWORD";
	private final Tomcat tomcatInstance;
	private final Supplier<Manager> sessionManager;

	/**
	 * Setups the baseline for tomcat to run.
	 *
	 * @param baseDir set to the CATALINA_BASE directory the build has set up
	 * @param port    Network port to listen on
	 */
	public TomcatServer(String baseDir, int port, String restWar, String guiWar) throws IOException
	{
		tomcatInstance = new Tomcat();
		tomcatInstance.setBaseDir(baseDir);
		Connector connector = new Connector();
		connector.setSecure(true);
		connector.setScheme("https");
		connector.setPort(port);
		tomcatInstance.setConnector(connector);
		Host host = tomcatInstance.getHost();
		host.setAppBase("webapps");
		String catalinaBase = tomcatInstance.getServer().getCatalinaBase().toString();
		Files.createDirectories(Paths.get(catalinaBase, "temp"));
		Files.createDirectories(Paths.get(catalinaBase, "webapps"));
		tomcatInstance.setPort(port);
		tomcatInstance.setSilent(false);
		tomcatInstance.enableNaming();
		StandardContext restApiContext = (StandardContext) tomcatInstance.addWebapp("/odcsapi", restWar);
		restApiContext.setDelegate(true);
		restApiContext.setParentClassLoader(TomcatServer.class.getClassLoader());
		restApiContext.setReloadable(true);
		restApiContext.setPrivileged(true);
		sessionManager = restApiContext::getManager;
		if(System.getProperty(DB_OFFICE) != null)
		{
			restApiContext.removeParameter("opendcs.rest.api.cwms.office");
			restApiContext.addParameter("opendcs.rest.api.cwms.office", System.getProperty(DB_OFFICE));
		}

		StandardContext guiContext = (StandardContext) tomcatInstance.addWebapp("", guiWar);
		guiContext.setDelegate(true);
		guiContext.setParentClassLoader(TomcatServer.class.getClassLoader());
		guiContext.setReloadable(true);
		guiContext.setPrivileged(true);
	}

	public void start() throws LifecycleException
	{
		tomcatInstance.start();
		log.info("Tomcat listening at http://localhost:{}", tomcatInstance.getConnector().getLocalPort());
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
			String baseDir = args[0];
			String port = args[1];
			String restWar = args[2];
			String guiWar = args[3];
			String dbType = args[4];
			setupDb(dbType);
			try(TomcatServer tomcat = new TomcatServer(baseDir, Integer.parseInt(port), restWar, guiWar))
			{
				tomcat.start();
				tomcat.await();
			}
		}
		catch(RuntimeException | LifecycleException | IOException ex)
		{
			log.atError().setCause(ex).log("Error starting tomcat", ex);
			System.exit(-1);
		}
	}

	public static Configuration setupDb(String dbType)
	{
		ConfigurationProvider provider = getProvider(dbType);
		try
		{
			File tmp = Files.createTempDirectory("configs-" + provider.getImplementation()).toFile();//NOSONAR
			Configuration config = provider.getConfig(tmp);
			if(isBypass())
			{
				setupDbForBypass(dbType);
			}
			else
			{
				startDbContainer(config, dbType);
			}
			return config;
		}
		catch(Exception ex)
		{
			throw new IllegalStateException("Error creating configuration for db: " + dbType, ex);
		}
	}

	private static void setupDbForBypass(String dbType)
	{
		if(CwmsOracleConfiguration.NAME.equals(dbType))
		{
			System.setProperty(DB_DRIVER_CLASS, "oracle.jdbc.driver.OracleDriver");
			System.setProperty(DB_DATASOURCE_CLASS, DataSourceFactory.class.getName());
			String dbOffice = System.getProperty("testcontainer.cwms.bypass.office.id");
			String initScript = String.format("BEGIN cwms_ccp_vpd.set_ccp_session_ctx(cwms_util.get_office_code('%s'), 2, '%s' ); END;", dbOffice, dbOffice);
			System.setProperty(DB_OFFICE, dbOffice);
			System.setProperty(DB_CONNECTION_INIT, initScript);
			System.setProperty(DB_VALIDATION_QUERY, "SELECT 1 FROM dual");
			System.setProperty(DB_URL, System.getProperty("testcontainer.cwms.bypass.url"));
			System.setProperty(DB_USERNAME, System.getProperty("testcontainer.cwms.bypass.office.eroc") + "WEBTEST");
			System.setProperty(DB_PASSWORD, System.getProperty("testcontainer.cwms.bypass.cwms.pass"));
		}
		else
		{
			String validationQuery = "SELECT 1";
			System.setProperty(DB_DRIVER_CLASS, "org.postgresql.Driver");
			System.setProperty(DB_DATASOURCE_CLASS, DataSourceFactory.class.getName());
			System.setProperty(DB_CONNECTION_INIT, validationQuery);
			System.setProperty(DB_VALIDATION_QUERY, validationQuery);
			System.setProperty(DB_URL, System.getProperty("testcontainer.opentsdb.bypass.url"));
			System.setProperty(DB_USERNAME, System.getProperty("testcontainer.opentsdb.bypass.username"));
			System.setProperty(DB_PASSWORD, System.getProperty("testcontainer.opentsdb.bypass.pass"));
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
			throw new IllegalArgumentException("Invalid dbtype: " + dbType);
		}
		return configProvider;
	}

	private static void startDbContainer(Configuration config, String dbType) throws Exception
	{
		SystemExit exit = new SystemExit();
		EnvironmentVariables environment = new EnvironmentVariables();
		SystemProperties properties = new SystemProperties();
		config.start(exit, environment, properties);
		try (DataTransaction tx = config.getOpenDcsDatabase().newTransaction();
			 Connection conn = tx.connection(Connection.class).orElseThrow();
			 PreparedStatement stmt = conn.prepareStatement("insert into tsdb_properties(prop_name, prop_value) values (?,?)"))
		{
			stmt.setString(1, "EditDatabaseType");
			stmt.setString(2, dbType);
		}
		catch (Throwable ex)
		{
			throw new OpenDcsDataException("Unable to set database type property.", ex);
		}
		environment.getVariables().forEach(System::setProperty);
		config.getEnvironment().forEach((key, value) -> System.setProperty(key.toString(), value.toString()));
		if(CwmsOracleConfiguration.NAME.equals(dbType))
		{
			System.setProperty(DB_DRIVER_CLASS, "oracle.jdbc.driver.OracleDriver");
			System.setProperty(DB_DATASOURCE_CLASS, "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			String dbOffice = System.getProperty(DB_OFFICE);
			String initScript = String.format("BEGIN cwms_ccp_vpd.set_ccp_session_ctx(cwms_util.get_office_code('%s'), 2, '%s' ); END;", dbOffice, dbOffice);
			System.setProperty(DB_CONNECTION_INIT, initScript);
			System.setProperty(DB_VALIDATION_QUERY, "SELECT 1 FROM dual");
		}
		else
		{
			String validationQuery = "SELECT 1";
			System.setProperty(DB_DRIVER_CLASS, "org.postgresql.Driver");
			System.setProperty(DB_DATASOURCE_CLASS, "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			System.setProperty(DB_CONNECTION_INIT, validationQuery);
			System.setProperty(DB_VALIDATION_QUERY, validationQuery);
		}
		setupClientUser(dbType);
	}


	private static void setupClientUser(String dbType)
	{
		if(CwmsOracleConfiguration.NAME.equals(dbType))
		{
			// I have no idea why this is suddenly required but it was also affecting operations in
			// runtime test environments where the required entries weren't present.
			String unlockUser = "begin cwms_sec.unlock_user(?,?); end;";
			String userPermissions = "begin execute immediate 'grant web_user to ' || ?; end;";
			String dbOffice = System.getProperty(DB_OFFICE);
			String setWebUserPermissions = "begin\n" +
					"   cwms_sec.add_user_to_group(?, 'CWMS User Admins',?) ;\n" +
					"   commit;\n" +
					"end;";
			try(Connection connection = DriverManager.getConnection(System.getProperty(DB_URL), "CWMS_20",
					System.getProperty(DB_PASSWORD));
				PreparedStatement unlockUserStmt = connection.prepareStatement(unlockUser);
				PreparedStatement userPermissionsStmt = connection.prepareStatement(userPermissions);
				PreparedStatement setWebUserPermissionsStmt = connection.prepareStatement(setWebUserPermissions))
			{
				final String username = System.getProperty(DB_USERNAME);
				unlockUserStmt.setString(1, username);
				unlockUserStmt.setString(2, dbOffice);
				unlockUserStmt.executeQuery();
				userPermissionsStmt.setString(1, username);
				userPermissionsStmt.executeQuery();
				setWebUserPermissionsStmt.setString(1, username);
				setWebUserPermissionsStmt.setString(2, dbOffice);
				setWebUserPermissionsStmt.executeQuery();
			}
			catch(SQLException ex)
			{
				log.atDebug().setCause(ex).log("Error setting up client user");
			}
		}
	}

	public Manager getTestSessionManager()
	{
		return sessionManager.get();
	}
}
