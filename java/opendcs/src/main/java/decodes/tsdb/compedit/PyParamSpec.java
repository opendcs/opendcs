package decodes.tsdb.compedit;

import decodes.tsdb.TimeSeriesIdentifier;

/**
 * For the python gui, this class holds info about a single time series
 * parameter.
 */
class PyParamSpec
{
	boolean isInput = true;
	String role;
	TimeSeriesIdentifier tsid = null;
	Double value = null;
	
	PyParamSpec(boolean isInput, String role)
	{
		this.isInput = isInput;
		this.role = role;
	}
}