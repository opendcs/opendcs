package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.DecodesPresentationGroup;
import opendcs.opentsdb.hydrojson.beans.DecodesRouting;
import opendcs.opentsdb.hydrojson.beans.DecodesScheduleEntry;
import opendcs.opentsdb.hydrojson.beans.RoutingRef;
import opendcs.opentsdb.hydrojson.beans.ScheduleEntryRef;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

public class RoutingDAO
	extends DaoBase
{
	public static String module = "RoutingDAO";

	public RoutingDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}

	public ArrayList<RoutingRef> getRoutingRefs()
		throws DbIoException
	{
		ArrayList<RoutingRef> ret = new ArrayList<RoutingRef>();
		
		String q = "select rs.ID, rs.NAME, ds.NAME, rs.CONSUMERTYPE, rs.CONSUMERARG, rs.LASTMODIFYTIME"
				+ " from ROUTINGSPEC rs, DATASOURCE ds"
				+ " where rs.DATASOURCEID = ds.ID"
				+ " order by rs.ID";
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				RoutingRef ref = new RoutingRef();
				ref.setRoutingId(rs.getLong(1));
				ref.setName(rs.getString(2));
				ref.setDataSourceName(rs.getString(3));
				String t = rs.getString(4);
				String a = rs.getString(5);
				ref.setDestination(t + "(" + (a==null?"":a) + ")");
				ref.setLastModified(db.getFullDate(rs, 6));
				ret.add(ref);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getRoutingRefs() error in query '" + q + "': " + ex);
		}
	}

	public DecodesRouting getRouting(long routingId)
		throws DbIoException, WebAppException
	{
		String q = "select rs.ID, rs.NAME, rs.DATASOURCEID, rs.ENABLEEQUATIONS, rs.OUTPUTFORMAT,"
				+ " rs.PRESENTATIONGROUPNAME, rs.SINCETIME, rs.UNTILTIME, rs.CONSUMERTYPE,"
				+ " rs.CONSUMERARG, rs.LASTMODIFYTIME, rs.ISPRODUCTION, ds.NAME, rs.OUTPUTTIMEZONE"
				+ " from ROUTINGSPEC rs, DATASOURCE ds"
				+ " where rs.id = " + routingId
				+ " and rs.DATASOURCEID = ds.ID";
		ResultSet rs = doQuery(q);
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No Routing with ID=" + routingId);
			
			DecodesRouting ret = new DecodesRouting();
			ret.setRoutingId(rs.getLong(1));
			ret.setName(rs.getString(2));
			ret.setDataSourceId(rs.getLong(3));
			ret.setEnableEquations(TextUtil.str2boolean(rs.getString(4)));
			ret.setOutputFormat(rs.getString(5));
			ret.setPresGroupName(rs.getString(6));
			ret.setSince(rs.getString(7));
			ret.setUntil(rs.getString(8));
			ret.setDestinationType(rs.getString(9));
			ret.setDestinationArg(rs.getString(10));
			ret.setLastModified(db.getFullDate(rs, 11));
			ret.setProduction(TextUtil.str2boolean(rs.getString(12)));
			ret.setDataSourceName(rs.getString(13));
			ret.setOutputTZ(rs.getString(14));
			
			q = "select NETWORKLISTNAME from ROUTINGSPECNETWORKLIST"
				+ " where ROUTINGSPECID = " + routingId;
			rs = doQuery(q);
			while(rs.next())
				ret.getNetlistNames().add(rs.getString(1));
			
			q = "select PROP_NAME, PROP_VALUE"
				+ " from ROUTINGSPECPROPERTY"
				+ " where ROUTINGSPECID = " + routingId
				+ " order by PROP_NAME";
			rs = doQuery(q);
System.out.println("q");
			while(rs.next())
			{
				String n = rs.getString(1);
				String ln = n.toLowerCase();
				String v = rs.getString(2);
System.out.println("prop name='" + n + "' lname='" + ln + "' v='" + v + "'");
				if (ln.equals("sc:ascending_time"))
					ret.setAscendingTime(TextUtil.str2boolean(v));
				else if (ln.equals("sc:rt_settle_delay"))
					ret.setSettlingTimeDelay(TextUtil.str2boolean(v));
				else if (ln.equals("sc:daps_status")
					&& v.toLowerCase().startsWith("a"))
					ret.setQualityNotifications(true);
				else if (ln.equals("sc:parity_error"))
				{
					ret.setParityCheck(true);
					// r = good only, o = bad only
					ret.setParitySelection(v.toLowerCase().startsWith("r") ? "Good" : "Bad");
				}
				else if (ln.startsWith("sc:source_"))
				{
					v = v.toLowerCase();
					if (v.equals("netdcp"))
						ret.setNetworkDCP(true);
					else if (v.equals("goes_random"))
						ret.setGoesRandom(true);
					else if (v.equals("goes_selftimed"))
						ret.setGoesSelfTimed(true);
					else if (v.equals("iridium"))
						ret.setIridium(true);
				}
				else if (ln.equals("sc:spacecraft"))
				{
					ret.setGoesSpacecraftCheck(true);
					ret.setGoesSpacecraftSelection(v.toLowerCase().startsWith("e") ? "East" : "West");
				}
				else if (ln.startsWith("sc:dcp_address_"))
					ret.getPlatformIds().add(v);
				else if (ln.startsWith("sc:dcp_name_"))
					ret.getPlatformNames().add(v);
				else if (ln.startsWith("sc:channel_"))
				{
					if (!Character.isDigit(v.charAt(0)))
						v = v.substring(1);
					try { ret.getGoesChannels().add(Integer.parseInt(v)); }
					catch(NumberFormatException ex) {}
				}
				else if (ln.startsWith("rs.timeapplyto"))
				{
					v = v.toLowerCase();
					if (v.startsWith("l"))
						ret.setApplyTimeTo("Local Receive Time");
					else if (v.startsWith("m"))
						ret.setApplyTimeTo("Platform Xmit Time");
					else if (v.startsWith("b"))
						ret.setApplyTimeTo("Both");
				}
				else
					ret.getProperties().setProperty(n, v);
			}
			
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getRouting(" + routingId
				+ ") Exception in query '" + q + "': " + ex);

		}
	}

	public void writeRouting(DecodesRouting routing)
		throws DbIoException, WebAppException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select ID from ROUTINGSPEC where lower(NAME) = " 
			+ sqlString(routing.getName().toLowerCase());
		if (routing.getRoutingId() != DbKey.NullKey.getValue())
			q = q + " and ID != " + routing.getRoutingId();
System.out.println(module + " Check for dup name: " + q);
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write RoutingSpec with name '" + routing.getName() 
					+ "' because another RoutingSpec with id=" + rs.getLong(1) 
					+ " also has that name.");

			routing.setLastModified(new Date());
			if (routing.getRoutingId() == DbKey.NullKey.getValue())
				insert(routing);
			else
				update(routing);
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".writeRouting error in query '" + q + "': " + ex);
		}

	}
	
	private void update(DecodesRouting routing)
		throws DbIoException
	{
		String q = "update ROUTINGSPEC set "
			+ "NAME = " + sqlString(routing.getName()) + ", "
			+ "DATASOURCEID = " + routing.getDataSourceId() + ", "
			+ "ENABLEEQUATIONS = " + sqlBoolean(routing.isEnableEquations()) + ", "
			+ "OUTPUTFORMAT = " + sqlString(routing.getOutputFormat()) + ", "
			+ "OUTPUTTIMEZONE = " + sqlString(routing.getOutputTZ()) + ", "
			+ "PRESENTATIONGROUPNAME = " + sqlString(routing.getPresGroupName()) + ", "
			+ "SINCETIME = " + sqlString(routing.getSince()) + ", "
			+ "UNTILTIME = " + sqlString(routing.getUntil()) + ", "
			+ "CONSUMERTYPE = " + sqlString(routing.getDestinationType()) + ", "
			+ "CONSUMERARG = " + sqlString(routing.getDestinationArg()) + ", "
			+ "LASTMODIFYTIME = " + db.sqlDate(routing.getLastModified()) + ", "
			+ "ISPRODUCTION = " + sqlBoolean(routing.isProduction())
			+ " where ID = " + routing.getRoutingId();
		doModify(q);
		
		deleteSubordinates(routing.getRoutingId());
		writeSubordinates(routing);
	}

	private void insert(DecodesRouting routing)
		throws DbIoException
	{
		routing.setRoutingId(getKey("ROUTINGSPEC").getValue());

		String q = "insert into ROUTINGSPEC(ID, NAME, DATASOURCEID, ENABLEEQUATIONS, "
				+ "OUTPUTFORMAT, OUTPUTTIMEZONE, PRESENTATIONGROUPNAME,"
				+ "SINCETIME, UNTILTIME, CONSUMERTYPE, CONSUMERARG, LASTMODIFYTIME, ISPRODUCTION)"
			+ " values("
					+ routing.getRoutingId() + ", "
					+ sqlString(routing.getName()) + ", "
					+ routing.getDataSourceId() + ", "
					+ sqlBoolean(routing.isEnableEquations()) + ", "
					+ sqlString(routing.getOutputFormat()) + ", "
					+ sqlString(routing.getOutputTZ()) + ", "
					+ sqlString(routing.getPresGroupName()) + ", "
					+ sqlString(routing.getSince()) + ", "
					+ sqlString(routing.getUntil()) + ", "
					+ sqlString(routing.getDestinationType()) + ", "
					+ sqlString(routing.getDestinationArg()) + ", "
					+ db.sqlDate(routing.getLastModified()) + ", "
					+ sqlBoolean(routing.isProduction())
			+ ")";
		doModify(q);
		
		deleteSubordinates(routing.getRoutingId());
		writeSubordinates(routing);
	}

	private void deleteSubordinates(long routingId)
		throws DbIoException
	{
		String q = "delete from ROUTINGSPECNETWORKLIST where ROUTINGSPECID = " + routingId;
		doModify(q);
		q = "delete from ROUTINGSPECPROPERTY where ROUTINGSPECID = " + routingId;
		doModify(q);
	}

	private void writeSubordinates(DecodesRouting routing)
		throws DbIoException
	{
		for(String nl : routing.getNetlistNames())
		{
			String q = "insert into ROUTINGSPECNETWORKLIST(ROUTINGSPECID, NETWORKLISTNAME) "
				+ "values(" + routing.getRoutingId() + ", " + sqlString(nl) + ")";
			doModify(q);
		}
		
		Properties props = new Properties();
		PropertiesUtil.copyProps(props, routing.getProperties());
		
		if (routing.isAscendingTime())
			props.setProperty("sc:ASCENDING_TIME", "true");
		if (routing.isSettlingTimeDelay())
			props.setProperty("sc:RT_SETTLE_DELAY", "true");
		if (routing.isQualityNotifications())
			props.setProperty("sc:DAPS_STATUS", "A");
		if (routing.isParityCheck())
			props.setProperty("sc:PARITY_ERROR",
				routing.getParitySelection().toLowerCase().equals("good") ? "R" : "A");
		if (routing.isGoesSpacecraftCheck())
			props.setProperty("sc:SPACECRAFT",
				routing.getGoesSpacecraftSelection().toLowerCase().startsWith("e") ? "E" : "W");

		// sc:SOURCE_nnnn properties
		int n=0;
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setMinimumIntegerDigits(4);
		nf.setGroupingUsed(false);
		if (routing.isNetworkDCP())
			props.setProperty("sc:SOURCE_" + nf.format(n++), "NETDCP");
		if (routing.isGoesRandom())
			props.setProperty("sc:SOURCE_" + nf.format(n++), "GOES_RANDOM");
		if (routing.isGoesSelfTimed())
			props.setProperty("sc:SOURCE_" + nf.format(n++), "GOES_SELFTIMED");
		if (routing.isIridium())
			props.setProperty("sc:SOURCE_" + nf.format(n++), "IRIDIUM");
		
		n = 0;
		for(String pid : routing.getPlatformIds())
			props.setProperty("sc:DCP_ADDRESS_" + nf.format(n++), pid);
		
		n = 0;
		for(String pn : routing.getPlatformNames())
			props.setProperty("sc:DCP_NAME_" + nf.format(n++), pn);

		n = 0;
		for(int ch : routing.getGoesChannels())
			props.setProperty("sc:CHANNEL_" + nf.format(n++), "|" + ch);

		String s = routing.getApplyTimeTo();
		if (s != null && s.length() > 0)
		{
			char c = s.charAt(0);
			props.setProperty("rs.timeApplyTo",
				c == 'L' ? "L" :
				c == 'P' ? "M" : "B");
		}

		for(Object k : props.keySet())
		{
			String pn = (String)k;
			String pv = props.getProperty(pn);
			String q = "insert into ROUTINGSPECPROPERTY(ROUTINGSPECID, PROP_NAME, PROP_VALUE)"
				+ " values (" + routing.getRoutingId() + ", "
				+ sqlString(pn) + ", " + sqlString(pv) + ")";
			doModify(q);
		}
	}
	
	public void deleteRouting(long routingId)
		throws DbIoException, WebAppException
	{
		deleteSubordinates(routingId);
		
		String q = "select SCHEDULE_ENTRY_ID from SCHEDULE_ENTRY where ROUTINGSPEC_ID = " + routingId;
		ResultSet rs = doQuery(q);
		try
		{
			while(rs.next())
				deleteSchedule(rs.getLong(1));
		}
		catch(SQLException ex) {}
		
		q = "delete from ROUTINGSPEC where ID = " + routingId;
		doModify(q);
	}

	
	public ArrayList<ScheduleEntryRef> getScheduleRefs()
			throws DbIoException
	{
		ArrayList<ScheduleEntryRef> ret = new ArrayList<ScheduleEntryRef>();
		
		// normal join with RS, inner join with Loading App, because appID may be null
		String q = "select se.SCHEDULE_ENTRY_ID, se.NAME, se.LAST_MODIFIED,"
				+ " rs.NAME, la.LOADING_APPLICATION_NAME"
				+ " from SCHEDULE_ENTRY se"
				+ " inner join ROUTINGSPEC rs on (se.ROUTINGSPEC_ID = rs.ID and se.NAME not like '%-manual')"
				+ " left join HDB_LOADING_APPLICATION la "
				+ "	on se.LOADING_APPLICATION_ID = la.LOADING_APPLICATION_ID";

		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				ScheduleEntryRef ref = new ScheduleEntryRef();
				ref.setSchedEntryId(rs.getLong(1));
				ref.setName(rs.getString(2));
				ref.setLastModified(db.getFullDate(rs, 3));
				ref.setRoutingSpecName(rs.getString(4));
				ref.setAppName(rs.getString(5));;

				ret.add(ref);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getScheduleRefs() error in query '" + q + "': " + ex);
		}
	}
	
	public DecodesScheduleEntry getSchedule(long id)
		throws DbIoException, WebAppException
	{
		String q = "select se.SCHEDULE_ENTRY_ID, se.NAME, se.LAST_MODIFIED,"
			+ " rs.NAME, la.LOADING_APPLICATION_NAME,"
			+ " se.LOADING_APPLICATION_ID, se.ROUTINGSPEC_ID, se.START_TIME, se.TIMEZONE, se.RUN_INTERVAL"
			+ " from SCHEDULE_ENTRY se"
			+ " inner join ROUTINGSPEC rs on "
			+ "(se.ROUTINGSPEC_ID = rs.ID and se.NAME not like '%-manual'"
				+ " and se.SCHEDULE_ENTRY_ID = " + id + ")"
			+ " left join HDB_LOADING_APPLICATION la "
			+ "	on se.LOADING_APPLICATION_ID = la.LOADING_APPLICATION_ID";

		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
			{
				DecodesScheduleEntry dse = new DecodesScheduleEntry();
				dse.setSchedEntryId(rs.getLong(1));
				dse.setName(rs.getString(2));
				dse.setLastModified(db.getFullDate(rs, 3));
				dse.setRoutingSpecName(rs.getString(4));
				dse.setAppName(rs.getString(5));;
				dse.setAppId(rs.getLong(6));
				dse.setRoutingSpecId(rs.getLong(7));
				dse.setStartTime(db.getFullDate(rs, 8));
				dse.setTimeZone(rs.getString(9));
				dse.setRunInterval(rs.getString(10));
				return dse;
			}
			else
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No such schedule entry with id=" + id);

		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getSchedule() error in query '" + q + "': " + ex);
		}
	}

	public DecodesScheduleEntry writeSchedule(DecodesScheduleEntry schedule)
		throws DbIoException, WebAppException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select SCHEDULE_ENTRY_ID from SCHEDULE_ENTRY where lower(NAME) = " 
			+ sqlString(schedule.getName().toLowerCase());
		if (schedule.getSchedEntryId() != DbKey.NullKey.getValue())
			q = q + " and SCHEDULE_ENTRY_ID != " + schedule.getSchedEntryId();
System.out.println(module + ".writeSchedule: Check for dup name: " + q);
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write ScheduleEntry with name '" + schedule.getName() 
					+ "' because another ScheduleEntry with id=" + rs.getLong(1) 
					+ " also has that name.");

			schedule.setLastModified(new Date());
			
			if (schedule.getSchedEntryId() == DbKey.NullKey.getValue())
			{
				schedule.setSchedEntryId(getKey("SCHEDULE_ENTRY").getValue());
	
				q = "insert into SCHEDULE_ENTRY(SCHEDULE_ENTRY_ID, NAME, LOADING_APPLICATION_ID,"
					+ " ROUTINGSPEC_ID, START_TIME, TIMEZONE, RUN_INTERVAL, ENABLED, LAST_MODIFIED)"
					+ " values("
						+ schedule.getSchedEntryId() + ", "
						+ sqlString(schedule.getName()) + ", "
						+ schedule.getAppId() + ", "
						+ schedule.getRoutingSpecId() + ", "
						+ db.sqlDate(schedule.getStartTime()) + ", "
						+ sqlString(schedule.getTimeZone()) + ", "
						+ sqlString(schedule.getRunInterval()) + ", "
						+ sqlBoolean(schedule.isEnabled()) + ", "
						+ db.sqlDate(schedule.getLastModified())
					+ ")";
				doModify(q);
			}
			else
			{
				q = "update SCHEDULE_ENTRY set "
					+ "NAME = " + sqlString(schedule.getName()) + ", "
					+ "LOADING_APPLICATION_ID = " + schedule.getAppId() + ", "
					+ "ROUTINGSPEC_ID = " + schedule.getRoutingSpecId() + ", "
					+ "START_TIME = " + db.sqlDate(schedule.getStartTime()) + ", "
					+ "TIMEZONE = " + sqlString(schedule.getTimeZone()) + ", "
					+ "RUN_INTERVAL = " + sqlString(schedule.getRunInterval()) + ", "
					+ "ENABLED = " + sqlBoolean(schedule.isEnabled()) + ", "
					+ "LAST_MODIFIED = " + db.sqlDate(schedule.getLastModified())
					+ " where SCHEDULE_ENTRY_ID = " + schedule.getSchedEntryId();
				doModify(q);
			}
		}
		catch (SQLException e)
		{
			throw new DbIoException(module + ".writeScheduleEntry Error in query '" + q + "'");
		}

		return schedule;
	}

	public void deleteSchedule(long scheduleId)
		throws DbIoException, WebAppException
	{
		String q = "delete from DACQ_EVENT where SCHEDULE_ENTRY_STATUS_ID in "
			+ "(select SCHEDULE_ENTRY_STATUS_ID from SCHEDULE_ENTRY_STATUS where SCHEDULE_ENTRY_ID = "
			+ scheduleId + ")";
		doModify(q);
		
		q = "update PLATFORM_STATUS set LAST_SCHEDULE_ENTRY_STATUS_ID = null "
			+ "where LAST_SCHEDULE_ENTRY_STATUS_ID in "
			+ "(select SCHEDULE_ENTRY_STATUS_ID from SCHEDULE_ENTRY_STATUS where SCHEDULE_ENTRY_ID = "
			+ scheduleId + ")";
		doModify(q);
		
		q = "delete from SCHEDULE_ENTRY_STATUS where SCHEDULE_ENTRY_ID = "
			+ scheduleId;
		doModify(q);
		
		q = "delete from SCHEDULE_ENTRY where SCHEDULE_ENTRY_ID = " + scheduleId;
		doModify(q);
	}
	
}
