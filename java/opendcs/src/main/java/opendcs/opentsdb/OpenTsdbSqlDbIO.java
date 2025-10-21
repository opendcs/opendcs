/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package opendcs.opentsdb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.IntervalDAI;

import decodes.db.DatabaseException;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;

public class OpenTsdbSqlDbIO extends SqlDatabaseIO
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public OpenTsdbSqlDbIO()
		throws DatabaseException
	{
		this(null);
	}

	public OpenTsdbSqlDbIO(String location)
		throws DatabaseException
	{
		super(location);
		log.info("Constructing OpenTsdbSqlDbIO");
	}

	@Override
	protected void postConnectInit()
		throws DatabaseException
	{
		super.postConnectInit();
		IntervalDAI intervalDAO = makeIntervalDAO();
		try { intervalDAO.loadAllIntervals(); }
		catch (DbIoException ex)
		{
			log.atWarn().setCause(ex).log("Cannot read intervals.");
		}
		finally
		{
			intervalDAO.close();
		}
	}

	@Override
	public Date getFullDate(ResultSet rs, int column)
	{
		// In OpenTSDB, date/times are stored as long integer
		try
		{
			long t = rs.getLong(column);
			if (rs.wasNull())
				return null;
			return new Date(t);
		}
		catch (SQLException ex)
		{
			log.atWarn().setCause(ex).log("Cannot convert date!");
			return null;
		}
	}

	@Override
	public String sqlDate(Date d)
	{
		if (d == null)
			return "NULL";
		return "" + d.getTime();
	}

	@Override
	public String sqlBoolean(boolean b)
	{
		return b ? "'TRUE'" : "'FALSE'";
	}

	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new OpenTsdbIntervalDAO(this);
	}

}
