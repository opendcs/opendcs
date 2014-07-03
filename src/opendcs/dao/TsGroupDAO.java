/*
* $Id$
* 
* $Log$
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;

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

	public TsGroupDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "TsGroupDAO");
	}

	protected static DbObjectCache<TsGroup> cache = new DbObjectCache<TsGroup>(15 * 60 * 1000L, false);
	
	public synchronized TsGroup getTsGroupById(DbKey groupId)
		throws DbIoException
	{
		TsGroup ret = cache.getByKey(groupId);
		if (ret != null)
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
				return null;
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

			if (db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_6)
			{
				// Now read the interval-code list.
				q = "SELECT interval_code from tsdb_group_member_interval "
					+ "WHERE group_id = " + group.getGroupId();
				rs = doQuery(q);
				while(rs != null && rs.next())
					group.addIntervalCode(rs.getString(1));
	
				// Now read the statistics-code list.
				q = "SELECT statistics_code from tsdb_group_member_statcode "
					+ "WHERE group_id = " + group.getGroupId();
				rs = doQuery(q);
				while(rs != null && rs.next())
					group.addStatisticsCode(rs.getString(1));
			}
			else // defined with tsdb_group_member_other
			{
				q = "select member_type, member_value from "
				  + "tsdb_group_member_other where group_id = "
				  + group.getGroupId();
				rs = doQuery(q);
				while(rs != null && rs.next())
					group.addOtherMember(rs.getString(1), rs.getString(2));
			}

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
		ArrayList<TsGroup> ret = new ArrayList<TsGroup>();
		String q = "SELECT " + GroupAttributes + " FROM tsdb_group";
		if (groupType != null && groupType.length() > 0)
			q = q + " WHERE group_type = " + sqlString(groupType);

		try
		{
			ResultSet rs = doQuery(q);
			while(rs != null && rs.next())
				ret.add(rs2group(rs));
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(
				"getTsGroupList: Cannot list groups: " + ex);
		}
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
//		int dbOfficeCode = group.getDbOfficeCode();
//		if (dbOfficeCode == Constants.undefinedId
//		 && tsdb.isCwms())
//		{
//			dbOfficeCode = ((CwmsTimeSeriesDb)tsdb).getDbOfficeCode();
//		}
//		
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
//			if (tsdb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
//				q = q + ", " + dbOfficeCode;
			q = q + ")";
		}
		else
		{
			q = "UPDATE tsdb_group SET "
			+ "group_name = " + sqlString(group.getGroupName()) + ", "
			+ "group_type = " + sqlString(group.getGroupType()) + ", "
			+ "group_description = " + sqlString(group.getDescription());
//			if (tsdb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
//				q = q + ", db_office_code = " + dbOfficeCode;
			q = q + " WHERE group_id = " + groupId;
		}
		doModify(q);
		
		// First delete all time-series member links, then re-add them.
		q = "DELETE from tsdb_group_member_ts WHERE group_id = " + groupId;
		doModify(q);
		for(TimeSeriesIdentifier dd : group.getTsMemberList())
		{
			q = "INSERT INTO tsdb_group_member_ts(group_id, data_id) "
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
//System.out.println("# included = " + group.getIncludedSubGroups().size());
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
//System.out.println("# excluded = " + group.getExcludedSubGroups().size());
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

		if (db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_6)
		{
			q = "DELETE from tsdb_group_member_interval where group_id = " 
				+ groupId;
			doModify(q);
			for(String X : group.getIntervalCodeList())
			{
				q = "INSERT INTO tsdb_group_member_interval values("
					+ groupId + ", " + sqlString(X) + ")";
				doModify(q);
			}
	
			q = "DELETE from tsdb_group_member_statcode where group_id = " 
				+ groupId;
			doModify(q);
			for(String X : group.getStatisticsCodeList())
			{
				q = "INSERT INTO tsdb_group_member_statcode values("
					+ groupId + ", " + sqlString(X) + ")";
				doModify(q);
			}
		}
		else // version >= 6
		{
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
		}

//		db.commit();
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

			
			if (db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_6)
			{
				q = "DELETE from tsdb_group_member_interval where group_id = " 
					+ groupId;
				doModify(q);
				q = "DELETE from tsdb_group_member_statcode where group_id = " 
					+ groupId;
				doModify(q);
			}
			else // version >= 6
			{
				q = "DELETE from tsdb_group_member_other where group_id = " 
					+ groupId;
				doModify(q);
			}

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
	
}



