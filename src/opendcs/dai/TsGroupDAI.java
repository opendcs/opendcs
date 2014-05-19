/*
 * $Id$
 * 
 * $Log$
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
{
	
	/**
	 * Return a TsGroup by its surrogate key ID, or null if no such group.
	 * @param groupId the surrogate key
	 * @return a TsGroup by its surrogate key ID, or null if no such group.
	 */
	public TsGroup getTsGroupById(DbKey groupId)
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
	 * Reads the members of an individual group.
	 * @param group the group
	 */
	public void readTsGroupMembers(TsGroup group)
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
}
