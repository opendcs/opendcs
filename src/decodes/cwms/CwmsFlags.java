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
package decodes.cwms;

/**
 * This class maps between quality code bit assignments for CWMS
 * and the Computation Engine Var Flags. 
 */
public class CwmsFlags 
{
	/** Computations use the 4 low-order bits, so we leave them alone. */
	public static final int RESERVED_4_COMP               = 0x0000000F;
	public static final int RESERVED_4_VAR                = 0xF0000000;

	// Definitions for use by computations:
	// NOTE: They are NOT the same as the bit-definitions used within the
	// Quality-Code value in the time-series table. Shifting had to be
	// done to protected the reservied bits above.
	public static final int SCREENED_MASK         = 0x00000010;
	public static final int UNSCREENED            = 0x00000000;
	public static final int SCREENED              = 0x00000010;

	public static final int VALIDITY_MASK         = 0x000001E0;
	public static final int VALIDITY_UNKNOWN      = 0x00000000;
	public static final int VALIDITY_OKAY         = 0x00000020;
	public static final int VALIDITY_MISSING      = 0x00000040;
	public static final int VALIDITY_QUESTIONABLE = 0x00000080;
	public static final int VALIDITY_REJECTED     = 0x00000100;
	
	public static final int RANGE_MASK            = 0x00000600;
	public static final int RANGE_NO_RANGE        = 0x00000000;
	public static final int RANGE_RANGE_1         = 0x00000200;
	public static final int RANGE_RANGE_2         = 0x00000400;
	public static final int RANGE_RANGE_3         = 0x00000600;
	
	public static final int DIFFERENT             = 0x00000800;

	public static final int REPLACEMENT_MASK      = 0x00007000;
	public static final int REPLACEMENT_NONE      = 0x00000000;
	public static final int REPLACEMENT_AUTOMATIC = 0x00001000;
	public static final int REPLACEMENT_INTERACTIVE=0x00002000;
	public static final int REPLACEMENT_MANUAL    = 0x00003000;
	public static final int REPLACEMENT_RESTORED  = 0x00004000;
	
	public static final int METHOD_MASK           = 0x00038000;
	public static final int METHOD_NONE           = 0x00008000;
	public static final int METHOD_LIN_INTERP     = 0x00010000;
	public static final int METHOD_EXPLICIT       = 0x00018000;
	public static final int METHOD_MISSING        = 0x00020000;
	public static final int METHOD_GRAPHICAL      = 0x00028000;
	
	
	public static final int TEST_MASK             = 0x07FC0000;
	public static final int TEST_ALL_PASS         = 0x00000000;
	public static final int TEST_ABSOLUTE_VALUE   = 0x00040000;
	public static final int TEST_CONSTANT_VALUE   = 0x00080000;
	public static final int TEST_RATE_OF_CHANGE   = 0x00100000;
	public static final int TEST_RELATIVE_VALUE   = 0x00200000;
	public static final int TEST_DURATION_VALUE   = 0x00400000;
	public static final int TEST_NEG_INCREMENT    = 0x00800000;
	public static final int TEST_SKIP_LIST        = 0x01000000;
	public static final int TEST_USER_DEFINED     = 0x02000000;
	public static final int TEST_DISTRIBUTION     = 0x04000000;
	
	public static final int PROTECTED             = 0x08000000;

	public static final int FLAG_MISSING_OR_REJECTED = (VALIDITY_MISSING|VALIDITY_REJECTED);
	
	
	// The following IS for use in selecting from the Quality Code
	// values in the time series tables:
	// THIS IS AS THE BITS ARE DEFINED IN CWMS
	public static final int QC_MISSING_OR_REJECTED = 0x14;
	
	
	/**
	 * Passed the integer flags value from the timed-variable returns
	 * the integer quality code to be written to CWMS.
	 * @param flag the flag value from the timed variable.
	 * @return single-character validation flag to write to HDB.
	 */
	public static int flag2CwmsQualityCode(int f)
	{
		int r = (f & 0x0003FFF0) >> 4;
		r |=    (f & 0x00FC0000) >> 3;
		r |=    (f & 0x01000000) >> 2;
		r |=    (f & 0x06000000) >> 1;
		if ((f & PROTECTED) != 0) r |= 0x80000000;
		return r;
	}


	/**
	 * Passed the integer CWMS quality code flag, return the
	 * integer flags value for the timed-variable.
	 * @param val single-character validation flag to write to HDB.
	 * @return flag the flag value from the timed variable.
	 */
	public static int cwmsQuality2flag(long cqc)
	{
		int r = (int)((cqc & 0x00003FFF) << 4);
		r |=    (cqc & 0x001F8000) << 3;
		r |=    (cqc & 0x00400000) << 2;
		r |=    (cqc & 0x03000000) << 1;
		if (cqc < 0) r |= PROTECTED;
		return r;
	}

	public static String flags2Display(long flag)
	{
		StringBuilder sb = new StringBuilder();
		if ((flag & SCREENED_MASK) == SCREENED)
		{
			sb.append("S");
			if ((flag & VALIDITY_MISSING) != 0)
				sb.append("M");
			if ((flag & VALIDITY_REJECTED) != 0)
				sb.append("R");
			if ((flag & VALIDITY_QUESTIONABLE) != 0)
				sb.append("Q");
		}
		return sb.toString();
	}
	
	public static void main(String args[])
		throws Exception
	{
		int x = Integer.parseInt(args[0]);
		System.out.println("Entered: " + Integer.toHexString(x));
		System.out.println("cwmsQuality2flag: " + Integer.toHexString(cwmsQuality2flag(x)));
	}

}
