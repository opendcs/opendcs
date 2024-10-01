/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:02  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/31 16:31:23  mjmaloney
*  javadoc
*
*  Revision 1.2  2002/06/03 13:08:27  mjmaloney
*  Fixed DR #49: Scan and Check operations weren't working. The
*  SwitchFormatException was being ignored.
*
*  Revision 1.1  2001/05/06 22:53:03  mike
*  dev
*
*/
package decodes.decoder;

import decodes.db.FormatStatement;

/**
This exception is thrown from DecodesException.execute to cause the
current execution stack to be unwound and a new format statement to
be executed.
*/
public class SwitchFormatException extends DecoderException
{
	FormatStatement newFmt;

	/**
	  Construct the exception.
	  @param newFmt the FormatStatement to switch to.
	*/
	public SwitchFormatException(FormatStatement newFmt)
	{
		super("");
		this.newFmt = newFmt;
	}

	/** @return the FormatStatement to switch to. */
	public FormatStatement getNewFormat()
	{
		return newFmt;
	}

	/** @return String for log messages. */
	public String toString()
	{
		return "SwitchFormatException: switching to format '"
			+ newFmt.label + "'";
	}
}

