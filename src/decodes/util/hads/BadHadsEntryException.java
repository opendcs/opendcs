package decodes.util.hads;

import ilex.util.WarningException;

/**
 * Exception Class used when an error occurs while parsing
 * a USGS Hads flat file.
 *
 */
public class BadHadsEntryException extends WarningException
{
	/** Constructor **/
	public BadHadsEntryException(String msg)
	{
		super(msg);
	}
}