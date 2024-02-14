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

import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.hydrojson.DbInterface;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.hdb.HdbTimeSeriesDb;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TimeSeriesDb;
import opendcs.opentsdb.OpenTsdb;

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

		// setConnection will also call determineTsdbVersion()
		ret.setConnection(dbi.getConnection());

		try
		{
			ret.postConnectInit("decodes", dbi.getConnection());
		}
		catch (BadConnectException ex)
		{
			throw new DbException(CompRunner.class.getName(), ex, "Error connecting to the decodes database: ");
		}

		return ret;
	}
}
