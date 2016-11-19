/*
* $Id$
* 
* $Log$
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
import java.util.Iterator;

import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.DbObjectCache.CacheIterator;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMember;
import decodes.tsdb.TsdbDatabaseVersion;

/**
This class is a helper to the TempestTsdb for reading & writing sites & names.
*/
public class TsGroupDAO
	extends DaoBase 
	implements TsGroupDAI
{
	protected String GroupAttributes = 
		"group_id, group_name, group_type, group_description";
	private static long lastCacheFill = 0L;
	public static long cacheTimeLimit = 15 * 60 * 1000L;
	protected static DbObjectCache<TsGroup> cache = new DbObjectCache<TsGroup>(cacheTimeLimit, false);

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
			return ret;
		
		String q = "SELECT " + GroupAttributes + " FROM tsdb_group "
			+ "WHERE group_id = " + groupId;
		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				ret = rs2group(rs);
				readTsGroupMembers(ret);
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
	
	@Override
	public void readTsGroupMembers(TsGroup group)
		throws DbIoException
	{
		group.clear();

		TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO();
		try
		{
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
			q = "SELECT a.group_id, a.group_name, a.group_type, a.group_description ";
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
				q = q + ", b.include_group";
			q = q + " FROM tsdb_group a, tsdb_group_member_group b "
				+ "WHERE b.parent_group_id = "
					+ group.getGroupId()
				+ " AND b.child_group_id = a.group_id";
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				TsGroup child = this.rs2group(rs);
				// before v6, all child groups were added.
				char combine = 'A';
				if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
				{
					String s = rs.getString(5);
					if (s != null && s.length() > 0)
						combine = s.charAt(0);
				}
				group.addSubGroup(child, combine);
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
		String q = "SELECT " + GroupAttributes + " FROM tsdb_group "
			+ "WHERE group_name = " + sqlString(grpName);
		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				TsGroup ret = rs2group(rs);
				readTsGroupMembers(ret);
				return ret;
			}
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
		
		if (db.isCwms() && db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_14)
		{
			q = "insert into cp_depends_notify(record_num, event_type, key, date_time_loaded) "
				+ "values(cp_depends_notifyidseq.nextval, 'G', "
				+ groupId + ", " + db.sqlDate(new Date()) + ")";
			doModify(q);
		}
		// Note: HDB does notification in trigger.
	}

	@Override
	public void deleteTsGroup(DbKey groupId)
		throws DbIoException
	{
		if (groupId != null && !groupId.isNull())
		{
			String q;
			//First delete all time-series member links.
			q = "DELETE from tsdb_group_member_ts WHERE group_id = " + groupId;
			doModify(q);
			
			//Delete sub-group associations, also delete any Group link
			//that has this group id
			q = "DELETE from tsdb_group_member_group "
				+ "WHERE parent_group_id = " + groupId
				+ " OR child_group_id = "  + groupId;
			doModify(q);
			
			q = "DELETE from tsdb_group_member_site where group_id = " 
				+ groupId;
			doModify(q);
			q = "DELETE from tsdb_group_member_dt where group_id = " + groupId;
			doModify(q);

			q = "DELETE from tsdb_group_member_other where group_id = " 
				+ groupId;
			doModify(q);

			//Delete ts Group
			q = "DELETE from tsdb_group WHERE group_id = " + groupId;
			doModify(q);

//			db.commit();
		}
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


	public void close()
	{
		super.close();
	}
	
	public void fillCache() 
		throws DbIoException
	{
		String q = "";

		cache.clear();
		TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO();
		try
		{
			q = "SELECT " + GroupAttributes + " FROM tsdb_group";
			ResultSet rs = doQuery(q);
			while(rs != null && rs.next())
				cache.put(rs2group(rs));
			
			q = "select group_id, ts_id from tsdb_group_member_ts";
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				TsGroup group = cache.getByKey(groupId);
				DbKey dataId = DbKey.createDbKey(rs, 2);
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
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				TsGroup group = cache.getByKey(groupId);
				group.addSiteId(DbKey.createDbKey(rs, 2));
			}

			// Now read the data-type list.
			q = "SELECT group_id, data_type_id from tsdb_group_member_dt";
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				TsGroup group = cache.getByKey(groupId);
				group.addDataTypeId(DbKey.createDbKey(rs, 2));
			}

			q = "select group_id, member_type, member_value from tsdb_group_member_other";
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				TsGroup group = cache.getByKey(groupId);
				group.addOtherMember(rs.getString(2), rs.getString(3));
			}

			q = "select parent_group_id, child_group_id, include_group from tsdb_group_member_group";
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

}



