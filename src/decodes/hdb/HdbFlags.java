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
		
		return ret;
	}
	
	public static final boolean isGoodQuality(int f)
	{
		return !isRejected(f) && !isQuestionable(f);
	}

	public static final boolean isRejected(int f)
	{
		return (f & HDBF_VALIDATION_MASK) == HDBF_FAILED_VALIDATION;
	}

	public static final boolean isQuestionable(int f)
	{
		return (f & HDBF_VALUE_QC_MASK) != 0;
	}

}
