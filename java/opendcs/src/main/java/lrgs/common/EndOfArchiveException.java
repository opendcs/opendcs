package lrgs.common;

import lrgs.common.LrgsErrorCode;

/**
Thrown when a end-of-archive is reached and a request for more data is
received.
*/
public class EndOfArchiveException extends ArchiveException
{
	/**
	 * Constructor.
	 */
	public EndOfArchiveException()
	{
		super("End of Archive", LrgsErrorCode.DMSGTIMEOUT, false);
	}
}
