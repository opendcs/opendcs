/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.hdb;

import java.util.StringTokenizer;

public class HdbFlags 
{
	/** Computations use the 4 low-order bits, so we leave them alone. */
	public static final int RESERVED_4_COMP               = 0x0000000F;

	// (HDBF = HDB Flag)
	// Integer bits corresponding to the HDB Validation Flag:
	public static final int HDBF_VALIDATION_MASK          = 0x000000F0;
	public static final int HDBF_BLANK_VALIDATION         = 0x00000000;
	public static final int HDBF_NO_VALIDATION            = 0x00000010;
	public static final int HDBF_TEMPORARY                = 0x00000020;
	public static final int HDBF_PROVISIONAL              = 0x00000030;
	public static final int HDBF_APPROVED                 = 0x00000040;
	public static final int HDBF_FAILED_VALIDATION        = 0x00000050;
	public static final int HDBF_VALIDATED                = 0x00000060;

	// Character-codes stored in the HDB VALIDATION Flag
	public static final char HDB_BLANK_VALIDATION         = 0; // null
	public static final char HDB_NO_VALIDATION            = 'Z';
	public static final char HDB_TEMPORARY                = 'T';
	public static final char HDB_PROVISIONAL              = 'P';
	public static final char HDB_APPROVED                 = 'A';
	public static final char HDB_FAILED_VALIDATION        = 'F';
	public static final char HDB_VALIDATED                = 'V';

	public static final int HDBF_DERIVATION_MASK          = 0x0000FF00;

	public static final int HDBF_VALUE_QC_MASK            = 0x00000300;
	public static final int HDBF_VALUE_RANGE_OK           = 0x00000000;
	public static final int HDBF_HIGH_RANGE_EXCEEDED      = 0x00000100;
	public static final int HDBF_LOW_RANGE_EXCEEDED       = 0x00000200;

	public static final int HDBF_RATE_OF_CHANGE_EXCEEDED  = 0x00000400;
	public static final int HDBF_NO_CHANGE_LIMIT_EXCEEDED = 0x00000800;
	public static final int HDBF_MATH_QC_FAILED           = 0x00001000;
	public static final int HDBF_EOP_WINDOW_EXCEEDED      = 0x00002000;
	public static final int HDBF_ESTIMATED                = 0x00004000;
	public static final int HDBF_MISSING_SAMPLES_EXCEEDED = 0x00008000;
	
	// Characters for the String code in HDB DERIVATION Flag
	public static final char HDB_ESTIMATED                = 'E';
	public static final char HDB_HIGH_RANGE_EXCEEDED      = '+';
	public static final char HDB_LOW_RANGE_EXCEEDED       = '-';
	public static final char HDB_EOP_WINDOW_EXCEEDED      = 'w';
	public static final char HDB_MISSING_SAMPLES_EXCEEDED = 'n';
	public static final char HDB_MATH_QC_FAILED           = '|';
	public static final char HDB_RATE_OF_CHANGE_EXCEEDED  = '^';
	public static final char HDB_NO_CHANGE_LIMIT_EXCEEDED = '~';
	
	// The value was successfully screened.
	public static final int SCREENED                      = 0x00010000;

	// Apply this mask and compare to SCR_VALUE_xxx definitions to obtain result
	public static final int SCR_VALUE_MASK         = 0x000E0000;
	public static final int SCR_VALUE_GOOD                = 0x00000000;
	public static final int SCR_VALUE_REJECT_HIGH         = 0x00020000;
	public static final int SCR_VALUE_CRITICAL_HIGH       = 0x00040000;
	public static final int SCR_VALUE_WARNING_HIGH        = 0x00060000;
	public static final int SCR_VALUE_WARNING_LOW         = 0x00080000;
	public static final int SCR_VALUE_CRITICAL_LOW        = 0x000A0000;
	public static final int SCR_VALUE_REJECT_LOW          = 0x000C0000;
	
	// Apply this mask and compare to SCR_ROC_xxx definitions to obtain result
	public static final int SCR_ROC_MASK           = 0x00700000;
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
	
	// Use this bit to indicate whether an 'O' should be written to the HDB Overwrite Flag
	public static final int HDBF_OVERWRITE_FLAG = 0x02000000;
	public static final char HDB_OVERWRITE_FLAG = 'O';
		
	// All screening faults, 0 means value good, no faults detected
	public static final int SCREENING_FAULTS = SCR_VALUE_MASK 
			| SCR_ROC_MASK | SCR_STUCK_SENSOR_DETECTED | SCR_MISSING_VALUES_EXCEEDED;
	// All bits used in screening:
	public static final int SCREENING_MASK = SCREENED | SCREENING_FAULTS; 

	/** Used internally by Var methods, defined in ilex.util.IFlags.java *
	public static final int RESERVED_ILEX_IFLAGS          = 0xF0000000;
	
	/**
	 * Passed the integer flags value from the timed-variable returns
	 * the single-character 'Validation' flag to use when writing to HDB.
	 * @param flag the flag value from the timed variable.
	 * @return single-character validation flag to write to HDB.
	 */
	public static char flag2HdbValidation(int flag)
	{
		flag &= HDBF_VALIDATION_MASK;
		switch(flag)
		{
		case HDBF_BLANK_VALIDATION:  return HDB_BLANK_VALIDATION;
		case HDBF_NO_VALIDATION:     return HDB_NO_VALIDATION;
		case HDBF_TEMPORARY:         return HDB_TEMPORARY;
		case HDBF_PROVISIONAL:       return HDB_PROVISIONAL;
		case HDBF_APPROVED:          return HDB_APPROVED;
		case HDBF_FAILED_VALIDATION: return HDB_FAILED_VALIDATION;
		case HDBF_VALIDATED:         return HDB_VALIDATED;
		}
		return (char)0;
	}

	/**
	 * Passed the integer flags value from the timed-variable returns
	 * the multi-character 'Derivation' flag to use when writing to HDB.
	 * @param flag the flag value from the timed variable.
	 * @return single-character derivation flag to write to HDB.
	 */
	public static String flag2HdbDerivation(int flag)
	{
		StringBuilder sb = new StringBuilder();

		switch(flag & HDBF_VALUE_QC_MASK)
		{
		case HDBF_HIGH_RANGE_EXCEEDED: 
			sb.append(HDB_HIGH_RANGE_EXCEEDED); 
			break;
		case HDBF_LOW_RANGE_EXCEEDED:
			sb.append(HDB_LOW_RANGE_EXCEEDED); 
			break;
		case HDBF_VALUE_RANGE_OK:
			// No character code
			break;
		}

		if ((flag & HDBF_RATE_OF_CHANGE_EXCEEDED) != 0)
			sb.append(HDB_RATE_OF_CHANGE_EXCEEDED); 
		if ((flag & HDBF_NO_CHANGE_LIMIT_EXCEEDED) != 0)
			sb.append(HDB_NO_CHANGE_LIMIT_EXCEEDED); 
		if ((flag & HDBF_MATH_QC_FAILED) != 0)
			sb.append(HDB_MATH_QC_FAILED); 
		if ((flag & HDBF_EOP_WINDOW_EXCEEDED) != 0)
			sb.append(HDB_EOP_WINDOW_EXCEEDED); 
		if ((flag & HDBF_ESTIMATED) != 0)
			sb.append(HDB_ESTIMATED); 
		if ((flag & HDBF_MISSING_SAMPLES_EXCEEDED) != 0)
			sb.append(HDB_MISSING_SAMPLES_EXCEEDED); 
		if ((flag & HDBF_FAILED_VALIDATION) != 0)
			sb.append(HDB_FAILED_VALIDATION); 
		
		String scr = flags2screeningString(flag);
		if (scr != null && scr.length() > 0)
			sb.append(scr);
		
		String ret = sb.toString();
		return ret.length() > 0 ? ret : null;
	}

	/**
	 * Passed the single-character 'Validation' flag, return the
	 * integer flags value for the timed-variable.
	 * @param val single-character validation flag to write to HDB.
	 * @return flag the flag value from the timed variable.
	 */
	public static int hdbValidation2flag(char val)
	{
		switch(val)
		{
		case HDB_BLANK_VALIDATION:  return HDBF_BLANK_VALIDATION;
		case HDB_NO_VALIDATION:     return HDBF_NO_VALIDATION;
		case HDB_TEMPORARY:         return HDBF_TEMPORARY;
		case HDB_PROVISIONAL:       return HDBF_PROVISIONAL;
		case HDB_APPROVED:          return HDBF_APPROVED;
		case HDB_FAILED_VALIDATION: return HDBF_FAILED_VALIDATION;
		case HDB_VALIDATED:         return HDBF_VALIDATED;
		}
		return 0;
	}

	/**
	 * Passed the multi-character 'Derivation' flag from HDB, returns
	 * integer flags value for the timed-variable.
	 * @param der single-character derivation flag to write to HDB.
	 * @return flag the flag value from the timed variable.
	 */
	public static int hdbDerivation2flag(String der)
	{
		int ret = 0;
		if (der == null)
			return ret;
		for(int i=0; i<der.length(); i++)
			switch(der.charAt(i))
			{
			case HDB_HIGH_RANGE_EXCEEDED:
				ret |= HDBF_HIGH_RANGE_EXCEEDED;
				break;
			case HDB_LOW_RANGE_EXCEEDED:
				ret |= HDBF_LOW_RANGE_EXCEEDED;
				break;
			case HDB_RATE_OF_CHANGE_EXCEEDED:
				ret |= HDBF_RATE_OF_CHANGE_EXCEEDED;
				break;
			case HDB_NO_CHANGE_LIMIT_EXCEEDED:
				ret |= HDBF_NO_CHANGE_LIMIT_EXCEEDED;
				break;
			case HDB_MATH_QC_FAILED:
				ret |= HDBF_MATH_QC_FAILED;
				break;
			case HDB_EOP_WINDOW_EXCEEDED:
				ret |= HDBF_EOP_WINDOW_EXCEEDED;
				break;
			case HDB_ESTIMATED:
				ret |= HDBF_ESTIMATED;
				break;
			case HDB_MISSING_SAMPLES_EXCEEDED:
				ret |= HDBF_MISSING_SAMPLES_EXCEEDED;
				break;
			case HDB_FAILED_VALIDATION:
				ret |= HDBF_FAILED_VALIDATION;
				break;
			}
		
		ret |= screening2flags(der);
		
		return ret;
	}
	
	public static final boolean isGoodQuality(int f)
	{
		return !isRejected(f) && !isQuestionable(f);
	}

	public static final boolean isRejected(int f)
	{
		if ((f & HDBF_VALIDATION_MASK) == HDBF_FAILED_VALIDATION)
			return true;
		
		if ((f & SCREENED) != 0)
		{
			if ((f & SCR_VALUE_MASK) == SCR_VALUE_REJECT_HIGH
			 || (f & SCR_VALUE_MASK) == SCR_VALUE_REJECT_LOW
			 || (f & SCR_ROC_MASK) == SCR_ROC_REJECT_HIGH
			 || (f & SCR_ROC_MASK) == SCR_ROC_REJECT_LOW)
				return true;
		}
		
		return false;
	}

	public static final boolean isQuestionable(int f)
	{
		return (f & HDBF_VALUE_QC_MASK) != 0;
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
		switch(flags & SCR_VALUE_MASK)
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
		switch(flags & SCR_ROC_MASK)
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
	 * <p/>
	 * This method can be used for individual condition codes or for the entire coded string
	 * caontained within S(...)
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
	
	/**
	 * Convert an integer containing bit flags into a string containing a full
	 * explanation of the flags.
	 * @param flags integer flag word
	 * @return string explaining screening conditions
	 */
	public static String flags2explanation(int flags)
	{
		if ((flags & SCREENED) == 0 || (flags&SCREENING_FAULTS) == 0)
			return "";
		
		StringBuilder sb = new StringBuilder();
		switch(flags & SCR_VALUE_MASK)
		{
		case SCR_VALUE_GOOD: break;
		case SCR_VALUE_REJECT_HIGH:   sb.append("VALUE_REJECT_HIGH "); break;
		case SCR_VALUE_CRITICAL_HIGH: sb.append("VALUE_CRITICAL_HIGH "); break;
		case SCR_VALUE_WARNING_HIGH:  sb.append("VALUE_WARNING_HIGH "); break;
		case SCR_VALUE_WARNING_LOW:   sb.append("VALUE_WARNING_LOW "); break;
		case SCR_VALUE_CRITICAL_LOW:  sb.append("VALUE_CRITICAL_LOW "); break;
		case SCR_VALUE_REJECT_LOW:    sb.append("VALUE_REJECT_LOW "); break;
		}
		
		switch(flags & SCR_ROC_MASK)
		{
		case SCR_ROC_GOOD: break;
		case SCR_ROC_REJECT_HIGH:   sb.append("ROC_REJECT_HIGH "); break;
		case SCR_ROC_CRITICAL_HIGH: sb.append("ROC_CRITICAL_HIGH "); break;
		case SCR_ROC_WARNING_HIGH:  sb.append("ROC_WARNING_HIGH "); break;
		case SCR_ROC_WARNING_LOW:   sb.append("ROC_WARNING_LOW "); break;
		case SCR_ROC_CRITICAL_LOW:  sb.append("ROC_CRITICAL_LOW "); break;
		case SCR_ROC_REJECT_LOW:    sb.append("ROC_REJECT_LOW "); break;
		}

		if ((flags & SCR_STUCK_SENSOR_DETECTED) != 0)
			sb.append("STUCK ");
		
		if ((flags & SCR_MISSING_VALUES_EXCEEDED) != 0)
			sb.append("MISSING ");

		return sb.toString();
	}
	
	/**
	 * Determine if the overwrite flag field in HDB should be written to.
	 * That field contains the character 'O' or is null
	 * @param flag integer flag word
	 * @return boolean
	 */
	public static boolean flag2Overwrite(int flag) 
	{
		return (flag & HDBF_OVERWRITE_FLAG) != 0;		
	}
	

}
