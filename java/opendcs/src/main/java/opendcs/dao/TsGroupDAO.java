/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package opendcs.dao;

import ilex.util.TextUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Stack;

import org.opendcs.utils.FailableResult;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
import decodes.tsdb.TsdbException;

public class TsGroupDAO extends DaoBase  implements TsGroupDAI
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			return ret;
		}

		String q = "SELECT " + GroupAttributes + " FROM tsdb_group "
			+ "WHERE group_id = ?";
		try
		{
			ret = getSingleResultOr(q, rs -> rs2group(rs), (TsGroup)null, groupId);
			if (ret != null)
			{
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
		catch(SQLException ex)
		{
			throw new DbIoException("getTsGroupById: Cannot get group for id=" + groupId, ex);
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
		try (Connection conn = this.getConnection();
			 TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO();
			 DaoHelper dao = new DaoHelper(db, module, conn))
		{
			timeSeriesDAO.setManualConnection(conn);
			String q = "select * from tsdb_group_member_ts where group_id = ?";

			final ArrayList<DbKey> dataIds = new ArrayList<DbKey>();
			dao.doQuery(q, rs -> dataIds.add(DbKey.createDbKey(rs, 2)), group.getGroupId());

			for(DbKey dataId : dataIds)
			{
				try
				{
					group.addTsMember(timeSeriesDAO.getTimeSeriesIdentifier(dataId));
				}
				catch(NoSuchObjectException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("tsdb_group id={} contains invalid ts member with data_id={} -- ignored.",
					   		group.getGroupId(), dataId);
				}
			}

			// Now read the site list.
			q = "SELECT site_id  from tsdb_group_member_site "
				+ "WHERE group_id = ?";
			dao.doQuery(q, rs -> group.addSiteId(DbKey.createDbKey(rs, 1)), group.getGroupId());

			// Now read the data-type list.
			q = "SELECT data_type_id  from tsdb_group_member_dt "
				+ "WHERE group_id = ?";
			dao.doQuery(q, rs -> group.addDataTypeId(DbKey.createDbKey(rs, 1)), group.getGroupId());

			q = "select member_type, member_value from "
			  + "tsdb_group_member_other where group_id = ?";
			dao.doQuery(q, rs -> group.addOtherMember(rs.getString(1), rs.getString(2)), group.getGroupId());

			// Now get the sub-groups.
			q = "SELECT child_group_id, include_group from tsdb_group_member_group "
				+ "where parent_group_id = ?";

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
			dao.doQuery(q, rs ->
			{
				DbKey childId = DbKey.createDbKey(rs, 1);
				String s = rs.getString(2);
				char combine = (s != null && s.length() > 0) ? s.charAt(0) : 'A';
				if (loopGuard.contains(childId))
				{
					log.warn("Group (id={}) {} -- has sub group ID={} Circular reference detected -- Ignored.",
							 group.getGroupId(), group.getGroupName(), childId);
				}
				else
				{
					children.add(new ChildGroup(childId, combine));
				}
			},
			group.getGroupId());

			for(ChildGroup cg : children)
			{
				TsGroup child = getTsGroupById(cg.gid);

				if (child != null)
				{
					group.addSubGroup(child, cg.combine);
				}
				else
				{
					log.warn("Group (id={}) {} -- has sub group ID={} " +
							 "that doesn't exist in database. -- Ignored.",
							 group.getGroupId(), group.getGroupName(), cg.gid);
				}
			}
		}
		catch(SQLException ex)
		{
			String msg = "readTsGroupMembers: Cannot read group members.";
			throw new DbIoException(msg, ex);
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
			 || TextUtil.strEqualIgnoreCase(groupType, g.getGroupType()))
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
		try (ResultSet rs = doQuery(q))
		{
			if (rs.next())
				return getTsGroupById(DbKey.createDbKey(rs, 1));
			else
				return null;
		}
		catch(Exception ex)
		{
			throw new DbIoException("getTsGroupByName: Cannot get group '" + grpName + "'", ex);
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

		cache.remove(groupId);
	}

	@Override
	public int countCompsUsingGroup(DbKey groupId)
		throws DbIoException
	{
		String q = "select count(*) from cp_computation where group_id = " + groupId;

		try (ResultSet rs = doQuery(q);)
		{
			if (rs.next())
			{
				return rs.getInt(1);
			}
			else
				return 0;
		}
		catch (SQLException ex)
		{
			String msg = "countCompsUsingGroup.";
			throw new DbIoException(msg, ex);
		}
	}


	public void fillCache()
		throws DbIoException
	{
		try
		{
			this.inTransaction(dao ->
			{
				String q = "";
				try (TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO())
				{
					timeSeriesDAO.inTransactionOf(dao);
					cache.clear();
					q = "SELECT " + GroupAttributes + " FROM tsdb_group";
					dao.doQuery(q, rs -> cache.put(rs2group(rs)));

					q = "select group_id, "
						+ (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ? "ts_id" : "data_id")
						+ " from tsdb_group_member_ts";
					// MJM CWMS-10515: Have to join all the child tables with the parent TSDB_GROUP
					// in order for VPD to do its magic.
					if (db.isCwms())
					{
						q = "select c.group_id, ts_id from tsdb_group_member_ts c, tsdb_group p "
							+ "where c.group_id = p.group_id";
					}

					dao.doQuery(q, rs ->
					{
						DbKey groupId = DbKey.createDbKey(rs, 1);
						TsGroup group = cache.getByKey(groupId);
						DbKey dataId = DbKey.createDbKey(rs, 2);

						if (group == null)
						{
							log.warn("Bad group id={} in tsdb_group_member_ts " +
									 "with ts_code={} -- no corresponding group.",
									 groupId, dataId);
							return;
						}
						if (DbKey.isNull(dataId))
						{
							log.warn("Null ts_id in tsdb_group_member_ts");
							return;
						}

						FailableResult<TimeSeriesIdentifier,TsdbException> result = timeSeriesDAO.findTimeSeriesIdentifier(dataId);
						if (result.isSuccess())
						{
							group.addTsMember(result.getSuccess());
						}
						else if (result.getFailure() instanceof NoSuchObjectException)
						{
							warning("tsdb_group id=" + group.getGroupId()
							+ " contains invalid ts member with data_id="
							+ dataId + " -- ignored.");
						}
						else
						{
							throw new SQLException("Unable to get timeseries identified for group member.", result.getFailure());
						}
					});

					// Now read the site list.
					q = "SELECT group_id, site_id from tsdb_group_member_site";

					if (db.isCwms())
					{
						q = "SELECT c.group_id, c.site_id from tsdb_group_member_site c, tsdb_group p "
							+ "where c.group_id = p.group_id";
					}
					dao.doQuery(q, rs ->
					{
						DbKey groupId = DbKey.createDbKey(rs, 1);
						TsGroup group = cache.getByKey(groupId);
						group.addSiteId(DbKey.createDbKey(rs, 2));
					});

					// Now read the data-type list.
					q = "SELECT c.group_id, data_type_id from tsdb_group_member_dt c, tsdb_group p "
						+ "where c.group_id = p.group_id";

					dao.doQuery(q, rs->
					{
						DbKey groupId = DbKey.createDbKey(rs, 1);
						TsGroup group = cache.getByKey(groupId);
						if (group == null)
						{
							log.warn("Bad group id={} in tsdb_group_member_dt -- no corresponding group.",
									 groupId);
						}
						else
						{
							group.addDataTypeId(DbKey.createDbKey(rs, 2));
						}
					});

					q = "select c.group_id, member_type, member_value from tsdb_group_member_other c, tsdb_group p "
						+ "where c.group_id = p.group_id";

					dao.doQuery(q, rs->
					{
						DbKey groupId = DbKey.createDbKey(rs, 1);
						TsGroup group = cache.getByKey(groupId);
						if (group == null)
						{
							log.warn("Bad group id={} in tsdb_group_member_other -- no corresponding group.",
									 groupId);
						}
						else
						{
							group.addOtherMember(rs.getString(2), rs.getString(3));
						}
					});

					q = "select parent_group_id, child_group_id, include_group from tsdb_group_member_group c, tsdb_group p "
						+ "where c.parent_group_id = p.group_id";

					dao.doQuery(q, rs ->
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
					});
				}
			});
		}
		catch (Exception ex)
		{
			throw new DbIoException("Error filling TsGroup cache.", ex);
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

		try (ResultSet rs = doQuery(q))
		{
			while(rs.next())
				comps2disable.add(DbKey.createDbKey(rs, 1));
		}
		catch (SQLException ex)
		{
			String msg = " Error listing comps that use group " + deletedGroupId;
			throw new DbIoException(msg, ex);
		}

		q = "UPDATE CP_COMPUTATION SET ENABLED = 'N', GROUP_ID = NULL,"
			+ " DATE_TIME_LOADED = " + db.sqlDate(new Date())
			+ " WHERE GROUP_ID = " + deletedGroupId;
		doModify(q);

		// Make a list of any group that uses this group as a child.
		q = "SELECT DISTINCT PARENT_GROUP_ID FROM TSDB_GROUP_MEMBER_GROUP "
			+ "WHERE CHILD_GROUP_ID = " + deletedGroupId;
		ArrayList<DbKey> modifiedGroupIds = new ArrayList<DbKey>();
		try (ResultSet rs = doQuery(q))
		{
			while(rs.next())
				modifiedGroupIds.add(DbKey.createDbKey(rs, 1));
		}
		catch (SQLException ex)
		{
			String msg = " Error listing groups that use group " + deletedGroupId;
			throw new DbIoException(msg, ex);
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