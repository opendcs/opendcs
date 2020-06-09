/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.4  2019/08/07 14:19:30  mmaloney
 * 6.6 RC04
 *
 * Revision 1.3  2019/07/02 13:50:19  mmaloney
 * 6.6RC04 First working Alarm Implementation
 *
 * Revision 1.2  2019/05/10 18:35:26  mmaloney
 * dev
 *
 * Revision 1.1  2019/03/05 14:53:01  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.3  2017/03/30 20:55:20  mmaloney
 * Alarm and Event monitoring capabilities for 6.4 added.
 *
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package opendcs.dai;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import decodes.sql.DbKey;
import decodes.tsdb.BadScreeningException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.alarm.Alarm;
import decodes.tsdb.alarm.AlarmConfig;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.AlarmScreening;

/**
 * Methods for reading/writing alarm definitions in the database.
 * @author mmaloney
 *
 */
public interface AlarmDAI
{
	/**
	 * Using the read time in the config and the last-modify time in each
	 * group in the database, check the config and make it current.
	 * <ul>
	 *   <li>Load any new groups that doen't exist in cfg.</li>
	 *   <li>Reload any groups in cfg that have been modified.</li>
	 *   <li>Remove any groups from cfg that were deleted from the database.</li>
	 * </ul>
	 * Passing an empty config will have the effect of an initial load of all
	 * groups defined in the database.
	 * @param cfg the aggregate object holding all the groups.
	 * @return true if any modifications were made to the config.
	 * @throws DbIoException on error accessing the database
	 */
	public boolean check(AlarmConfig cfg)
		throws DbIoException;
	
	/**
	 * Writes an alarm group to the database.
	 * If the group's ID is not yet set, attempt to match name for a group
	 * in the database and overwrite that group. If no match, create a new
	 * group in the database.
	 * @param grp the group to be written
	 * @throws DbIoException on error accessing the database
	 */
	public void write(AlarmGroup grp)
		throws DbIoException;
	
	/**
	 * Deletes an alarm group from the database.
	 * @param groupID the surrogate key of the group to delete
	 * @throws DbIoException
	 * @return true if a matching group was found and deleted, false if no match.
	 */
	public boolean deleteAlarmGroup(DbKey groupID)
		throws DbIoException;
	
	/**
	 * Lookup the surrogate key ID from the name - non-case-sensitive.
	 * @param groupName the group name to look up
	 * @return the DbKey ID or DbKey.NullKey if not found.
	 * @throws DbIoException on any database error.
	 */
	public DbKey groupName2id(String groupName)
		throws DbIoException;

	/**
	 * Search the cache for an alarm screenings for the specified site, datatype.
	 * Returned list will be sorted by start_date_time
	 * @param siteId
	 * @param datatypeId
	 * @return matching screening if found, null if not.
	 * @throws DbIoException
	 */
	public ArrayList<AlarmScreening> getScreenings(DbKey siteId, DbKey datatypeId)
		throws DbIoException;
	
	/**
	 * Writes (either insert or update) a screening to the database.
	 * @param as
	 * @throws DbIoException
	 */
	public void writeScreening(AlarmScreening as)
		throws DbIoException, BadScreeningException;
	
	public void writeLimitSet(AlarmLimitSet als)
		throws DbIoException;
	
	/**
	 * Deletes a screening, all of its limit sets and any alarm assertions
	 * associated with those limit sets.
	 * @param screeningId
	 * @throws DbIoException on sql error
	 */
	public void deleteScreening(DbKey screeningId)
		throws DbIoException;
	
	/**
	 * Deletes a limit set and any current or historical alarm assertions associated
	 * with it.
	 * @param limitSetId
	 * @throws DbIoException
	 */
	public void deleteLimitSet(DbKey limitSetId)
		throws DbIoException;
	
	/** Closes any resources opened by the DAO. */
	public void close();
	
	/** Fills the cache and returns all screenings in the db */
	public ArrayList<AlarmScreening> getAllScreenings()
		throws DbIoException;
	
	/** Retrieve a screening by its ID */
	public AlarmScreening getScreening(DbKey screeningId)
		throws DbIoException;

	/** Refresh the map: load with new, update existing, delete obsolete 
	 * @throws DbIoException */
	public void refreshCurrentAlarms(HashMap<DbKey, Alarm> alarmMap) throws DbIoException;

	public void deleteCurrentAlarm(DbKey tsidKey)
		throws DbIoException;
	
	public void moveToHistory(Alarm alarm);

	public void writeToCurrent(Alarm alarm);

	/**
	 * Read records from ALARM_HISTORY table.
	 * @param tsids list of TSIDs to retrieve alarms for. If empty, retrieve all
	 * @return
	 * @throws DbIoException
	 */
	public ArrayList<Alarm> readAlarmHistory(ArrayList<TimeSeriesIdentifier> tsids) throws DbIoException;

	/**
	 * Delete any alarms with the given TSID within a time range from the history table.
	 * @param tsidKey Required - the key of the time series identifier
	 * @param since delete alarms since this time, null means no lower bound
	 * @param until delete alarms until this time, null means no upper bound
	 * @throws DbIoException
	 */
	public void deleteHistoryAlarms(DbKey tsidKey, Date since, Date until)
		throws DbIoException;

	/**
	 * @param tsid
	 * @return The end_time of the last historical alarm for this TSID, or null if no alarms.
	 * @throws DbIoException 
	 */
	public Date lastHistoryAlarmTime(TimeSeriesIdentifier tsid)
		throws DbIoException;
}
