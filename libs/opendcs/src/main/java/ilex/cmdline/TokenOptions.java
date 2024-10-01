/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:16  mjmaloney
*  Javadocs
*
*  Revision 1.1  1999/09/30 18:16:41  mike
*  9/30/1999
*
*
*/

package ilex.cmdline;

/**
Constant values to be used in the options argument for Token constructors.
*/
public class TokenOptions
{
	/** At least one value for this token is required. */
	public static final int optRequired     = 0x0001;

	/** This is an argument (no minus sign) that appears after all options. */
	public static final int optArgument     = 0x0002;

	/** Multiple values for this token are allowed. */
	public static final int optMultiple     = 0x0004;

	/** ?? */
	public static final int optAlreadyUsed  = 0x0008;

	/** This is a switch, as opposed to an argument */
	public static final int optSwitch       = 0x0010;
}
