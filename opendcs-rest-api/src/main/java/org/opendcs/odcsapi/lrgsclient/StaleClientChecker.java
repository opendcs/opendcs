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

package org.opendcs.odcsapi.lrgsclient;

import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.hydrojson.DbInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This runs a continuous thread to check periodically for stale LDDS client connections
 * and close them.
 */
public class StaleClientChecker
	extends Thread
{
	public static String module = "StaleClientChecker";
	private static final Logger LOGGER = LoggerFactory.getLogger(StaleClientChecker.class);

	@Override
	public void run()
	{
		while(true)
		{
			try
			{
				sleep(30000L);
				checkClients();
			}
			catch (InterruptedException | DbException e)
			{
				LOGGER.error("There was an error checking for stale clients: {0}", e);
			}
		}

	}
	
	private void checkClients() throws DbException
	{
        try {
            DbInterface.getTokenManager().checkStaleConnections();
        }
		catch (DbException e)
		{
            throw new DbException(module, e, "Error checking client connections.");
        }
    }

}
