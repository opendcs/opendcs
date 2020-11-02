/*
*  $Id$
*/
package decodes.db;

import java.util.Enumeration;
import java.util.Properties;
import java.util.ArrayList;

import decodes.cwms.CwmsConstants;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

import ilex.util.HasProperties;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

/**
 * Platform-specific information about sensors is stored in
 * objects of this class.
 * Each sensor belonging to a particular Platform might have its
 * own Site, and / or it might have its own set of properties.
 */
public class PlatformSensor 
	extends DatabaseObject
	implements HasProperties, PropertiesOwner
{
	/** A unique number for this sensor on this Platform.  */
	public int sensorNumber;

	/**
	 * A list of properties specific to this sensor on this platform.
	 * This is never null.
	 */
	private Properties properties = new Properties();

	/** A reference to this object's parent Platform.  */
	public Platform platform;

	/**
	 * The Site at which this sensor resides.
	 * If this is null, that indicates that this sensor resides at the
	 * same Site as the Platform to which it belongs.
	 */
	public Site site;

	/**
	 * USGS Database Descriptor Number, 0 if undefined.
	 */
	private int usgsDdno;

	/** Flag used by GUIs to coordinate platform & config sensor records. */
	public boolean guiCheck;

	/** USGS list of valid DDNOs for this sensor and param-code. */
	private ArrayList<String> validDdnos;

	private PropertySpec[] platSensorPropSpecs =
	{
		new PropertySpec("dataOrder", PropertySpec.STRING,
			"Used to override default data order in config. Set to 'A' for ascending," +
			" or 'D' for descending."),
		new PropertySpec("omit", PropertySpec.BOOLEAN,
			"Set to true to have this sensor omitted from the DECODES output."),
		new PropertySpec("ignoreSeason", PropertySpec.DECODES_ENUM + Constants.enum_Season,
			"Set to have this sensor ignored during a specified season."),
		new PropertySpec("processSeason", PropertySpec.DECODES_ENUM + Constants.enum_Season,
			"Set to have this sensor only processed during a specified season."),
		new PropertySpec("preoffset", PropertySpec.NUMBER,
			"Equation for DECODES Output: (value + preoffset) * scale + offset"),
		new PropertySpec("scale", PropertySpec.NUMBER,
			"Equation for DECODES Output: (value + preoffset) * scale + offset"),
		new PropertySpec("offset", PropertySpec.NUMBER,
			"Equation for DECODES Output: (value + preoffset) * scale + offset"),
		new PropertySpec("minReplaceValue", PropertySpec.STRING,
			"If below minimum, replace with this value."),
		new PropertySpec("maxReplaceValue", PropertySpec.STRING,
			"If above maximum, replace with this value."),
		new PropertySpec("HydstraTransCode", PropertySpec.INT,
			"Hydstra Translation Code used by the Hydstra Output Formatter."),
		new PropertySpec("HydstraMaxGap", PropertySpec.INT,
			"Maximum gap used by the Hydstra Output Formatter."),
		new PropertySpec("ADAPS_Medium_Type", PropertySpec.DECODES_ENUM + Constants.enum_TMType,
			"Medium Type to use in the USGS StdMsg Output Formatter."),
		new PropertySpec("tvacode", PropertySpec.INT,
			"Data Type Code to use in transaction file in TVA Output Formatter."),
		new PropertySpec(CwmsConstants.CWMS_PARAM_TYPE, PropertySpec.STRING,
			"Param Type to use in Time Series ID in CWMS Consumer"),
		new PropertySpec(CwmsConstants.CWMS_DURATION, PropertySpec.STRING,
			"Duration to use in Time Series ID in CWMS Consumer"),
		new PropertySpec(CwmsConstants.CWMS_VERSION, PropertySpec.STRING,
			"Version to use in Time Series ID in CWMS Consumer"),
		new PropertySpec("interval", PropertySpec.STRING,
			"In HDB Consumer, use this for the INTERVAL part of Time Series ID."),
		new PropertySpec("modeled", PropertySpec.BOOLEAN,
			"In HDB Consumer, set to true if this is considered a modeled value."),
		new PropertySpec("modelId", PropertySpec.INT,
			"In HDB Consumer, you can set a model ID for the output value."),
		new PropertySpec("TimeOffsetSec", PropertySpec.INT,
			"Time adjustment (positive or negative seconds) to add to this sensor's"
			+ " samples after decoding.")
	};

	
	
	/** Constructor.  */
	public PlatformSensor()
	{
		sensorNumber = Constants.undefinedIntKey;
		platform = null;
		site = null;
		usgsDdno = 0;
		guiCheck = false;
		validDdnos = new ArrayList<String>();
	}

	/**
	   Construct with a reference to this object's parent, and a
	   sensor number.
	   @param platform the owning platform
	   @param sensorNumber the sensor number
	 */
	public PlatformSensor(Platform platform, int sensorNumber)
	{
		this();
		this.platform = platform;
		this.sensorNumber = sensorNumber;
	}

	/**
	  This returns true if this PlatformSensor has no sensor-specific
	  information.  That implies that this sensor is at the same Site
	  as the Platform to which it belongs, and that this sensor has no
	  platform-sensor-specific properties.
	  @return true if there are no properties or site.
	*/
	public boolean isEmpty()
	{
		return getProperties().size() == 0 && site == null && usgsDdno <= 0;
	}

	/**
	  This overrides the DatabaseObject's method; this always returns
	  "PlatformSensor".
	  @return "PlatformSensor"
	*/
	public String getObjectType() {
		return "PlatformSensor";
	}

	/**
	 * This returns true if this PlatformSensor can be considered equal to
	 * another.  Two PlatformSensors are equal if their sensor numbers
	 * are equal, the site members are either both null or are equal, and
	 * the list of properties are equal.
	 */
	public boolean equals(Object ob)
	{
		if (!(ob instanceof PlatformSensor))
			return false;
		PlatformSensor ps = (PlatformSensor)ob;
		if (this == ps)
			return true;
		if (sensorNumber != ps.sensorNumber)
			return false;
		if (!PropertiesUtil.propertiesEqual(getProperties(), ps.getProperties()))
		{
			Logger.instance().debug3("Sensor " + sensorNumber
				+ " has different properties, this: "
				+ PropertiesUtil.props2string(getProperties())
				+ ", that: " + PropertiesUtil.props2string(ps.getProperties()));
			return false;
		}
		if (site == null)
		{
			if (ps.site != null)
			{
				Logger.instance().debug3("Sensor " + sensorNumber
					+ " this has null site, that has site="
					+ ps.site.getDisplayName());
				return false;
			}
		}
		else if (ps.site == null)
		{
			Logger.instance().debug3("Sensor " + sensorNumber
				+ " this has site=" + site.getDisplayName()
				+ ", that has null site");
			return false;
		}
		else if (!TextUtil.strEqualIgnoreCase(site.getDisplayName(),
			ps.site.getDisplayName()))
		{
			Logger.instance().debug3("Sensor " + sensorNumber
				+ " this different site. this=" + site.getDisplayName()
				+ ", that=" + ps.site.getDisplayName());
			return false;
		}
		else if (usgsDdno != ps.usgsDdno)
		{
			Logger.instance().debug3("Sensor " + sensorNumber
				+ " has different usgsDdno, this=" + usgsDdno
				+ ", that=" + ps.usgsDdno);
			return false;
		}

		return true;
	}

	/**
	 * This overrides the DatabaseObject method; this does nothing.
	 */
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	 * This overrides the DatabaseObject method; this always returns true.
	 */
	public boolean isPrepared()
	{
		return true;
	}

	/**
	 * This overrides the DatabaseObject method; this does nothing.
	 */
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	 * This overrides the DatabaseObject method.
	 * This does nothing; I/O for this class is handled by the Platform.
	 */
	public void read()
		throws DatabaseException
	{
	}

	/**
	 * This overrides the DatabaseObject method.
	 * This does nothing; I/O for this class is handled by the Platform.
	 */
	public void write()
		throws DatabaseException
	{
	}

	/**
	 * @return the USGS Database Descriptor Number (0 means unassigned).
	 */
	public int getUsgsDdno()
	{
		return usgsDdno;
	}

	/**
	 * Sets the USGS Database Descriptor Number (0 means unassigned).
	 * @param ddno the DDNO.
	 */
	public void setUsgsDdno(int ddno)
	{
		usgsDdno = ddno;
	}
	public void clearValidDdnos()
	{
		validDdnos.clear();
	}
	/**
	 * Called from USGS NWIS interface to set list of valid DDNOs for this
	 * site and param code. This is called when the platform data is read.
	 */
	public void addValidDdno(String ddDesc )
	{
		boolean inList = false;
		for (int i = 0 ; i < validDdnos.size(); i++ ) {
			if ( validDdnos.get(i).equals(ddDesc) )
				 inList = true;
		}
		if ( !inList ) 
			validDdnos.add(new String(ddDesc));
	}

	/**
	 * @return the list of valid DDNO's for this site & param code, or null
	 * if there are no known DDNOs.
	 */
	public String[] getValidDdnos()
	{
		String [] ret = new String[validDdnos.size()];
		for(int i=0; i<ret.length; i++)
			ret[i] = validDdnos.get(i);
		return ret;
	}

	/**
	 * Make a copy of this sensor object, but set platform ref explicitely.
	 * This is called from Platform.copy()
	 * @param newplat the platform reference that will own the new sensor.
	 */
	PlatformSensor copy(Platform newplat)
	{
		PlatformSensor nps = new PlatformSensor(newplat, this.sensorNumber);
		nps.sensorNumber = this.sensorNumber;
		nps.site = this.site;
		PropertiesUtil.copyProps(nps.getProperties(), this.getProperties());
		nps.usgsDdno = this.usgsDdno;
		nps.validDdnos = this.validDdnos;
		return nps;
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return platSensorPropSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return false;
	}

	@Override
	public void setProperty(String name, String value)
	{
		rmProperty(name);
		getProperties().setProperty(name, value);
	}

	@Override
	public String getProperty(String name)
	{
		return PropertiesUtil.getIgnoreCase(getProperties(), name);
	}

	@Override
	public Enumeration getPropertyNames()
	{
		return getProperties().keys();
	}

	@Override
	public void rmProperty(String name)
	{
		PropertiesUtil.rmIgnoreCase(getProperties(), name);
	}

	/**
	 * @return the properties
	 */
	public Properties getProperties()
	{
		return properties;
	}
}

