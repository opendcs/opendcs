/*
*  $Id$
*/
package decodes.db;

import java.util.*;

import decodes.cwms.CwmsConstants;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import ilex.util.HasProperties;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

/**
 This class holds information about a single sensor which is
 part of a PlatformConfig object.
 Information in this object may be shared by multiple platforms.
*/
public class ConfigSensor
	extends DatabaseObject
	implements HasProperties, PropertiesOwner
{
	/**
	* This uniquely identifies this ConfigSensor among all the ConfigSensors
	* that belong to the same PlatformConfig.
	*/
	public int sensorNumber;

	/**
	* This is the sensor's name.  Note that although this is set to the
	* empty string in the constructor, this should be a non-empty string.
	* I.e. the routine(s) that construct this object should set the name
	* to a non-empty string at some later point.
	*/
	public String sensorName;

	/**
	* The recording mode.  This must match one of the EnumValues for
	* the "RecordingMode" Enum.
	* [7/15/2002, cfm:  Note, though, that currently, the default value
	* for this is 'U', for undefined, and that that is not one of the
	* RecordingMode EnumValues.]
	*/
	public char recordingMode;

	/**
	* The duration between successive samples, in seconds.
	*/
	public int recordingInterval;

	/**
	* The time-of-day of the first sample, in seconds since midnight.
	*/
	public int timeOfFirstSample;

	/** The minimum value for a data sample from this sensor.  */
	public double absoluteMin;

	/** The maximum value for a data sample from this sensor.  */
	public double absoluteMax;

	/**
	* The PlatformConfig to which this ConfigSensor belongs.
	* This cannot be null.
	*/
	public PlatformConfig platformConfig;

	/**
	* The EquipmentModel object which further describes the type of
	* equipment that this ConfigSensor is.  This is optional; i.e.
	* its value might be null.
	*/
	public EquipmentModel equipmentModel;

	/**
	* The list of properties of this ConfigSensor.
	* This will never be null.  If there are no properties, this will
	* refer to an empty Properties object.
	*/
	private Properties properties;

	/**
	* The DataTypes. A sensor may have one or more data types defined.
	*/
	private Vector<DataType> dataTypes;
	//public DataType dataType;

	/**
	* USGS statistics code, null means undefined.
	*/
	private String usgsStatCode;

	private PropertySpec[] propSpecs =
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
			"In HDB Consumer, you can set a model ID for the output value.")
	};

	//======================================================================
	// Constructors and Related Methods.

	/**
	  Construct from a PlatformConfig and a sensor number.  The DataType
	  must be set later.
	  @param platformConfig owner of this sensor
	  @param sensorNumber number of this sensor
	*/
	public ConfigSensor(PlatformConfig platformConfig, int sensorNumber)
	{
		this.sensorNumber = sensorNumber;
		sensorName = "";
		recordingMode = Constants.recordingModeUndefined;
		recordingInterval = 3600;
		timeOfFirstSample = 0;
		absoluteMin = Constants.undefinedDouble;
		absoluteMax = Constants.undefinedDouble;

		this.platformConfig = platformConfig;
		equipmentModel = null;
		properties = new Properties();
		dataTypes = new Vector<DataType>();
		usgsStatCode = null;
	}

	/**
	  Adds a datatype to this config sensor.
	  Only one data type of a given standard may be present in a config sensor.
	  If a datatype with the same standard as 'dt' already exists, it will be
	  overwritten.
	  @param dt the data type to add
	*/
	public void addDataType(DataType dt)
	{
		for(Iterator<DataType> it = dataTypes.iterator(); it.hasNext(); )
		{
			DataType edt = it.next();
			if (edt.getStandard().equalsIgnoreCase(dt.getStandard()))
			{
				it.remove();
				break;
			}
		}
		dataTypes.add(dt);
	}

	/**
	  Returns the data type of this sensor belonging to a particular standard.
	  If no datatype with the given standard is defined in this sensor,
	  this method attempts to convert from a datatype from another standard.
	  <p>
	  Example: Request SHEF data type but none defined. EPA 00065 is
	  defined. Converts from EPA 00065 and returns SHEF HG.
	  <p>
	  If the 'standard' argument is null, this method returns the first
	  data type defined for this sensor, or null if none are defined.
	  <p>
	  @param standard the standard of the desired data type.
	  @return DataType object with matching standard or null.
	*/
	public DataType getDataType(String standard)
	{
		for(Iterator<DataType> it = dataTypes.iterator(); it.hasNext(); )
		{
			DataType edt = it.next();
			if (standard == null)
				return edt;
			if (edt.getStandard().equalsIgnoreCase(standard))
				return edt;
		}
		if (dataTypes.size() > 0)
		{
			DataType edt = dataTypes.get(0);
			return edt.findEquivalent(standard);
		}
		else // no data types defined.
			return null;
	}

	/**
	  Equivalent to getDataType(null)
	  @return preferred data type
	*/
	public DataType getDataType() { return getDataType(null); }

	/// Clears all data types defined for this sensor.
	public void clearDataTypes()
	{
		dataTypes.clear();
	}

	/**
	  @return an iterator that can be used to access all defined DataType
	  objects in this sensor.
	*/
	public Iterator<DataType> getDataTypes() { return dataTypes.iterator(); }
	
	public Vector<DataType> getDataTypeVec() { return dataTypes; }

	/** @return number of data types defined. */
	public int getNumDataTypes() { return dataTypes.size(); }

	/**
	  This overrides the DatabaseObject's getObjectType method.
	  This returns "ConfigSensor".
	  @return "ConfigSensor"
	*/
	public String getObjectType() { return "ConfigSensor"; }

	/**
	  This makes a copy of this ConfigSensor.  The copy will owned by the
	  newPc PlatformConfig and will have the sensor number of this original
	  ConfigSensor.  The copy will refer to the same EquipmentModel, if
	  any, and DataType (i.e. the EquipmentModel and the DataType objects
	  to which this refers will not be copied).
	  The properties will be copied.
	  Called from PlatformConfig.copy()
	  @param newPc the PlatformConfig object that will own the copy
	  @return the copy
	*/
	ConfigSensor copy(PlatformConfig newPc)
	{
		ConfigSensor ret = new ConfigSensor(newPc, sensorNumber);
		ret.sensorName = sensorName;
		ret.recordingMode = recordingMode;
		ret.recordingInterval = recordingInterval;
		ret.timeOfFirstSample = timeOfFirstSample;
		ret.absoluteMin = absoluteMin;
		ret.absoluteMax = absoluteMax;

		ret.equipmentModel = equipmentModel;
		ret.properties = (Properties)properties.clone();
		ret.dataTypes = (Vector<DataType>)dataTypes.clone();

		ret.usgsStatCode = usgsStatCode;

		return ret;
	}

	/**
	  Returns true if this ConfigSensor is equal to the argument.
	  Note that two ConfigSensors that belong to different PlatformConfigs
	  can be equal, if all their other members are equal.
	  @param ob the object to compare
	  @return true if objects are equal, else false.
	*/
	public boolean equals(Object ob)
	{
		if (!(ob instanceof ConfigSensor))
			return false;
		ConfigSensor cs = (ConfigSensor)ob;

		if (sensorNumber != cs.sensorNumber
		 || !sensorName.equals(cs.sensorName)
		 || recordingMode != cs.recordingMode
		 || recordingInterval != cs.recordingInterval)
			return false;
		if (timeOfFirstSample != cs.timeOfFirstSample
		 || absoluteMin != cs.absoluteMin
		 || absoluteMax != cs.absoluteMax
		 || !TextUtil.strEqual(usgsStatCode, cs.usgsStatCode)
		 || !PropertiesUtil.propertiesEqual(this.properties, cs.properties))
			return false;

		// All DataTypes must match, but not necessarily in same order.
		if (dataTypes.size() != cs.dataTypes.size())
			return false;
		for(Iterator<DataType> dtit = dataTypes.iterator(); dtit.hasNext(); )
		{
			DataType dt = dtit.next();
			if (!cs.dataTypes.contains(dt))
				return false;
		}

		if (equipmentModel != null)
		{
			if (cs.equipmentModel == null) return false;
			if (!equipmentModel.equals(cs.equipmentModel))
				return false;
		}
		else if (cs.equipmentModel != null)
			return false;

		return true;
	}

	/** Do nothing. */
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	  Overrides the DatabaseObject's method; this always returns true.
	  @return true
	*/
	public boolean isPrepared()
	{
		return true;
	}

	/**
	* Overrides the DatabaseObject's read() method.
	* This does nothing, since I/O for this class is handled by the
	* PlatformConfig.
	*/
	public void read()
		throws DatabaseException
	{
	}

	/**
	* Overrides the DatabaseObject's write() method.
	* This does nothing, since I/O for this class is handled by the
	* PlatformConfig.
	*/
	public void write()
		throws DatabaseException
	{
	}

	/**
	 * @return the USGS Statistics Code, or null if unassigned.
	 */
	public String getUsgsStatCode()
	{
		return usgsStatCode;
	}

	/**
	 * Sets the USGS Statistics Code.
	 * @param code the code.
	 */
	public void setUsgsStatCode(String code)
	{
		usgsStatCode = code;
	}
	
	public Properties getProperties() { return properties; }

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
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
}

