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

package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.odcsapi.beans.ApiAppRef;
import org.opendcs.odcsapi.beans.ApiDacqEvent;
import org.opendcs.odcsapi.beans.ApiInterval;
import org.opendcs.odcsapi.beans.ApiRouting;
import org.opendcs.odcsapi.beans.ApiRoutingExecStatus;
import org.opendcs.odcsapi.beans.ApiScheduleEntry;
import org.opendcs.odcsapi.beans.ApiRoutingRef;
import org.opendcs.odcsapi.beans.ApiRoutingStatus;
import org.opendcs.odcsapi.beans.ApiScheduleEntryRef;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.UserToken;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.opendcs.odcsapi.util.ApiTextUtil;

public class ApiRoutingDAO
	extends ApiDaoBase
{
	public static String module = "RoutingDAO";
	public static final String evPriorities[] = { "DEBUG3", "DEBUG2", "DEBUG1", "INFO", "WARNING", "FAILURE", "FATAL" };

	public ApiRoutingDAO(DbInterface dbi)
	{
		super(dbi, module);
	}

	public ArrayList<ApiRoutingRef> getRoutingRefs()
		throws DbException
	{
		ArrayList<ApiRoutingRef> ret = new ArrayList<ApiRoutingRef>();
		
		String q = "select rs.ID, rs.NAME, ds.NAME, rs.CONSUMERTYPE, rs.CONSUMERARG, rs.LASTMODIFYTIME"
				+ " from ROUTINGSPEC rs, DATASOURCE ds"
				+ " where rs.DATASOURCEID = ds.ID"
				+ " order by rs.ID";
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				ApiRoutingRef ref = new ApiRoutingRef();
				ref.setRoutingId(rs.getLong(1));
				ref.setName(rs.getString(2));
				ref.setDataSourceName(rs.getString(3));
				String t = rs.getString(4);
				String a = rs.getString(5);
				ref.setDestination(t + "(" + (a==null?"":a) + ")");
				ref.setLastModified(dbi.getFullDate(rs, 6));
				ret.add(ref);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public Long getRoutingId(String routingName)
		throws DbException, SQLException
	{
		String q = "select rs.ID from ROUTINGSPEC rs where lower(rs.NAME) = ?"; 
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, routingName.toLowerCase());
		try
		{
			if (rs.next())
				return rs.getLong(1);
			else
				return null;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	public ApiRouting getRouting(long routingId)
		throws DbException, WebAppException, SQLException
	{
		String q = "select rs.ID, rs.NAME, rs.DATASOURCEID, rs.ENABLEEQUATIONS, rs.OUTPUTFORMAT,"
				+ " rs.PRESENTATIONGROUPNAME, rs.SINCETIME, rs.UNTILTIME, rs.CONSUMERTYPE,"
				+ " rs.CONSUMERARG, rs.LASTMODIFYTIME, rs.ISPRODUCTION, ds.NAME, rs.OUTPUTTIMEZONE"
				+ " from ROUTINGSPEC rs, DATASOURCE ds"
				+ " where rs.id = ?"
				+ " and rs.DATASOURCEID = ds.ID";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, routingId);
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No Routing with ID=" + routingId);
			
			ApiRouting ret = new ApiRouting();
			ret.setRoutingId(rs.getLong(1));
			ret.setName(rs.getString(2));
			ret.setDataSourceId(rs.getLong(3));
			ret.setEnableEquations(ApiTextUtil.str2boolean(rs.getString(4)));
			ret.setOutputFormat(rs.getString(5));
			ret.setPresGroupName(rs.getString(6));
			ret.setSince(rs.getString(7));
			ret.setUntil(rs.getString(8));
			ret.setDestinationType(rs.getString(9));
			ret.setDestinationArg(rs.getString(10));
			ret.setLastModified(dbi.getFullDate(rs, 11));
			ret.setProduction(ApiTextUtil.str2boolean(rs.getString(12)));
			ret.setDataSourceName(rs.getString(13));
			ret.setOutputTZ(rs.getString(14));
			
			q = "select NETWORKLISTNAME from ROUTINGSPECNETWORKLIST"
				+ " where ROUTINGSPECID = ?";
			rs = doQueryPs(conn, q, routingId);
			while(rs.next())
				ret.getNetlistNames().add(rs.getString(1));
			
			q = "select PROP_NAME, PROP_VALUE"
				+ " from ROUTINGSPECPROPERTY"
				+ " where ROUTINGSPECID = ?"
				+ " order by PROP_NAME";
			rs = doQueryPs(conn, q, routingId);
			while(rs.next())
			{
				String n = rs.getString(1);
				String ln = n.toLowerCase();
				String v = rs.getString(2);
				if (ln.equals("sc:ascending_time"))
					ret.setAscendingTime(ApiTextUtil.str2boolean(v));
				else if (ln.equals("sc:rt_settle_delay"))
					ret.setSettlingTimeDelay(ApiTextUtil.str2boolean(v));
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
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	public void writeRouting(ApiRouting routing)
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		ArrayList<Object> args = new ArrayList<Object>();
		String q = "select ID from ROUTINGSPEC where lower(NAME) = ?"; 
		args.add(routing.getName());
		if (routing.getRoutingId() != null)
		{
			q = q + " and ID != ?";
			args.add(routing.getRoutingId());
		}
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write RoutingSpec with name '" + routing.getName() 
					+ "' because another RoutingSpec with id=" + rs.getLong(1) 
					+ " also has that name.");

			routing.setLastModified(new Date());
			if (routing.getRoutingId() == null)
				insert(routing);
			else
				update(routing);
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}

	}
	
	private void update(ApiRouting routing)
		throws DbException
	{
		String q = "update ROUTINGSPEC set "
			+ "NAME = ?, "
			+ "DATASOURCEID = ?, "
			+ "ENABLEEQUATIONS = ?, "
			+ "OUTPUTFORMAT = ?, "
			+ "OUTPUTTIMEZONE = ?, "
			+ "PRESENTATIONGROUPNAME = ?, "
			+ "SINCETIME = ?, "
			+ "UNTILTIME = ?, "
			+ "CONSUMERTYPE = ?, "
			+ "CONSUMERARG = ?, "
			+ "LASTMODIFYTIME = ?, "
			+ "ISPRODUCTION = ?"
			+ " where ID = ?";
		doModifyV(q, routing.getName(),
				routing.getDataSourceId(),
				sqlBoolean(routing.isEnableEquations()),
				routing.getOutputFormat(),
				routing.getOutputTZ(),
				routing.getPresGroupName(),
				routing.getSince(),
				routing.getUntil(),
				routing.getDestinationType(),
				routing.getDestinationArg(),
				dbi.sqlDateV(routing.getLastModified()),
				sqlBoolean(routing.isProduction()),
				routing.getRoutingId());
		
		deleteSubordinates(routing.getRoutingId());
		writeSubordinates(routing);
	}

	private void insert(ApiRouting routing)
		throws DbException
	{
		routing.setRoutingId(getKey("ROUTINGSPEC"));

		String q = "insert into ROUTINGSPEC(ID, NAME, DATASOURCEID, ENABLEEQUATIONS, "
				+ "OUTPUTFORMAT, OUTPUTTIMEZONE, PRESENTATIONGROUPNAME,"
				+ "SINCETIME, UNTILTIME, CONSUMERTYPE, CONSUMERARG, LASTMODIFYTIME, ISPRODUCTION)"
			+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		doModifyV(q, routing.getRoutingId(),
				routing.getName(),
				routing.getDataSourceId(),
				sqlBoolean(routing.isEnableEquations()),
				routing.getOutputFormat(),
				routing.getOutputTZ(),
				routing.getPresGroupName(),
				routing.getSince(),
				routing.getUntil(),
				routing.getDestinationType(),
				routing.getDestinationArg(),
				dbi.sqlDateV(routing.getLastModified()),
				sqlBoolean(routing.isProduction()));
		
		deleteSubordinates(routing.getRoutingId());
		writeSubordinates(routing);
	}

	private void deleteSubordinates(long routingId)
		throws DbException
	{
		String q = "delete from ROUTINGSPECNETWORKLIST where ROUTINGSPECID = ?";
		doModifyV(q, routingId);
		q = "delete from ROUTINGSPECPROPERTY where ROUTINGSPECID = ?";
		doModifyV(q, routingId);
	}

	private void writeSubordinates(ApiRouting routing)
		throws DbException
	{
		for(String nl : routing.getNetlistNames())
		{
			String q = "insert into ROUTINGSPECNETWORKLIST(ROUTINGSPECID, NETWORKLISTNAME) "
				+ "values(?, ?)";
			doModifyV(q, routing.getRoutingId(), nl);
		}
		
		Properties props = new Properties();
		ApiPropertiesUtil.copyProps(props, routing.getProperties());
		
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
				+ " values (?, ?, ?)";
			doModifyV(q, routing.getRoutingId(), pn, pv);
		}
	}
	
	public void deleteRouting(long routingId)
		throws DbException, WebAppException, SQLException
	{
		deleteSubordinates(routingId);
		
		String q = "select SCHEDULE_ENTRY_ID from SCHEDULE_ENTRY where ROUTINGSPEC_ID = ?";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, routingId);
		try
		{
			while(rs.next())
				deleteSchedule(rs.getLong(1));
		}
		catch(SQLException ex) {}
		
		q = "delete from ROUTINGSPEC where ID = ?";
		doModifyV(q, routingId);
	}

	
	public ArrayList<ApiScheduleEntryRef> getScheduleRefs()
			throws DbException
	{
		ArrayList<ApiScheduleEntryRef> ret = new ArrayList<ApiScheduleEntryRef>();
		
		// normal join with RS, inner join with Loading App, because appID may be null
		String q = "select se.SCHEDULE_ENTRY_ID, se.NAME, se.LAST_MODIFIED,"
				+ " rs.NAME, la.LOADING_APPLICATION_NAME, SE.ENABLED"
				+ " from SCHEDULE_ENTRY se"
				+ " inner join ROUTINGSPEC rs on (se.ROUTINGSPEC_ID = rs.ID and se.NAME not like '%-manual')"
				+ " left join HDB_LOADING_APPLICATION la "
				+ "	on se.LOADING_APPLICATION_ID = la.LOADING_APPLICATION_ID";

		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				ApiScheduleEntryRef ref = new ApiScheduleEntryRef();
				ref.setSchedEntryId(rs.getLong(1));
				ref.setName(rs.getString(2));
				ref.setLastModified(dbi.getFullDate(rs, 3));
				ref.setRoutingSpecName(rs.getString(4));
				ref.setAppName(rs.getString(5));;
				ref.setEnabled(ApiTextUtil.str2boolean(rs.getString(6)));

				ret.add(ref);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public ApiScheduleEntry getSchedule(long id)
		throws DbException, WebAppException, SQLException
	{
		String q = "select se.SCHEDULE_ENTRY_ID, se.NAME, se.LAST_MODIFIED,"
			+ " rs.NAME, la.LOADING_APPLICATION_NAME,"
			+ " se.LOADING_APPLICATION_ID, se.ROUTINGSPEC_ID, se.START_TIME, se.TIMEZONE, se.RUN_INTERVAL,"
			+ " se.ENABLED"
			+ " from SCHEDULE_ENTRY se"
			+ " inner join ROUTINGSPEC rs on "
			+ "(se.ROUTINGSPEC_ID = rs.ID and se.NAME not like '%-manual'"
				+ " and se.SCHEDULE_ENTRY_ID = ?)"
			+ " left join HDB_LOADING_APPLICATION la "
			+ "	on se.LOADING_APPLICATION_ID = la.LOADING_APPLICATION_ID";

		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, id);
		try
		{
			if (rs.next())
			{
				ApiScheduleEntry dse = new ApiScheduleEntry();
				dse.setSchedEntryId(rs.getLong(1));
				dse.setName(rs.getString(2));
				dse.setLastModified(dbi.getFullDate(rs, 3));
				dse.setRoutingSpecName(rs.getString(4));
				dse.setAppName(rs.getString(5));;
				dse.setAppId(rs.getLong(6));
				dse.setRoutingSpecId(rs.getLong(7));
				dse.setStartTime(dbi.getFullDate(rs, 8));
				dse.setTimeZone(rs.getString(9));
				dse.setRunInterval(rs.getString(10));
				dse.setEnabled(ApiTextUtil.str2boolean(rs.getString(11)));
				return dse;
			}
			else
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No such schedule entry with id=" + id);

		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	public ApiScheduleEntry writeSchedule(ApiScheduleEntry schedule)
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		ArrayList<Object> args = new ArrayList<Object>();
		String q = "select SCHEDULE_ENTRY_ID from SCHEDULE_ENTRY where lower(NAME) = ?";
		args.add(schedule.getName().toLowerCase());
		if (schedule.getSchedEntryId() != null)
		{
			q = q + " and SCHEDULE_ENTRY_ID != ?";
			args.add(schedule.getSchedEntryId());
		}
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write ScheduleEntry with name '" + schedule.getName() 
					+ "' because another ScheduleEntry with id=" + rs.getLong(1) 
					+ " also has that name.");

			schedule.setLastModified(new Date());
			
			if (schedule.getSchedEntryId() == null)
			{
				schedule.setSchedEntryId(getKey("SCHEDULE_ENTRY"));
	
				q = "insert into SCHEDULE_ENTRY(SCHEDULE_ENTRY_ID, NAME, LOADING_APPLICATION_ID,"
					+ " ROUTINGSPEC_ID, START_TIME, TIMEZONE, RUN_INTERVAL, ENABLED, LAST_MODIFIED)"
					+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
				doModifyV(q, schedule.getSchedEntryId(),
						schedule.getName(),
						schedule.getAppId(),
						schedule.getRoutingSpecId(),
						dbi.sqlDateV(schedule.getStartTime()),
						schedule.getTimeZone(),
						schedule.getRunInterval(),
						sqlBoolean(schedule.isEnabled()),
						dbi.sqlDateV(schedule.getLastModified()));
			}
			else
			{
				q = "update SCHEDULE_ENTRY set "
					+ "NAME = ?, "
					+ "LOADING_APPLICATION_ID = ?, "
					+ "ROUTINGSPEC_ID = ?, "
					+ "START_TIME = ?, "
					+ "TIMEZONE = ?, " 
					+ "RUN_INTERVAL = ?, "
					+ "ENABLED = ?, "
					+ "LAST_MODIFIED = ? " 
					+ " where SCHEDULE_ENTRY_ID = ?";
				doModifyV(q, schedule.getName(),
						schedule.getAppId(),
						schedule.getRoutingSpecId(),
						dbi.sqlDateV(schedule.getStartTime()),
						schedule.getTimeZone(),
						schedule.getRunInterval(),
						sqlBoolean(schedule.isEnabled()),
						dbi.sqlDateV(schedule.getLastModified()),
						schedule.getSchedEntryId());
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}

		return schedule;
	}

	public void deleteSchedule(long scheduleId)
		throws DbException, WebAppException
	{
		String q = "delete from DACQ_EVENT where SCHEDULE_ENTRY_STATUS_ID in "
			+ "(select SCHEDULE_ENTRY_STATUS_ID from SCHEDULE_ENTRY_STATUS where SCHEDULE_ENTRY_ID = ?)";
		doModifyV(q, scheduleId);
		
		q = "update PLATFORM_STATUS set LAST_SCHEDULE_ENTRY_STATUS_ID = null "
			+ "where LAST_SCHEDULE_ENTRY_STATUS_ID in "
			+ "(select SCHEDULE_ENTRY_STATUS_ID from SCHEDULE_ENTRY_STATUS where SCHEDULE_ENTRY_ID = ?)";
		doModifyV(q, scheduleId);
		
		q = "delete from SCHEDULE_ENTRY_STATUS where SCHEDULE_ENTRY_ID = ?";
		doModifyV(q, scheduleId);
		
		q = "delete from SCHEDULE_ENTRY where SCHEDULE_ENTRY_ID = ?";
		doModifyV(q, scheduleId);
	}
	
	public ArrayList<ApiRoutingStatus> getRsStatus()
		throws DbException, WebAppException
	{
		ArrayList<ApiRoutingStatus> ret = new ArrayList<ApiRoutingStatus>();
		
		String q = "select rs.ID, rs.NAME from ROUTINGSPEC rs";
		try (ApiAppDAO appDAO = new ApiAppDAO(dbi))
		{
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				ApiRoutingStatus rstat = new ApiRoutingStatus();
				rstat.setRoutingSpecId(rs.getLong(1));
				rstat.setName(rs.getString(2));
				ret.add(rstat);
			}
			q = "select se.SCHEDULE_ENTRY_ID, se.LOADING_APPLICATION_ID, se.ROUTINGSPEC_ID, "
				+ "se.RUN_INTERVAL, se.ENABLED, se.NAME "
				+ "from SCHEDULE_ENTRY se";
			
			rs = doQuery(q);
			while(rs.next())
			{
				long rsId = rs.getLong(3);
				String seName = rs.getString(6);
				ApiRoutingStatus ars = null;
				if (seName.endsWith("-manual"))
				{
					ars = new ApiRoutingStatus();
					ars.setRoutingSpecId(rsId);
					ars.setManual(true);
					ret.add(ars);
				}
				else 
					for(ApiRoutingStatus tars : ret)
					{
						if (rsId == tars.getRoutingSpecId())
						{
							ars = tars;
							break;
						}
					}
				if (ars != null)
				{
					ars.setName(seName);
					ars.setScheduleEntryId(rs.getLong(1));
					ars.setAppId(rs.getLong(2));
					ars.setRunInterval(rs.getString(4));
					ars.setEnabled(ApiTextUtil.str2boolean(rs.getString(5)));
				}
			}
			
			q = "select se.SCHEDULE_ENTRY_ID, ses.LAST_MODIFIED, ses.NUM_MESSAGES, ses.NUM_DECODE_ERRORS, ses.LAST_MESSAGE_TIME"
				+ "	from SCHEDULE_ENTRY se, SCHEDULE_ENTRY_STATUS ses"
				+ "	where se.SCHEDULE_ENTRY_ID = ses.SCHEDULE_ENTRY_ID"
				+ "	and ses.SCHEDULE_ENTRY_STATUS_ID = "
				+ "		(select max(SCHEDULE_ENTRY_STATUS_ID) from SCHEDULE_ENTRY_STATUS "
				+ "		 where SCHEDULE_ENTRY_ID = se.SCHEDULE_ENTRY_ID)";
			rs = doQuery(q);
			while(rs.next())
			{
				Long seid = rs.getLong(1);
				for(ApiRoutingStatus ars : ret)
				{
					if (seid.equals(ars.getScheduleEntryId()))
					{
						long x = rs.getLong(2);
						if (!rs.wasNull())
							ars.setLastActivity(new Date(x));
						ars.setNumMessages(rs.getInt(3));
						ars.setNumErrors(rs.getInt(4));
						x = rs.getLong(5);
						if (!rs.wasNull())
							ars.setLastMsgTime(new Date(x));
						break;
					}
				}
			}
			
			ArrayList<ApiAppRef> appRefs = appDAO.getAppRefs();
			for(ApiRoutingStatus ars : ret)
				if (ars.getAppId() != null)
					for(ApiAppRef appRef : appRefs)
						if (ars.getAppId().equals(appRef.getAppId()))
						{
							ars.setAppName(appRef.getAppName());
							break;
						}
			
			Collections.sort(ret,
				new Comparator<ApiRoutingStatus>()
				{
					@Override
					public int compare(ApiRoutingStatus o1, ApiRoutingStatus o2)
					{
						long did = o1.getRoutingSpecId() - o2.getRoutingSpecId();
						return did < 0 ? -1 :
							did > 0 ? 1 :
							o1.getName().compareTo(o2.getName());
					}
				
				});

			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public ArrayList<ApiRoutingExecStatus> getRoutingExecStatus(Long scheduleEntryId)
		throws DbException
	{
		ArrayList<ApiRoutingExecStatus> ret = new ArrayList<ApiRoutingExecStatus>();
		
		String q = "select rs.ID, se.NAME, se.SCHEDULE_ENTRY_ID,"
				+ "	ses.SCHEDULE_ENTRY_STATUS_ID, ses.RUN_START_TIME,"
				+ "	ses.RUN_COMPLETE_TIME, ses.LAST_MESSAGE_TIME, ses.HOSTNAME,"
				+ "	ses.RUN_STATUS, ses.NUM_MESSAGES, ses.NUM_DECODE_ERRORS,"
				+ "	ses.NUM_PLATFORMS, ses.LAST_SOURCE, ses.LAST_CONSUMER,"
				+ "	ses.LAST_MODIFIED"
				+ " from ROUTINGSPEC rs, SCHEDULE_ENTRY_STATUS ses, SCHEDULE_ENTRY se"
				+ " where ses.SCHEDULE_ENTRY_ID = se.SCHEDULE_ENTRY_ID"
				+ " and se.ROUTINGSPEC_ID = rs.ID"
				+ " and se.SCHEDULE_ENTRY_ID = ?"
				+ " order by ses.RUN_START_TIME desc";
		
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, scheduleEntryId);
			while(rs.next())
			{
				ApiRoutingExecStatus res = new ApiRoutingExecStatus();
				res.setRoutingSpecId(rs.getLong(1));
				res.setScheduleEntryId(scheduleEntryId);
				res.setRoutingExecId(rs.getLong(4));
				res.setRunStart(new Date(rs.getLong(5)));
				long x = rs.getLong(6);
				if (!rs.wasNull())
					res.setRunStop(new Date(x));
				x = rs.getLong(7);
				if (!rs.wasNull())
					res.setLastMsgTime(new Date(x));
				res.setHostname(rs.getString(8));
				res.setRunStatus(rs.getString(9));
				res.setNumMessages(rs.getInt(10));
				res.setNumErrors(rs.getInt(11));
				res.setNumPlatforms(rs.getInt(12));
				res.setLastInput(rs.getString(13));
				res.setLastOutput(rs.getString(14));
				res.setLastActivity(new Date(rs.getLong(15)));
				ret.add(res);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
				
		return ret;
	}
	
	public ArrayList<ApiDacqEvent> getDacqEvents(Long appId, Long routingExecId,
		Long platformId, String backlog, UserToken userToken)
		throws DbException
	{
		ArrayList<ApiDacqEvent> ret = new ArrayList<ApiDacqEvent>();
		
		String q = "select DACQ_EVENT_ID, SCHEDULE_ENTRY_STATUS_ID, PLATFORM_ID, "
			+ "EVENT_TIME, EVENT_PRIORITY, SUBSYSTEM, MSG_RECV_TIME, EVENT_TEXT, "
			+ "LOADING_APPLICATION_ID "
			+ "from DACQ_EVENT";
		ArrayList<Object> args = new ArrayList<Object>();
		String c = "where";
		if (appId != null)
		{
			q = q + " where LOADING_APPLICATION_ID = ?";
			c = "and";
			args.add(appId);
		}
		if (routingExecId != null)
		{
			q = q + " " + c + " SCHEDULE_ENTRY_STATUS_ID = ?";
			c = "and";
			args.add(routingExecId);
		}
		if (platformId != null)
		{
			q = q + " " + c + " PLATFORM_ID = ?";
			c = "and";
			args.add(routingExecId);
		}
		
		if (backlog != null && backlog.trim().length() > 0)
		{
			if (backlog.equalsIgnoreCase("last"))
			{
				if (userToken.getLastDacqEventId() != null)
				{	
					q = q + " " + c + " DACQ_EVENT_ID > ?";
					args.add(userToken.getLastDacqEventId());
				}
			}
			else
				try(ApiRefListDAO rlDao = new ApiRefListDAO(this.dbi))
				{
					HashMap<Long, ApiInterval> intmap = rlDao.getIntervals();
					for(ApiInterval intv : intmap.values())
					{
						if (backlog.equalsIgnoreCase(intv.getName()))
						{
							Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
							cal.setTimeInMillis(System.currentTimeMillis());
							int cconst = 
								intv.getCalConstant().equalsIgnoreCase("second") ? Calendar.SECOND :
								intv.getCalConstant().equalsIgnoreCase("minute") ? Calendar.MINUTE :
								intv.getCalConstant().equalsIgnoreCase("hour") ? Calendar.HOUR_OF_DAY :
								intv.getCalConstant().equalsIgnoreCase("day") ? Calendar.DAY_OF_YEAR :
								intv.getCalConstant().equalsIgnoreCase("week") ? Calendar.WEEK_OF_YEAR :
								intv.getCalConstant().equalsIgnoreCase("month") ? Calendar.MONTH :
								intv.getCalConstant().equalsIgnoreCase("year") ? Calendar.YEAR : -1;
							if (cconst != -1)
							{
								cal.add(cconst, -intv.getCalMultilier());
								q = q + " " + c + " EVENT_TIME >= ?";
								args.add(cal.getTimeInMillis());
								userToken.setLastDacqEventId(null);
							}
							
							break;
						}
					}
				}
		}
		
		q = q + " ORDER BY DACQ_EVENT_ID";

		try(ApiAppDAO appDAO = new ApiAppDAO(dbi))
		{
			Connection conn = null;
			ArrayList<ApiAppRef> appRefs = appDAO.getAppRefs();
			ResultSet rs = doQueryPs(conn, q, args.toArray());
			while(rs.next())
			{
				ApiDacqEvent ev = new ApiDacqEvent();
				ev.setEventId(rs.getLong(1)); // not null
				long x = rs.getLong(2);
				if (!rs.wasNull())
					ev.setRoutingExecId(x);
				x = rs.getLong(3);
				if (!rs.wasNull())
					ev.setPlatformId(x);
				ev.setEventTime(new Date(rs.getLong(4)));
				int p = rs.getInt(5);
				if (p >= 0 && p < evPriorities.length)
					ev.setPriority(evPriorities[p]);
				else
					ev.setPriority("INFO");
				ev.setSubsystem(rs.getString(6));
				x = rs.getLong(7);
				if (!rs.wasNull())
					ev.setMsgRecvTime(new Date(x));
				ev.setEventText(rs.getString(8));
				x = rs.getLong(9);
				if (!rs.wasNull())
				{
					ev.setAppId(x);
					for(ApiAppRef ar : appRefs)
						if (x == ar.getAppId())
						{
							ev.setAppName(ar.getAppName());
							break;
						}
				}
				ret.add(ev);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}

		if (ret.size() > 0)
			userToken.setLastDacqEventId(ret.get(ret.size()-1).getEventId());
		return ret;
	}
}
