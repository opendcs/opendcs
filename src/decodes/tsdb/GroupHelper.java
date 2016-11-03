package decodes.tsdb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import decodes.sql.DbKey;


public abstract class GroupHelper
{
	protected TimeSeriesDb tsdb;
	
	// to save expanded groups in a file after evaluation (for debugging).
	protected File groupCacheDumpDir = null;
	
	@SuppressWarnings("serial")
	protected class TsIdSet extends TreeSet<TimeSeriesIdentifier>
	{
		public TsIdSet()
		{
			super(
				new Comparator<TimeSeriesIdentifier>()
				{
					public int compare(TimeSeriesIdentifier dd1, TimeSeriesIdentifier dd2)
					{
						long diff = dd1.getKey().getValue() - dd2.getKey().getValue();
						return diff < 0L ? -1 : diff > 0L ? 1 : 0;
					}
					public boolean equals(Object obj) { return false; }
				});
		}
	}

	
	public GroupHelper(TimeSeriesDb tsdb)
	{
		this.tsdb = tsdb;
	}
	
	public void evalAll()
		throws DbIoException
	{
		TsGroupDAI tsGroupDAO = tsdb.makeTsGroupDAO();
		ArrayList<TsGroup> allGroups = null;
		try
		{
			allGroups = tsGroupDAO.getTsGroupList(null);
		}
		catch(DbIoException ex)
		{
			tsdb.warning("GroupHelper.evalAll error listing groups: " + ex);
			return;
		}
		finally
		{
			tsGroupDAO.close();
		}
		for(TsGroup grp : allGroups)
			expandTsGroup(grp);
	}
	
	/**
	 * Recursively expand groups to find all ts_ids under the 
	 * specified group. This method would be called by report-generator
	 * programs that are given a group and must process all time-series
	 * contained within it or within its sub-groups.
	 * Warning: Not thread safe.
	 * @param tsGroup the top-level group to expand
	 * @return list of all time series IDs under this group or sub-groups
	 */
	public void expandTsGroup(TsGroup tsGroup)
		throws DbIoException
	{
		tsdb.debug("GroupHelper.expandTsGroup group '" + tsGroup.getGroupName() + "'");
		prepareForExpand(tsGroup);
		
		tsGroup.clearExpandedList();

		ArrayList<DbKey> groupIdsDone = new ArrayList<DbKey>();
		TsIdSet tsIdSet = doExpandTsGroup(tsGroup, groupIdsDone);
		
		for(TimeSeriesIdentifier dd : tsIdSet)
			tsGroup.addToExpandedList(dd);
		
		tsGroup.setIsExpanded(true);
		// This group is now completely expanded.
		
		if (groupCacheDumpDir != null)
			dumpGroup(tsGroup);
		
	}
	
	/**
	 * This method is called from expandTsGroup prior to calling passesParts for each TSID
	 * in the cache. It allows the subclass to do any pump priming necessary.
	 * @param grp the TsGroup
	 * @throws DbIoException
	 */
	protected abstract void prepareForExpand(TsGroup grp)
		throws DbIoException;
	
	/**
	 * This recursive method does the actual work for expanding a group.
	 * @param tsGroup The group to expand.
	 * @param groupIdsDone list of groups already expanded to prevent circular recursion
	 * @return a set of time series IDs that are members of the group.
	 * @throws DbIoException
	 */
	protected TsIdSet doExpandTsGroup(TsGroup grp, ArrayList<DbKey> groupIdsDone)
		throws DbIoException
	{
		TsIdSet tsIdSet = new TsIdSet();
		
		// There may be dups & circular references in the hierarchy.
		// Only process each group once.
		for(DbKey id : groupIdsDone)
			if (id.equals(grp.getGroupId()))
			{
				tsIdSet.addAll(grp.getExpandedList());
				return tsIdSet;
			}

		groupIdsDone.add(grp.getGroupId());
//		if (!grp.isTransient())
//			// transient groups are built programmatically -- not in DB.
//			// So if this is transient, leave it alone.
//			tsdb.readTsGroupMembers(grp);

		tsdb.debug1("Evaluating group '" + grp.getGroupName() + "'");

		// Add the explicitely named tsid members
		for(TimeSeriesIdentifier tsid : grp.getTsMemberList())
			tsIdSet.add(tsid);

		// Check the TSID part filters against the entire TSID cache.
		TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		try
		{
			for(TimeSeriesIdentifier tsid : timeSeriesDAO.listTimeSeries())
				if (passesParts(grp, tsid))
					tsIdSet.add(tsid);
		}
		finally
		{
			timeSeriesDAO.close();
		}

		// Do the include/exclude/intersect with sub-groups
		for(TsGroup inclGroup : grp.getIncludedSubGroups())
		{
			TreeSet<TimeSeriesIdentifier> addedTsids = doExpandTsGroup(inclGroup, groupIdsDone);
			tsIdSet.addAll(addedTsids);
		}
		for(TsGroup exclGroup : grp.getExcludedSubGroups())
		{
			TreeSet<TimeSeriesIdentifier> exclTsids = doExpandTsGroup(exclGroup, groupIdsDone);
			tsIdSet.removeAll(exclTsids);
		}
		
		for(TsGroup intsGroup : grp.getIntersectedGroups())
		{
			TreeSet<TimeSeriesIdentifier> intsTsids = doExpandTsGroup(intsGroup, groupIdsDone);
			tsIdSet.retainAll(intsTsids);
		}
		
		return tsIdSet;
	}

	
	

	/**
	 * Recurse UP the tree to re-evaluate any parents to a modified group.
	 * @param groupId the modified group ID
	 * @param affected The list of parent group IDs that are affected.
	 */
	public void evaluateParents(DbKey groupId, ArrayList<DbKey> affected)
		throws DbIoException
	{
		TreeSet<DbKey> needsEval = new TreeSet<DbKey>();
		
		TsGroupDAI tsGroupDAO = tsdb.makeTsGroupDAO();
		try
		{
			findParentsOf(groupId, needsEval, tsGroupDAO);
			for(DbKey affectedGroupId : needsEval)
			{
				TsGroup affectedGroup = tsGroupDAO.getTsGroupById(affectedGroupId);
				if (affectedGroup == null)
					continue;
				expandTsGroup(affectedGroup);
				affected.add(affectedGroupId);
			}
		}
		finally
		{
			tsGroupDAO.close();
		}
	}
	
	/**
	 * Recurse up the tree of groups finding all parent groups that might be affected
	 * by a change in the group specified by groupId.
	 * @param groupId The ID of the group we are checking
	 * @param needsEval collect all affected group IDs here
	 * @param tsGroupDAO
	 * @throws DbIoException 
	 */
	private void findParentsOf(DbKey groupId, TreeSet<DbKey> needsEval, TsGroupDAI tsGroupDAO)
		throws DbIoException
	{
		if (needsEval.contains(groupId))
		// Guard against circular references:
			return;
		
		needsEval.add(groupId);
		
		// Find any group that includes/excludes/or intersects this one
		for(TsGroup parent : tsGroupDAO.getTsGroupList(null))
		{
			if (parent.getKey().equals(groupId))
				continue;
			
			boolean affects = false;
			for(TsGroup parentIncludes : parent.getIncludedSubGroups())
				if (parentIncludes.getGroupId() == groupId)
				{
					affects = true;
					break;
				}
			if (!affects)
				for(TsGroup parentExcludes : parent.getExcludedSubGroups())
					if (parentExcludes.getGroupId() == groupId)
					{
						affects = true;
						break;
					}
			if (!affects)
				for(TsGroup parentIntersects : parent.getIntersectedGroups())
					if (parentIntersects.getGroupId() == groupId)
					{
						affects = true;
						break;
					}
			if (affects)
				findParentsOf(parent.getKey(), needsEval, tsGroupDAO);
		}
	}

	
	public void setGroupCacheDumpDir(File groupCacheDumpDir)
	{
		this.groupCacheDumpDir = groupCacheDumpDir;
	}
	
	public File getGroupCacheDumpDir()
	{
		return groupCacheDumpDir;
	}

	public void dumpGroup(TsGroup grp)
	{
		// Make a file name by replacing invalid chars with '_'.
		StringBuilder fn = new StringBuilder(grp.getGroupName());
		for(int i=0; i<fn.length(); i++)
		{
			char c = fn.charAt(i);
			if (!Character.isDigit(c) && !Character.isLetter(c)
			 && c != '-' && c != '_' && c != '.')
				fn.setCharAt(i, '_');
		}
		File f = new File(groupCacheDumpDir, fn.toString());
		try
		{
			tsdb.info("Writing '" + f.getPath() + "'");
			PrintWriter pw = new PrintWriter(new FileWriter(f));
			for(TimeSeriesIdentifier tsid : grp.getExpandedList())
				pw.println(tsid.getUniqueString());
			pw.close();
		}
		catch (IOException ex)
		{
			tsdb.info("Cannot write group-cache file '" + f.getPath() + "': " + ex);
		}
	}
	
	/**
	 * Recursively, check the passed tsid against the group criteria, 
	 * including sub-groups. 
	 * If the passed tsid is a member, add it to the expanded list.
	 * @param grp the group
	 * @param tsid the time series identifier
	 * @return true if tsid passes local group criteria
	 */
	protected boolean checkMembership(TsGroup grp, TimeSeriesIdentifier tsid,
		ArrayList<DbKey> grpIdsDone)
	{
		// There may be dups & circular references in the hierarchy.
		// Only process each group once.
		for(DbKey id : grpIdsDone)
			if (id.equals(grp.getGroupId()))
				return grp.expandedListContains(tsid);
		grpIdsDone.add(grp.getGroupId());

		boolean passes = false;
		
		// Check explicit TSIDs
		for(TimeSeriesIdentifier explicitTsid : grp.getTsMemberList())
			if (tsid.getKey() == explicitTsid.getKey()
			 || tsid.getUniqueString().equalsIgnoreCase(explicitTsid.getUniqueString()))
			{
				passes = true;
				break;
			}
		
		// Check the parts
		if (!passes)
			passes = passesParts(grp, tsid);
		
		// Check the sub-groups.
		for(TsGroup included : grp.getIncludedSubGroups())
			if (checkMembership(included, tsid, grpIdsDone))
			{
				passes = true;
				break;
			}
		for(TsGroup excluded : grp.getExcludedSubGroups())
			if (checkMembership(excluded, tsid, grpIdsDone))
			{
				passes = false;
				break;
			}
		if (passes)
			for(TsGroup intersected : grp.getIntersectedGroups())
				if (!checkMembership(intersected, tsid, grpIdsDone))
				{
					passes = false;
					break;
				}
		if (passes)
			grp.addToExpandedList(tsid);
		return passes;
	}
	
	/**
	 * Check against TSID Parts specified in the group.
	 * @param grp the group
	 * @param tsid the time series ID
	 * @return true if the passed tsid the criteria specified in parts.
	 */
	protected abstract boolean passesParts(TsGroup grp, TimeSeriesIdentifier tsid);

	/**
	 * Check a new tsid against all groups and adjust accordingly if it
	 * is a member of any group in the cache.
	 * @param tsid
	 * @throws DbIoException 
	 */
	public void checkGroupMembership(TimeSeriesIdentifier tsid) 
		throws DbIoException
	{
		ArrayList<DbKey> grpIdsDone = new ArrayList<DbKey>();
		TsGroupDAI tsGroupDAO = tsdb.makeTsGroupDAO();
		try
		{
			for(TsGroup grp : tsGroupDAO.getTsGroupList(null))
				checkMembership(grp, tsid, grpIdsDone);
		}
		finally
		{
			tsGroupDAO.close();
		}
	}


}
