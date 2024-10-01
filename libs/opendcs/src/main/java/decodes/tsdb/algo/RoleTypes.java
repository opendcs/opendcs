package decodes.tsdb.algo;

/**
This class contains static strings and arrays representing the known
role types. These are used by the various GUIs.
*/
public class RoleTypes
{
	/** Simple role type names. */
	public static String[] roleTypes = 
	{
		"i",
		"o",
		"id",
		"idh",
		"idd",
		"idld",
		"idlm",
		"idly",
		"idlwy",
		"id5min",
		"id10min",
		"id15min",
		"id20min",
		"id30min"
	};

	/** Expanded role types with an explanation. Indexes match above. */
	private static String[] expandedRoleTypes = 
	{
		"i: Simple Input",
		"o: Simple Output",
		"id: Delta with Implicit Period",
		"idh: Hourly Delta",
		"idd: Daily Delta",
		"idld: Delta from end of last day",
		"idlm: Delta from end of last month",
		"idly: Delta from end of last year",
		"idlwy: Delta from end of last water-year",
		"id5min: Delta for last 5 minutes",
		"id10min: Delta for last 10 minutes",
		"id15min: Delta for last 15 minutes",
		"id20min: Delta for last 20 minutes",
		"id30min: Delta for last 30 minutes"
	};

	public static String[] getExpandedRoleTypes()
	{
		return expandedRoleTypes;
	}

	public static String getRoleType(int idx)
	{
		return expandedRoleTypes[idx];
	}

	/**
	 * Return the index of the passed role type or -1 if not found.
	 */
	public static int getIndex(String roleType)
	{
		for(int i=0; i<roleTypes.length; i++)
			if (roleTypes[i].equalsIgnoreCase(roleType))
				return i;
		return -1;
	}
}
