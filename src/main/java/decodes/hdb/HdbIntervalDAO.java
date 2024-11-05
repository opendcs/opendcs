package decodes.hdb;

import java.util.Calendar;

import decodes.db.IntervalList;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import opendcs.dai.IntervalDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.Interval;

public class HdbIntervalDAO extends DaoBase implements IntervalDAI
{
	private static String[] validIntervalCodes = 
	{
		IntervalCodes.int_instant,
		IntervalCodes.int_hour,
		IntervalCodes.int_day,
		IntervalCodes.int_month,
		IntervalCodes.int_year,
		IntervalCodes.int_wy
	};

	protected HdbIntervalDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "HdbIntgervalDAO");
	}

	@Override
	public void loadAllIntervals() throws DbIoException
	{
		// Intervals are constant in HDB - there is no HDB_INTERVAL table.
		IntervalList dbIntervals = IntervalList.instance();
		long id = 1L;
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_irregular, 0, 0));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_hour, Calendar.HOUR_OF_DAY, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_day, Calendar.DAY_OF_MONTH, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_month, Calendar.MONTH, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_year, Calendar.YEAR, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_wy, Calendar.YEAR, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_instant, 0, 0));
	}

	@Override
	public void writeInterval(Interval intv) throws DbIoException
	{
		warning("Cannot write Intervals to HDB.");
	}

	@Override
	public String[] getValidIntervalCodes()
	{
		return validIntervalCodes;
	}

	@Override
	public String[] getValidDurationCodes()
	{
		return validIntervalCodes;
	}

}
