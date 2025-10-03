/*
*  $Id$
*/
package decodes.util;

import ilex.util.WarningException;

public class BadPdtEntryException extends WarningException
{
	public BadPdtEntryException(String msg)
	{
		super(msg);
	}

	public BadPdtEntryException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
