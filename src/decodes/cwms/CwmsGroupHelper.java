/*
* $Id$
*  This is open-source software written by Sutron Corporation, under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
* Open Source Software
* 
* $Log$
* Revision 1.10  2013/04/23 13:25:23  mmaloney
* Office ID filtering put back into Java.
*
* Revision 1.9  2013/03/21 18:27:40  mmaloney
* DbKey Implementation
*
* Revision 1.8  2012/07/05 18:24:23  mmaloney
* tsKey is stored as a long.
*
* Revision 1.7  2011/04/27 16:12:38  mmaloney
* comment
*
* Revision 1.6  2011/04/19 19:14:10  gchen
* (1) Add a line to set Site.explicitList = true in cwmsTimeSeriesDb.java to fix the multiple location entries on Location Selector in TS Group GUI.
*
* (2) Fix a bug in getDataType(String standard, String code, int id) method in decodes.db.DataType.java because the data id wasn't set up previously.
*
* (3) Fix the null point exception in line 154 in cwmsGroupHelper.java.
*
* Revision 1.5  2011/02/04 21:30:48  mmaloney
* Intersect groups
*
* Revision 1.4  2011/02/03 20:00:23  mmaloney
* Time Series Group Editor Mods
*
* Revision 1.3  2011/01/27 19:07:11  mmaloney
* When expanding group, must compare UPPER for all path string components.
*
* Revision 1.2  2011/01/12 18:57:16  mmaloney
* dev
*
* Revision 1.1  2010/11/28 21:05:25  mmaloney
* Refactoring for CCP Time-Series Groups
*
*/
package decodes.cwms;

import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;

/**
This class is a helper to the TempestTsdb for reading & writing groups.
*/
public class CwmsGroupHelper
{
	private CwmsTimeSeriesDb tsdb;
	
	

	public CwmsGroupHelper(CwmsTimeSeriesDb tsdb)
	{
		this.tsdb = tsdb;
	}

	/**
	 * Recursively expand groups to find all ts_ids under the 
	 * specified group. This method would be called by report-generator
	 * programs that are given a group and must process all time-series
	 * contained within it or within its sub-groups.
	 * @param tsGroup the top-level group to expand
	 * @return list of all data-descriptors under this group or sub-groups
	 */
	public void expandTsGroupDescriptors(TsGroup tsGroup)
		throws DbIoException
	{
		tsdb.debug("CwmsGroupHelper.expandTsGroupDescriptors");
		tsGroup.clearExpandedList();

		ArrayList<DbKey> idsDone = new ArrayList<DbKey>();
		TreeSet<TimeSeriesIdentifier> tsIdSet 
			= new TreeSet<TimeSeriesIdentifier>(
				new Comparator<TimeSeriesIdentifier>()
				{
					public int compare(TimeSeriesIdentifier dd1, TimeSeriesIdentifier dd2)
					{
						long diff = dd1.getKey().getValue() - dd2.getKey().getValue();
						return diff < 0L ? -1 : diff > 0L ? 1 : 0;
					}
					public boolean equals(Object obj) { return false; }
				});
		doExpandTsGroup(tsGroup, idsDone, tsIdSet);
		
		for(TimeSeriesIdentifier dd : tsIdSet)
			tsGroup.addToExpandedList(dd);
		
		// Now expand all the 'excluded' groups and remove them from the
		// expanded list.
		tsIdSet.clear();
		idsDone.clear();
		for(TsGroup excludedGroup : tsGroup.getExcludedSubGroups())
			doExpandTsGroup(excludedGroup, idsDone, tsIdSet);
		for(TimeSeriesIdentifier dd : tsIdSet)
			tsGroup.rmFromExpandedList(dd);
		
		// Now handle all 'intersected' groups.
		if (tsGroup.getIntersectedGroups().size() > 0)
		{
			tsIdSet.clear();
			idsDone.clear();
			for(TsGroup intersectedGroup : tsGroup.getIntersectedGroups())
				doExpandTsGroup(intersectedGroup, idsDone, tsIdSet);
			for(Iterator<TimeSeriesIdentifier> tsit = tsGroup.getExpandedList().iterator();
				tsit.hasNext(); )
			{
				TimeSeriesIdentifier tsInGroup = tsit.next();
				boolean found = false;
				for(TimeSeriesIdentifier intTsId : tsIdSet)
					if (tsInGroup.getKey() == intTsId.getKey())
					{
						found = true;
						break;
					}
				if (!found)
					tsit.remove();
			}
		}
		
		tsGroup.setIsExpanded(true);
	}

	private void doExpandTsGroup(TsGroup tsGroup, ArrayList<DbKey> idsDone,
		TreeSet<TimeSeriesIdentifier> tsIdSet)
		throws DbIoException
	{
tsdb.debug("CwmsGroupHelper.doExpandTsGroup " + tsGroup.getGroupName());
tsdb.debug("  #sites=" + tsGroup.getSiteIdList().size()
+ ", isTransient=" + tsGroup.isTransient());

		// There may be dups & circular references in the hierarchy.
		// Only process each group once.
		for(DbKey id : idsDone)
			if (id.equals(tsGroup.getGroupId()))
				return;

		tsdb.debug("CwmsGroupHelper.doExpandTsGroup - 2");
		idsDone.add(tsGroup.getGroupId());
		if (!tsGroup.isTransient())
			// transient groups are built programmatically -- not in DB.
			// So if this is transient, leave it alone.
			tsdb.readTsGroupMembers(tsGroup);

		// Add explicitly included DDs to the list.
		for(TimeSeriesIdentifier tsid : tsGroup.getTsMemberList())
			tsIdSet.add(tsid);

		// Convert data type IDs to a list of string 'params'
		ArrayList<String> params = new ArrayList<String>();
		for(DbKey dtId : tsGroup.getDataTypeIdList())
		{
			DataType dt = DataType.getDataType(dtId);
			if (dt != null)
			  params.add(dt.getCode());
		}

		// Now read time-series determined by site, data-type, interval,
		// and statcode lists.
		ArrayList<DbKey> siteIds = tsGroup.getSiteIdList();
		ArrayList<String> paramTypes = tsGroup.getOtherMembers("ParamType");
		ArrayList<String> intervals = tsGroup.getOtherMembers("Interval");
		ArrayList<String> durations = tsGroup.getOtherMembers("Duration");
		ArrayList<String> versions = tsGroup.getOtherMembers("Version");
		if (siteIds.size() > 0 || params.size() > 0 || paramTypes.size() > 0
		 || intervals.size() > 0 || durations.size() > 0 || versions.size() > 0)
		{
			StringBuilder where = new StringBuilder();
			where.append("where a.db_office_code = a.db_office_code ");
//			where.append("where a.db_office_code = " + tsdb.getDbOfficeCode());
			if (siteIds.size() > 0)
			{
				where.append(" and a.location_code in(");
				int n = 0;
				for(DbKey X : siteIds)
				{
					if (n++ > 0)
						where.append(", ");
					where.append(X);
				}
				where.append(") ");
			}

			if (params.size() > 0)
			{
				where.append(" and upper(a.parameter_id) in(");
				int n = 0;
				for(String X : params)
				{
					if (n++ > 0)
						where.append(", ");
					where.append(tsdb.sqlString(X.toUpperCase()));
				}
				where.append(") ");
			}

			if (paramTypes.size() > 0)
			{
				where.append(" and upper(a.parameter_type_id) in(");
				int n = 0;
				for(String X : paramTypes)
				{
					if (n++ > 0)
						where.append(", ");
					where.append(tsdb.sqlString(X.toUpperCase()));
				}
				where.append(") ");
			}

			if (intervals.size() > 0)
			{
				where.append(" and upper(a.interval_id) in(");
				int n = 0;
				for(String X : intervals)
				{
					if (n++ > 0)
						where.append(", ");
					where.append(tsdb.sqlString(X.toUpperCase()));
				}
				where.append(") ");
			}

			if (durations.size() > 0)
			{
				where.append(" and upper(a.duration_id) in(");
				int n = 0;
				for(String X : durations)
				{
					if (n++ > 0)
						where.append(", ");
					where.append(tsdb.sqlString(X.toUpperCase()));
				}
				where.append(") ");
			}

			if (versions.size() > 0)
			{
				where.append(" and upper(a.version_id) in(");
				int n = 0;
				for(String X : versions)
				{
					if (n++ > 0)
						where.append(", ");
					where.append(tsdb.sqlString(X.toUpperCase()));
				}
				where.append(") ");
			}
			
			String q = "SELECT distinct a.TS_CODE, a.CWMS_TS_ID, a.VERSION_FLAG, "
				+ "a.INTERVAL_UTC_OFFSET, a.UNIT_ID, a.PARAMETER_ID, "
				+ "b.id, c.PUBLIC_NAME "
				+ "FROM CWMS_V_TS_ID a, DataType b, CWMS_V_LOC c " 
				+ where.toString()
				+ " and UPPER(a.parameter_id) = UPPER(b.code) "
				+ " and b.standard = " + tsdb.sqlString(Constants.datatype_CWMS)
				+ " and a.LOCATION_CODE = c.LOCATION_CODE "
				+ " and upper(a.DB_OFFICE_ID) = " + tsdb.sqlString(tsdb.getDbOfficeId());
			
			try
			{
				tsdb.debug("Expanding Group: " + q);
				ResultSet rs = tsdb.doQuery(q);
				while (rs != null && rs.next())
				{
					String param = rs.getString(6);
					DataType dt = DataType.getDataType(
						Constants.datatype_CWMS, param, DbKey.createDbKey(rs, 7));
					String desc = param + " at " + rs.getString(8);
					
					CwmsTsId cti = new CwmsTsId(DbKey.createDbKey(rs, 1), 
						rs.getString(2), dt, desc,
						TextUtil.str2boolean(rs.getString(3)),
						rs.getInt(4), rs.getString(5));
					cti.setDisplayName(desc);
					tsIdSet.add(cti);
				}
			}
			catch(SQLException ex)
			{
				tsdb.warning("Error reading group members with query '"
					+ q + "': " + ex);
			}
		}

		for(TsGroup subGroup : tsGroup.getIncludedSubGroups())
			doExpandTsGroup(subGroup, idsDone, tsIdSet);
	}
}
