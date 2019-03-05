/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.3  2017/03/30 20:55:20  mmaloney
 * Alarm and Event monitoring capabilities for 6.4 added.
 *
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package opendcs.dai;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.alarm.AlarmConfig;
import decodes.tsdb.alarm.AlarmGroup;

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
	public boolean delete(DbKey groupID)
		throws DbIoException;
	
	/**
	 * Lookup the surrogate key ID from the name - non-case-sensitive.
	 * @param groupName the group name to look up
	 * @return the DbKey ID or DbKey.NullKey if not found.
	 * @throws DbIoException on any database error.
	 */
	public DbKey groupName2id(String groupName)
		throws DbIoException;

	
	/** Closes any resources opened by the DAO. */
	public void close();
}
