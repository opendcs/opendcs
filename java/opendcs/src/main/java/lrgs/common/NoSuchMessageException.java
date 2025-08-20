
package lrgs.common;

/**
Thrown on attempt to retrieve a message that doesn't exist.
*/
public class NoSuchMessageException extends ArchiveException
{
	public NoSuchMessageException(String msg)
	{
		super(msg, LrgsErrorCode.DARCFILEIO, false);
	}
}
