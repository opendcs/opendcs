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
import java.util.Calendar;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.IntervalList;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import opendcs.dai.IntervalDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;

/**
 * Data Access Object for Open TSDB Interval reading/writing
 * @author mmaloney Mike Maloney, Cove Software, LLC
 *
 */
public class OpenTsdbIntervalDAO extends DaoBase implements IntervalDAI
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static String validIntervals[] = new String[0];

	public OpenTsdbIntervalDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "IntervalDAO");
	}

	@Override
	public void loadAllIntervals()
		throws DbIoException
	{
		String q = "select INTERVAL_ID, NAME, CAL_CONSTANT, CAL_MULTIPLIER "
			+ "from INTERVAL_CODE";
		try (ResultSet rs = doQuery(q))
		{
			while(rs.next())
			{
				int calconst = str2const(rs.getString(3));
				if (calconst == -1)
				{
					log.warn("Invalid calendar constant '{}' -- skipped.", rs.getString(3));
				}
				Interval intv = new Interval(DbKey.createDbKey(rs, 1),
					rs.getString(2), calconst, rs.getInt(4));
				IntervalList.instance().add(intv);
			}

			// "0" needs to be a built-in interval because it's used often for duration.
			Interval zeroInt = IntervalList.instance().getByName("0");
			if (zeroInt == null)
			{
				log.debug("After loading intervals, there is no '0' interval. Will add.");
				zeroInt = new Interval(DbKey.NullKey, "0", Calendar.MINUTE, 0);
				if (db.getKeyGenerator() != null)
					writeInterval(zeroInt);
				// note: writeInterval will add to the list.
			}
			else
			{
				log.info("After loading intervals, '0' interval has key={}", zeroInt.getKey());
			}
		}
		catch (Exception ex)
		{
			String msg = "Cannot read INTERVAL_CODE table.";
			throw new DbIoException(msg, ex);
		}

		// All intervals, including built-ins are valid in OpenTSDB.
		validIntervals = new String[IntervalList.instance().getList().size()];
		int i=0;
		for(Interval intv : IntervalList.instance().getList())
			validIntervals[i++] = intv.getName();
	}

	@Override
	public void writeInterval(Interval intv)
		throws DbIoException
	{
		String q = null;
		if (intv.getKey().isNull())
		{
			q = "select interval_id from interval_code where lower(name) = "
				+ sqlString(intv.getName().toLowerCase());
			try (ResultSet rs = doQuery(q))
			{
				if (rs.next())
				{
					intv.setKey(DbKey.createDbKey(rs, 1));
				}
			}
			catch (SQLException ex)
			{
				String msg = "Error in query '" + q + "'";
				throw new DbIoException(msg, ex);
			}
		}
		if (intv.getKey().isNull())
		{
			intv.setKey(getKey("interval_code"));
			q = "insert into interval_code(interval_id, name, cal_constant, cal_multiplier) "
				+ "values("
				+ intv.getKey() + ", "
				+ sqlString(intv.getName()) + ", "
				+ sqlString(IntervalCodes.getCalConstName(intv.getCalConstant())) + ", "
				+ intv.getCalMultiplier() + ")";
		}
		else
		{
			q = "update interval_code "
				+ "set name = " + sqlString(intv.getName()) + ", "
				+ "cal_constant = " + sqlString(IntervalCodes.getCalConstName(intv.getCalConstant())) + ", "
				+ "cal_multiplier = " + intv.getCalMultiplier()
				+ " where interval_id = " + intv.getKey();
		}
		doModify(q);
		IntervalList.instance().add(intv);
	}

	private int str2const(String s)
	{
		/** One of MINUTE, HOUR_OF_DAY, DAY_OF_MONTH, WEEK_OF_YEAR, MONTH, YEAR */
		if (s.length() == 0)
			return -1;
		s = s.toUpperCase();
		if (s.charAt(0) == 'H')
			return Calendar.HOUR_OF_DAY;
		else if (s.charAt(0) == 'D')
			return Calendar.DAY_OF_MONTH;
		else if (s.charAt(0) == 'W')
			return Calendar.WEEK_OF_YEAR;
		else if (s.charAt(0) == 'Y')
			return Calendar.YEAR;
		else if (s.startsWith("MI"))
			return Calendar.MINUTE;
		else if (s.startsWith("MO"))
			return Calendar.MONTH;
		else return -1;
	}

	@Override
	public String[] getValidIntervalCodes()
	{
		if (validIntervals.length == 0)
		{
			try { loadAllIntervals(); }
			catch (DbIoException ex)
			{
				log.atError().setCause(ex).log("Cannot load intervals.");
				validIntervals = new String[0];
			}
		}
		return validIntervals;
	}

	@Override
	public String[] getValidDurationCodes()
	{
		if (validIntervals.length == 0)
		{
			try { loadAllIntervals(); }
			catch (DbIoException ex)
			{
				log.atError().setCause(ex).log("Cannot load intervals.");
				validIntervals = new String[0];
			}
		}
		return validIntervals;
	}

}