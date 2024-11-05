package opendcs.opentsdb;

import ilex.util.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

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
public class OpenTsdbIntervalDAO
	extends DaoBase
	implements IntervalDAI
{
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
		try
		{
			ResultSet rs = doQuery(q);
			while(rs != null && rs.next())
			{
				int calconst = str2const(rs.getString(3));
				if (calconst == -1)
					warning("Invalid calendar constant '" + rs.getString(3) + "' -- skipped.");
				Interval intv = new Interval(DbKey.createDbKey(rs, 1),
					rs.getString(2), calconst, rs.getInt(4));
				IntervalList.instance().add(intv);
			}
			
			// "0" needs to be a built-in interval because it's used often for duration.
			Interval zeroInt = IntervalList.instance().getByName("0");
			if (zeroInt == null)
			{
				Logger.instance().debug1("After loading intervals, there is no '0' interval. Will add.");
				zeroInt = new Interval(DbKey.NullKey, "0", Calendar.MINUTE, 0);
				if (db.getKeyGenerator() != null)
					writeInterval(zeroInt);
				// note: writeInterval will add to the list.
			}
			else
				Logger.instance().info("After loading intervals, '0' interval has key=" + zeroInt.getKey());
		}
		catch (Exception ex)
		{
			String msg = "Cannot read INTERVAL_CODE table: " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace();
			throw new DbIoException(msg);
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
			ResultSet rs = doQuery(q);
			try
			{
				if (rs != null && rs.next())
					intv.setKey(DbKey.createDbKey(rs, 1));
			}
			catch (SQLException ex)
			{
				String msg = "Error in query '" + q + "': " + ex;
				failure(msg);
				throw new DbIoException(msg);
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
				failure("Cannot load intervals: " + ex);
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
				failure("Cannot load intervals: " + ex);
				validIntervals = new String[0];
			}
		}
		return validIntervals;
	}
	
}
