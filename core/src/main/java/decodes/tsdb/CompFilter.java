/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.4  2019/08/07 14:18:22  mmaloney
 * Add execClassName to possible criteria
 *
 * Revision 1.3  2017/08/22 19:56:39  mmaloney
 * Refactor
 *
 * Revision 1.2  2016/06/27 15:26:03  mmaloney
 * Added ability to filter by Enabled flag. Code cleanup.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.8  2013/07/24 15:28:28  mmaloney
 * dev
 *
 * Revision 1.7  2013/07/24 13:40:31  mmaloney
 * Must do site & datatype check in the passes() method for the new portable implementation.
 *
 * Revision 1.6  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
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
//Logger.instance().debug3("CompFilter: Interval Failed for comp " + dbComp.getId() + ", " + dbComp.getName());
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
//Logger.instance().debug3("CompFilter: Site Failed for comp " + dbComp.getId() + ", " + dbComp.getName());
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
