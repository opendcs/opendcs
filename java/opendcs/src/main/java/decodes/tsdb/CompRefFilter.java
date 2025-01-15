/**
 * $Id$
 *
 * Open Source Software
 *
 **/

package decodes.tsdb;

import java.util.function.Predicate;
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
	protected boolean enabledOnly = false;
	protected String group;

	// internal string validation
	private final Predicate<String> validString = s -> (s != null) && !s.isEmpty();
	private final Predicate<DbComputation> validSite = comp -> {
		if (site != null && !site.isEmpty())
		{
			if (comp.getParmList() == null || comp.getParmList().isEmpty())
			{
				return false;
			}
			else
			{
				return comp.getParmList()
					.stream()
					.anyMatch(item -> {
						if (item.getSiteName() == null
								|| item.getSiteName().getNameValue() == null
								|| item.getSiteName().getNameValue().isEmpty())
						{
							return false;
						}
						else
						{
							return item.getSiteName().getNameValue().equalsIgnoreCase(site);
						}
					});
			}
		}
		else
		{
			return true;
		}
	};
	private final Predicate<DbComputation> validDataType = comp ->
	{
		if (dataType != null
				&& !dataType.isEmpty())
		{
			if (comp.getParmList() == null || comp.getParmList().isEmpty())
			{
				return false;
			}
			return comp.getParmList()
				.stream()
				.anyMatch(item -> {
					if (item.getDataType() == null
							|| item.getDataType().getDisplayName() == null
							|| item.getDataType().getDisplayName().isEmpty())
					{
						return false;
					}
					else
					{
						return item.getDataType().getDisplayName().equalsIgnoreCase(dataType);
					}
				});
		}
		else
		{
			return true;
		}
	};
	private final Predicate<DbComputation> validInterval = comp -> {
		if (intervalCode != null
				&& !intervalCode.isEmpty())
		{
			if (comp.getParmList() == null || comp.getParmList().isEmpty())
			{
				return false;
			}
			else
			{
				return comp.getParmList()
					.stream()
					.anyMatch(item ->
					{
						if (item.getInterval() == null)
						{
							return false;
						}
						else
						{
							return item.getInterval().equalsIgnoreCase(intervalCode);
						}
					});
			}
		}
		else
		{
			return true;
		}
	};

	// external value validation
	private final Predicate<String> validProcess = match ->
	{
		if (process == null || process.isEmpty())
		{
			return true;
		}
		else
		{
			return process.equalsIgnoreCase(match);
		}
	};
	private final Predicate<String> validAlgorithm = match -> {
		if (algorithm == null || algorithm.isEmpty())
		{
			return true;
		}
		else
		{
			return algorithm.equalsIgnoreCase(match);
		}
	};
	private final Predicate<String> validGroup = match -> {
		if (group == null || group.isEmpty())
		{
			return true;
		}
		else
		{
			return group.equalsIgnoreCase(match);
		}
	};
	private final Predicate<Boolean> enabled = match -> {
		if (enabledOnly)
		{
			return match;
		}
		else
		{
			return true;
		}
	};
	public final Predicate<DbComputation> validComputation = comp -> validProcess.test(comp.getApplicationName())
				&& validAlgorithm.test(comp.getAlgorithmName())
				&& enabled.test(comp.isEnabled())
				&& validGroup.test(comp.getGroupName())
				&& validSite.test(comp)
				&& validDataType.test(comp)
				&& validInterval.test(comp);

	public String toString()
	{
		StringBuilder sb = new StringBuilder("CompFilter: ");
		if (validString.test(site))
			sb.append("siteId=" + site + " ");
		if (validString.test(dataType))
			sb.append("dataTypeId=" + dataType + " ");
		if (validString.test(intervalCode))
			sb.append("intervalCode=" + intervalCode + " ");
		if (validString.test(process))
			sb.append("processId=" + process + " ");
		if (validString.test(algorithm))
			sb.append("algoId=" + algorithm + " ");
		if (validString.test(group))
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
