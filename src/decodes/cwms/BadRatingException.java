package decodes.cwms;

import decodes.tsdb.TsdbException;

public class BadRatingException
	extends TsdbException
{
	public BadRatingException(String msg)
	{
		super(msg);
	}
}
