/*
* $Id: TsGroupDAO.java,v 1.17 2020/02/14 15:21:48 mmaloney Exp $
* 
* $Log: TsGroupDAO.java,v $
* Revision 1.17  2020/02/14 15:21:48  mmaloney
* Updates for OpenTSDB
*
* Revision 1.16  2019/08/26 20:52:50  mmaloney
* Removed unneeded debugs.
*
* Revision 1.15  2017/11/14 16:08:05  mmaloney
* Increase cache limit to 20 min.
*
* Revision 1.14  2017/08/22 19:58:40  mmaloney
* Refactor
*
* Revision 1.13  2017/05/04 12:19:12  mmaloney
* Fixed recursion bug.
*
* Revision 1.12  2017/05/03 16:59:46  mmaloney
* Guard against circular references when reading sub groups.
*
* Revision 1.11  2017/04/04 21:22:04  mmaloney
* CWMS-10515 Null Ptr.
*
* Revision 1.10  2017/01/10 21:16:30  mmaloney
* Code cleanup.
*
* Revision 1.9  2017/01/06 16:42:10  mmaloney
* Misc Bug Fixes
*
* Revision 1.8  2016/12/16 14:49:16  mmaloney
* TYPO
*
* Revision 1.7  2016/12/16 14:30:54  mmaloney
* Moved code to adjust comp dependencies when a group is modified to the DAO.
*
* Revision 1.6  2016/11/19 15:56:30  mmaloney
* Generate a NOTIFY record on saving a group if CWMS and version >= 14.
*
* Revision 1.5  2016/11/03 19:08:38  mmaloney
* Refactoring for group evaluation to make HDB work the same way as CWMS.
*
* Revision 1.4  2016/10/17 17:52:24  mmaloney
* Add sub/base accessors for OpenDCS 6.3 CWMS Naming Standards
*
* Revision 1.3  2014/12/19 19:26:56  mmaloney
* Handle version change for column name tsdb_group_member_ts data_id vs. ts_id.
*
* Revision 1.2  2014/07/03 12:53:41  mmaloney
* debug improvements.
*
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* This software was written by Cove Software, LLC ("COVE") under contract
* to the United States Government. No warranty is provided or implied other 
* than specific contractual terms between COVE and the U.S. Government.
*
* Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
* All rights reserved.
*/
package opendcs.dao;

import hec.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import opendcs.dai.CompDependsNotifyDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import decodes.sql.DbKey;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMember;
import decodes.tsdb.TsdbDatabaseVersion;

public class TsGroupDAO
	extends DaoBase 
	implements TsGroupDAI
{
	protected String GroupAttributes = 
		"group_id, group_name, group_type, group_description";
	private static long lastCacheFill = 0L;
	public static long cacheTimeLimit = 20 * 60 * 1000L;
	protected static DbObjectCache<TsGroup> cache = new DbObjectCache<TsGroup>(cacheTimeLimit, false);
	
	// Used to guard against endless loop in subgroup associations.
	private Stack<DbKey> loopGuard = new Stack<DbKey>();

	public TsGroupDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "TsGroupDAO");
	}

	public synchronized TsGroup getTsGroupById(DbKey groupId)
		throws DbIoException
	{
		return getTsGroupById(groupId, false);
	}
	
	@Override
	public TsGroup getTsGroupById(DbKey groupId, boolean forceDbRead)
		throws DbIoException
	{
		TsGroup ret = null;
		
		if (!forceDbRead && (ret = cache.getByKey(groupId)) != null)
		{
//debug2("Returning FROM CACHE group id " + ret.getGroupId() + " - " + ret.getGroupName());
			return ret;
		}
//else debug2("READING FROM DATABASE group id " + groupId);
		
		String q = "SELECT " + GroupAttributes + " FROM tsdb_group "
			+ "WHERE group_id = " + groupId;
		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				ret = rs2group(rs);

				loopGuard.push(groupId);
				readTsGroupMembers(ret);
				loopGuard.pop();
				cache.put(ret);
				return ret;
			}
			else
			{
				cache.remove(groupId);
				return null;
			}
		}
		catch(Exception ex)
		{
			throw new DbIoException(
				"getTsGroupById: Cannot get group for id=" + groupId + ": "+ex);
		}
	}


	/**
	 * Called with result set with 4 columns: ID, name, type, description
	 * @param rs the result set
	 * @return the new TsGroup object
	 * @throws SQLException
	 */
	protected TsGroup rs2group(ResultSet rs)
		throws SQLException
	{
		TsGroup ret = new TsGroup();
		ret.setGroupId(DbKey.createDbKey(rs, 1));
		ret.setGroupName(rs.getString(2));
		ret.setGroupType(rs.getString(3));
		ret.setDescription(rs.getString(4));
		return ret;
	}
	
	public void readTsGroupMembers(TsGroup group)
		throws DbIoException
	{
		group.clear();

		TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO();
		try
		{
			timeSeriesDAO.setManualConnection(getConnection());
			String q = "select * from tsdb_group_member_ts "
				+ "where group_id = " + group.getGroupId();
			
			ResultSet rs = doQuery(q);
			ArrayList<DbKey> dataIds = new ArrayList<DbKey>();
			while(rs != null && rs.next())
				dataIds.add(DbKey.createDbKey(rs, 2));
			
			for(DbKey dataId : dataIds)
			{
				try { group.addTsMember(timeSeriesDAO.getTimeSeriesIdentifier(dataId)); }
				catch(NoSuchObjectException ex)
				{
					warning("tsdb_group id=" + group.getGroupId()
						+ " contains invalid ts member with data_id="
						+ dataId + " -- ignored.");
				}
			}

			// Now read the site list.
			q = "SELECT site_id  from tsdb_group_member_site "
				+ "WHERE group_id = " + group.getGroupId();
			rs = doQuery(q);
			while(rs != null && rs.next())
				group.addSiteId(DbKey.createDbKey(rs, 1));

			// Now read the data-type list.
			q = "SELECT data_type_id  from tsdb_group_member_dt "
				+ "WHERE group_id = " + group.getGroupId();
			rs = doQuery(q);
			while(rs != null && rs.next())
				group.addDataTypeId(DbKey.createDbKey(rs, 1));

			q = "select member_type, member_value from "
			  + "tsdb_group_member_other where group_id = "
			  + group.getGroupId();
			rs = doQuery(q);
			while(rs != null && rs.next())
				group.addOtherMember(rs.getString(1), rs.getString(2));

			// Now get the sub-groups.
			q = "SELECT child_group_id, include_group from tsdb_group_member_group "
				+ "where parent_group_id = " + group.getGroupId();
			
			class ChildGroup 
			{
				DbKey gid; char combine;
				public ChildGroup(DbKey gid, char combine)
				{
					this.gid = gid;
					this.combine = combine;
				}
			}
			ArrayList<ChildGroup> children = new ArrayList<ChildGroup>();
			
			// Have to read first then recurse. Otherwise ResultSet gets closed in recursion.
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey childId = DbKey.createDbKey(rs, 1);
				String s = rs.getString(2);
				char combine = (s != null && s.length() > 0) ? s.charAt(0) : 'A';
				if (loopGuard.contains(childId))
				{
					warning("Group (id=" + group.getGroupId() + ") " + group.getGroupName()
						+ " -- has sub group ID=" + childId 
						+ " Circular reference detected -- Ignored.");
					continue;
				}

				children.add(new ChildGroup(childId, combine));
			}
			
			for(ChildGroup cg : children)
			{
				TsGroup child = getTsGroupById(cg.gid);
				
				if (child != null)
					group.addSubGroup(child, cg.combine);
				else
					warning("Group (id=" + group.getGroupId() + ") " + group.getGroupName()
						+ " -- has sub group ID=" + cg.gid 
						+ " that doesn't exist in database. -- Ignored.");
			}
		}
		catch(SQLException ex)
		{
			String msg = "readTsGroupMembers: Cannot read group members: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	@Override
	public ArrayList<TsGroup> getTsGroupList(String groupType)
		throws DbIoException
	{
		if (System.currentTimeMillis() - lastCacheFill > cacheTimeLimit)
			fillCache();
		
		ArrayList<TsGroup> ret = new ArrayList<TsGroup>();

		for(Iterator<TsGroup> ci = cache.iterator(); ci.hasNext();)
		{
			TsGroup g = ci.next();
			if (groupType == null || groupType.length() == 0
			 || TextUtil.equalsIgnoreCase(groupType, g.getGroupType()))
				ret.add(g);
		}
		return ret;
	}

	@Override
	public TsGroup getTsGroupByName(String grpName)
		throws DbIoException
	{
		String q = "SELECT group_id FROM tsdb_group "
			+ "WHERE lower(group_name) = " + sqlString(grpName.toLowerCase());
		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
				return getTsGroupById(DbKey.createDbKey(rs, 1));
			else
				return null;
		}
		catch(Exception ex)
		{
			throw new DbIoException(
				"getTsGroupByName: Cannot get group '" + grpName + "': " + ex);
		}
	}

	@Override
	public void writeTsGroup(TsGroup group)
		throws DbIoException
	{
		DbKey groupId = group.getGroupId();
		String q = "";
		if (groupId.isNull())
		{
			groupId = getKey("tsdb_group");
			group.setGroupId(groupId);
			q = "INSERT INTO tsdb_group(" + GroupAttributes + ") "
				+ "VALUES("
				+ groupId + ", "
				+ sqlString(group.getGroupName()) + ", "
				+ sqlString(group.getGroupType()) + ", "
				+ sqlString(group.getDescription());
			q = q + ")";
		}
		else
		{
			q = "UPDATE tsdb_group SET "
			+ "group_name = " + sqlString(group.getGroupName()) + ", "
			+ "group_type = " + sqlString(group.getGroupType()) + ", "
			+ "group_description = " + sqlString(group.getDescription());
			q = q + " WHERE group_id = " + groupId;
		}
		doModify(q);
		
		// First delete all time-series member links, then re-add them.
		q = "DELETE from tsdb_group_member_ts WHERE group_id = " + groupId;
		doModify(q);
		for(TimeSeriesIdentifier dd : group.getTsMemberList())
		{
			q = "INSERT INTO tsdb_group_member_ts(group_id, "
				+ (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ? "ts_id" : "data_id")
				+ ") "
				+ "VALUES(" + groupId + ", " + dd.getKey() + ")";
			doModify(q);
		}

		// Likewise, delete sub-group associations, then re-add them.
		q = "DELETE from tsdb_group_member_group "
			+ "WHERE parent_group_id = " + groupId;
		doModify(q);
		String grp_grp_columns = "parent_group_id, child_group_id";
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
			grp_grp_columns += ", include_group";
		for(TsGroup tg : group.getIncludedSubGroups())
		{
			q = "INSERT INTO tsdb_group_member_group("
				+ grp_grp_columns + ") "
				+ "VALUES(" + groupId + ", " + tg.getGroupId();
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
				q = q + ", 'A'";
			q = q + ")";
			doModify(q);
		}
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
			for(TsGroup tg : group.getExcludedSubGroups())
			{
				q = "INSERT INTO tsdb_group_member_group("
					+ grp_grp_columns + ") "
					+ "VALUES(" + groupId + ", " + tg.getGroupId()
					+ ", 'S')";
				doModify(q);
			}
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
			for(TsGroup tg : group.getIntersectedGroups())
			{
				q = "INSERT INTO tsdb_group_member_group("
					+ grp_grp_columns + ") "
					+ "VALUES(" + groupId + ", " + tg.getGroupId()
					+ ", 'I')";
				doModify(q);
			}

		// Likewise, delete query fields ids.
		q = "DELETE from tsdb_group_member_site where group_id = " + groupId;
		doModify(q);
		for(DbKey id : group.getSiteIdList())
		{
			q = "INSERT INTO tsdb_group_member_site values("
				+ groupId + ", " + id + ")";
			doModify(q);
		}

		q = "DELETE from tsdb_group_member_dt where group_id = " + groupId;
		doModify(q);
		for(DbKey id : group.getDataTypeIdList())
		{
			q = "INSERT INTO tsdb_group_member_dt values("
				+ groupId + ", " + id + ")";
			doModify(q);
		}

		q = "DELETE from tsdb_group_member_other where group_id = " 
			+ groupId;
		doModify(q);
		for (TsGroupMember tgm : group.getOtherMembers())
		{
			q = "INSERT INTO tsdb_group_member_other values("
				+ groupId + ", " + sqlString(tgm.getMemberType())
				+ ", " + sqlString(tgm.getMemberValue()) + ")";
			doModify(q);
		}
		
		cache.put(group);
		
		if ((db.isCwms() && db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_14)
		  ||(db.isOpenTSDB() && db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_67))
		{
			try (CompDependsNotifyDAI dai = db.makeCompDependsNotifyDAO())
			{
				CpDependsNotify cdn = new CpDependsNotify();
				cdn.setEventType(CpDependsNotify.GRP_MODIFIED);
				cdn.setKey(groupId);
				dai.saveRecord(cdn);
			}
		}
		// Note: HDB does notification in trigger.
	}

	@Override
	public void deleteTsGroup(DbKey groupId)
		throws DbIoException
	{
		if (DbKey.isNull(groupId))
			return;
		
		removeDependenciesFor(groupId);

		//First delete all time-series member links.
		String q = "DELETE from tsdb_group_member_ts WHERE group_id = " + groupId;
		doModify(q);
			
		//Delete sub-group associations, also delete any Group link
		//that has this group id
		q = "DELETE from tsdb_group_member_group "
			+ "WHERE parent_group_id = " + groupId
			+ " OR child_group_id = "  + groupId;
		doModify(q);
			
		q = "DELETE from tsdb_group_member_site where group_id = " + groupId;
		doModify(q);
		q = "DELETE from tsdb_group_member_dt where group_id = " + groupId;
		doModify(q);

		q = "DELETE from tsdb_group_member_other where group_id = " + groupId;
		doModify(q);

		q = "DELETE from tsdb_group WHERE group_id = " + groupId;
		doModify(q);
	}

	@Override
	public int countCompsUsingGroup(DbKey groupId)
		throws DbIoException
	{
		String q = "select count(*) from cp_computation where group_id = " + groupId;
		ResultSet rs = doQuery(q);
		try
		{
			if (rs != null && rs.next())
			{
				return rs.getInt(1);
			}
			else
				return 0;
		}
		catch (SQLException ex)
		{
			String msg = "countCompsUsingGroup: " + ex;
			throw new DbIoException(msg);
		}
	}


	public void fillCache() 
		throws DbIoException
	{
		String q = "";

		cache.clear();
		TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO();
		if (timeSeriesDAO == null)
			throw new DbIoException("Can't make timeSeriesDAO!");
		try
		{
			timeSeriesDAO.setManualConnection(getConnection());
			q = "SELECT " + GroupAttributes + " FROM tsdb_group";
			ResultSet rs = doQuery(q);
			while(rs != null && rs.next())
				cache.put(rs2group(rs));

			q = "select group_id, "
				+ (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ? "ts_id" : "data_id")
				+ " from tsdb_group_member_ts";
			// MJM CWMS-10515: Have to join all the child tables with the parent TSDB_GROUP
			// in order for VPD to do its magic.
			if (db.isCwms())
				q = "select c.group_id, ts_id from tsdb_group_member_ts c, tsdb_group p "
					+ "where c.group_id = p.group_id";
			
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				TsGroup group = cache.getByKey(groupId);
				DbKey dataId = DbKey.createDbKey(rs, 2);

if (group == null)
{
	warning("Bad group id=" + groupId + " in tsdb_group_member_ts with ts_code=" + dataId + " -- no corresponding group.");
	continue;
}
if (DbKey.isNull(dataId))
{
	warning("Null ts_id in tsdb_group_member_ts ");
	continue;
}
				try { group.addTsMember(timeSeriesDAO.getTimeSeriesIdentifier(dataId)); }
				catch(NoSuchObjectException ex)
				{
					warning("tsdb_group id=" + group.getGroupId()
						+ " contains invalid ts member with data_id="
						+ dataId + " -- ignored.");
				}
			}

			// Now read the site list.
			q = "SELECT group_id, site_id from tsdb_group_member_site";
			
			if (db.isCwms())
				q = "SELECT c.group_id, c.site_id from tsdb_group_member_site c, tsdb_group p "
					+ "where c.group_id = p.group_id";

			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				TsGroup group = cache.getByKey(groupId);
				group.addSiteId(DbKey.createDbKey(rs, 2));
			}

			// Now read the data-type list.
			q = "SELECT c.group_id, data_type_id from tsdb_group_member_dt c, tsdb_group p "
				+ "where c.group_id = p.group_id";
			
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				TsGroup group = cache.getByKey(groupId);
if (group == null)
{
	warning("Bad group id=" + groupId + " in tsdb_group_member_dt -- no corresponding group.");
	continue;
}

				group.addDataTypeId(DbKey.createDbKey(rs, 2));
			}

			q = "select c.group_id, member_type, member_value from tsdb_group_member_other c, tsdb_group p "
				+ "where c.group_id = p.group_id";

			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				TsGroup group = cache.getByKey(groupId);
if (group == null)
{
	warning("Bad group id=" + groupId + " in tsdb_group_member_other -- no corresponding group.");
	continue;
}

				group.addOtherMember(rs.getString(2), rs.getString(3));
			}

			q = "select parent_group_id, child_group_id, include_group from tsdb_group_member_group c, tsdb_group p "
				+ "where c.parent_group_id = p.group_id";

			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey parentGroupId = DbKey.createDbKey(rs, 1);
				TsGroup parentGroup = cache.getByKey(parentGroupId);
				DbKey childGroupId = DbKey.createDbKey(rs, 2);
				TsGroup childGroup = cache.getByKey(childGroupId);
				char combine = 'A';
				String s = rs.getString(3);
				if (s != null && s.length() > 0)
					combine = s.charAt(0);
				parentGroup.addSubGroup(childGroup, combine);
			}
		}
		catch(SQLException ex)
		{
			throw new DbIoException("TsGroupDAO.fillCache: Error in query '" + q + "': " + ex);
		}
		finally
		{
			timeSeriesDAO.close();
		}
		lastCacheFill = System.currentTimeMillis();
	}

	@Override
	public void removeDependenciesFor(DbKey deletedGroupId) 
		throws DbIoException
	{
		if (DbKey.isNull(deletedGroupId))
			return;
		
		// Disable any computation that uses this group directly and null the reference
		String q = "SELECT COMPUTATION_ID FROM CP_COMPUTATION WHERE GROUP_ID = " + deletedGroupId;
		ArrayList<DbKey> comps2disable = new ArrayList<DbKey>();
		ResultSet rs = doQuery(q);
		try
		{
			while(rs.next())
				comps2disable.add(DbKey.createDbKey(rs, 1));
		}
		catch (SQLException ex)
		{
			String msg = " Error listing comps that use group " + deletedGroupId + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
		
		q = "UPDATE CP_COMPUTATION SET ENABLED = 'N', GROUP_ID = NULL,"
			+ " DATE_TIME_LOADED = " + db.sqlDate(new Date())
			+ " WHERE GROUP_ID = " + deletedGroupId;
		doModify(q);
		
		// Make a list of any group that uses this group as a child.
		q = "SELECT DISTINCT PARENT_GROUP_ID FROM TSDB_GROUP_MEMBER_GROUP "
			+ "WHERE CHILD_GROUP_ID = " + deletedGroupId;
		ArrayList<DbKey> modifiedGroupIds = new ArrayList<DbKey>();
		try
		{
			while(rs.next())
				modifiedGroupIds.add(DbKey.createDbKey(rs, 1));
		}
		catch (SQLException ex)
		{
			String msg = " Error listing groups that use group " + deletedGroupId + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}

		// Remove the child references to this group from the affected groups.
		q = "DELETE FROM TSDB_GROUP_MEMBER_GROUP WHERE CHILD_GROUP_ID = " + deletedGroupId;
		doModify(q);

		if ((db.isCwms() && db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_14)
		  ||(db.isOpenTSDB() && db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_67))
		{
			// Enqueue "comp modified" notifications for all the now-disabled computations.
			try(CompDependsNotifyDAI dai = db.makeCompDependsNotifyDAO())
			{
				for(DbKey compId : comps2disable)
				{
					CpDependsNotify cdn = new CpDependsNotify();
					cdn.setEventType(CpDependsNotify.CMP_MODIFIED);
					cdn.setKey(compId);
					dai.saveRecord(cdn);
				}

				// Enqueue "group modified" notifications for groups in the hash set.
				// This will let the comp depends updater manage the computations.
				for(DbKey groupId : modifiedGroupIds)
				{
					CpDependsNotify cdn = new CpDependsNotify();
					cdn.setEventType(CpDependsNotify.GRP_MODIFIED);
					cdn.setKey(groupId);
					dai.saveRecord(cdn);
				}
			}
		}

		// Note HDB does notify via trigger.

		// Reread any modified groups to keep cache up to date.
		for(DbKey groupId : modifiedGroupIds)
			getTsGroupById(groupId, true);
	}

}
