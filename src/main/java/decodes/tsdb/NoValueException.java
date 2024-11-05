package decodes.tsdb;

public class NoValueException extends TsdbException
{
	/**
	 * Constructor.
	 * @param msg explanatory message.
	 */
	public NoValueException(String msg)
	{
		super(msg);
	}
}
