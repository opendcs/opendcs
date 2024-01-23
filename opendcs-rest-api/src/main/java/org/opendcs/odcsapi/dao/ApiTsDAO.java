/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.opendcs.odcsapi.beans.ApiDataType;
import org.opendcs.odcsapi.beans.ApiInterval;
import org.opendcs.odcsapi.beans.ApiSiteName;
import org.opendcs.odcsapi.beans.ApiSiteRef;
import org.opendcs.odcsapi.beans.ApiTimeSeriesData;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.ApiTimeSeriesSpec;
import org.opendcs.odcsapi.beans.ApiTimeSeriesValue;
import org.opendcs.odcsapi.beans.ApiTsGroup;
import org.opendcs.odcsapi.beans.ApiTsGroupRef;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.opendcs.odcsapi.util.ApiTextUtil;

public class ApiTsDAO
	extends ApiDaoBase
{
	public static String module = "ApiTsDAO";

	public ApiTsDAO(DbInterface dbi)
	{
		super(dbi, module);
	}
	
	public Properties getTsdbProperties()
		throws DbException
	{
		String q = "select prop_name, prop_value from tsdb_property";
		ResultSet rs = doQuery(q);
		try
		{
			Properties ret = new Properties();
			while (rs.next())
			{
				ret.setProperty(rs.getString(1), rs.getString(2));
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	/**
	 * Note call with null value to delete the property
	 * @param props
	 */
	public void setTsdbProperties(Properties props)
		throws DbException
	{
		String q = "";
		try
		{
			q = "select prop_name, prop_value from tsdb_property";
			ResultSet rs = doQuery(q);
			Properties origProps = new Properties();
			while(rs.next())
			{
				String n = rs.getString(1);
				String v = rs.getString(2);
				origProps.setProperty(n, v);
			}
			
			for (Enumeration<?>  pen = props.propertyNames(); pen.hasMoreElements(); )
			{
				String name = (String)pen.nextElement();
				String value = props.getProperty(name);
				
				String oldValue = ApiPropertiesUtil.getIgnoreCase(origProps, name);
				if (oldValue != null)
				{
					ApiPropertiesUtil.rmIgnoreCase(origProps, name);
					if (!oldValue.equals(value))
					{
						q = "update tsdb_property set prop_value = ?"
							+ " where lower(prop_name) = ?";
						doModifyV(q, value, name.toLowerCase());
					}
					// Else the values are the same -- leave it alone.
				}
				else // this is a new property. Insert it.
				{
					q = "insert into tsdb_property(prop_name, prop_value) values (?, ?)";
					doModifyV(q, name, value);
				}
			}
			// We are left with origProps that was NOT in the passed set. Delete them.
			for (Enumeration<?>  pen = origProps.propertyNames(); pen.hasMoreElements(); )
			{
				String name = (String)pen.nextElement();
				q = "delete from tsdb_property where prop_name = ?";
				doModifyV(q, name);
			}

		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public ApiTimeSeriesSpec getSpec(long tsKey)
		throws DbException, WebAppException
	{
		String q = "select SITE_ID, DATATYPE_ID, STATISTICS_CODE, INTERVAL_ID, DURATION_ID, "
				+ "TS_VERSION, ACTIVE_FLAG, STORAGE_UNITS, STORAGE_TABLE, STORAGE_TYPE, "
				+ "MODIFY_TIME, DESCRIPTION, UTC_OFFSET, ALLOW_DST_OFFSET_VARIATION, "
				+ "OFFSET_ERROR_ACTION "
				+ "from TS_SPEC "
				+ "where TS_ID = ?";
		try (ApiSiteDAO siteDAO = new ApiSiteDAO(dbi))
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, tsKey);
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No such time series with id="
					+ tsKey);

			ApiTimeSeriesSpec ret = new ApiTimeSeriesSpec();
			long id = rs.getLong(1);
			ret.setSiteId(rs.wasNull() ? null : id);
			id = rs.getLong(2);
			ret.setDatatypeId(rs.wasNull() ? null : id);
			ret.setStatCode(rs.getString(3));
			id = rs.getLong(4);
			ret.setIntervalId(rs.wasNull() ? null : id);
			id = rs.getLong(5);
			ret.setDurationId(rs.wasNull() ? null : id);
			ret.setVersion(rs.getString(6));
			ret.setActive(ApiTextUtil.str2boolean(rs.getString(7)));
			String units = rs.getString(8);
			ret.setStorageTable(rs.getInt(9));
			String s = rs.getString(10);
			if (s != null && s.length() > 0)
				ret.setStorageType(s.charAt(0));
			ret.setLastModified(dbi.getFullDate(rs, 11));
			String desc = rs.getString(12);
			ret.setUtcOffset(rs.getInt(13));
			ret.setAllowDSTVariation(ApiTextUtil.str2boolean(rs.getString(14)));
			ret.setOffsetErrorAction(rs.getString(15));
			
			ret.setLocation(siteDAO.getPreferredSiteName(ret.getSiteId()));
			
			ApiTimeSeriesIdentifier tsid = new ApiTimeSeriesIdentifier();
			tsid.setKey(tsKey);
			tsid.setStorageUnits(units);
			tsid.setDescription(desc);
			ret.setTsid(tsid);
			
			q = "select CODE from DATATYPE where ID = ?";
			rs = doQueryPs(conn, q, ret.getDatatypeId());
			rs.next();
			ret.setParam(rs.getString(1));
			
			q = "select NAME from INTERVAL_CODE where INTERVAL_ID = ?";
			rs = doQueryPs(conn, q, ret.getIntervalId());
			rs.next();
			ret.setInterval(rs.getString(1));
			
			q = "select NAME from INTERVAL_CODE where INTERVAL_ID = ?";
			rs = doQueryPs(conn, q, ret.getDurationId());
			rs.next();
			ret.setDuration(rs.getString(1));

			String uniqueStr = ret.getLocation() + "." + ret.getParam() + "." + ret.getStatCode()
				+ "." + ret.getInterval() + "." + ret.getDuration() + "." + ret.getVersion();
			tsid.setUniqueString(uniqueStr);
			
			String tab = makeTableName(ret.getStorageTable(), true);
			
			q = "select SAMPLE_TIME, TS_VALUE, FLAGS from ?"
					+ " where TS_ID = ?"
					+ "	and SAMPLE_TIME = (select max(SAMPLE_TIME) from ?" 
					+ " where TS_ID = ?)";
			rs = doQueryPs(conn, q, tab, tsKey, tab, tsKey);
			if (rs.next())
			{
				ApiTimeSeriesValue tv = new ApiTimeSeriesValue();
				tv.setSampleTime(dbi.getFullDate(rs, 1));
				tv.setValue(rs.getDouble(2));
				tv.setFlags(rs.getLong(3));
				ret.setNewest(tv);
			}
			
			q = "select SAMPLE_TIME, TS_VALUE, FLAGS from ?"
					+ " where TS_ID = ?"
					+ "	and SAMPLE_TIME = (select min(SAMPLE_TIME) from ?" 
					+ " where TS_ID = ?)";
			rs = doQueryPs(conn, q, tab, tsKey, tab, tsKey);
			if (rs.next())
			{
				ApiTimeSeriesValue tv = new ApiTimeSeriesValue();
				tv.setSampleTime(dbi.getFullDate(rs, 1));
				tv.setValue(rs.getDouble(2));
				tv.setFlags(rs.getLong(3));
				ret.setOldest(tv);
			}
			
			q = "select SAMPLE_TIME, TS_VALUE, FLAGS from ?"
				+ " where TS_ID = ?"
				+ "	and TS_VALUE = (select max(TS_VALUE) from ?" 
				+ " where TS_ID = ?)";
			rs = doQueryPs(conn, q, tab, tsKey, tab, tsKey);
			if (rs.next())
			{
				ApiTimeSeriesValue tv = new ApiTimeSeriesValue();
				tv.setSampleTime(dbi.getFullDate(rs, 1));
				tv.setValue(rs.getDouble(2));
				tv.setFlags(rs.getLong(3));
				ret.setMax(tv);
			}
			
			q = "select SAMPLE_TIME, TS_VALUE, FLAGS from ?"
				+ " where TS_ID = ?"
				+ "	and TS_VALUE = (select min(TS_VALUE) from ?" 
				+ " where TS_ID = ?)";
			rs = doQueryPs(conn, q, tab, tsKey, tab, tsKey);
			if (rs.next())
			{
				ApiTimeSeriesValue tv = new ApiTimeSeriesValue();
				tv.setSampleTime(dbi.getFullDate(rs, 1));
				tv.setValue(rs.getDouble(2));
				tv.setFlags(rs.getLong(3));
				ret.setMin(tv);
			}
			
			q = "select count(*) from ? where TS_ID = ?";
			rs = doQueryPs(conn, q, tab, tsKey);
			if (rs.next())
				ret.setNumValues(rs.getInt(1));

			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}	
	}
	
	public String makeTableName(int tableNum, boolean numeric)
	{
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setMinimumIntegerDigits(4);
		nf.setGroupingUsed(false);
		return (numeric ? "ts_num_" : "ts_string_") + nf.format(tableNum);
	}
	
	public ArrayList<ApiTimeSeriesIdentifier> getTsRefs(boolean activeOnly)
		throws DbException
	{
		ArrayList<ApiTimeSeriesIdentifier> ret = new ArrayList<ApiTimeSeriesIdentifier>();
		String q = "";
		
		try (ApiSiteDAO siteDAO = new ApiSiteDAO(dbi);
			ApiRefListDAO rlDAO = new ApiRefListDAO(dbi))
		{
			// Make a hashmap of all sites that have at least one time series.
			// I can't use a join because there may be multiple site names and there
			// may not be a preferred name.
			q = "select SITEID, NAMETYPE, SITENAME from SITENAME sn"
				+ "	where exists (select SITE_ID from TS_SPEC ts"
				+ "	where sn.SITEID = ts.SITE_ID)";
			HashMap<Long, ApiSiteName> siteNames = new HashMap<Long, ApiSiteName>();
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				Long siteId = rs.getLong(1);
				String nt = rs.getString(2);
				String nv = rs.getString(3);
				ApiSiteName name4site = siteNames.get(siteId);
				// If don't yet have a name for this site, OR if this is the preferred name:
				if (name4site == null || nt.equalsIgnoreCase(DbInterface.siteNameTypePreference))
					siteNames.put(siteId, new ApiSiteName(siteId, nt, nv));
			}

			HashMap<Long, ApiInterval> intervals = rlDAO.getIntervals();
			
			q = "select ts.TS_ID, ts.SITE_ID, ts.DATATYPE_ID, ts.STATISTICS_CODE, ts.INTERVAL_ID, ts.DURATION_ID, "
				+ "ts.TS_VERSION, ts.STORAGE_UNITS, ts.DESCRIPTION, ts.ACTIVE_FLAG, dt.CODE "
				+ "from TS_SPEC ts, DATATYPE dt "
				+ "where ts.DATATYPE_ID = dt.ID ";
			rs = doQuery(q);
			while(rs.next())
			{
				boolean active = ApiTextUtil.str2boolean(rs.getString(10));
				if (activeOnly && !active)
					continue;
				
				long tsKey = rs.getLong(1);
				
				long siteId = rs.getLong(2);
				ApiSiteName asn = siteNames.get(siteId);
				String siteName = asn != null ? asn.getNameValue() : "";
				
//				long datatypeId = rs.getLong(3);
				String statCode = rs.getString(4);
				long intervalId = rs.getLong(5);
				ApiInterval interval = intervals.get(intervalId);
				long durationId = rs.getLong(6);
				ApiInterval duration = intervals.get(durationId);
				String version = rs.getString(7);
				String units = rs.getString(8);
				String desc = rs.getString(9);
				String param = rs.getString(11);
				
				ApiTimeSeriesIdentifier tsid = new ApiTimeSeriesIdentifier();
				tsid.setKey(tsKey);
				tsid.setStorageUnits(units);
				tsid.setDescription(desc);
				tsid.setActive(active);
				
				String uniqueStr = siteName + "." + param + "." + statCode
					+ "." + interval.getName() + "." + duration.getName() + "." + version;
				tsid.setUniqueString(uniqueStr);
				
				ret.add(tsid);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}	
	}
	
	public ApiTimeSeriesData getTsData(long tsKey, Date start, Date end)
		throws DbException, WebAppException
	{
		ApiTimeSeriesData ret = new ApiTimeSeriesData();

		ApiTimeSeriesSpec spec = getSpec(tsKey);
		ret.setTsid(spec.getTsid());
		
		String tabname = makeTableName(spec.getStorageTable(), true);
		ArrayList<Object> args = new ArrayList<Object>();
		String q = "select SAMPLE_TIME, TS_VALUE, FLAGS from ?"
			+ " where TS_ID = ?";
		args.add(tabname);
		args.add(tsKey);
		if (start != null)
		{
			q = q + " and SAMPLE_TIME >= ?";
			args.add(dbi.sqlDateV(start));
		}
		if (end != null)
		{
			q = q + " and SAMPLE_TIME <= ?";
			args.add(dbi.sqlDateV(end));
		}
		
		q = q + " order by SAMPLE_TIME";
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, args.toArray());
			while (rs.next())
				ret.getValues().add(
					new ApiTimeSeriesValue(dbi.getFullDate(rs, 1), rs.getLong(2), rs.getInt(3)));
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public ArrayList<ApiTsGroupRef> getTsGroupRefs()
		throws DbException
	{
		ArrayList<ApiTsGroupRef> ret = new ArrayList<ApiTsGroupRef>();
		
		String q = "select GROUP_ID, GROUP_NAME, GROUP_TYPE, GROUP_DESCRIPTION "
				+ "from TSDB_GROUP";
		ResultSet rs = doQuery(q);
		try
		{
			while(rs.next())
			{
				ApiTsGroupRef gf = new ApiTsGroupRef();
				gf.setGroupId(rs.getLong(1));
				gf.setGroupName(rs.getString(2));
				gf.setGroupType(rs.getString(3));
				gf.setDescription(rs.getString(4));
				ret.add(gf);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		return ret;
	}
	
	public ApiTsGroup getTsGroup(Long groupId)
		throws DbException, WebAppException
	{
		String q = "select GROUP_ID, GROUP_NAME, GROUP_TYPE, GROUP_DESCRIPTION "
			+ "from TSDB_GROUP"
			+ " where GROUP_ID = ?";
		try (ApiSiteDAO siteDao = new ApiSiteDAO(this.dbi))
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, groupId);
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No TsGroup with groupid=" + groupId);
			ApiTsGroup ret = new ApiTsGroup();
			ret.setGroupId(rs.getLong(1));
			ret.setGroupName(rs.getString(2));
			ret.setGroupType(rs.getString(3));
			ret.setDescription(rs.getString(4));
			
			q = "select dt.ID, dt.STANDARD, dt.CODE, dt.DISPLAY_NAME "
				+ "from TSDB_GROUP_MEMBER_DT gmd, DATATYPE dt "
				+ "where gmd.DATA_TYPE_ID = dt.ID "
				+ "and gmd.GROUP_ID = ?";
			rs = doQueryPs(conn, q, groupId);
			while(rs.next())
			{
				Long id = rs.getLong(1);
				String std = rs.getString(2);
				String code = rs.getString(3);
				String nm = rs.getString(4);
				ApiDataType dt = new ApiDataType();
				dt.setId(id);
				dt.setStandard(std);
				dt.setCode(code);
				dt.setDisplayName(nm);
				ret.getGroupDataTypes().add(dt);
			}
			
			ArrayList<ApiTsGroupRef> grpRefs = getTsGroupRefs();
			q = "select CHILD_GROUP_ID, INCLUDE_GROUP "
				+ "from TSDB_GROUP_MEMBER_GROUP "
				+ "where PARENT_GROUP_ID = ?";
			rs = doQueryPs(conn, q, groupId);
		nextSubGrp:
			while(rs.next())
			{
				long childId = rs.getLong(1);
				String op = rs.getString(2).toLowerCase();
				for (ApiTsGroupRef subGrpRef : grpRefs)
				{
					if (subGrpRef.getGroupId() == childId)
					{
						if (op.startsWith("a"))
							ret.getIncludeGroups().add(subGrpRef);
						else if (op.startsWith("s"))
							ret.getExcludeGroups().add(subGrpRef);
						else if (op.startsWith("i"))
							ret.getIntersectGroups().add(subGrpRef);
						continue nextSubGrp;
					}
				}
			}
		
			ArrayList<ApiTimeSeriesIdentifier> tsids = getTsRefs(true);
			q = "select TS_ID from TSDB_GROUP_MEMBER_TS "
				+ "where GROUP_ID = ?";
			rs = doQueryPs(conn, q, groupId);
		nextTsId:
			while(rs.next())
			{
				long tsKey = rs.getLong(1);
				for(ApiTimeSeriesIdentifier tsid : tsids)
				{
					if (tsid.getKey() == tsKey)
					{
						ret.getTsIds().add(tsid);
						continue nextTsId;
					}
				}
			}
		
			HashMap<Long, ApiSiteRef> siteRefMap = siteDao.getSiteRefMap();
			q = "select SITE_ID from TSDB_GROUP_MEMBER_SITE "
				+ "where GROUP_ID = ?";
			rs = doQueryPs(conn, q, groupId);
			while(rs.next())
			{
				ApiSiteRef sr = siteRefMap.get(rs.getLong(1));
				if (sr != null)
					ret.getGroupSites().add(sr);
			}
			
			q = "select MEMBER_TYPE, MEMBER_VALUE from TSDB_GROUP_MEMBER_OTHER "
				+ "where GROUP_ID = ?";
			rs = doQueryPs(conn, q, groupId);
			while(rs.next())
				ret.getGroupAttrs().add(rs.getString(1) + "=" + rs.getString(2));

			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "getTsGroup: Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}

	}

	public ApiTsGroup writeGroup(ApiTsGroup grp)
		throws DbException, WebAppException
	{
		if (grp.getGroupName() == null || grp.getGroupName().trim().length() == 0)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Cannot write group without a name.");
			
		if (grp.getGroupId() == null)
			insertGroup(grp);
		else
			updateGroup(grp);
		writeGroupSubordinates(grp);
		return grp;
	}

	private void insertGroup(ApiTsGroup grp)
		throws DbException
	{
		String q = "insert into TSDB_GROUP(GROUP_ID, GROUP_NAME, GROUP_TYPE, GROUP_DESCRIPTION)"
			+ " values(?,?,?,?)";
		grp.setGroupId(getKey(DbInterface.Sequences.TSDB_GROUP));
		doModifyV(q, grp.getGroupId(), grp.getGroupName(), grp.getGroupType(), grp.getDescription());
	}

	private void updateGroup(ApiTsGroup grp)
		throws DbException
	{
		String q = "update TSDB_GROUP set GROUP_NAME = ?, GROUP_TYPE = ?, GROUP_DESCRIPTION = ?"
			+ " where GROUP_ID = ?";
		doModifyV(q, grp.getGroupName(), grp.getGroupType(), grp.getDescription(), grp.getGroupId());
	}
	
	private void writeGroupSubordinates(ApiTsGroup grp)
		throws DbException
	{
		String q = "delete from TSDB_GROUP_MEMBER_TS "
			+ "where GROUP_ID = " + grp.getGroupId();
		doModify(q);
		for(ApiTimeSeriesIdentifier tsid : grp.getTsIds())
		{
			q = "insert into TSDB_GROUP_MEMBER_TS(GROUP_ID, TS_ID) "
				+ "values(?, ?)";
			doModifyV(q, grp.getGroupId(), tsid.getKey());
		}
		
		q = "delete from TSDB_GROUP_MEMBER_GROUP where PARENT_GROUP_ID = ?";
		doModifyV(q, grp.getGroupId());
		
		for(ApiTsGroupRef gref : grp.getExcludeGroups())
		{
			q = "insert into TSDB_GROUP_MEMBER_GROUP(PARENT_GROUP_ID, CHILD_GROUP_ID, "
				+ "INCLUDE_GROUP) "
				+ "VALUES(?,?,?)";
			doModifyV(q, grp.getGroupId(), gref.getGroupId(), "S");
		}
		for(ApiTsGroupRef gref : grp.getIncludeGroups())
		{
			q = "insert into TSDB_GROUP_MEMBER_GROUP(PARENT_GROUP_ID, CHILD_GROUP_ID, "
				+ "INCLUDE_GROUP) "
				+ "VALUES(?,?,?)";
			doModifyV(q, grp.getGroupId(), gref.getGroupId(), "A");
		}
		for(ApiTsGroupRef gref : grp.getIntersectGroups())
		{
			q = "insert into TSDB_GROUP_MEMBER_GROUP(PARENT_GROUP_ID, CHILD_GROUP_ID, "
				+ "INCLUDE_GROUP) "
				+ "VALUES(?,?,?)";
			doModifyV(q, grp.getGroupId(), gref.getGroupId(), "I");
		}
		
		q = "delete from TSDB_GROUP_MEMBER_SITE where GROUP_ID = ?";
		doModifyV(q, grp.getGroupId());
		
		for(ApiSiteRef siteRef : grp.getGroupSites())
		{
			q = "insert into TSDB_GROUP_MEMBER_SITE(GROUP_ID, SITE_ID) "
				+ "values(?, ?)";
			doModifyV(q, grp.getGroupId(), siteRef.getSiteId());
		}
		
		q = "delete from TSDB_GROUP_MEMBER_DT where GROUP_ID = ?";
		doModifyV(q, grp.getGroupId());
		for(ApiDataType dt : grp.getGroupDataTypes())
		{
			q = "insert into TSDB_GROUP_MEMBER_DT(GROUP_ID, DATA_TYPE_ID) values (?, ?)";
			doModifyV(q, grp.getGroupId(), dt.getId());
		}
		
		q = "delete from TSDB_GROUP_MEMBER_OTHER where GROUP_ID = ?";
		doModifyV(q, grp.getGroupId());
		for(String gmo : grp.getGroupAttrs())
		{
			int eq = gmo.indexOf("=");
			if (eq < 0 || gmo.length() <= eq-1)
				continue;
			String typ = gmo.substring(0, eq);
			String val = gmo.substring(eq+1);
			q = "insert into TSDB_GROUP_MEMBER_OTHER(GROUP_ID, MEMBER_TYPE, MEMBER_VALUE) "
				+ "values(?,?,?)";
			doModifyV(q, grp.getGroupId(), typ, val);
		}
	}
	
	public void deleteGroup(Long groupId) throws DbException, WebAppException
	{
		String q = "select COMPUTATION_ID from CP_COMPUTATION where GROUP_ID = ?";
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, groupId);
			int nComps = 0;
			StringBuilder sb = new StringBuilder();
			while (rs.next())
			{
				sb.append(" " + rs.getInt(1));
				nComps++;
			}
			if (nComps > 0)
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete time series group with id=" + groupId
						+ " because it is being used by computations with the following IDs:" + sb.toString());

			q = "select PARENT_GROUP_ID from TS_GROUP_MEMBER_GROUP " + "where CHILD_GROUP_ID = ?";
			rs = doQueryPs(conn, q, groupId);
			int nGrps = 0;
			sb.setLength(0);
			while (rs.next())
			{
				sb.append(" " + rs.getInt(1));
				nGrps++;
			}
			if (nGrps > 0)
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete time series group with id=" + groupId
						+ " because it is being used within other groups with the following IDs:" + sb.toString());

			q = "delete from TSDB_GROUP_MEMBER_DT where GROUP_ID = ?";
			doModifyV(q, groupId);
			q = "delete from TSDB_GROUP_MEMBER_GROUP where PARENT_GROUP_ID = ?";
			doModifyV(q, groupId);
			q = "delete from TSDB_GROUP_MEMBER_OTHER where GROUP_ID = ?";
			doModifyV(q, groupId);
			q = "delete from TSDB_GROUP_MEMBER_SITE where GROUP_ID = ?";
			doModifyV(q, groupId);
			q = "delete from TSDB_GROUP_MEMBER_TS where GROUP_ID = ?";
			doModifyV(q, groupId);
			q = "delete from TSDB_GROUP where GROUP_ID = ?";
			doModifyV(q, groupId);

		}
		catch (SQLException ex)
		{
			String msg = "deleteGroup: Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}


}
