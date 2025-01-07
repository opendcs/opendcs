/**
 * $Id$
 *
 * Open Source Software
 *
 **/

package decodes.tsdb;

import java.util.Iterator;
import decodes.util.DecodesSettings;

/**
 * A filter used by the Comp Ref retrieval method of the REST API. This filter is used
 * to filter the Db Computations that show up in the response.
 */
public class CompRefFilter
{
	protected int minCompId = 0;

	protected String site;
	protected String dataType;
	protected String intervalCode = null;
	protected String process;
	protected String algorithm;
	public static boolean doIntervalCheck = true;
	protected boolean enabledOnly = false;
	protected String group;

	public String toString()
	{
		StringBuilder sb = new StringBuilder("CompFilter: ");
		if ((site != null) && !site.isEmpty())
			sb.append("siteId=" + site + " ");
		if ((dataType != null) && !dataType.isEmpty())
			sb.append("dataTypeId=" + dataType + " ");
		if ((intervalCode != null) &&!intervalCode.isEmpty())
			sb.append("intervalCode=" + intervalCode + " ");
		if ((process != null) && !process.isEmpty())
			sb.append("processId=" + process + " ");
		if ((algorithm != null) && !algorithm.isEmpty())
			sb.append("algoId=" + algorithm + " ");
		sb.append("doIntervalCheck=" + doIntervalCheck + " ");
		if ((group != null) && !group.isEmpty())
			sb.append("groupId=" + group + " ");

		return sb.toString();
	}

	/** Constructor */
	public CompRefFilter()
	{
		//Get the DbComp config - tsdb.conf file. This file will contain
		//the minCompId property which is used to determine the computations
		//that will be displayed on the list
		minCompId = DecodesSettings.instance().minCompId;
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
		{
			return false;
		}

		if (dbComp.getId().getValue() < minCompId)
		{
			return false;
		}

		if (!intervalCode.isEmpty() && doIntervalCheck)
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

		if (!site.isEmpty())
		{
			boolean passes = false;
			for(Iterator<DbCompParm> pit = dbComp.getParms(); pit.hasNext(); )
			{
				DbCompParm dcp = pit.next();
				if (site.equalsIgnoreCase(dcp.getSiteName().getNameValue()))
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

		if (!dataType.isEmpty())
		{
			boolean passes = false;
			for(Iterator<DbCompParm> pit = dbComp.getParms(); pit.hasNext(); )
			{
				DbCompParm dcp = pit.next();
				if (dataType.equalsIgnoreCase(dcp.getDataType().getDisplayName()))
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

		return group.isEmpty() || group.equalsIgnoreCase(dbComp.getGroup().getGroupName());
	}

	public void setSite(String x)
	{
		site = x;
	}

	public void setDataType(String x)
	{
		dataType = x;
	}

	public void setIntervalCode(String x)
	{
		if (x != null && x.trim().isEmpty())
			x = null;
		intervalCode = x;
	}

	public void setProcess(String x)
	{
		process = x;
	}

	public void setAlgorithm(String x)
	{
		algorithm = x;
	}

	public String getSite()
	{
		return site;
	}

	public String getDataType()
	{
		return dataType;
	}

	public String getIntervalCode()
	{
		return intervalCode;
	}

	public String getProcess()
	{
		return process;
	}

	public String getAlgorithm()
	{
		return algorithm;
	}

	public void setEnabledOnly(boolean enabledOnly)
	{
		this.enabledOnly = enabledOnly;
	}

	public void setGroup(String group)
	{
		this.group = group;
	}

	public String getGroup()
	{
		return group;
	}

	public boolean isEnabledOnly()
	{
		return enabledOnly;
	}
}
