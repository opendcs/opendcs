package opendcs.opentsdb;

import java.util.Properties;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

/**
 * Set from TSDB_PROPERTIES
 * @author mmaloney Mike Maloney, Cove Software LLC
 *
 */
public class OpenTsdbSettings
{
	private static OpenTsdbSettings _instance = null;
	
	/** Allow the UTC Offset of time series to vary by an hour over DST change. */
	public boolean allowDstOffsetVariation = true;
	
	public OffsetErrorAction offsetErrorActionEnum = OffsetErrorAction.ROUND;

	/** Determines how time series with an offset error are handled. */
	public String offsetErrorAction = OffsetErrorAction.ROUND.name();
	
	/** Name of presentation group that determines storage units */
	public String storagePresentationGroup = "CWMS-English";
	
	public void setFromProperties(Properties props)
	{
		PropertiesUtil.loadFromProps(this, props);
		
		try
		{
			offsetErrorActionEnum = OffsetErrorAction.valueOf(offsetErrorAction);
		}
		catch(IllegalArgumentException ex)
		{
			Logger.instance().warning("OpenTsdbSettings: Bad offsetErrorAction '" 
				+ offsetErrorAction + "': defaulting to round.");
			offsetErrorActionEnum = OffsetErrorAction.ROUND;
		}
	}
	
	public static OpenTsdbSettings instance()
	{
		if (_instance == null)
			_instance = new OpenTsdbSettings();
		return _instance;
	}
}
