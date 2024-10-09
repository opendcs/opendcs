package decodes.tsdb;

/**
* Cannot perform a requested operation because one or more of the inputs
* are outside the required range.
*/
public class RangeException extends TsdbException
{
	/**
	 * Constructor.
	 * @param msg explanatory message.
	 */
	public RangeException(String msg)
	{
		super(msg);
	}

	/**
	 * Constructor with message and cause.
	 * @param msg
	 * @param cause
	 */
	public RangeException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
