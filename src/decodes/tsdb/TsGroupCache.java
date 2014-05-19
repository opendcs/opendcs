/*
*  $Id$
*
*  This is open-source software written under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*  
*  $Log$
*  Revision 1.5  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import ilex.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import opendcs.dai.TimeSeriesDAI;

import decodes.sql.DbKey;

/**
 * Implement a cache of TS Groups used in GUIs and for evaluation.
 * @author mmaloney
 */
public class TsGroupCache
{
	private ArrayList<TsGroup> tsGroupCache = new ArrayList<TsGroup>();
	
	// Optional feature to save expanded groups in a file after evaluation (for debugging).
	private File groupCacheDumpDir = null;
	
	private TimeSeriesDAI timeSeriesDAO = null;
	
	/**
	 * Call setTheDb if you want dynamic checking -- whenever a group is requested
	 * from the cache it is checked to see if it is either A: Not yet in the cache
	 * or B: stale - i.e. modified in the DB more recently than loaded.
	 */
	private TimeSeriesDb theDb = null;
	
	public TsGroupCache(TimeSeriesDAI timeSeriesDAO)
	{
		this.timeSeriesDAO = timeSeriesDAO;
	}

	public void clear()
	{
		tsGroupCache.clear();
	}
	
	/**
	 * Add a group to the cache. Replace any group with the same ID.
	 * @param grp the group to add/replace.
	 */
	public void add(TsGroup grp)
	{
		removeById(grp.getGroupId());
		tsGroupCache.add(grp);
	}
	
	/**
	 * Remove group with the passed ID.
	 * @param groupId
	 */
	public void removeById(DbKey groupId)
	{
		for(Iterator<TsGroup> grpit = tsGroupCache.iterator(); grpit.hasNext(); )
		{
			TsGroup grp = grpit.next();
			if (grp.getGroupId().equals(groupId))
			{
				grpit.remove();
				return;
			}
		}
	}
	
	public TsGroup getGroupFromCache(DbKey grpId)
	{
		for(TsGroup grp : tsGroupCache)
			if (grp.getGroupId().equals(grpId))
				return grp;
		
		// group doesn't yet exist in the cache.
		if (theDb != null)
		{
			try
			{
				TsGroup grp = theDb.getTsGroupById(grpId);
				if (grp != null)
				{
					add(grp);
					return grp;
				}
			}
			catch(DbIoException ex)
			{
				Logger.instance().warning("TsGroupCache: Error reading group  with ID="
					+ grpId + ": " + ex);
			}
		}
		return null;
	}


	public void evalAll()
	{
		ArrayList<DbKey> grpIdsDone = new ArrayList<DbKey>();
		for(TsGroup grp : tsGroupCache)
			evalGroup(grp, grpIdsDone);
	}
	
	/**
	 * Evaluate a group recursively. Calls evalGroup for any group in the cache
	 * that uses passed 'grp'.
	 * If (evalComps) also evaluate any computations in the cache that use this group.
	 * @param grp the group
	 * @param grpIdsDone Used to only process each group once. If null, then no recursion.
	 * @param evalComps set to true to also evaluate computation in the cache.
	 */
	public void evalGroup(TsGroup grp, ArrayList<DbKey> grpIdsDone)
	{
		// There may be dups & circular references in the hierarchy.
		// Only process each group once.
		if (grpIdsDone != null)
		{
			for(DbKey id : grpIdsDone)
				if (id.equals(grp.getGroupId()))
					return;
			grpIdsDone.add(grp.getGroupId());
		}

		info("Evaluating group '" + grp.getGroupName() + "'");
		grp.clearExpandedList();

		// Add the explicitely named tsid members
		for(TimeSeriesIdentifier tsid : grp.getTsMemberList())
		{
			grp.addToExpandedList(tsid);
			info("TSID '" + tsid.getUniqueString() + "' is explicitely named.");
		}
		
		// Determine locally defined tsid members
		try
		{
			for(TimeSeriesIdentifier tsid : timeSeriesDAO.listTimeSeries())
				if (passesParts(grp, tsid))
				{
					grp.addToExpandedList(tsid);
					info("TSID '" + tsid.getUniqueString() + "' passes component specs.");
				}
		}
		catch (DbIoException ex)
		{
			theDb.failure("Cannot list time series: " + ex);
		}

		// Do the include/exclude/intersect with sub-groups
		for(TsGroup included : grp.getIncludedSubGroups())
		{
			included = this.getGroupFromCache(included.getGroupId());
			if (included == null) // should always be false
				continue;
			if (grpIdsDone != null)
				evalGroup(included, grpIdsDone);
			info("Included Group: " + included.getGroupName()
				+ " with " + included.getExpandedList().size() + " members.");
			for(TimeSeriesIdentifier tsid : included.getExpandedList())
			{
				grp.addToExpandedList(tsid);
				info("TSID '" + tsid.getUniqueString() + "' included from subgroup "
					+ included.getGroupName());
			}
		}
		for(TsGroup excluded : grp.getExcludedSubGroups())
		{
			excluded = this.getGroupFromCache(excluded.getGroupId());
			if (excluded == null) // should always be false
				continue;
			if (grpIdsDone != null)
				evalGroup(excluded, grpIdsDone);
			info("Excluded Group: " + excluded.getGroupName());
			for(TimeSeriesIdentifier tsid : excluded.getExpandedList())
			{
				grp.rmFromExpandedList(tsid);
				info("TSID '" + tsid.getUniqueString() + "' EXcluded from subgroup "
					+ excluded.getGroupName());
			}
		}
		
		info("There are " + grp.getIntersectedGroups().size() + " intersected groups.");
		for(TsGroup subgrp : grp.getIncludedSubGroups())
		{
			subgrp = this.getGroupFromCache(subgrp.getGroupId());
			if (subgrp != null) // should always be true
			{
				if (grpIdsDone != null)
					evalGroup(subgrp, grpIdsDone);
				info("Intersecting with Group: " + subgrp.getGroupName()
					+ " which has " + subgrp.getExpandedList().size() + " members.");
			}
		}

		for(Iterator<TimeSeriesIdentifier> tsidit = grp.getExpandedList().iterator();
				tsidit.hasNext(); )
		{
			TimeSeriesIdentifier tsid = tsidit.next();
			for(TsGroup intersected : grp.getIntersectedGroups())
			{
				intersected = this.getGroupFromCache(intersected.getGroupId());
				if (intersected != null // should always be true
				 && !intersected.isInExpandedList(tsid.getKey()))
				{
					tsidit.remove();
					info("TSID '" + tsid.getUniqueString() + "' Excluded by intersection with "
						+ intersected.getGroupName());
				}
			}
		}

		info("After eval, expanded group has " + grp.getExpandedList().size() + " members.");
		
		// This group is now completely expanded.
		if (groupCacheDumpDir != null)
			dumpGroup(grp);
	}


	/**
	 * Recurse UP the tree to re-evaluate sub-groups of any parents to a modified group.
	 * @param groupId
	 * @param affected
	 */
	public void evaluateParents(DbKey groupId, ArrayList<DbKey> affected)
	{
		// Guard against circular references:
		if (affected.contains(groupId))
			return;
		affected.add(groupId);

		TsGroup grp = getGroupFromCache(groupId);
		if (grp == null)
			return;

		// Find any group that includes/excludes/or intersects this one
		for(TsGroup parent : tsGroupCache)
		{
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
			{
				// This parent is affected by groupId being changed!
				evalGroup(parent, null); // null list means don't recurse down.

				// Recurse UP the hierarchy.
				evaluateParents(parent.getGroupId(), affected);
			}
		}
	}

	/**
	 * Check against TSID Parts specified in the group.
	 * @param grp
	 * @param tsid
	 * @return true if the passed tsid the criteria specified in parts.
	 */
	private boolean passesParts(TsGroup grp, TimeSeriesIdentifier tsid)
	{
info("passesParts(" + grp.getGroupName() + ") tsid=" + tsid.getUniqueString());
		// In order to pass the 'parts', there must be at least one match.
		// I.e., an empty group definition contains nothing.
		int matches = 0;

		// Is this tsid's site in the site list?
		ArrayList<DbKey> siteIds = grp.getSiteIdList();
		debug("   siteIds.size()=" + siteIds.size());
		if (siteIds.size() > 0)
		{
			boolean found = false;
			DbKey tsSiteId = tsid.getSite().getId();
			for(DbKey siteId : siteIds)
				if (siteId.equals(tsSiteId))
				{
					found = true;
					matches++;
					break;
				}
			if (!found)
				return false; 
		}

		// Data Types are specified in this group?
		ArrayList<DbKey> dataTypeIds = grp.getDataTypeIdList();
		debug("   dataTypeIds.size()=" + dataTypeIds.size());
		if (dataTypeIds.size() > 0) 
		{
			boolean found = false;
			for(DbKey dtId : dataTypeIds)
				if (dtId.equals(tsid.getDataTypeId()))
				{
					found = true;
					matches++;
					break;
				}
			if (!found)
				return false;
		}
			
		String parts[] = tsid.getParts();
		// The first two parts are always site (aka location) and datatype (aka param).
		// So skip those and check all the subsequent parts.
		for (int pi = 2; pi < parts.length; pi++)
		{
			debug("   checking " + parts[pi]);

			ArrayList<TsGroupMember> otherParts = grp.getOtherMembers();
			boolean found = false;
			boolean thisPartSpecified = false;
			for(TsGroupMember tgm : otherParts)
			{
				if (tgm.getMemberType().equalsIgnoreCase(parts[pi]))
				{
					thisPartSpecified = true;
					String tsidVal = tsid.getPart(parts[pi]);
					debug("      -- groupValue=" + tgm.getMemberValue() + ", tsidValue=" + tsidVal);
					if (tgm.getMemberValue().equalsIgnoreCase(tsidVal))
					{
						found = true;
						matches++;
						break;
					}
				}
			}
			if (thisPartSpecified && !found)
				return false;
		}
		// If at least one part matched, then this tsid passes.
		return matches > 0;
	}

	/**
	 * Check this (new) tsid against all groups and adjust accordingly if it
	 * is a member of any group in the cache.
	 * @param tsid
	 */
	public void checkGroupMembership(TimeSeriesIdentifier tsid)
	{
		ArrayList<DbKey> grpIdsDone = new ArrayList<DbKey>();
		for(TsGroup grp : tsGroupCache)
			checkMembership(grp, tsid, grpIdsDone);
	}
	
	/**
	 * Recursively, check the passed tsid against the group criteria, 
	 * including sub-groups. 
	 * If the passed tsid is a member, add it to the expanded list.
	 * @param grp the group
	 * @param tsid the time series identifier
	 * @return true if tsid passes local group criteria
	 */
	private boolean checkMembership(TsGroup grp, TimeSeriesIdentifier tsid,
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

	/** Use this sparingly!! */
	public ArrayList<TsGroup> getList() { return tsGroupCache; }
	
	private void info(String msg)
	{
		Logger.instance().info("TsGroupCache: " + msg);
	}

	private void debug(String msg)
	{
		Logger.instance().debug3("TsGroupCache: " + msg);
	}

	public void setGroupCacheDumpDir(File groupCacheDumpDir)
	{
		this.groupCacheDumpDir = groupCacheDumpDir;
	}
	
	public File getGroupCacheDumpDir()
	{
		return groupCacheDumpDir;
	}

	private void dumpGroup(TsGroup grp)
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
			info("Writing '" + f.getPath() + "'");
			PrintWriter pw = new PrintWriter(new FileWriter(f));
			for(TimeSeriesIdentifier tsid : grp.getExpandedList())
				pw.println(tsid.getUniqueString());
			pw.close();
		}
		catch (IOException ex)
		{
			info("Cannot write group-cache file '" + f.getPath() + "': " + ex);
		}
	}

	public void setTheDb(TimeSeriesDb theDb)
	{
		this.theDb = theDb;
	}

}
