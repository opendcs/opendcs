package decodes.cwms;

public class TsidMorpher
{
	/**
	 * For version 6.3, morph the tsid part by the computation param part.
	 * @param tsidComponent The component from the TSID
	 * @param parmComponent The component in the comp parm, which may contain wildcards.
	 * @return the tsid component masked by the parm component, or null if can't match.
	 */
	public static String morph(String tsidComponent, String parmComponent)
	{
		// Examples:
		// tsid: A-B-C   parm: D-*-F   result: D-B-F
		// tsid: A-B     parm: *-E-F   result: A-E-F
		// tsid: A-B-C   parm: D-*     result: D-B-C
		// tsid: A       parm: D-*     result: null
		// tsid: A-B-C   parm: *-D     result: A-D
		// tsid: A-B     parm: *-      result: A
		
		// Check for a partial location specification (OpenDCS 6.3)
		String tps[] = tsidComponent.split("-");
		String pps[] = parmComponent.split("-");
		StringBuilder sb = new StringBuilder();
		for(int idx = 0; idx < pps.length; )
		{
			if (pps[idx].equals("*"))
			{
				if (idx >= tps.length)
					return null;
				else
				{
					// A trailing asterisk in the mask means copy in rest of tsid.
					// However a trailing hyphen means lop off the rest of tsid.
					if (idx == pps.length - 1
					 && !parmComponent.endsWith("-"))
					{
						for(int tidx = idx; tidx < tps.length; tidx++)
							sb.append(tps[tidx] + (tidx < tps.length-1 ? "-" : ""));
					}
					else
						sb.append(tps[idx]);
				}
			}
			else
				sb.append(pps[idx]);
			if (++idx < pps.length)
				sb.append("-");
		}
		return sb.toString();
		
		/*
		 * Note: the table_selector in cp_comp_ts_parm will be empty for a component that is 
		 * completely undefined. The syntax is ParamType.Duration.Version[.SiteSpec.ParmSpec],
		 * So "Total.1Hour." means that Version is undefined and shows as <var> in the gui.
		 * This is different from "Total.1Hour.Something-*". Meaning that the first part of
		 * the subversion can be anything.
		 */
	}

}
