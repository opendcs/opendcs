package opendcs.opentsdb;

import java.util.StringTokenizer;

public class OpenTsdbFlags
{
	public static final int RESERVED_4_COMP               = 0x0000000F;
	public static final int RESERVED_4_IFLAG              = 0xF0000000;
	
	// The value was successfully screened.
	public static final int SCREENED                      = 0x00010000;
	
	// Apply this mask and compare to SCR_VALUE_xxx definitions to obtain result
	public static final int SCR_VALUE_RESULT_MASK         = 0x000E0000;
	public static final int SCR_VALUE_GOOD                = 0x00000000;
	public static final int SCR_VALUE_REJECT_HIGH         = 0x00020000;
	public static final int SCR_VALUE_CRITICAL_HIGH       = 0x00040000;
	public static final int SCR_VALUE_WARNING_HIGH        = 0x00060000;
	public static final int SCR_VALUE_WARNING_LOW         = 0x00080000;
	public static final int SCR_VALUE_CRITICAL_LOW        = 0x000A0000;
	public static final int SCR_VALUE_REJECT_LOW          = 0x000C0000;
	
	// Apply this mask and compare to SCR_ROC_xxx definitions to obtain result
	public static final int SCR_ROC_RESULT_MASK           = 0x00700000;
	public static final int SCR_ROC_GOOD                  = 0x00000000;
	public static final int SCR_ROC_REJECT_HIGH           = 0x00100000;
	public static final int SCR_ROC_CRITICAL_HIGH         = 0x00200000;
	public static final int SCR_ROC_WARNING_HIGH          = 0x00300000;
	public static final int SCR_ROC_WARNING_LOW           = 0x00400000;
	public static final int SCR_ROC_CRITICAL_LOW          = 0x00500000;
	public static final int SCR_ROC_REJECT_LOW            = 0x00600000;
	
	public static final int SCR_STUCK_SENSOR_DETECTED     = 0x00800000;

	// The following is NOT stored in data values, but used by the
	// alarm system only
	public static final int SCR_MISSING_VALUES_EXCEEDED   = 0x01000000;

	// All bits used in screening:
	public static final int SCREENING_MASK = SCREENED | SCR_VALUE_RESULT_MASK 
		| SCR_ROC_RESULT_MASK | SCR_STUCK_SENSOR_DETECTED | SCR_MISSING_VALUES_EXCEEDED;


	public OpenTsdbFlags()
	{
		// TODO Auto-generated constructor stub
	}

	/**
	 * Convert an integer containing bit flags into a string of the form
	 * S(conditions), where 'conditions' is a string representation of the
	 * asserted screening conditions.
	 * @param flags integer flag word
	 * @return string representation of screening conditions
	 */
	public static String flags2screeningString(int flags)
	{
		if ((flags & SCREENED) == 0)
			return null;
		StringBuilder sb = new StringBuilder("S(");
		String val = null;
		boolean notOk = false;
		switch(flags & SCR_VALUE_RESULT_MASK)
		{
		case SCR_VALUE_GOOD: break;
		case SCR_VALUE_REJECT_HIGH:   val = "R+"; break;
		case SCR_VALUE_CRITICAL_HIGH: val = "++"; break;
		case SCR_VALUE_WARNING_HIGH:  val = "+"; break;
		case SCR_VALUE_WARNING_LOW:   val = "-"; break;
		case SCR_VALUE_CRITICAL_LOW:  val = "--"; break;
		case SCR_VALUE_REJECT_LOW:    val = "R-"; break;
		}
		if (val != null)
		{
			sb.append(val);
			notOk = true;
		}
		
		String roc = null;
		switch(flags & SCR_ROC_RESULT_MASK)
		{
		case SCR_ROC_GOOD: break;
		case SCR_ROC_REJECT_HIGH:   roc = "R^"; break;
		case SCR_ROC_CRITICAL_HIGH: roc = "^^"; break;
		case SCR_ROC_WARNING_HIGH:  roc = "^"; break;
		case SCR_ROC_WARNING_LOW:   roc = "v"; break;
		case SCR_ROC_CRITICAL_LOW:  roc = "vv"; break;
		case SCR_ROC_REJECT_LOW:    roc = "Rv"; break;
		}
		if (roc != null)
		{
			sb.append((notOk ? " " : "") + roc);
			notOk = true;
		}

		if ((flags & SCR_STUCK_SENSOR_DETECTED) != 0)
		{
			sb.append((notOk ? " " : "") + "~");
			notOk = true;
		}
		
		if ((flags & SCR_MISSING_VALUES_EXCEEDED) != 0)
		{
			sb.append((notOk ? " " : "") + "m");
			notOk = true;
		}

		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Accept a string in the form S(conditions) and convert it to an
	 * integer containing corresponding bit representation of the screening
	 * conditions asserted.
	 * @param scr The "data_flag" string stored in HDB.
	 * @return integer containing corresponding bits.
	 */
	public static final int screening2flags(String scr)
	{
		if (scr == null)
			return 0;
		
		int start = scr.indexOf("S(");
		if (start < 0 || start+2 >= scr.length())
			return 0; // No screening definitions here
		
		scr = scr.substring(start+2);
		int end = scr.indexOf(")");
		if (end > 0)
			scr = scr.substring(0, end);
		
		int ret = SCREENED;
		StringTokenizer st = new StringTokenizer(scr);
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			if      (tok.equalsIgnoreCase("R+")) ret |= SCR_VALUE_REJECT_HIGH;
			else if (tok.equalsIgnoreCase("++")) ret |= SCR_VALUE_CRITICAL_HIGH;
			else if (tok.equalsIgnoreCase("+"))  ret |= SCR_VALUE_WARNING_HIGH;
			else if (tok.equalsIgnoreCase("R-")) ret |= SCR_VALUE_REJECT_LOW;
			else if (tok.equalsIgnoreCase("--")) ret |= SCR_VALUE_CRITICAL_LOW;
			else if (tok.equalsIgnoreCase("-"))  ret |= SCR_VALUE_WARNING_LOW;
			
			else if (tok.equalsIgnoreCase("R^")) ret |= SCR_ROC_REJECT_HIGH;
			else if (tok.equalsIgnoreCase("^^")) ret |= SCR_ROC_CRITICAL_HIGH;
			else if (tok.equalsIgnoreCase("^"))  ret |= SCR_ROC_WARNING_HIGH;
			else if (tok.equalsIgnoreCase("Rv")) ret |= SCR_ROC_REJECT_LOW;
			else if (tok.equalsIgnoreCase("vv")) ret |= SCR_ROC_CRITICAL_LOW;
			else if (tok.equalsIgnoreCase("v"))  ret |= SCR_ROC_WARNING_LOW;
			
			else if (tok.equalsIgnoreCase("~"))  ret |= SCR_STUCK_SENSOR_DETECTED;
			else if (tok.equalsIgnoreCase("m"))  ret |= SCR_MISSING_VALUES_EXCEEDED;
		}
		
		return ret;
	}

}
