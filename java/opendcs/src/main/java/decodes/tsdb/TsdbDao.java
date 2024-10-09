package decodes.tsdb;

/**
 * Parent class for Data Access Objects
 * @author mmaloney - Mike Maloney, Cove Software, LLC.
 */
public abstract class TsdbDao
{
	protected TimeSeriesDb tsdb = null;
	
	protected TsdbDao(TimeSeriesDb tsdb)
	{
		this.tsdb = tsdb;
	}

}
