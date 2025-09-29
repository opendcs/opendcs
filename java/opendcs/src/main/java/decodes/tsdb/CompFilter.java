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
package decodes.tsdb;

import java.util.Iterator;
import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;
import ilex.util.TextUtil;

/**
 * A filter used by the Comp Edit GUI. This filter is used
 * to filter the Db Computations that show up in the Db Computation
 * tab list.
 */
public class CompFilter
{
	protected int minCompId = 0;

	protected DbKey siteId = Constants.undefinedId;
	protected DbKey dataTypeId = Constants.undefinedId;
	protected String intervalCode = null;
	protected DbKey processId = Constants.undefinedId;
	protected DbKey algoId = Constants.undefinedId;
	protected boolean filterLowIds = false;
	static public boolean doIntervalCheck = true;
	protected boolean enabledOnly = false;
	protected DbKey groupId = DbKey.NullKey;
	protected String execClassName = null;

	/**
	 * Return true if this filter includes checks on computation parameters
	 * for data type, interval, or site. Return false if it does not.
	 * This method is used in the GUI to determine if group computations must
	 * first be expanded in order to determine if it passes the filter.
	 * @return true if comp parms must be checked.
	 */
	public boolean hasParamConditions()
	{
		return !siteId.isNull() || !dataTypeId.isNull() || intervalCode != null;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder("CompFilter: ");
		if (siteId != Constants.undefinedId)
			sb.append("siteId=" + siteId + " ");
		if (dataTypeId != Constants.undefinedId)
			sb.append("dataTypeId=" + dataTypeId + " ");
		if (intervalCode != null)
			sb.append("intervalCode=" + intervalCode + " ");
		if (processId != Constants.undefinedId)
			sb.append("processId=" + processId + " ");
		if (algoId != Constants.undefinedId)
			sb.append("algoId=" + algoId + " ");
		if (filterLowIds)
			sb.append("filterLowIds=" + filterLowIds + " ");
		sb.append("doIntervalCheck=" + doIntervalCheck + " ");
		if (!DbKey.isNull(groupId))
			sb.append("groupId=" + groupId + " ");

		return sb.toString();
	}

	/** Constructor */
	public CompFilter()
	{
		//Get the DbComp config - tsdb.conf file. This file will contain
		//the minCompId property which is used to determine the computations
		//that will be displayed on the list
		minCompId = DecodesSettings.instance().minCompId;
	}

	public void setFilterLowIds(boolean filterLowIds)
	{
		this.filterLowIds = filterLowIds;
	}

	/**
	 * Used to determine if a given computation will be displayed on
	 * the Computation List or not. If the DbComputation id is greater
	 * or equal to the minCompId read from the tsdb.conf file, this Db
	 * Computation will show up in the list, otherwise it won't.
	 * It also tests the 'intervalCode' if one is specified.
	 *
	 * @param dbComp
	 * @return true if DbComputation will show up in the list, false
	 * otherwise
	 */
	public boolean passes(DbComputation dbComp)
	{
		if (dbComp == null)
			return false;

		if (!DbKey.isNull(processId) && !processId.equals(dbComp.getAppId()))
		{
			return false;
		}

		if (!DbKey.isNull(algoId) && !algoId.equals(dbComp.getAlgorithmId()))
		{
			return false;
		}

		if (filterLowIds && dbComp.getId().getValue() < minCompId)
			return false;

		if (intervalCode != null && doIntervalCheck)
		{
			boolean passes = false;
			for(Iterator<DbCompParm> pit = dbComp.getParms(); pit.hasNext(); )
			{
				DbCompParm dcp = pit.next();
				if (intervalCode.equalsIgnoreCase(dcp.getInterval()))
				{
					passes = true;
					break;
				}
			}
			if (!passes)
			{
				return false;
			}
		}

		if (!siteId.isNull())
		{
			boolean passes = false;
			for(Iterator<DbCompParm> pit = dbComp.getParms(); pit.hasNext(); )
			{
				DbCompParm dcp = pit.next();
				if (siteId.equals(dcp.getSiteId()))
				{
					passes = true;
					break;
				}
			}
			if (!passes)
			{
				return false;
			}
		}

		if (!dataTypeId.isNull())
		{
			boolean passes = false;
			for(Iterator<DbCompParm> pit = dbComp.getParms(); pit.hasNext(); )
			{
				DbCompParm dcp = pit.next();
				if (dataTypeId.equals(dcp.getDataTypeId()))
				{
					passes = true;
					break;
				}
			}
			if (!passes)
				return false;
		}

		if (enabledOnly && !dbComp.isEnabled())
			return false;

		if (!DbKey.isNull(groupId) && !groupId.equals(dbComp.getGroupId()))
			return false;

		if (execClassName != null)
		{
			if (dbComp.getAlgorithm() == null
			 || !TextUtil.strEqual(execClassName, dbComp.getAlgorithm().getExecClass()))
				return false;
		}

		return true;
	}

	public void setSiteId(DbKey x)
	{
		siteId = x;
	}

	public void setDataTypeId(DbKey x)
	{
		dataTypeId = x;
	}

	public void setIntervalCode(String x)
	{
		if (x != null && x.trim().length() == 0)
			x = null;
		intervalCode = x;
	}

	public void setProcessId(DbKey x)
	{
		processId = x;
	}

	public void setAlgoId(DbKey x)
	{
		algoId = x;
	}

	public DbKey getSiteId()
	{
		return siteId;
	}

	public DbKey getDataTypeId()
	{
		return dataTypeId;
	}

	public String getIntervalCode()
	{
		return intervalCode;
	}

	public DbKey getProcessId()
	{
		return processId;
	}

	public DbKey getAlgoId()
	{
		return algoId;
	}

	public void setEnabledOnly(boolean enabledOnly)
	{
		this.enabledOnly = enabledOnly;
	}

	public void setGroupId(DbKey groupId)
	{
		this.groupId = groupId;
	}

	public DbKey getGroupId()
	{
		return groupId;
	}

	public String getExecClassName()
	{
		return execClassName;
	}

	public void setExecClassName(String execClassName)
	{
		this.execClassName = execClassName;
	}

	public boolean isEnabledOnly()
	{
		return enabledOnly;
	}
}
