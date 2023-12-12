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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.opendcs.odcsapi.beans.ApiCompParm;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiSiteName;
import org.opendcs.odcsapi.beans.ApiComputationRef;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiTextUtil;

import java.util.logging.Logger;


public class ApiComputationDAO
	extends ApiDaoBase
{
	private static String module = "ApiComputationDao";
	
	public String compTableColumnsNoTabName =
			"computation_id, computation_name, "
			+ "algorithm_id, "
			+ "cmmnt, loading_application_id, date_time_loaded, enabled, "
			+ "effective_start_date_time, effective_end_date_time, group_id";


	public ApiComputationDAO(DbInterface dbi)
	{
		super(dbi, module);
	}

	public void close()
	{
		super.close();
	}

	public ArrayList<ApiComputationRef> getComputationRefs(
		String site, String algorithm, String datatype, String group,
		String process, Boolean enabled, String interval)
		throws DbException, WebAppException
	{
		String q = "";
		try
		{
			
			Long siteId = null;
			if (site != null)
			{
				q = "select siteid from sitename where lower(sitename) = ?";
				siteId = lookupId(q, site.toLowerCase());
				if (siteId == null)
					throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
						"No such site '" + site + "'");
			}
			
			Long algoId = null;
			if (algorithm != null)
			{
				q = "select ALGORITHM_ID from CP_ALGORITHM where lower(ALGORITHM_NAME) = ?";
				algoId = lookupId(q, algorithm.toLowerCase());
				if (algoId == null)
					throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
						"No such algorithm '" + algorithm + "'");
			}
			
			Long datatypeId = null;
			if (datatype != null)
			{
				q = "select ID from DATATYPE where lower(CODE) = ?";
				datatypeId = lookupId(q, datatype.toLowerCase());
				if (datatypeId == null)
					throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
						"No such datatype '" + datatype + "'");
			}
			
			Long groupId = null;
			if (group != null)
			{
				q = "select GROUP_ID from TSDB_GROUP"
					+ " where lower(GROUP_NAME) = ?";
				groupId = lookupId(q, group.toLowerCase());
				if (groupId == null)
					throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
						"No such time series group '" + group + "'");
			}
			
			Long appId = null;
			if (process != null)
			{
				q = "select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION"
						+ " where lower(LOADING_APPLICATION_NAME) = ?";
				appId = lookupId(q, process.toLowerCase());
				if (appId == null)
					throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
							"No such process '" + process + "'");
	
			}
			
			ArrayList<String> compWhereClauses = new ArrayList<String>();
			ArrayList<Object> args = new ArrayList<Object>();
			if (algoId != null)
			{
				compWhereClauses.add("ALGORITHM_ID = ?");
				args.add(algoId);
			}
			if (groupId != null)
			{
				compWhereClauses.add("GROUP_ID = ?");
				args.add(groupId);
			}
			if (appId != null)
			{
				compWhereClauses.add("LOADING_APPLICATION_ID = ?");
				args.add(appId);
			}
			if (enabled != null)
			{
				if (enabled)
				{
					compWhereClauses.add("(lower(ENABLED) like 'y%' OR lower(ENABLED) like 't%')");
				}
				else
				{
					compWhereClauses.add("(lower(ENABLED) like 'n%' OR lower(ENABLED) like 'f%')");
				}
			}
			
			ArrayList<String> parmWhereClauses = new ArrayList<String>();
			if (siteId != null)
			{
				parmWhereClauses.add("SITE_ID = ?");
				args.add(siteId);
			}
			if (interval != null)
			{
				parmWhereClauses.add("lower(INTERVAL_ABBR) = ?");
				args.add(getSingleWord(interval).toLowerCase());
			}
			if (datatypeId != null)
			{
				parmWhereClauses.add("DATATYPE_ID = ?");
				args.add(datatypeId);
			}
	
			StringBuilder sb = new StringBuilder(
				"select c.COMPUTATION_ID, c.COMPUTATION_NAME, c.ALGORITHM_ID, c.CMMNT, "
				+ "c.LOADING_APPLICATION_ID, c.ENABLED, c.GROUP_ID "
				+ "from CP_COMPUTATION c");
			int n = 0;
			for(String cwc : compWhereClauses)
			{
				if (n++ == 0)
					sb.append(" WHERE ");
				else
					sb.append(" AND ");
				sb.append(cwc);
			}
			
			if (!parmWhereClauses.isEmpty())
			{
				if (n++ == 0)
					sb.append(" WHERE ");
				else
					sb.append(" AND ");
				sb.append(" EXISTS (select * from CP_COMP_TS_PARM p "
					+ "where p.COMPUTATION_ID = c.COMPUTATION_ID ");
				for(String pwc : parmWhereClauses)
					sb.append(" AND " + pwc);
				sb.append(")");
			}
			
			q = sb.toString();
			Logger.getLogger(ApiConstants.loggerName).info(module + ".getComputationRefs q=" + q);
					
			ArrayList<ApiComputationRef> ret = new ArrayList<ApiComputationRef>();
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, args.toArray());
			
			HashSet<Long> algoIdL = new HashSet<Long>();
			HashSet<Long> procIdL = new HashSet<Long>();
			HashSet<Long> grpIdL = new HashSet<Long>();
			while(rs.next())
			{
				ApiComputationRef ref = new ApiComputationRef();
				ref.setComputationId(rs.getLong(1));
				ref.setName(rs.getString(2));
				Long ll = rs.getLong(3);
				if (!rs.wasNull())
				{
					ref.setAlgorithmId(ll);
					algoIdL.add(ll);
				}
				ref.setDescription(rs.getString(4));
				ll = rs.getLong(5);
				if (!rs.wasNull())
				{
					ref.setProcessId(ll);
					procIdL.add(ll);
				}
				ref.setEnabled(ApiTextUtil.str2boolean(rs.getString(6)));
				ll = rs.getLong(7);
				if (!rs.wasNull())
				{
					ref.setGroupId(ll);
					grpIdL.add(ll);
				}
				
				ret.add(ref);
			}
			
			if (algoIdL.size() > 0)
			{
				StringBuilder algoIds = new StringBuilder();
				ArrayList<Long> algoIdArgs = new ArrayList<Long>();
				for(Long aid : algoIdL)
				{
					if (algoIds.length() > 0)
						algoIds.append(", ");
					algoIds.append("" + aid);
					algoIdArgs.add(aid);
				}
				String[] placeHolders = new String[algoIdArgs.size()];
				Arrays.fill(placeHolders, "%s");
				String[] qMarks = new String[algoIdArgs.size()];
				Arrays.fill(qMarks, "?");

				String tempQueryStr = "select ALGORITHM_ID, ALGORITHM_NAME from CP_ALGORITHM where ALGORITHM_ID in (" + String.join(",", placeHolders) + ")";
				q = String.format(tempQueryStr, qMarks);
				rs = doQueryPs(conn, q, algoIdArgs.toArray());
				
				while(rs.next())
				{
					long id = rs.getLong(1);
					String nm = rs.getString(2);
					for(ApiComputationRef ref : ret)
						if (ref.getAlgorithmId() != null && ref.getAlgorithmId() == id)
							ref.setAlgorithmName(nm);
				}
			}
			
			if (procIdL.size() > 0)
			{
				StringBuilder procIds = new StringBuilder();
				ArrayList<Long> procIdArgs = new ArrayList<Long>();
				for(Long pid : procIdL)
				{
					if (procIds.length() > 0)
					procIds.append(", ");
					procIds.append("" + pid);
					procIdArgs.add(pid);
				}

				String[] placeHolders = new String[procIdArgs.size()];
				Arrays.fill(placeHolders, "%s");
				String[] qMarks = new String[procIdArgs.size()];
				Arrays.fill(qMarks, "?");

				String tempQueryStr = "select LOADING_APPLICATION_ID, LOADING_APPLICATION_NAME from HDB_LOADING_APPLICATION where LOADING_APPLICATION_ID in (" + String.join(",", placeHolders) + ")";
				//String tempQueryStr = "select ALGORITHM_ID, ALGORITHM_NAME from CP_ALGORITHM where ALGORITHM_ID in (" + String.join(",", placeHolders) + ")";
				q = String.format(tempQueryStr, qMarks);
				rs = doQueryPs(conn, q, procIdArgs.toArray());
				
				while(rs.next())
				{
					long id = rs.getLong(1);
					String nm = rs.getString(2);
					for(ApiComputationRef ref : ret)
						if (ref.getProcessId() != null && ref.getProcessId() == id)
							ref.setProcessName(nm);
				}
			}
	
			if (grpIdL.size() > 0)
			{
				StringBuilder grpIds = new StringBuilder();
				ArrayList<Long> grpIdArgs = new ArrayList<Long>();
				for(Long gid : grpIdL)
				{
					if (grpIds.length() > 0)
						grpIds.append(", ");
					grpIds.append("" + gid);
					grpIdArgs.add(gid);
				}

				String[] placeHolders = new String[grpIdArgs.size()];
				Arrays.fill(placeHolders, "%s");
				String[] qMarks = new String[grpIdArgs.size()];
				Arrays.fill(qMarks, "?");
				
				String tempQueryStr = "select GROUP_ID, GROUP_NAME from TSDB_GROUP  where GROUP_ID in (" + String.join(",", placeHolders) + ")"; // + grpIds.toString() + ")";
				q = String.format(tempQueryStr, qMarks);
				
				rs = doQueryPs(conn, q, grpIdArgs.toArray());
				while(rs.next())
				{
					long id = rs.getLong(1);
					String nm = rs.getString(2);
					for(ApiComputationRef ref : ret)
						if (ref.getGroupId() != null && ref.getGroupId() == id)
							ref.setGroupName(nm);
				}
			}
			
			return ret;
		
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}

	public ApiComputation getComputation(long compId)
		throws DbException, WebAppException
	{
		
		String q = "select computation_id, computation_name, algorithm_id, "
					+ "cmmnt, loading_application_id, date_time_loaded, enabled, "
					+ "effective_start_date_time, effective_end_date_time, group_id "
					+ " from CP_COMPUTATION where COMPUTATION_ID = ?";
		try (ApiSiteDAO siteDAO = new ApiSiteDAO(dbi))
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, compId);
			if (rs.next())
			{
				ApiComputation comp = rs2comp(rs);
				if (comp.getAlgorithmId() != null)
				{
					q = "select ALGORITHM_NAME from CP_ALGORITHM "
							+ "where ALGORITHM_ID = ?";
					rs = doQueryPs(conn, q, comp.getAlgorithmId());
					if (rs.next())
						comp.setAlgorithmName(rs.getString(1));
				}
				if (comp.getAppId() != null)
				{
					q = "select LOADING_APPLICATION_NAME "
							+ "from HDB_LOADING_APPLICATION "
							+ "where LOADING_APPLICATION_ID = ?";
					rs = doQueryPs(conn, q, comp.getAppId());
					if (rs.next())
						comp.setApplicationName(rs.getString(1));
				}
				if (comp.getGroupId() != null)
				{
					q = "select GROUP_NAME "
							+ "from TSDB_GROUP "
							+ "where GROUP_ID = ?";
					rs = doQueryPs(conn, q, comp.getGroupId());
					if (rs.next())
						comp.setGroupName(rs.getString(1));
				}

				q = "select ALGO_ROLE_NAME, SITE_DATATYPE_ID, "
					+ "INTERVAL_ABBR, TABLE_SELECTOR, DELTA_T, MODEL_ID, DATATYPE_ID, "
					+ "DELTA_T_UNITS, SITE_ID "
					+ "from CP_COMP_TS_PARM where COMPUTATION_ID = ?";
				rs = doQueryPs(conn, q, comp.getComputationId());
				while(rs.next())
				{
					ApiCompParm acp = new ApiCompParm();
					acp.setAlgoRoleName(rs.getString(1));
					Long tsKey = rs.getLong(2);
					if (rs.wasNull() || tsKey == -1L)
						tsKey = null;
					acp.setTsKey(tsKey);
					acp.setInterval(rs.getString(3));
					
					String tabsel = rs.getString(4);
					if (DbInterface.isCwms || DbInterface.isOpenTsdb)
					{
						// tabsel is parmtype.duration.version.locSpec.parmSpec
						String parsed[] = tabsel.split("\\.");
						if (parsed != null)
						{
							if (parsed.length >= 1)
								acp.setParamType(parsed[0]);
							if (parsed.length >= 2)
								acp.setDuration(parsed[1]);
							if (parsed.length >= 3)
								acp.setVersion(parsed[2]);
							if (parsed.length >= 4)
								acp.setSiteName(parsed[3]);
							if (parsed.length >= 5)
								acp.setDataType(parsed[4]);
						}
					}
					else if (DbInterface.isHdb)
						acp.setTableSelector(tabsel);

					acp.setDeltaT(rs.getInt(5));
					
					int ii = rs.getInt(6);
					acp.setModelId(rs.wasNull() || ii == -1 ? null : ii);
					
					long ll = rs.getLong(7);
					acp.setDataTypeId(rs.wasNull() || ll == -1 ? null : ll);
					
					acp.setDeltaTUnits(rs.getString(8));
					ll = rs.getLong(9);
					acp.setSiteId(rs.wasNull() || ll == -1 ? null : ll);
					
					comp.getParmList().add(acp);
					
				}
				for (ApiCompParm acp : comp.getParmList())
				{
					if (acp.getDataTypeId() != null)
					{
						q = "select CODE from DATATYPE where ID = ?";
						rs = doQueryPs(conn, q, acp.getDataTypeId());
						if (rs.next())
							acp.setDataType(rs.getString(1));
					}
					if (comp.getAlgorithmId() != null)
					{
						q = "select PARM_TYPE from CP_ALGO_TS_PARM where "
								+ "ALGORITHM_ID = ? and lower(ALGO_ROLE_NAME) = ?";
						rs = doQueryPs(conn, q, comp.getAlgorithmId(), acp.getAlgoRoleName().toLowerCase());
						if (rs.next())
							acp.setAlgoParmType(rs.getString(1));
					}
					if (acp.getSiteId() != null)
					{
						ArrayList<ApiSiteName> sns = siteDAO.getSiteNames(acp.getSiteId());
						if (sns != null)
						{
							for(ApiSiteName sn : sns)
							{
								if (sn.getNameType().equalsIgnoreCase(DbInterface.siteNameTypePreference))
								{
									acp.setSiteName(sn.getNameValue());
									break;
								}
							}
							if (acp.getSiteName() == null && sns.size() > 0)
								acp.setSiteName(sns.get(0).getNameValue());
						}
					}
				}
				
				q = "select PROP_NAME, PROP_VALUE from CP_COMP_PROPERTY where computation_id = ?";
				rs = doQueryPs(conn, q, comp.getComputationId());
				while(rs.next())
				{
					String name = rs.getString(1);
					String value = rs.getString(2);
					
					if (name.equalsIgnoreCase("EffectiveStart"))
					{
						int idx = value.indexOf('-');
						if (idx >= 0)
						{
							comp.setEffectiveStartInterval(value.substring(idx+1).trim());
							comp.setEffectiveStartType("Now -");
						}
						else
							comp.setEffectiveStartType("No Limit");
	
					}
					else if (name.equalsIgnoreCase("EffectiveEnd"))
					{
						if (value.equalsIgnoreCase("now"))
							comp.setEffectiveEndType("Now");
						else
						{
							int idx = value.indexOf('-');
							if (idx > 0)
							{
								comp.setEffectiveEndInterval(value.substring(idx+1).trim());
								comp.setEffectiveEndType("Now -");
							}
							else if ((idx = value.indexOf('+')) > 0)
							{
								comp.setEffectiveEndType("Now +");
								comp.setEffectiveEndInterval(value.substring(idx+1).trim());
							}
						}
					}
					else if (name.toLowerCase().endsWith("_eu"))
					{
						String parmName = name.substring(0, name.length()-3);
						ApiCompParm cp = comp.findParm(parmName);
						if (cp != null)
							cp.setUnitsAbbr(value);
					}
					else if (name.toLowerCase().endsWith("_missing"))
					{
						String parmName = name.substring(0, name.length()-8);
						ApiCompParm cp = comp.findParm(parmName);
						if (cp != null)
							cp.setIfMissing(name);
					}

					else
						comp.getProps().setProperty(name, value);
		
					
				}

				return comp;
			}
			else
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No computation with ID=" + compId);
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}
		
	/**
	 * Create computation object and fill with current row of result set.
	 */
	protected ApiComputation rs2comp(ResultSet rs)
		throws DbException, SQLException
	{
		ApiComputation ret = new ApiComputation();
		ret.setComputationId(rs.getLong(1));
		ret.setName(rs.getString(2));
		long ll = rs.getLong(3);
		ret.setAlgorithmId(rs.wasNull() ? null : ll);
		ret.setComment(rs.getString(4));
		ll = rs.getLong(5);
		ret.setAppId(rs.wasNull() ? null : ll);
		ret.setLastModified(dbi.getFullDate(rs, 6));
		ret.setEnabled(ApiTextUtil.str2boolean(rs.getString(7)));
		ret.setEffectiveStartDate(dbi.getFullDate(rs, 8));
		if (ret.getEffectiveStartDate() != null)
			ret.setEffectiveStartType("Calendar");
		else
			ret.setEffectiveStartType("No Limit");

		ret.setEffectiveEndDate(dbi.getFullDate(rs, 9));
		if (ret.getEffectiveEndDate() != null)
			ret.setEffectiveEndType("Calendar");
		else
			ret.setEffectiveEndType("No Limit");
		
		ll = rs.getLong(10);
		ret.setGroupId(rs.wasNull() || ll == -1L ? null : ll);
		return ret;
	}

	
	public ApiComputation writeComputation(ApiComputation comp) 
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		ArrayList<Object> args = new ArrayList<Object>();
		String q = "select COMPUTATION_ID from CP_COMPUTATION where lower(COMPUTATION_NAME) = ?"; 
		args.add(comp.getName().toLowerCase());
		if (comp.getComputationId() != null)
		{
			q = q + " and COMPUTATION_ID != ?";
			args.add(comp.getComputationId());
		}
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write computation with name '" + comp.getName() 
					+ "' because another compuation with id=" + rs.getLong(1) 
					+ " also has that name.");
			
			if (comp.getComputationId() == null)
				insert(comp);
			else
				update(comp);
		}
		catch (SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}

		return comp;
	}

	private void insert(ApiComputation comp)
		throws DbException
	{
		comp.setComputationId(getKey("CP_COMPUTATION"));
		comp.setLastModified(new Date());
		
		String q = "insert into CP_COMPUTATION(computation_id, computation_name, "
			+ "algorithm_id, "
			+ "cmmnt, loading_application_id, date_time_loaded, enabled, "
			+ "effective_start_date_time, effective_end_date_time, group_id) "
			+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		doModifyV(q, comp.getComputationId(), 
					comp.getName(), 
					comp.getAlgorithmId(), 
					comp.getComment(), 
					comp.getAppId(), 
					dbi.sqlDateV(comp.getLastModified()),
					sqlBoolean(comp.isEnabled()),
					dbi.sqlDateV(comp.getEffectiveStartDate()),
					dbi.sqlDateV(comp.getEffectiveEndDate()),
					comp.getGroupId()
				);

		writeSubordinates(comp);
	}
	
	private void writeSubordinates(ApiComputation comp)
		throws DbException
	{
		String q = "";
		for(ApiCompParm acp : comp.getParmList())
		{
			String tabsel = "";
			// build table selector from TSID attributes
			if (DbInterface.isCwms || DbInterface.isOpenTsdb)
				tabsel = 
					(acp.getAlgoParmType()==null?"":acp.getAlgoParmType()) + "."
					+ (acp.getDuration()==null?"":acp.getDuration()) + "."
					+ (acp.getVersion()==null?"":acp.getVersion()) + "."
					+ (acp.getSiteName()==null?"":acp.getSiteName()) + "."
					+ (acp.getDataType()==null?"":acp.getDataType());
			else if (DbInterface.isHdb)
				tabsel = acp.getTableSelector();

			q = "INSERT INTO CP_COMP_TS_PARM(computation_id, algo_role_name, site_datatype_id, "
				+ "interval_abbr, table_selector, delta_t, model_id, datatype_id, "
				+ "delta_t_units, site_id) "
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			doModifyV(q,
					comp.getComputationId(),
					acp.getAlgoRoleName(),
					acp.getTsKey(),
					acp.getInterval(),
					tabsel,
					acp.getDeltaT(),
					acp.getModelId(),
					acp.getDataTypeId(),
					acp.getDeltaTUnits(),
					acp.getSiteId());
			
			if (acp.getUnitsAbbr() != null)
				writeProp(comp.getComputationId(), acp.getAlgoRoleName() + "_EU", acp.getUnitsAbbr());
			if (acp.getIfMissing() != null)
				writeProp(comp.getComputationId(), acp.getAlgoRoleName() + "_MISSING", acp.getUnitsAbbr());
		}
		
		for(Object k : comp.getProps().keySet())
		{
			String name = (String)k;
			String value = comp.getProps().getProperty(name);
			writeProp(comp.getComputationId(), name, value);
		}
		
		if (comp.getEffectiveStartType().toLowerCase().startsWith("now -"))
		{
			String name = "EffectiveStart";
			String value = "now - " + comp.getEffectiveStartInterval();
			writeProp(comp.getComputationId(), name, value);
		}

		if (comp.getEffectiveEndType().toLowerCase().startsWith("now -"))
		{
			String name = "EffectiveEnd";
			String value = "now - " + comp.getEffectiveEndInterval();
			writeProp(comp.getComputationId(), name, value);
		}
		else if (comp.getEffectiveEndType().toLowerCase().startsWith("now +"))
		{
			String name = "EffectiveEnd";
			String value = "now + " + comp.getEffectiveEndInterval();
			writeProp(comp.getComputationId(), name, value);
		}
	}

	private void writeProp(Long computationId, String name, String value)
		throws DbException
	{
		String q = "insert into CP_COMP_PROPERTY(COMPUTATION_ID, PROP_NAME, PROP_VALUE) "
			+ " values(?, ?, ?)";
		doModifyV(q, computationId, name, value);
	}

	private void update(ApiComputation comp)
			throws DbException
	{
		deleteSubordinates(comp.getComputationId());
		
		Date curDate = new Date();  //OLAV ADDED 
		
		String q = "update CP_COMPUTATION set " 
			+ "computation_name = ?, algorithm_id = ?, cmmnt = ?, loading_application_id = ?, date_time_loaded = ?, "
            + "enabled = ?, effective_start_date_time = ?, effective_end_date_time = ?, group_id = ? where COMPUTATION_ID = ?";
		doModifyV(q, 
				comp.getName(), 
				comp.getAlgorithmId(), 
				comp.getComment(), 
				comp.getAppId(),
				dbi.sqlDateV(curDate),
				sqlBoolean(comp.isEnabled()),
				dbi.sqlDateV(comp.getEffectiveStartDate()),
				dbi.sqlDateV(comp.getEffectiveEndDate()),
				comp.getGroupId(),
				comp.getComputationId());
					
		writeSubordinates(comp);
	}

	private void deleteSubordinates(Long id)
		throws DbException
	{
		String q = "DELETE FROM CP_COMP_TS_PARM WHERE COMPUTATION_ID = ?";
		doModifyV(q, id);
		q = "DELETE FROM CP_COMP_PROPERTY WHERE COMPUTATION_ID = ?";
		doModifyV(q, id);
	}

	public void deleteComputation(Long computationId)
		throws DbException
	{
		deleteSubordinates(computationId);
		String q = "delete from CP_COMPUTATION where COMPUTATION_ID = ?";
		doModifyV(q, computationId);
	}
}
