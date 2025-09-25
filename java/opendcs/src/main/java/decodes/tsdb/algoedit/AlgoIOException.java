package decodes.tsdb.algoedit;

import decodes.tsdb.TsdbException;

public class AlgoIOException
	extends TsdbException
{
	public AlgoIOException(String msg)
	{
		super(msg);
	}

	public AlgoIOException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
