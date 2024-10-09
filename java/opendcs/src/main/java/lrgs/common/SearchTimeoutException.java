package lrgs.common;

import lrgs.common.LrgsErrorCode;

/**
Thrown when a search timed-out, meaning the designated wait time has
elapsed and no messages were retrieved.
*/
public class SearchTimeoutException extends ArchiveException
{
	/**
	 * Constructor.
	 * @param msg the message
	 */
	public SearchTimeoutException(String msg)
	{
		super(msg, LrgsErrorCode.DMSGTIMEOUT, false);
	}
}
