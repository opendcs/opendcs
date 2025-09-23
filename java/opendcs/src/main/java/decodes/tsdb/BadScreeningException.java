package decodes.tsdb;

public class BadScreeningException 
	extends TsdbException
{
	public BadScreeningException(String msg)
	{
		super(msg);
	}

	public BadScreeningException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
