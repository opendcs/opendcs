/*
*  $Id$
*/
package decodes.decoder;

import java.util.Iterator;

import ilex.util.PropertiesUtil;
import ilex.util.Logger;
import decodes.db.Constants;
import decodes.db.ConfigSensor;
import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.db.ScriptSensor;
import decodes.db.PlatformSensor;
import decodes.db.Platform;
import decodes.db.DataType;
import decodes.db.Site;

/**
This class wraps the information from config, platform, and script sensors
into a single object for use by the decoder.
*/
public class Sensor
{
	/** The ConfigSensor */
	public ConfigSensor configSensor;
	/** The ScriptSensor */
	public ScriptSensor scriptSensor;
	/** The PlatformSensor */
	public PlatformSensor platformSensor;
	/** The Platform that owns this sensor. */
	private Platform platform;

	private int _tofs;
	private boolean isPrepared = false;
	private Season ignoreSeason = null;
	private Season processSeason = null;

	/**
	  Constructor.
	  @param  cfg the ConfigSensor object 
	  @param  scr the ScriptSensor object
	  @param  plat the PlatformSensor object
	*/
	public Sensor(ConfigSensor cfg, ScriptSensor scr, PlatformSensor plat,
		Platform platform)
	{
		configSensor = cfg;
		scriptSensor = scr;
		platformSensor = plat;
		this.platform = platform;
		_tofs = -1;
	}

	/** @return the sensor number */
	public int getNumber() { return configSensor.sensorNumber; }

	/**
	  Finds a property associated with this sensor.
	  The name is not case-sensitive.
	  Searches for properties in the following order:
	  <ul>
	   <li>Platform Sensor</li>
	   <li>Config Sensor</li>
	  </ul>
	  @return property value or null if property is not found.
	*/
	public String getProperty(String nm)
	{
		String ret;
		if (platformSensor != null)
		{
			ret = platformSensor.getProperty(nm);
			if (ret != null)
				return ret;
		}
		if (configSensor != null)
		{
			ret = PropertiesUtil.getIgnoreCase(configSensor.getProperties(), nm);
			if (ret != null)
				return ret;
		}
		return null;
	}

	/** @return equipment model name. */
	public String equipmentModelName()
	{
		if (configSensor == null)
			return null;
		if (configSensor.equipmentModel == null)
			return null;
		return configSensor.equipmentModel.name;
	}

	/**
	  Use getDataType(String standard) to return data type for specific
	  standard.
	  @return first data type defined for this sensor.
	*/
	public DataType getDataType()
	{
		if (configSensor == null)
			return null;
		return configSensor.getDataType();
	}

	/**
	  @return data type of this sensor for a specific standard,
	  or null if no config sensor or no data type defined.
	*/
	public DataType getDataType(String std)
	{
		return configSensor != null ? configSensor.getDataType(std)
			: null;
	}

	/** @return iterator for all data types defined. */
	public Iterator<DataType> getAllDataTypes()
	{
		return configSensor != null ? configSensor.getDataTypes() : null;
	}

	/** @return the number of data types defined. */
	public int getNumDataTypes()
	{
		return configSensor == null ? 0 : configSensor.getNumDataTypes();
	}

	/** @return sensor name */
	public String getName()
	{
		if (configSensor == null)
			return null;
		return configSensor.sensorName;
	}

	/** @return sensor recording mode */
	public char getRecordingMode()
	{
		if (configSensor == null)
			return Constants.recordingModeUndefined;
		return configSensor.recordingMode;
	}
		
	public int getRecordingInterval()
	{
		if (configSensor == null)
			return 0;
		return configSensor.recordingInterval;
	}

	/** @return time-of-day for first recorded sample for this sensor */
	public int getTimeOfFirstSample()
	{
		if (configSensor == null)
			return 0;
		if (_tofs == -1)
		{
			_tofs = configSensor.timeOfFirstSample;
			if (configSensor.recordingMode == Constants.recordingModeFixed)
				while(_tofs >= configSensor.recordingInterval
				   && _tofs > 0 && configSensor.recordingInterval > 0)
					_tofs -= configSensor.recordingInterval;
		}
		return _tofs;
	}

	/**
	  If this method returns null, use the site name associated with the
	  platform.
	  @return sensor-specific site name if one is defined or null
	  if not. 
	*/
	public String getSensorSiteName()
	{
		if (platformSensor != null && platformSensor.site != null)
			return platformSensor.site.getPreferredName().getNameValue();
		else
			return null;
	}

	/**
	  If this method returns null, use the site name associated with the
	  platform.
	  @return sensor-specific site if one is defined or null if not. 
	*/
	public Site getSensorSite()
	{
		if (platformSensor != null && platformSensor.site != null)
			return platformSensor.site;
		else
			return null;
	}

	/**
	 * @return the site for this sensor, which can be explicitely defined
	 * or inherited from the platform.
	 */
	public Site getSite()
	{
		if (platformSensor != null && platformSensor.site != null)
			return platformSensor.site;
		return platform.getSite();
	}

	public String getDisplayName()
	{
		Site s = getSite();
		StringBuilder sb = new StringBuilder();
		if (s != null)
			sb.append(s.getDisplayName() + ":");
		sb.append(getName());
		return sb.toString();
	}

	/**
	  @return the DBNO stored as a property on the platform sensor,
	  or -1 if there is no platform sensor or no DBNO property.
	*/
	public String getDBNO()
	{
		Site site = getSite();
		if (site == null)
			return null;
		String dbno = site.getUsgsDbno();
		String dn = getProperty("DBNO");
		if ( dn != null )
			dbno = dn;
		return dbno;
	}

	/**
	  Currently this is stored in the ConfigSensor object, or in a "min"
	  property in the PlatformSensor object. The latter takes precidence if
	  both are defined.
	  @return absolute minimum for this sensor, or 'Constants.undefinedDouble'
	  if no minimum is defined.
	*/
	public double getMinimum()
	{
		double ret = configSensor.absoluteMin;
		String s = getProperty("min");
		if (s == null)
			s = getProperty("minimum");
		if (s != null)
		{
			try { ret = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"Cannot retrieve minimum for sensor # " + getNumber()
					+ " (" + getName() + "): invalid number format '"
					+ s + "'");
				return Constants.undefinedDouble;
			}
		}
		return ret;
	}

	/**
	  Currently this is stored in the ConfigSensor object, or in a "min"
	  property in the PlatformSensor object. The latter takes precidence if
	  both are defined.
	  @return absolute maximum for this sensor, or 'Constants.undefinedDouble'
	  if no minimum is defined.
	*/
	public double getMaximum()
	{
		double ret = configSensor.absoluteMax;
		String s = getProperty("max");
		if (s == null)
			s = getProperty("maximum");
		if (s != null)
		{
			try { ret = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"Cannot retrieve maximum for sensor # " + getNumber()
					+ " (" + getName() + "): invalid number format '"
					+ s + "'");
				return Constants.undefinedDouble;
			}
		}
		return ret;
	}

	public String getUsgsStatCode()
	{
		return configSensor.getUsgsStatCode();
	}

	public int getUsgsDdno()
	{
		return platformSensor != null ? platformSensor.getUsgsDdno() : 0;
	}

	public void prepareForExec()
	{
		if (isPrepared)
			return;
		isPrepared = true;
		
		DbEnum seasonEnum = Database.getDb().enumList.getEnum(Constants.enum_Season);
		if (seasonEnum != null)
		{
			String seasonAbbr = getProperty("ignoreSeason");
			if (seasonAbbr != null && seasonAbbr.trim().length() > 0)
			{
				EnumValue ev = seasonEnum.findEnumValue(seasonAbbr);
				if (ev == null)
					Logger.instance().warning("Sensor " + getDisplayName()
						+ " Unknown 'ignoreSeason' property value '" + seasonAbbr + "'");
				else
				{
					try
					{
						ignoreSeason = new Season(ev);
					}
					catch (FieldParseException ex)
					{
						Logger.instance().warning("Sensor " + getDisplayName()
							+ " ignoreSeason: " + ex);
						ignoreSeason = null;
					}
				}
			}
			if ((seasonAbbr = getProperty("processSeason")) != null && seasonAbbr.trim().length() > 0)
			{
				EnumValue ev = seasonEnum.findEnumValue(seasonAbbr);
				if (ev == null)
					Logger.instance().warning("Sensor " + getDisplayName()
						+ " Unknown 'processSeason' property value '" + seasonAbbr + "'");
				else
				{
					try
					{
						processSeason = new Season(ev);
					}
					catch (FieldParseException ex)
					{
						Logger.instance().warning("Sensor " + getDisplayName()
							+ " processSeason: " + ex);
						processSeason = null;
					}
				}
			}
		}
	}

	public Season getIgnoreSeason()
	{
		if (!isPrepared)
			prepareForExec();
		return ignoreSeason;
	}

	public Season getProcessSeason()
	{
		if (!isPrepared)
			prepareForExec();
		return processSeason;
	}

}


