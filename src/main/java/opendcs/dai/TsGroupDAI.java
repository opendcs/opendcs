/*
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2016/12/16 14:30:54  mmaloney
 * Moved code to adjust comp dependencies when a group is modified to the DAO.
 *
 * Revision 1.2  2016/11/03 19:08:38  mmaloney
 * Refactoring for group evaluation to make HDB work the same way as CWMS.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 */
package opendcs.dai;

import java.util.ArrayList;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsGroup;


/**
 * Defines public interface for reading/writing Time Series Group objects.
 * @author mmaloney - Mike Maloney, Cove Software, LLC
 */
public interface TsGroupDAI
	extends DaiBase
{
	
	/**
	 * Return a TsGroup by its surrogate key ID, or null if no such group.
	 * @param groupId the surrogate key
	 * @return a TsGroup by its surrogate key ID, or null if no such group.
	 */
	public TsGroup getTsGroupById(DbKey groupId)
		throws DbIoException;
	
	/**
	 * Return a TsGroup by its surrogate key ID, or null if no such group.
	 * @param groupId the surrogate key
	 * @param forceDbRead if true then read from DB and not from cache.
	 * @return a TsGroup by its surrogate key ID, or null if no such group.
	 */
	public TsGroup getTsGroupById(DbKey groupId, boolean forceDbRead)
		throws DbIoException;

	
	/**
	 * Lists the Time Series Groups.
	 * This does a SHALLOW read of just the group's id, name, type
	 * and description, suitable for populating a GUI list.
	 * @param groupType type of groups to list, null to list all groups.
	 * @return ArrayList of un-expanded TS Groups.
	 */
	public ArrayList<TsGroup> getTsGroupList(String groupType)
		throws DbIoException;
	
	/**
	 * @return a TsGroup by its name, or null if no such group.
	 */
	public TsGroup getTsGroupByName(String grpName)
		throws DbIoException;

	/**
	 * Writes a group to the database.
	 * @param group the group
	 */
	public void writeTsGroup(TsGroup group)
		throws DbIoException;

	/**
	 * Delete a Ts Group.
	 * @param groupId the surrogate key of the group
	 * @throws DbIoException
	 */
	public void deleteTsGroup(DbKey groupId)
		throws DbIoException;

	/** @return number of computations using the specified group ID */
	public int countCompsUsingGroup(DbKey groupId)
		throws DbIoException;
	
	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();
	
	/**
	 * Fill cache with all groups.
	 */
	public void fillCache()
		throws DbIoException;

	/**
	 * Removes any computation dependencies for the group. That is computations
	 * that use the group, or that use a group that uses this group somewhere
	 * in its hierarchy.
	 * @param deletedGroupId
	 * @throws DbIoException
	 */
	public void removeDependenciesFor(DbKey deletedGroupId)
		throws DbIoException;

}
