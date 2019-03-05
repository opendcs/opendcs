/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
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
import java.util.Iterator;

import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.alarm.AlarmConfig;
import decodes.tsdb.alarm.AlarmEvent;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.EmailAddr;
import decodes.tsdb.alarm.FileMonitor;
import decodes.tsdb.alarm.ProcessMonitor;
import opendcs.dai.AlarmDAI;
import opendcs.dai.LoadingAppDAI;

/**
 * Implements AlarmDAI for reading/writing from SQL database.
 * @author mmaloney
 */
public class AlarmDAO extends DaoBase implements AlarmDAI
{
	private static String grpColumns = "ALARM_GROUP_ID, ALARM_GROUP_NAME, "
		+ "LAST_MODIFIED";
	private static String fileMonColumns = "ALARM_GROUP_ID, PATH, PRIORITY, "
		+ "MAX_FILES, MAX_FILES_HINT, MAX_LMT, MAX_LMT_HINT, ALARM_ON_DELETE, "
		+ "ON_DELETE_HINT, MAX_SIZE, MAX_SIZE_HINT, ALARM_ON_EXISTS, "
		+ "ON_EXISTS_HINT, ENABLED";
	// For DB Version < 17
	private static String alarmDefColumns = "ALARM_DEF_ID, ALARM_GROUP_ID, "
		+ "LOADING_APPLICATION_ID, PRIORITY, PATTERN";
	// For Db Version >= 17
	private static String alarmEventColumns = "ALARM_EVENT_ID, ALARM_GROUP_ID, "
			+ "LOADING_APPLICATION_ID, PRIORITY, PATTERN";


	/**
	 * @param tsdb
	 */
	public AlarmDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "AlarmDAO");
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
				q = "select " 
					+ (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_17 
						? alarmDefColumns : alarmEventColumns)
					+ " from "
					+ (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_17 
							? "ALARM_DEF" : "ALARM_EVENT")
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
	
		if (!DbKey.isNull(grp.getAlarmGroupId()))
			delete(grp.getAlarmGroupId());
		else
			grp.setAlarmGroupId(getKey("ALARM_GROUP"));
		
		grp.setLastModifiedMsec(System.currentTimeMillis());
		String q = "insert into ALARM_GROUP(" + grpColumns + ") values ("
			+ grp.getAlarmGroupId() + ", "
			+ sqlString(grp.getName()) + ", "
			+ grp.getLastModifiedMsec() + ")";
		doModify(q);
		
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

			for(AlarmEvent def : pm.getDefs())
			{
				if (DbKey.isNull(def.getAlarmEventId()))
					def.setAlarmEventId(getKey("ALARM_DEF"));
				q = "insert into "
					+ (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_17 
						? "ALARM_DEF" : "ALARM_EVENT")
					+ "(" + alarmDefColumns + ") values ("
					+ def.getAlarmEventId() + ", "
					+ grp.getAlarmGroupId() + ", "
					+ pm.getAppId() + ", "
					+ def.getPriority() + ", "
					+ sqlString(def.getPattern()) + ")";
				doModify(q);
			}
		}
	}

	/* (non-Javadoc)
	 * @see covesw.azul.alarm.dao.AlarmDAI#delete(decodes.sql.DbKey)
	 */
	@Override
	public boolean delete(DbKey groupID) throws DbIoException
	{
		if (!DbKey.isNull(groupID))
		{
			String q = "delete from ALARM_DEF where alarm_group_id = " + groupID;
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

}
