package decodes.comp;

import java.util.TimeZone;

import ilex.util.ArrayUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.db.RoutingSpec;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

/**
 * This module filters all data around the current time. Use it for a real-time
 * routing spec to enforce reasonableness in the times.
 * 
 * To enable this module, check "Enable In-Line Computations" in the routing
 * spec and add the property "TimeRangeFilterEnable" set to true.
 * Properties that can be set in RoutingSpec:
 * <ul>
 *   <li>TimeRangeFilterEnable - set to true to enable this module</li>
 *   <li>MaxFutureMinutes - (default=6) discard data farther than this in the future</li>
 *   <li>MaxAgeHours - (default=48) discard data older than this</li>
 * </ul>
 */
public class TimeRangeFilterCompResolver 
	extends CompResolver
	implements PropertiesOwner
{
	private String module = "TimeRangeFilterCompResolver";
	private boolean isEnabled = false;
	private TimeRangeFilter timeRangeFilter = null;
	
	private PropertySpec propSpecs[] = 
	{
		new PropertySpec("TimeRangeFilterEnable", PropertySpec.BOOLEAN, 
			"Set to true to enable the Time Range Filter module"),
		new PropertySpec("MaxFutureMinutes", PropertySpec.INT, 
			"(default=6) discard data farther than this in the future"),
		new PropertySpec("MaxAgeHours", PropertySpec.INT, 
			"(default=48) discard data older than this")
	};

	/**
	 * Resolves and returns an array based on the provided data collection.
	 * If the time range filter is not initialized, it returns an empty array.
	 *
	 * @param msg the data collection to which the filter will be applied.
	 * @return an array of Computation objects, or an empty array if no filter is enabled.
	 */
	@Override
	public Computation[] resolve(IDataCollection msg)
	{
		if (timeRangeFilter == null)
			return new Computation[0];
		return new Computation[] { timeRangeFilter };
	}


	/**
	 * Initializes the TimeRangeFilter. It configures the filter to discard
	 * data outside the specified time range.
	 *
	 * @param routingSpec the RoutingSpec from which properties are loaded.
	 */
	@Override
	public void init(RoutingSpec routingSpec)
	{
		PropertiesUtil.copyProps(props, routingSpec.getProperties());
		isEnabled = TextUtil.str2boolean(getProperty("TimeRangeFilterEnable"));
		if (isEnabled)
		{
			int maxFutureMinutes = 6;
			String s = getProperty("MaxFutureMinutes");
			if (s != null)
			{
				try { maxFutureMinutes = Integer.parseInt(s.trim()); }
				catch(Exception ex)
				{
					Logger.instance().warning(module + " bad MaxFutureMinutes "
						+ "property '" + s + "' -- using default of 6.");
					maxFutureMinutes = 6;
				}
			}
			int maxAgeHours = 48;
			s = getProperty("MaxAgeHours");
			if (s != null)
			{
				try { maxAgeHours = Integer.parseInt(s.trim()); }
				catch(Exception ex)
				{
					Logger.instance().warning(module + " bad MaxAgeHours "
						+ "property '" + s + "' -- using default of 48.");
					maxFutureMinutes = 48;
				}
			}
			timeRangeFilter = new TimeRangeFilter(maxFutureMinutes, maxAgeHours);
			timeRangeFilter.setLoggerTz(routingSpec.outputTimeZone);
		}
		else
			timeRangeFilter = null;
	}

	/**
	 * Returns the list of supported property specifications.
	 *
	 * @return an array of PropertySpec objects representing the supported properties.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		PropertySpec[] ret = new PropertySpec[super.getSupportedProps().length + propSpecs.length];
		int i = 0;
		for (PropertySpec ps : super.getSupportedProps())
			ret[i++] = ps;
		for(PropertySpec ps : propSpecs)
			ret[i++] = ps;
		return ret;
	}

}
