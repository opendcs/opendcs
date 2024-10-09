package lrgs.common;


/**
Base class for exceptions thrown from a DCP message archive.
*/
public class ArchiveException extends Exception
{
	/** One of the codes defined in LrgsErrorCode */
	protected int errorCode;

	/**
	 * If not recoverable, set 'hangup' to true, meaning that a client would
	 * need to re-initialize this connection before continuing.
	 */
	protected boolean hangup;

	/**
	 * Constructor.
	 * @param msg the error message.
	 */
	public ArchiveException(String msg, int errorCode, boolean hangup)
	{
		super(msg);
		this.errorCode = errorCode;
		this.hangup = hangup;
	}

	public ArchiveException(String msg, int errorCode, boolean hangup, Throwable cause)
	{
		super(msg,cause);
		this.errorCode = errorCode;
		this.hangup = hangup;
	}

	/** @return the error code associated with this exception. */
	public int getErrorCode() { return errorCode; }

	/** @return the hangup flag */
	public boolean getHangup() { return hangup; }

	/** Sets the hangup flag */
	public void setHangup(boolean tf) { hangup = tf; }
}
