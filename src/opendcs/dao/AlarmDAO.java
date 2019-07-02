/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.4  2019/06/10 19:37:33  mmaloney
 * Added Screening I/O
 *
 * Revision 1.3  2019/05/10 18:35:25  mmaloney
 * dev
 *
 * Revision 1.2  2019/03/05 20:48:09  mmaloney
 * Support new table names for ALARM
 *
 * Revision 1.1  2019/03/05 14:53:02  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.5  2018/03/23 20:12:19  mmaloney
 * Added 'Enabled' flag for process and file monitors.
 *
 * Revision 1.4  2017/05/17 20:36:40  mmaloney
 * First working version.
 *
 * Revision 1.3  2017/03/30 20:55:20  mmaloney
 * Alarm and Event monitoring capabilities for 6.4 added.
 *
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package opendcs.dao;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.BadScreeningException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.alarm.Alarm;
import decodes.tsdb.alarm.AlarmConfig;
import decodes.tsdb.alarm.AlarmEvent;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.AlarmScreening;
import decodes.tsdb.alarm.EmailAddr;
import decodes.tsdb.alarm.FileMonitor;
import decodes.tsdb.alarm.ProcessMonitor;
import opendcs.dai.AlarmDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;

/**
 * Implements AlarmDAI for reading/writing from SQL database.
 * @author mmaloney
 */
public class AlarmDAO extends DaoBase implements AlarmDAI
{
	private static String module = "AlarmDAO";
	private static String grpColumns = "ALARM_GROUP_ID, ALARM_GROUP_NAME, "
		+ "LAST_MODIFIED";
	private static String fileMonColumns = "ALARM_GROUP_ID, PATH, PRIORITY, "
		+ "MAX_FILES, MAX_FILES_HINT, MAX_LMT, MAX_LMT_HINT, ALARM_ON_DELETE, "
		+ "ON_DELETE_HINT, MAX_SIZE, MAX_SIZE_HINT, ALARM_ON_EXISTS, "
		+ "ON_EXISTS_HINT, ENABLED";
	private static String alarmEventTable = "ALARM_EVENT";
	private static String alarmEventColumns = "ALARM_EVENT_ID, ALARM_GROUP_ID, "
			+ "LOADING_APPLICATION_ID, PRIORITY, PATTERN";
	
	private static String alarmScreeningColumns =
		"screening_id, screening_name, site_id, datatype_id, "
		+ "start_date_time, last_modified, enabled, alarm_group_id, screening_desc";
	private static String alarmLimitSetColumns =
		"limit_set_id, screening_id, season_name, reject_high, critical_high, "
		+ "warning_high, warning_low, critical_low, reject_low, "
		+ "stuck_duration, stuck_tolerance, stuck_min_to_check, stuck_max_gap, "
		+ "roc_interval, reject_roc_high, critical_roc_high, "
		+ "warning_roc_high, warning_roc_low, critical_roc_low, reject_roc_low, "
		+ "missing_period, missing_interval, missing_max_values, hint_text";
	private static String alarmCurrentColumns = 
		"ts_id, limit_set_id, assert_time, data_value, data_time, alarm_flags, message, "
		+ "last_notification_sent";
	private static String alarmHistoryColumns = 
			"ts_id, limit_set_id, assert_time, data_value, data_time, alarm_flags, message, "
			+ "end_time, cancelled_by";

	// Objects in cache never expire (a million hours), but cache is reloaded periodically
	protected static DbObjectCache<AlarmScreening> screeningCache = 
			new DbObjectCache<AlarmScreening>(Long.MAX_VALUE, false);
	private long lastCacheLoadMsec = 0L;
	private static final long CACHE_RELOAD_MSEC = 1 * 60 * 60 * 1000L;

	/**
	 * @param tsdb
	 */
	public AlarmDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "AlarmDAO");
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_17)
		{
			alarmEventTable = "ALARM_DEF";
			alarmEventColumns = "ALARM_DEF_ID, ALARM_GROUP_ID, "
				+ "LOADING_APPLICATION_ID, PRIORITY, PATTERN";
		}
		screeningCache.testMode = true;
	}

	
	/* (non-Javadoc)
	 * @see covesw.azul.alarm.dao.AlarmDAI#check(covesw.azul.alarm.AlarmConfig)
	 */
	@Override
	public boolean check(AlarmConfig cfg) throws DbIoException
	{
		class LP { DbKey gid; long lmt; LP(DbKey g, long l) { gid = g; lmt = l; }};
		ArrayList<LP> lps = new ArrayList<LP>();
		String q = "select ALARM_GROUP_ID, LAST_MODIFIED from ALARM_GROUP order by LAST_MODIFIED";
		String what = "reading database LMTs";
		try
		{
			int mods = 0;
			ResultSet rs = this.doQuery(q);
			while(rs.next())
				lps.add(new LP(DbKey.createDbKey(rs, 1), rs.getLong(2)));
			for(AlarmGroup grp : cfg.getGroups())
				grp.setChecked(false);
		nextLP:
			for(LP lp : lps)
			{
				for(AlarmGroup grp : cfg.getGroups())
					if (!grp.isChecked() && lp.gid.equals(grp.getAlarmGroupId()))
					{
						grp.setChecked(true);
						if (lp.lmt > grp.getLastModifiedMsec())
						{
							what = "reading group '" + grp.getName() + "'";
							readGroup(grp);
							mods++;
						}
						continue nextLP;
					}
				// Got to here means that lp doesn't yet exist in cfg.
				AlarmGroup grp = new AlarmGroup(lp.gid);
				readGroup(grp);
				grp.setChecked(true);
				cfg.getGroups().add(grp);
				mods++;
			}
			for(Iterator<AlarmGroup> git = cfg.getGroups().iterator(); git.hasNext(); )
			{
				AlarmGroup grp = git.next();
				if (!grp.isChecked())
				{
					git.remove();
					mods++;
				}
			}
			return mods > 0;
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error checking groups while " + what + ": " + ex); 
		}
	}

	/**
	 * Read the group from the database.
	 * @param grp must contain the DbKey ID.
	 */
	private void readGroup(AlarmGroup grp)
		throws DbIoException
	{
		LoadingAppDAI appDAO = db.makeLoadingAppDAO();
		String q = "select " + grpColumns + " from ALARM_GROUP where ALARM_GROUP_ID = "
			+ grp.getAlarmGroupId();
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
			{
				grp.setName(rs.getString(2));
				grp.setLastModifiedMsec(rs.getLong(3));
			}
			
			q = "select ADDR from EMAIL_ADDR where ALARM_GROUP_ID = " + grp.getAlarmGroupId();
			grp.getEmailAddrs().clear();
			rs = doQuery(q);
			while(rs.next())
				grp.getEmailAddrs().add(new EmailAddr(rs.getString(1)));
			
			q = "select " + fileMonColumns + " from FILE_MONITOR where ALARM_GROUP_ID = "
				+ grp.getAlarmGroupId() + " order by PATH";
			grp.getFileMonitors().clear();
			rs = doQuery(q);
			while(rs.next())
			{
				FileMonitor fm = new FileMonitor(rs.getString(2));
				fm.setPriority(rs.getInt(3));
				fm.setMaxFiles(rs.getInt(4));
				if (rs.wasNull()) fm.setMaxFiles(0);
				fm.setMaxFilesHint(rs.getString(5));
				fm.setMaxLMT(rs.getString(6));
				fm.setMaxLMTHint(rs.getString(7));
				fm.setAlarmOnDelete(TextUtil.str2boolean(rs.getString(8)));
				fm.setAlarmOnDeleteHint(rs.getString(9));
				fm.setMaxSize(rs.getLong(10));
				if (rs.wasNull()) fm.setMaxSize(0L);
				fm.setMaxSizeHint(rs.getString(11));
				fm.setAlarmOnExists(TextUtil.str2boolean(rs.getString(12)));
				fm.setAlarmOnExistsHint(rs.getString(13));
				fm.setEnabled(TextUtil.str2boolean(rs.getString(14)));
				if (rs.wasNull())
					fm.setEnabled(true);
				grp.getFileMonitors().add(fm);
			}
			
			q = "select LOADING_APPLICATION_ID, ENABLED from PROCESS_MONITOR where ALARM_GROUP_ID = "
				+ grp.getAlarmGroupId() + " order by LOADING_APPLICATION_ID";
			grp.getProcessMonitors().clear();
			rs = doQuery(q);
			while(rs.next())
			{
				ProcessMonitor pm = new ProcessMonitor(DbKey.createDbKey(rs, 1));
				pm.setEnabled(TextUtil.str2boolean(rs.getString(2)));
				if (rs.wasNull())
					pm.setEnabled(true);
				grp.getProcessMonitors().add(pm);
			}
			
			for(ProcessMonitor pm : grp.getProcessMonitors())
			{
				q = "select " + alarmEventColumns + " from " + alarmEventTable 
					+ " where "
					+ "ALARM_GROUP_ID = " + grp.getAlarmGroupId()
					+ " and LOADING_APPLICATION_ID = " + pm.getAppId();
				rs = doQuery(q);
				while(rs.next())
				{
					AlarmEvent def = new AlarmEvent(DbKey.createDbKey(rs, 1));
					def.setPriority(rs.getInt(4));
					def.setPattern(rs.getString(5));
					pm.getDefs().add(def);
				}
				
				try
				{
					pm.setAppInfo(appDAO.getComputationApp(pm.getAppId()));
				}
				catch (NoSuchObjectException e)
				{
					Logger.instance().warning(module + " ProcessMonitor with invalid app ID="
						+ pm.getAppId());
				}
			}
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error reading group query: " + q + ": " + ex);
		}
		finally
		{
			appDAO.close();
		}
	}


	/* (non-Javadoc)
	 * @see covesw.azul.alarm.dao.AlarmDAI#write(covesw.azul.alarm.AlarmGroup)
	 */
	@Override
	public void write(AlarmGroup grp) throws DbIoException
	{
		if (grp.getName() == null)
		{
			Logger.instance().warning(module + " cannot write group with no name!");
			return;
		}

		// Null key could mean overwriting from an XML file or a new group.
		if (DbKey.isNull(grp.getAlarmGroupId()))
			grp.setAlarmGroupId(groupName2id(grp.getName()));
	
		String q = null;
		grp.setLastModifiedMsec(System.currentTimeMillis());
		if (!DbKey.isNull(grp.getAlarmGroupId()))
		{
			q = "update ALARM_GROUP set "
				+ "ALARM_GROUP_NAME = " + sqlString(grp.getName())
				+ ", LAST_MODIFIED = " + grp.getLastModifiedMsec();
			doModify(q);
			
			// Delete file monitors, email addresses, and process monitors.
			// They will be re-inserted below.
			q = "delete from ALARM_EVENT where alarm_group_id = " + grp.getAlarmGroupId();
			doModify(q);
			q = "delete from PROCESS_MONITOR where alarm_group_id = " + grp.getAlarmGroupId();
			doModify(q);
			q = "delete from FILE_MONITOR where ALARM_GROUP_ID = " + grp.getAlarmGroupId();
			doModify(q);
			q = "delete from EMAIL_ADDR where alarm_group_id = " + grp.getAlarmGroupId();
			doModify(q);
		}
		else
		{
			grp.setAlarmGroupId(getKey("ALARM_GROUP"));
			q = "insert into ALARM_GROUP(" + grpColumns + ") values ("
					+ grp.getAlarmGroupId() + ", "
					+ sqlString(grp.getName()) + ", "
					+ grp.getLastModifiedMsec() + ")";
			doModify(q);
		}
		
		
		for(EmailAddr addr : grp.getEmailAddrs())
		{
			q = "insert into EMAIL_ADDR(ALARM_GROUP_ID, ADDR) values ("
				+ grp.getAlarmGroupId() + ", " + sqlString(addr.getAddr()) + ")";
			doModify(q);
		}
		
		for(FileMonitor fm : grp.getFileMonitors())
		{
			q = "insert into FILE_MONITOR(" + fileMonColumns + ") values ("
				+ grp.getAlarmGroupId() + ", "
				+ sqlString(fm.getPath()) + ", "
				+ fm.getPriority() + ", "
				+ fm.getMaxFiles() + ", "
				+ sqlString(fm.getMaxFilesHint()) + ", "
				+ sqlString(fm.getMaxLMT()) + ", "
				+ sqlString(fm.getMaxLMTHint()) + ", "
				+ sqlBoolean(fm.isAlarmOnDelete()) + ", "
				+ sqlString(fm.getAlarmOnDeleteHint()) + ", "
				+ fm.getMaxSize() + ", "
				+ sqlString(fm.getMaxSizeHint()) + ", "
				+ sqlBoolean(fm.isAlarmOnExists()) + ", "
				+ sqlString(fm.getAlarmOnExistsHint()) + ", "
				+ sqlBoolean(fm.isEnabled()) + ")";
			doModify(q);
		}
		
		for(ProcessMonitor pm : grp.getProcessMonitors())
		{
			// If read from XML, the pm will have a name but no app ID.
			// Lookup the app ID which must exist in the database.
			if (DbKey.isNull(pm.getAppId()))
			{
				LoadingAppDAI appDAO = db.makeLoadingAppDAO();
				try
				{
					pm.setAppInfo(appDAO.getComputationApp(pm.getProcName()));
				}
				catch (NoSuchObjectException e)
				{
					Logger.instance().warning(module + " ProcessMonitor references app '"
						+ pm.getProcName() + "' which does not exist in this database -- skipped.");
					continue;
				}
				finally
				{
					appDAO.close();
				}
			}
			q = "insert into PROCESS_MONITOR(ALARM_GROUP_ID, LOADING_APPLICATION_ID, ENABLED) values ("
				+ grp.getAlarmGroupId() + ", " + pm.getAppId() + ", " + sqlBoolean(pm.isEnabled()) + ")";
			doModify(q);

			for(AlarmEvent alarmEvt : pm.getDefs())
			{
				if (DbKey.isNull(alarmEvt.getAlarmEventId()))
					alarmEvt.setAlarmEventId(getKey(alarmEventTable));
				q = "insert into " + alarmEventTable + "(" + alarmEventColumns + ") values ("
					+ alarmEvt.getAlarmEventId() + ", "
					+ grp.getAlarmGroupId() + ", "
					+ pm.getAppId() + ", "
					+ alarmEvt.getPriority() + ", "
					+ sqlString(alarmEvt.getPattern()) + ")";
				doModify(q);
			}
		}
	}

	/* (non-Javadoc)
	 * @see covesw.azul.alarm.dao.AlarmDAI#delete(decodes.sql.DbKey)
	 */
	@Override
	public boolean deleteAlarmGroup(DbKey groupID) throws DbIoException
	{
		if (!DbKey.isNull(groupID))
		{
			String q = "delete from " + alarmEventTable + " where alarm_group_id = " + groupID;
			doModify(q);
			
			q = "delete from ALARM_EVENT where alarm_group_id = " + groupID;
			doModify(q);

			q = "delete from PROCESS_MONITOR where alarm_group_id = " + groupID;
			doModify(q);

			q = "delete from FILE_MONITOR where alarm_group_id = " + groupID;
			doModify(q);
			
			q = "delete from EMAIL_ADDR where alarm_group_id = " + groupID;
			doModify(q);
			
			q = "delete from ALARM_GROUP where alarm_group_id = " + groupID;
			doModify(q);
			return true;
		}
		return false;
	}
	
	@Override
	public DbKey groupName2id(String groupName)
		throws DbIoException
	{
		String q = "select ALARM_GROUP_ID from ALARM_GROUP where upper(ALARM_GROUP_NAME) = "
			+ sqlString(groupName.toUpperCase());
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				return DbKey.createDbKey(rs, 1);
			else
				return DbKey.NullKey;
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error in query '" + q + "': " + ex);
		}
	}


	@Override
	public ArrayList<AlarmScreening> getScreenings(DbKey siteId, DbKey datatypeId)
		throws DbIoException
	{
		if (System.currentTimeMillis() - lastCacheLoadMsec >= CACHE_RELOAD_MSEC)
			fillScreeningCache();
		
		// siteId may be null (for default screening at any site) but datatype may not be null.
		if (DbKey.isNull(datatypeId))
			return null;
		
		ArrayList<AlarmScreening> ret = new ArrayList<AlarmScreening>();
	  nextScit:
		for(Iterator<AlarmScreening> scit = screeningCache.iterator(); scit.hasNext(); )
		{
			AlarmScreening as = scit.next();
			if (siteId.equals(as.getSiteId()) && datatypeId.equals(as.getDatatypeId()))
			{
				for(int idx = 0; idx < ret.size(); idx++)
					if (ret.get(idx).getStartDateTime().after(as.getStartDateTime()))
					{
						ret.add(idx, as);
						continue nextScit;
					}
				ret.add(as);
			}
		}
		return ret;
	}
	
	private void fillScreeningCache()
		throws DbIoException
	{
		screeningCache.clear();
		
		// Read all of the screening objects
		String q = "select " + alarmScreeningColumns + " from alarm_screening";
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				AlarmScreening as = new AlarmScreening();
				rs2AlarmScreening(rs, as);
				screeningCache.put(as);
			}
		}
		catch (SQLException ex)
		{
			DbIoException tt = new DbIoException("Error in query '" + q + "': " + ex);
			tt.initCause(ex);
			throw tt;
		}

		// Read all of the limit sets and assign to screening objects
		q = "select " + alarmLimitSetColumns + " from alarm_limit_set";
		rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				AlarmLimitSet als = rs2LimitSet(rs);
				AlarmScreening as = screeningCache.getByKey(als.getScreeningId());
				if (as == null)
				{
					warning("ALARM_LIMIT_SET with LIMIT_SET_ID="
						+ als.getLimitSetId() + " refers to non-existent screening ID="
						+ als.getScreeningId() + " -- ignored.");
				}
				as.addLimitSet(als);
			}
		}
		catch (SQLException ex)
		{
			DbIoException tt = new DbIoException("Error in query '" + q + "': " + ex);
			tt.initCause(ex);
			throw tt;
		}
	}
	
	private void rs2AlarmScreening(ResultSet rs, AlarmScreening as)
		throws SQLException
	{
		as.setScreeningId(DbKey.createDbKey(rs, 1));
		as.setScreeningName(rs.getString(2));
		as.setSiteId(DbKey.createDbKey(rs, 3));
		as.setDatatypeId(DbKey.createDbKey(rs, 4));
		long msec = rs.getLong(5);
		as.setStartDateTime(rs.wasNull() || msec==0 ? null : new Date(msec));
		msec = rs.getLong(6);
		as.setLastModified(rs.wasNull() || msec==0 ? null : new Date(msec));
		as.setEnabled(TextUtil.str2boolean(rs.getString(7)));
		as.setAlarmGroupId(DbKey.createDbKey(rs, 8));
		as.setDescription(rs.getString(9));
		as.setTimeLoaded(new Date());
	}
	
	/**
	 * Construct and return a new limit set from the passed result set.
	 * 
	 * @param rs
	 * @return new limit set, or null if invalid limit set
	 * @throws SQLException
	 */
	private AlarmLimitSet rs2LimitSet(ResultSet rs)
		throws SQLException
	{
		AlarmLimitSet als = new AlarmLimitSet();
		als.setLimitSetId(DbKey.createDbKey(rs, 1));
		als.setScreeningId(DbKey.createDbKey(rs, 2));
		als.setSeasonName(rs.getString(3));
		double d = rs.getDouble(4);
		if (!rs.wasNull())
			als.setRejectHigh(d);
		d = rs.getDouble(5);
		if (!rs.wasNull())
			als.setCriticalHigh(d);
		d = rs.getDouble(6);
		if (!rs.wasNull())
			als.setWarningHigh(d);
		d = rs.getDouble(7);
		if (!rs.wasNull())
			als.setWarningLow(d);
		d = rs.getDouble(8);
		if (!rs.wasNull())
			als.setCriticalLow(d);
		d = rs.getDouble(9);
		if (!rs.wasNull())
			als.setRejectLow(d);
		als.setStuckDuration(rs.getString(10));
		d = rs.getDouble(11);
		if (!rs.wasNull())
			als.setStuckTolerance(d);
		d = rs.getDouble(12);
		if (!rs.wasNull())
			als.setMinToCheck(d);
		als.setMaxGap(rs.getString(13));
		als.setRocInterval(rs.getString(14));
		
		d = rs.getDouble(15);
		if (!rs.wasNull())
			als.setRejectRocHigh(d);
		d = rs.getDouble(16);
		if (!rs.wasNull())
			als.setCriticalRocHigh(d);
		d = rs.getDouble(17);
		if (!rs.wasNull())
			als.setWarningRocHigh(d);
		d = rs.getDouble(18);
		if (!rs.wasNull())
			als.setWarningRocLow(d);
		d = rs.getDouble(19);
		if (!rs.wasNull())
			als.setCriticalRocLow(d);
		d = rs.getDouble(20);
		if (!rs.wasNull())
			als.setRejectRocLow(d);
		
		als.setMissingPeriod(rs.getString(21));
		als.setMissingInterval(rs.getString(22));
		als.setMaxMissingValues(rs.getInt(23));
		
		als.setHintText(rs.getString(24));

//		as.addLimitSet(als);

		return als;
	}


	@Override
	public void writeScreening(AlarmScreening as)
		throws DbIoException, BadScreeningException
	{
		if (DbKey.isNull(as.getDatatypeId()))
			throw new BadScreeningException("Datatype cannot be null.");
		
		// NOTE: Site can be null to establish a default screening for a datatype
		
		// If no key, try to lookup from the name and start time.
		if (DbKey.isNull(as.getScreeningId()))
			as.setScreeningId(screeningName2Id(as.getScreeningName(), as.getStartDateTime()));
		
		Date now = new Date();
		AlarmScreening oldas = null;
		if (!DbKey.isNull(as.getScreeningId())
		 && (oldas = readAlarmScreening(as.getScreeningId())) != null)
		{
			StringBuilder qb = new StringBuilder("update alarm_screening set ");
			
			qb.append("last_modified = " + now.getTime());

			if (!TextUtil.strEqual(as.getScreeningName(), oldas.getScreeningName()))
				qb.append(", screening_name = " + sqlString(as.getScreeningName()));
			if (!as.getSiteId().equals(oldas.getSiteId()))
				qb.append(", site_id = " + as.getSiteId());
			if (!as.getDatatypeId().equals(oldas.getDatatypeId()))
				qb.append(", datatype_id = " + as.getDatatypeId());
			if (!dateEqual(as.getStartDateTime(), oldas.getStartDateTime()))
				qb.append(", start_date_time = " 
					+ (as.getStartDateTime() == null ? "null" : as.getStartDateTime().getTime()));
			if (as.isEnabled() != oldas.isEnabled())
				qb.append(", enabled = " + sqlBoolean(as.isEnabled()));
			if (!as.getAlarmGroupId().equals(oldas.getAlarmGroupId()))
				qb.append(", alarm_group_id = " + as.getAlarmGroupId());
			if (!TextUtil.strEqual(as.getDescription(), oldas.getDescription()))
				qb.append(", desc = " + sqlString(as.getDescription()));
			
			qb.append(" where screening_id = " + as.getScreeningId());
			doModify(qb.toString());
		}
		else
		{
			as.setScreeningId(getKey("ALARM_SCREENING"));
			String q = "insert into alarm_screening(" + alarmScreeningColumns
				+ ") values (" + as.getScreeningId()
				+ ", " + sqlString(as.getScreeningName())
				+ ", " + as.getSiteId()
				+ ", " + as.getDatatypeId()
				+ ", " + (as.getStartDateTime()==null ? "null" 
						: (""+as.getStartDateTime().getTime()))
				+ ", " + now.getTime()
				+ ", " + sqlBoolean(as.isEnabled())
				+ ", " + as.getAlarmGroupId()
				+ ", " + sqlString(as.getDescription())
				+ ")";
			doModify(q);
		}
		
		as.setLastModified(now); // Always update LMT.
		
		Logger.instance().debug1(module + " adding screening id=" + as.getKey()
			+ " '" + as.getUniqueName() + "' to cache.");
		screeningCache.put(as);
		
		ArrayList<DbKey> limitSetIds = new ArrayList<DbKey>();
		for(AlarmLimitSet als : as.getLimitSets())
		{
			// If this is new screening, set the screening_id in limit set to match new ID
			als.setScreeningId(as.getScreeningId());
			writeLimitSet(als);
			limitSetIds.add(als.getLimitSetId());
		}
		
		// Now there may have been old limit sets that are no longer in the screening and
		// we should delete them. Delete any limit set that I didn't just write.
		String q = "delete from alarm_limit_set where screening_id = " + as.getScreeningId()
			+ " and limit_set_id not in (";
		for(int idx = 0; idx < limitSetIds.size(); idx++)
			q = q + (idx > 0 ? ", " : "") + limitSetIds.get(idx);
		q = q + ")";
		doModify(q);
	}
	
	// compare two dates and allow for null
	private boolean dateEqual(Date d1, Date d2)
	{
		return d1 == null ? d2 == null : 
			   d2 != null && d1.getTime() == d2.getTime();
	}


	@Override
	public void writeLimitSet(AlarmLimitSet als)
		throws DbIoException
	{
		String q = "";
		try
		{
			if (DbKey.isNull(als.getLimitSetId()))
			{
				q = "select limit_set_id from alarm_limit_set where "
					+ "screening_id = " + als.getScreeningId()
					+ " and season_name "
					+ (als.getSeasonName()==null ? "is null" : ("= " + sqlString(als.getSeasonName())));
				ResultSet rs = doQuery(q);
				if (rs != null && rs.next())
					als.setLimitSetId(DbKey.createDbKey(rs, 1));
			}
			
			AlarmLimitSet oldls = null;
			if (!DbKey.isNull(als.getLimitSetId())
			 && (oldls = readLimitSet(als.getLimitSetId())) != null)
			{
				// UPDATE the existing limit set
				StringBuilder qb = new StringBuilder("update alarm_limit_set set ");
				int n = 0;
				if (!TextUtil.strEqual(als.getSeasonName(), oldls.getSeasonName()))
				{
					qb.append("season_name = " + sqlString(als.getSeasonName()));
					n++;
				}
				if (als.getRejectHigh() != oldls.getRejectHigh())
				{
					qb.append((n>0 ? ", " : "")
						+ "reject_high = "
						+ (als.getRejectHigh() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getRejectHigh()));
					n++;
				}
				if (als.getCriticalHigh() != oldls.getCriticalHigh())
				{
					qb.append((n>0 ? ", " : "")
						+ "critical_high = "
						+ (als.getCriticalHigh() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getCriticalHigh()));
					n++;
				}
				if (als.getWarningHigh() != oldls.getWarningHigh())
				{
					qb.append((n>0 ? ", " : "")
						+ "warning_high = "
						+ (als.getWarningHigh() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getWarningHigh()));
					n++;
				}
				if (als.getWarningLow() != oldls.getWarningLow())
				{
					qb.append((n>0 ? ", " : "")
						+ "warning_low = "
						+ (als.getWarningLow() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getWarningLow()));
					n++;
				}
				if (als.getCriticalLow() != oldls.getCriticalLow())
				{
					qb.append((n>0 ? ", " : "")
						+ "critical_low = "
						+ (als.getCriticalLow() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getCriticalLow()));
					n++;
				}
				if (als.getRejectLow() != oldls.getRejectLow())
				{
					qb.append((n>0 ? ", " : "")
						+ "reject_low = "
						+ (als.getRejectLow() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getRejectLow()));
					n++;
				}
				if (!TextUtil.strEqual(als.getStuckDuration(), oldls.getStuckDuration()))
				{
					qb.append((n>0 ? ", " : "") 
						+ "stuck_duration = " + sqlString(als.getStuckDuration()));
					n++;
				}
				if (als.getStuckTolerance() != oldls.getStuckTolerance())
				{
					qb.append((n>0 ? ", " : "")
						+ "stuck_tolerance = "
						+ (als.getStuckTolerance() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getStuckTolerance()));
					n++;
				}
				if (als.getMinToCheck() != oldls.getMinToCheck())
				{
					qb.append((n>0 ? ", " : "")
						+ "stuck_min_to_check = "
						+ (als.getMinToCheck() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getMinToCheck()));
					n++;
				}
				if (!TextUtil.strEqual(als.getMaxGap(), oldls.getMaxGap()))
				{
					qb.append((n>0 ? ", " : "") 
						+ "stuck_max_gap = " + sqlString(als.getMaxGap()));
					n++;
				}
				if (!TextUtil.strEqual(als.getRocInterval(), oldls.getRocInterval()))
				{
					qb.append((n>0 ? ", " : "") 
						+ "roc_interval = " + sqlString(als.getRocInterval()));
					n++;
				}
				if (als.getRejectRocHigh() != oldls.getRejectRocHigh())
				{
					qb.append((n>0 ? ", " : "")
						+ "reject_roc_high = "
						+ (als.getRejectRocHigh() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getRejectRocHigh()));
					n++;
				}
				if (als.getCriticalRocHigh() != oldls.getCriticalRocHigh())
				{
					qb.append((n>0 ? ", " : "")
						+ "critical_roc_high = "
						+ (als.getCriticalRocHigh() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getCriticalRocHigh()));
					n++;
				}
				if (als.getWarningRocHigh() != oldls.getWarningRocHigh())
				{
					qb.append((n>0 ? ", " : "")
						+ "warning_roc_high = "
						+ (als.getWarningRocHigh() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getWarningRocHigh()));
					n++;
				}
				if (als.getWarningRocLow() != oldls.getWarningRocLow())
				{
					qb.append((n>0 ? ", " : "")
						+ "warning_roc_low = "
						+ (als.getWarningRocLow() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getWarningRocLow()));
					n++;
				}
				if (als.getCriticalRocLow() != oldls.getCriticalRocLow())
				{
					qb.append((n>0 ? ", " : "")
						+ "critical_roc_low = "
						+ (als.getCriticalRocLow() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getCriticalRocLow()));
					n++;
				}
				if (als.getRejectRocLow() != oldls.getRejectRocLow())
				{
					qb.append((n>0 ? ", " : "")
						+ "reject_roc_low = "
						+ (als.getRejectRocLow() == AlarmLimitSet.UNASSIGNED_LIMIT ? "null" :
						   als.getRejectRocLow()));
					n++;
				}
				if (!TextUtil.strEqual(als.getMissingPeriod(), oldls.getMissingPeriod()))
				{
					qb.append((n>0 ? ", " : "") 
						+ "missing_period = " + sqlString(als.getMissingPeriod()));
					n++;
				}
				if (!TextUtil.strEqual(als.getMissingInterval(), oldls.getMissingInterval()))
				{
					qb.append((n>0 ? ", " : "") 
						+ "missing_interval = " + sqlString(als.getMissingInterval()));
					n++;
				}
				if (als.getMaxMissingValues() != oldls.getMaxMissingValues())
				{
					qb.append((n>0 ? ", " : "")
						+ "missing_max_values = " + als.getMaxMissingValues());
					n++;
				}
				if (!TextUtil.strEqual(als.getHintText(), oldls.getHintText()))
				{
					qb.append((n>0 ? ", " : "") 
						+ "hint_text = " + sqlString(als.getHintText()));
					n++;
				}

				if (n > 0)
				{
					qb.append(" where limit_set_id = " + als.getLimitSetId());
					doModify(q = qb.toString());
				}
				// Else nothing to update.
			}
			else // this is a new limit set
			{
				als.setLimitSetId(getKey("alarm_limit_set"));
				q = "insert into ALARM_LIMIT_SET(" + alarmLimitSetColumns + ") "
					+ "values("
					+ als.getLimitSetId() + ", "
					+ als.getScreeningId() + ", "
					+ sqlString(als.getSeasonName()) + ", "
					+ (als.getRejectHigh()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getRejectHigh()) + ", "
					+ (als.getCriticalHigh()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getCriticalHigh()) + ", "
					+ (als.getWarningHigh()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getWarningHigh()) + ", "
					+ (als.getWarningLow()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getWarningLow()) + ", "
					+ (als.getCriticalLow()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getCriticalLow()) + ", "
					+ (als.getRejectLow()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getRejectLow()) + ", "
					+ sqlString(als.getStuckDuration()) + ", "
					+ (als.getStuckTolerance()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getStuckTolerance()) + ", "
					+ (als.getMinToCheck()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getMinToCheck()) + ", "
					+ sqlString(als.getMaxGap()) + ", "
					+ sqlString(als.getRocInterval()) + ", "
					+ (als.getRejectRocHigh()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getRejectRocHigh()) + ", "
					+ (als.getCriticalRocHigh()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getCriticalRocHigh()) + ", "
					+ (als.getWarningRocHigh()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getWarningRocHigh()) + ", "
					+ (als.getWarningRocLow()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getWarningRocLow()) + ", "
					+ (als.getCriticalRocLow()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getCriticalRocLow()) + ", "
					+ (als.getRejectRocLow()==AlarmLimitSet.UNASSIGNED_LIMIT ? "null" : als.getRejectRocLow()) + ", "
					+ sqlString(als.getMissingPeriod()) + ", "
					+ sqlString(als.getMissingInterval()) + ", "
					+ als.getMaxMissingValues() + ", "
					+ sqlString(als.getHintText())
					+ ")";
				doModify(q);
			}
		}
		catch(SQLException ex)
		{
			String msg = "writeLimitSet SQLException in queriy '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}



	private DbKey screeningName2Id(String screeningName, Date start)
		throws DbIoException
	{
		String q = "select screening_id from alarm_screening "
			+ "where screening_name = " + sqlString(screeningName)
			+ " and start_date_time "
			+ (start == null ? "is null" : ("= " + start.getTime()));
		ResultSet rs = doQuery(q);
		try
		{
			if (rs == null || !rs.next())
				return DbKey.NullKey;
			return DbKey.createDbKey(rs, 1);
		}
		catch(SQLException ex)
		{
			warning("SQL Error in query '" + q + "': " + ex);
			return DbKey.NullKey;
		}
	}


	@Override
	public void deleteScreening(DbKey screeningId)
		throws DbIoException
	{
		String q = "select limit_set_id from alarm_limit_set where screening_id = "
			+ screeningId;
		ResultSet rs = doQuery(q);
		ArrayList<DbKey> limitSetIds = new ArrayList<DbKey>();
		String action = "Listing Limit Set IDs";
		try
		{
			while(rs != null && rs.next())
				limitSetIds.add(DbKey.createDbKey(rs, 1));
			
			for(DbKey limitSetId : limitSetIds)
			{
				action = "Removing limit set with id=" + limitSetId;
				deleteLimitSet(limitSetId);
			}
			action = "Removing ALARM_SCREENING";
			q = "delete from alarm_screening where screening_id = " + screeningId;
			doModify(q);
		}
		catch(SQLException ex)
		{
			warning("deleteScreening Error while " + action);
		}
	}


	@Override
	public void deleteLimitSet(DbKey limitSetId)
		throws DbIoException
	{
		String q = "delete from ALARM_CURRENT where limit_set_id = " + limitSetId;
		doModify(q);
		q = "delete from ALARM_HISTORY where limit_set_id = " + limitSetId;
		doModify(q);
		q = "delete from ALARM_LIMIT_SET where limit_set_id = " + limitSetId;
		doModify(q);
	}


	private AlarmLimitSet readLimitSet(DbKey limitSetId)
		throws DbIoException, SQLException
	{
		String q = "select " + alarmLimitSetColumns + " from alarm_limit_set "
			+ "where limit_set_id = " + limitSetId;
		ResultSet rs = doQuery(q);
		if (rs == null || !rs.next())
			return null;
		return rs2LimitSet(rs);
	}
	
	private AlarmScreening readAlarmScreening(DbKey screeningId)
		throws DbIoException
	{
		String q = "select " + alarmScreeningColumns + " from alarm_screening "
				+ "where screening_id = " + screeningId;
		
		ResultSet rs = doQuery(q);
		try
		{
			if (rs == null || !rs.next())
				return null;
			AlarmScreening ret = new AlarmScreening();
			rs2AlarmScreening(rs, ret);
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException("readAlarmScreening error in query '" + q + "': " + ex);
		}
	}


	@Override
	public ArrayList<AlarmScreening> getAllScreenings() throws DbIoException
	{
		fillScreeningCache();
		
		ArrayList<AlarmScreening> ret = new ArrayList<AlarmScreening>();
		for(Iterator<AlarmScreening> scit = screeningCache.iterator(); scit.hasNext(); )
			ret.add(scit.next());

		return ret;
	}
	
	@Override
	public void refreshCurrentAlarms(HashMap<DbKey, Alarm> alarmMap)
		throws DbIoException
	{
		for(Alarm alarm : alarmMap.values())
			alarm.setChecked(false);
		
		String q = "select " + alarmCurrentColumns + " from alarm_current";
		ArrayList<Alarm> needsFilling = new ArrayList<Alarm>();
		ResultSet rs = doQuery(q);
		TimeSeriesDAI tsDAO = this.db.makeTimeSeriesDAO();
		try
		{
			while(rs != null && rs.next())
			{
				DbKey tsKey = DbKey.createDbKey(rs, 1);
				Alarm alarm = alarmMap.get(tsKey);

				if (alarm == null) // alarm is new?
				{
					alarm = new Alarm();
					alarm.setTsidKey(tsKey);
					alarmMap.put(alarm.getTsidKey(), alarm);
					needsFilling.add(alarm);
				}
				else // alarm exists in the map
				{
					long assertTimeMs = rs.getLong(3);
					if (assertTimeMs < alarm.getLastDbSyncMs())
						continue; // already up to date
				}
				
				alarm.setAssertTime(new Date(rs.getLong(3)));
				alarm.setChecked(true);
				DbKey limitSetId = DbKey.createDbKey(rs, 2);
				if (!limitSetId.equals(alarm.getLimitSetId()))
				{
					alarm.setLimitSetId(limitSetId);
					alarm.setLimitSet(null);
					needsFilling.add(alarm);
				}
				alarm.setDataValue(rs.getDouble(4));
				alarm.setDataTime(new Date(rs.getLong(5)));
				alarm.setAlarmFlags(rs.getInt(6));
				alarm.setMessage(rs.getString(7));
				alarm.setLastNotificationTime(new Date(rs.getLong(8)));
			}
			
			
			q = "[filling limit set and tsids]";
			for(Alarm alarm : needsFilling)
			{
				// Fill in the Limit Set and the TSID.
				alarm.setLimitSet(readLimitSet(alarm.getLimitSetId()));
				try { alarm.setTsid(tsDAO.getTimeSeriesIdentifier(alarm.getTsidKey())); }
				catch(NoSuchObjectException ex)
				{
					warning("Invalid TsIdKey=" + alarm.getTsidKey() + " -- alarm will be removed.");
					deleteCurrentAlarm(alarm.getTsidKey());
					alarm.setChecked(false);
				}
			}
			
			// After the above, any alarm that is not 'checked' must have been deleted from the DB.
			ArrayList<DbKey> toDelete = new ArrayList<DbKey>();
			for (Alarm alarm : alarmMap.values())
				if (!alarm.isChecked())
					toDelete.add(alarm.getTsidKey());
			for(DbKey tsIdKey : toDelete)
				alarmMap.remove(tsIdKey);

		}
		catch(SQLException ex)
		{
			throw new DbIoException("readAlarmScreening error in query '" + q + "': " + ex);
		}
		finally
		{
			tsDAO.close();
		}
	}
	
//	private Alarm rs2alarm(ResultSet rs)
//		throws SQLException
//	{
//		DbKey tsKey = DbKey.createDbKey(rs, 1);
//		Alarm alarm = new Alarm();
//		alarm.setTsidKey(tsKey);
//		alarm.setAssertTime(new Date(rs.getLong(3)));
//		alarm.setLimitSetId(DbKey.createDbKey(rs, 2));
//		alarm.setDataValue(rs.getDouble(4));
//		alarm.setDataTime(new Date(rs.getLong(5)));
//		alarm.setAlarmFlags(rs.getInt(6));
//		alarm.setMessage(rs.getString(7));
//		alarm.setLastNotificationTime(new Date(rs.getLong(8)));
//
//		return alarm;
//	}

	@Override
	public void deleteCurrentAlarm(DbKey tsidKey) 
		throws DbIoException
	{
		String q = "delete from ALARM_CURRENT where ts_id = " + tsidKey;
		doModify(q);
	}


	@Override
	public void moveToHistory(Alarm alarm)
	{
		String q = "insert into alarm_history(" + alarmHistoryColumns + ") values ("
			+ alarm.getTsidKey() + ", "
			+ alarm.getLimitSetId() + ", "
			+ alarm.getAssertTime().getTime() + ", "
			+ alarm.getDataValue() + ", "
			+ alarm.getDataTime().getTime() + ", "
			+ alarm.getAlarmFlags() + ", "
			+ sqlString(alarm.getMessage()) + ", "
			+ alarm.getEndTime().getTime() + ", "
			+ sqlString(alarm.getCancelledBy())
			+ ")";
		try
		{
			doModify(q);
			deleteCurrentAlarm(alarm.getTsidKey());
		}
		catch (DbIoException ex)
		{
			Logger.instance().warning(module + " Error writing alarm for tskey=" + alarm.getTsidKey()
				+ " at time " + alarm.getAssertTime() + ": " + ex);
		}
	}
	
	@Override
	public void writeToCurrent(Alarm alarm)
	{
		String q = "delete from alarm_current where ts_id = " + alarm.getTsidKey();
		try
		{
			doModify(q);
			
			q = "insert into alarm_current(" + alarmCurrentColumns + ") values ("
					+ alarm.getTsidKey() + ", "
					+ alarm.getLimitSetId() + ", "
					+ alarm.getAssertTime().getTime() + ", "
					+ alarm.getDataValue() + ", "
					+ alarm.getDataTime().getTime() + ", "
					+ alarm.getAlarmFlags() + ", "
					+ sqlString(alarm.getMessage())
					+ ")";
			doModify(q);
		}
		catch (DbIoException ex)
		{
			Logger.instance().warning(module + " Error writing current alarm for tskey=" + alarm.getTsidKey()
				+ " at time " + alarm.getAssertTime() + ": " + ex);
		}
	}
	

}
