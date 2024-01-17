/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

package org.opendcs.odcsapi.jetty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.start.StartException;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiEnvExpander;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.postgresql.ds.PGSimpleDataSource;

public class Start
{
	private static ApiCmdLineArgs apiCmdLineArgs = new ApiCmdLineArgs();
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiConstants.loggerName);

	public static void main(String[] args)
		throws Exception
	{
		ArrayList<String[]> corsList = new ArrayList<>();
		corsList.add(new String[] { "Access-Control-Allow-Origin", CrossOriginFilter.ALLOWED_ORIGINS_PARAM });
		corsList.add(new String[] { "Access-Control-Allow-Headers", CrossOriginFilter.ALLOWED_HEADERS_PARAM });
		corsList.add(new String[] { "Access-Control-Allow-Methods", CrossOriginFilter.ALLOWED_METHODS_PARAM });
		corsList.add(new String[] { "Access-Control-Allow-Credentials", CrossOriginFilter.ALLOW_CREDENTIALS_PARAM });

		apiCmdLineArgs.parseArgs(args);

		LOGGER.info("DCSTOOL_USERDIR={0}, parsing args...", System.getProperty("DCSTOOL_USERDIR"));
		LOGGER.info("Listening Http Port={}", apiCmdLineArgs.getHttpPort());
		LOGGER.info("Listening Https Port={}", apiCmdLineArgs.getHttpsPort());
		LOGGER.info("Top Context={}", apiCmdLineArgs.getContext());
		LOGGER.info("Cors File={}", apiCmdLineArgs.getCorsFile());
		LOGGER.info("Secure Mode={}", apiCmdLineArgs.isSecureMode());

		// Initialize the JETTY server and servlet holders.
		org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();
		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
		ctx.setContextPath("/");
		server.setHandler(ctx);

		String corsFile = apiCmdLineArgs.getCorsFile();
		if (corsFile != null)
		{
			Path corsPath = Paths.get(corsFile);
			boolean fExists = corsPath.toFile().exists();
			if (!fExists)
			{
				LOGGER.warn("Cors File={} does not exist.  Doing nothing with CORS for now.", corsFile);
			}
			else
			{
				try
				{
					LOGGER.info("Looking for Cors Filters.");
					Scanner scanner = new Scanner(new File(corsFile));
					FilterHolder cors = null;
					while (scanner.hasNextLine())
					{
						String curLine = scanner.nextLine();
						String[] splitString = curLine.split(":");
						String corsId = splitString[0].trim().toLowerCase();
						String corsValue = splitString[1].trim();
						for (int x = 0; x < corsList.size(); x++)
						{
							String curCorsId = corsList.get(x)[0].toLowerCase();
							if (curCorsId.toLowerCase().contentEquals(corsId))
							{
								if (cors == null)
								{
									cors = ctx.addFilter(CrossOriginFilter.class,"/*",EnumSet.of(DispatcherType.REQUEST));
								}
								String corsFilter = corsList.get(x)[1];
								LOGGER.info("Adding the following cors filter: {} : {}", corsFilter, corsValue);
								cors.setInitParameter(corsList.get(x)[1], corsValue);
							}
						}
					}
					if (cors != null)
					{
						server.setHandler(ctx);
					}
					scanner.close();
				}
				catch (FileNotFoundException e)
				{
					LOGGER.error("There was an error loading file.", e);
				}
			}
		}

		ServletHolder serHol = ctx.addServlet(ServletContainer.class,
			"/" + apiCmdLineArgs.getContext() + "/*");
		serHol.setInitOrder(1);
		serHol.setInitParameter("jersey.config.server.provider.packages", 
			"org.opendcs.odcsapi.res");
		
		// Get whatever is needed from decodes properties.
		Properties decodesProps = new Properties();
		try (FileReader fr = new FileReader(ApiEnvExpander.expand(apiCmdLineArgs.getDecodesPropFile())))
		{
			decodesProps.load(fr);
		}
		String dbUrl = null;
		String dbType = null;
		String dbAuthFile = null;
		for (Object key : decodesProps.keySet())
		{
			String n = (String)key;
			if ("editDatabaseType".equalsIgnoreCase(n))
				dbType = decodesProps.getProperty(n);
			else if ("editDatabaseLocation".equalsIgnoreCase(n))
				dbUrl = decodesProps.getProperty(n);
			else if ("DbAuthFile".equalsIgnoreCase(n))
				dbAuthFile = decodesProps.getProperty(n);
			else if ("siteNameTypePreference".equalsIgnoreCase(n))
				DbInterface.siteNameTypePreference = n;
		}
		ApiPropertiesUtil.copyProps(DbInterface.decodesProperties, decodesProps);
		DbInterface.secureMode = apiCmdLineArgs.isSecureMode();
		
		PGSimpleDataSource ds = new PGSimpleDataSource();
		ds.setURL(dbUrl);
		String afn = ApiEnvExpander.expand(dbAuthFile);
		AuthFileReader afr = new AuthFileReader(afn);
		try
		{
			afr.read();
		}
		catch(Exception ex)
		{
			String msg = String.format("Cannot read DB auth from file '%s': %s", afn, ex);
			LOGGER.error(msg);
			throw new StartException(String.format("Cannot read auth file: %s", ex));
		}
		ds.setUser(afr.getUsername());
		ds.setPassword(afr.getPassword());
		DbInterface.setDataSource(ds);
		DbInterface.setDatabaseType(dbType);

		// Setup Swagger-UI static resources
		String resourceBasePath = Start.class.getResource("/swaggerui").toExternalForm();
		ctx.setWelcomeFiles(new String[] {"index.html"});
		ctx.setResourceBase(resourceBasePath);
		ctx.addServlet(new ServletHolder(new DefaultServlet()), "/*");
		ServerConnector connector = new ServerConnector(server);
		
		ArrayList<ServerConnector> connectors = new ArrayList<>();
		if (apiCmdLineArgs.getHttpPort() >= 0)
		{
			connector.setPort(apiCmdLineArgs.getHttpPort());
			connectors.add(connector);
		}
		
		if (apiCmdLineArgs.getHttpsPort() >= 0)
		{
			HttpConfiguration https = new HttpConfiguration();
			https.addCustomizer(new SecureRequestCustomizer());
			https.setSendServerVersion(false);
			SslContextFactory sslContextFactory = new SslContextFactory();
			sslContextFactory.setKeyStorePath(apiCmdLineArgs.getKeyStorePath());
			sslContextFactory.setKeyStorePassword(apiCmdLineArgs.getKeyStorePassword());
			ServerConnector sslConnector = new ServerConnector(server,
			    new SslConnectionFactory(sslContextFactory, "http/1.1"),
			    new HttpConnectionFactory(https));
			sslConnector.setPort(apiCmdLineArgs.getHttpsPort());
			connectors.add(sslConnector);
		}
		
		server.setConnectors(connectors.toArray(new ServerConnector[connectors.size()]));

		// Start the server.
		server.start();
		server.join();
	}
}
