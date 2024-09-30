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

package org.opendcs.odcsapi.lrgsclient;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.odcsapi.dao.ApiAppDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This runs a continuous thread to check periodically for stale LDDS client connections
 * and close them.
 */
@WebListener
public class ClientConnectionChecker implements ServletContextListener
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionChecker.class);

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		executor.scheduleAtFixedRate(this::checkClients, 0, 30, TimeUnit.SECONDS);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		executor.shutdown();
	}

	/**
	 * Called periodically to hang up any DDS clients or event clients
	 * that have gone stale, e.g. client starts a message retrieval and never completes it, or
	 * event clients with a stale heartbeat.
	 */
	private void checkClients()
	{
		checkLddsClients();
		try
		{
			checkApiEventClients();
		}
		catch(DbException e)
		{
			LOGGER.error("There was an error checking for stale clients.", e);
		}
	}

	private void checkApiEventClients() throws DbException
	{
		ArrayList<ApiAppStatus> appStatii;
		try(DbInterface dbi = new DbInterface();
			ApiAppDAO appDao = new ApiAppDAO(dbi))
		{
			appStatii = appDao.getAppStatus();
		}
		ClientConnectionCache.getInstance().removeExpiredApiEventClients(appStatii);
	}

	private void checkLddsClients()
	{
		try
		{
			ClientConnectionCache.getInstance().removeExpiredLddsClients();
		}
		catch(RuntimeException e)
		{
			LOGGER.error("There was an error checking for expired LDDS clients.", e);
		}
	}
}
