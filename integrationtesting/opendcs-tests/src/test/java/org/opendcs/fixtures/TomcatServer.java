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

package org.opendcs.fixtures;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import decodes.sql.DbKey;
import decodes.util.DecodesSettings;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.jdbc.pool.DataSourceFactory;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.jdbi.v3.core.Handle;
import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInIdentityProvider;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInProviderCredentials;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.fixtures.configurations.cwms.CwmsOracleConfiguration;
import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.fixtures.spi.ConfigurationProvider;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
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
	public static final String DB_DRIVER_CLASS = "DB_DRIVER_CLASS";
	public static final String DB_DATASOURCE_CLASS = "DB_DATASOURCE_CLASS";
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
		((StandardJarScanner) restApiContext.getJarScanner()).setScanClassPath(false);
		sessionManager = restApiContext::getManager;

		StandardContext guiContext = (StandardContext) tomcatInstance.addWebapp("", guiWar);
		((StandardJarScanner) guiContext.getJarScanner()).setScanClassPath(false);
		guiContext.setDelegate(true);
		guiContext.setParentClassLoader(null);
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
			System.setProperty(DB_VALIDATION_QUERY, "SELECT 1 FROM dual");
			System.setProperty(DB_URL, System.getProperty("testcontainer.cwms.bypass.url"));
			System.setProperty(DB_USERNAME, System.getProperty("testcontainer.cwms.bypass.dcs.user", "dcs_user"));
			System.setProperty(DB_PASSWORD, System.getProperty("testcontainer.cwms.bypass.dcs.pass", "dcs_user"));
		}
		else
		{
			String validationQuery = "SELECT 1";
			System.setProperty(DB_DRIVER_CLASS, "org.postgresql.Driver");
			System.setProperty(DB_DATASOURCE_CLASS, DataSourceFactory.class.getName());
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
		log.info("DbType {}", dbType);
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
		try (var tx = config.getOpenDcsDatabase().newTransaction())
		{
			// looks odd, need ro make surer the transaction closes it.
			var conn = tx.connection(Handle.class).orElseThrow();
			try (var upsertProp = conn.prepareBatch("""
				merge into tsdb_property p
				using (select :name prop_name, :value prop_value <dual>) input
				on (p.prop_name = input.prop_name)
				when matched then
				  update set prop_value = input.prop_value
				when not matched then
				  insert (prop_name, prop_value)
				  values(input.prop_name, input.prop_value)
				""").define("dual", tx.getContext().getDatabase() == DatabaseEngine.ORACLE ? "from dual" : ""))
			{
				DecodesSettings settings = config.getOpenDcsDatabase()
												.getSettings(DecodesSettings.class)
												.orElseThrow();
				Properties props = new Properties();
				settings.saveToProps(props);
				for(var k: props.keySet())
				{
					final String value = props.getProperty(k.toString(), null);
					upsertProp.bind("name", k.toString())
							.bind("value", value)
							.add();
				}
				
				upsertProp.execute();
			
			}
		}
		catch (Throwable ex)
		{
			log.atInfo().setCause(ex).log("error setting props");
			throw new OpenDcsDataException("Unable to set database type property.", ex);
		}

		try
		{
			setupTestUser(config.getOpenDcsDatabase());
		}
		catch (Throwable ex)
		{
			log.atInfo().setCause(ex).log("error setting up initial user");
			throw new OpenDcsDataException("Unable to setup initial user.");
		}
		environment.getVariables().forEach(System::setProperty);
		config.getEnvironment().forEach((key, value) -> System.setProperty(key.toString(), value.toString()));
		if(CwmsOracleConfiguration.NAME.equals(dbType))
		{
			System.setProperty(DB_DRIVER_CLASS, "oracle.jdbc.driver.OracleDriver");
			System.setProperty(DB_DATASOURCE_CLASS, "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			System.setProperty(DB_VALIDATION_QUERY, "SELECT 1 FROM dual");
		}
		else
		{
			String validationQuery = "SELECT 1";
			System.setProperty(DB_DRIVER_CLASS, "org.postgresql.Driver");
			System.setProperty(DB_DATASOURCE_CLASS, "org.apache.tomcat.jdbc.pool.DataSourceFactory");
			System.setProperty(DB_VALIDATION_QUERY, validationQuery);
		}
		setupClientUser(dbType);
	}

	public static void setupTestUser(OpenDcsDatabase db) throws OpenDcsDataException
	{
		
		var dao = db.getDao(UserManagementDao.class)
					.orElseThrow(() -> new OpenDcsDataException("No User Management class available for this implementation."));
		try (var tx = db.newTransaction())
		{
			var idps = dao.getIdentityProviders(tx, -1, -1);
			IdentityProvider idp = null;
			for (var provider: idps)
			{
				if (provider instanceof BuiltInIdentityProvider)
				{
					idp = provider;
					break;
				}
			}
			if (idp == null)
			{
				throw new OpenDcsDataException("Database not initialized with builtin identity provider.");
			}
			final var ub = new UserBuilder();
			String[] roles = new String[] {"ODCS_API_USER", "ODCS_API_ADMIN"};
			for (var role: roles) {
				ub.withRole(new Role(DbKey.NullKey, role, null, null));
			}
			final String userName = "dcs_user";
			ub.withEmail(userName);

			ub.withIdentityMapping(new IdentityProviderMapping(idp, userName));
			var user = dao.addUser(tx, ub.build());
			var creds = new BuiltInProviderCredentials(userName, "dcs_user"); // NOSONAR
			idp.updateUserCredentials(db, tx, user, creds);
		}
		catch (OpenDcsAuthException ex)
		{
			throw new OpenDcsDataException("Unable to set initial user credentials.", ex);
		}
	}

	private static void setupClientUser(String dbType)
	{
		if(CwmsOracleConfiguration.NAME.equals(dbType))
		{
			// I have no idea why this is suddenly required but it was also affecting operations in
			// runtime test environments where the required entries weren't present.
			String userPermissions = "begin execute immediate 'grant web_user to ' || ?; end;";
			String userPermissions2 = "begin cwms_sec.ADD_USER_TO_GROUP(?, 'CWMS PD Users', 'HQ'); end;";
			try(Connection connection = DriverManager.getConnection(System.getProperty(DB_URL), "CWMS_20",
					System.getProperty(DB_PASSWORD)))
			{
				try(PreparedStatement stmt1 = connection.prepareStatement(userPermissions);
					PreparedStatement stmt2 = connection.prepareStatement(userPermissions2))
				{
					String username = System.getProperty(DB_USERNAME);
					stmt1.setString(1, username);
					stmt1.executeQuery();
					stmt2.setString(1, username);
					stmt2.executeQuery();
				}
			}
			catch(SQLException ex)
			{
				log.atDebug().setCause(ex).log("Error setting up web user");
			}
		}
	}

	public Manager getTestSessionManager()
	{
		return sessionManager.get();
	}
}
