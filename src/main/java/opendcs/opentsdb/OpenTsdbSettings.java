package opendcs.opentsdb;

import java.util.Properties;

import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

/**
 * Set from TSDB_PROPERTIES
 * @author mmaloney Mike Maloney, Cove Software LLC
 *
 */
public class OpenTsdbSettings
	implements PropertiesOwner
{
	private static OpenTsdbSettings _instance = null;
	
	/** Allow the UTC Offset of time series to vary by an hour over DST change. */
	public boolean allowDstOffsetVariation = true;
	
	public OffsetErrorAction offsetErrorActionEnum = OffsetErrorAction.ROUND;

	/** Determines how time series with an offset error are handled. */
	public String offsetErrorAction = OffsetErrorAction.ROUND.name();
	
	/** Name of presentation group that determines storage units */
	public String storagePresentationGroup = "CWMS-English";
	
	private static PropertySpec propSpecs[] =
	{
		new PropertySpec("allowDstOffsetVariation", PropertySpec.BOOLEAN,
			"(default=true) allows daylight time offset variation in time series data."),
		new PropertySpec("offsetErrorAction", PropertySpec.JAVA_ENUM+"opendcs.opentsdb.OffsetErrorAction",
			"Action when UTC Offset is detected when storing data. One of IGNORE, REJECT, ROUND."),
		new PropertySpec("storagePresentationGroup", PropertySpec.STRING,
			"Name of presentation group that determines storage units for each data type."),
	};
	
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
	
	public Properties getPropertiesSet()
	{
		Properties ret = new Properties();
		ret.setProperty("allowDstOffsetVariation", "" + allowDstOffsetVariation);
		ret.setProperty("offsetErrorAction", offsetErrorAction);
		ret.setProperty("storagePresentationGroup", storagePresentationGroup);
		return ret;
	}
	
	public static OpenTsdbSettings instance()
	{
		if (_instance == null)
			_instance = new OpenTsdbSettings();
		return _instance;
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return false;
	}
}
