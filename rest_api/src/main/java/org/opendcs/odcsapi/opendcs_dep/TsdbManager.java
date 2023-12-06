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

package org.opendcs.odcsapi.opendcs_dep;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.hdb.HdbTimeSeriesDb;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TimeSeriesDb;
import opendcs.opentsdb.OpenTsdb;
import opendcs.opentsdb.OpenTsdbSettings;

/**
 * A few operations require using the openDCS TimeSeriesDb subclasses.
 * This class instantiates and manages those classes given a connection
 * provided by the web container.
 * 
 * @author mmaloney
 *
 */
public class TsdbManager
{
	/**
	 * Make an appropriate TimeSeriesDb subclass depending on the database type (CWMS,
	 * HDB, or OpenTSDB).
	 * Note: Currently the tsdb is provided with the one connection being used by this
	 * session, created by the web container. So don't close the connection because
	 * the container is managing this.
	 * @param dbi
	 * @return
	 * @throws DbException
	 */
	public static TimeSeriesDb makeTsdb(DbInterface dbi)
		throws DbException
	{
		TimeSeriesDb ret = null;
		if (DbInterface.isCwms)
			ret = new CwmsTimeSeriesDb();
		else if (DbInterface.isHdb)
			ret = new HdbTimeSeriesDb();
		else
			ret = new OpenTsdb();
		
		ret.setConnection(dbi.getConnection());
		
		// setConnection will also call determineTsdbVersion()
		
		// ret.setupKeyGenerator(); - No need, the API will not use Tsdb to create records.

		try
		{
			ret.postConnectInit("decodes", dbi.getConnection());
		}
		catch (BadConnectException e)
		{
			Logger.getLogger(ApiConstants.loggerName).info("Using DataSource provided by Jetty main class.");

			e.printStackTrace();
		}

		// OpenTsdbSettings.instance().setFromProperties(props); - No need. Use defaults.

		return ret;
	}
}
