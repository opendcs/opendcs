package decodes.db;

import java.util.Vector;

import decodes.datasource.GoesPMParser;
import decodes.datasource.IridiumPMParser;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;
import ilex.util.Logger;
import ilex.util.TextUtil;

/**
Data structure for a Platform Config record.
*/
public class PlatformConfig extends IdDatabaseObject
{
	/**
	* The name of this PlatformConfig.  Note that this is unique among all
	* the PlatformConfigs in this database.  It is often used to uniquely
	* identify this PlatformConfig.
	*/
	public String configName;

	/** A description of this PlatformConfig. */
	public String description;

	/** Vector of ConfigSensor objects */
	private Vector<ConfigSensor> configSensors;

	/** Vector of DecodesScript objects */
	public Vector<DecodesScript> decodesScripts;

	/** EquipmentModel or null if none selected. */
	public EquipmentModel equipmentModel;

	/** Number of platforms using this configuration (not saved in database) */
	public int numPlatformsUsing;

	/** Last time this config was read from database. */
	public Date lastReadTime;

	/** Constructor.  */
	public PlatformConfig()
	{
		super(); // sets _id to Constants.undefinedId;

		configName = "";
		description = "";
		//equipmentId = Constants.undefinedId;
		configSensors = new Vector<ConfigSensor>();
		decodesScripts = new Vector<DecodesScript>();
		equipmentModel = null;
		numPlatformsUsing = 0;
		lastReadTime = new Date(0L);
//System.out.println("Constructed new config.");
	}

	/** 
	  Construct with a name.  
	  @param name the name
	*/
	public PlatformConfig(String name)
	{
		this();
		configName = name;
//System.out.println("Name Constructor for '" + name + "'");
//try { throw new Exception("ctor"); }
//catch(Exception ex)
//{ ex.printStackTrace(System.out); }
	}

	/**
	* Overrides the DatabaseObject's method.  
	  @return "PlatformConfig".
	*/
	public String getObjectType() {
	  return "PlatformConfig";
	}

	/**
	* @param ob the other PlatformConfig
	* @return true if two PlatformConfigs are equal.
	*/
	public boolean equals(Object ob)
	{
		if (ob == null)
			return false;
		if (!(ob instanceof PlatformConfig))
			return false;
		PlatformConfig pc = (PlatformConfig)ob;
		if (this == pc)
			return true;

		if (!configName.equals(pc.configName))
		{
//			Logger.instance().debug3("configNames differ");
			return false;
		}
		if (!TextUtil.strEqual(description, pc.description)
		 || configSensors.size() != pc.configSensors.size()
		 || decodesScripts.size() != pc.decodesScripts.size())
		{
			Logger.instance().debug3("configs differ on desc, # sensors, or #scripts");
			return false;
		}

		for(int i=0; i<configSensors.size(); i++)
		{
			ConfigSensor cs1 = configSensors.elementAt(i);
			ConfigSensor cs2 = pc.configSensors.elementAt(i);

			if (!cs1.equals(cs2))
			{
				Logger.instance().debug3("config sensor " + cs1.sensorNumber
					+ " differs");
				return false;
			}
		}
		for(int i=0; i<decodesScripts.size(); i++)
		{
			DecodesScript ds1 = (DecodesScript)decodesScripts.elementAt(i);
			DecodesScript ds2 = (DecodesScript)pc.decodesScripts.elementAt(i);
			if (!ds1.equals(ds2))
			{
				Logger.instance().debug3("script[" + i + "] differs");
				return false;
			}
		}
	
		return true;
	}

	/**
	* Makes a string containing the config-name suitable for use as
	* a filename.
	  @return String suitable for use as a filename.
	*/
	public String makeFileName()
	{
		StringBuffer ret = new StringBuffer(configName);
		for(int i=0; i<ret.length(); i++)
		{
			char c = ret.charAt(i);
			if (!Character.isLetterOrDigit(ret.charAt(i))
			 && c != '_' && c != '-')
				ret.setCharAt(i, '_');
		}
		return ret.toString();
	}

	/**
	* Returns the sensor for a given sensor number.
	* Note that the sensor number is not necessarily the same as the
	* index into the configSensors Vector that the ConfigSensor is
	* stored in.  If no ConfigSensor is found with a number matching the
	* argument, this returns null.
	  @param sensnum the sensor number
	  @return ConfigSensor or null if no match.
	*/
	public ConfigSensor getSensor(int sensnum)
	{
		int n = configSensors.size();
		for(int i = 0; i < n; i++)
		{
			ConfigSensor cs = configSensors.elementAt(i);
			if (cs.sensorNumber == sensnum)
				return cs;
		}
		return null;
	}

	/**
	* @return an Iterator into the list of ConfigSensors.
	*/
	public Iterator<ConfigSensor> getSensors()
	{
		return configSensors.iterator();
	}

	/**
	* Adds a sensor to this configuration.
	* If a sensor with the same number already exists, it will be replaced.
	  @param newSensor the sensor to add
	*/
	public void addSensor(ConfigSensor newSensor)
	{
		ConfigSensor cs = getSensor(newSensor.sensorNumber);
		if (cs != null)
			configSensors.remove(cs);
		for(int i=0; i<configSensors.size(); i++)
		{
			cs = configSensors.elementAt(i);
			if (newSensor.sensorNumber < cs.sensorNumber)
			{
				configSensors.insertElementAt(newSensor, i);
				return;
			}
		}
		configSensors.add(newSensor);
	}

	/**
	* Removes the ConfigSensor indicated by the argument from this
	* PlatformConfig's list of sensors.
	  @param cs the sensor to remove
	*/
	public void removeSensor(ConfigSensor cs)
	{
		configSensors.remove(cs);
	}

	
	
	/**
	* @return number of sensors in this config.
	*/
	public int getNumSensors()
	{
		return configSensors.size();
	}

	public Vector<ConfigSensor> getSensorVec() { return configSensors; }

	/**
	  @param name the DecodesScript name to get.
	* @return the DecodesScript object with the given name.
	*/
	public DecodesScript getScript(String name)
	{
		for (Iterator<DecodesScript> it = decodesScripts.iterator(); it.hasNext(); )
		{
			DecodesScript ds = it.next();
			if (ds.scriptName.equalsIgnoreCase(name))
				return ds;
		}
		return null;
	}

	/**
	* Add a DecodesScript to this PlatformConfig.
	  @param ds the script to add
	*/
	public void addScript(DecodesScript ds)
	{
		DecodesScript oldDs = getScript(ds.scriptName);
		if (oldDs != null)
			decodesScripts.remove(oldDs);
		decodesScripts.add(ds);
	}

	/**
	* Removes a decodes script from this config, if it existed.
	* The argument must be a reference to the actual object that's in
	* this PlatformConfig's list.
	  @param ds the script to remove
	  @return true if it existed and was removed.
	*/
	public boolean rmScript(DecodesScript ds)
	{
		return decodesScripts.remove(ds);
	}

	/**
	* @return an iterator over this PlatformConfig's DecodesScripts.
	*/
	public Iterator<DecodesScript> getScripts()
	{
		return decodesScripts.iterator();
	}

	/**
	*	@return equimentModel for this configuration
	*/
	public EquipmentModel getEquipmentModel()
	{
		return(equipmentModel);
	}
	
	public String getEquipmentModelName()
	{
		if (equipmentModel == null)
			return null;
		return equipmentModel.getName();
	}
	/**
	* @return the number of DecodesScripts associated with this
	* PlatformConfig.
	*/
	public int getNumScripts()
	{
		return decodesScripts.size();
	}

	/**
	* This overrides the DatabaseObject method.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		if (equipmentModel != null)
			equipmentModel.prepareForExec();

// MJM 2004 07/02
// The scripts are now prepared in DecodesScript.decodeMessage()
// so that unused scripts that may contain errors do not cause exceptions.
// This also is more efficient.
//		for(Iterator it = getScripts(); it.hasNext(); )
//		{
//			DecodesScript ds = (DecodesScript)it.next();
//			ds.prepareForExec();
//		}
		for(Iterator<ConfigSensor> it = configSensors.iterator(); it.hasNext(); )
		{
			ConfigSensor cs = it.next();
			cs.prepareForExec();
		}
	}

	/**
	* This overrides the DatabaseObject method.
	  @return true if this config is prepared
	*/
	public boolean isPrepared()
	{
		if (equipmentModel != null && equipmentModel.isPrepared())
			return false;
		for(Iterator<DecodesScript> it = getScripts(); it.hasNext(); )
		{
			DecodesScript ds = it.next();
			if (!ds.isPrepared())
				return false;
		}
		for(Iterator<ConfigSensor> it = configSensors.iterator(); it.hasNext(); )
		{
			ConfigSensor cs = it.next();
			if (!cs.isPrepared())
				return false;
		}
		return true;
	}

	/**
	* Makes a device config name from its (legacy) component parts.
	* By convention, old DECODES and EMIT named the devices as follows:
	*	deviceID-locationCode-SequenceNum
	  @param dev the device name
	  @param loc a short location abbreviation
	  @param seq the sequence for uniqueness
	  @return String with no embedded blanks of the form dev-loc-seq
	*/
	public static String makeConfigName(String dev, String loc, String seq)
	{
		StringBuffer ret = new StringBuffer(
			dev.trim() + "-" + loc.trim() + "-" + seq.trim());

		// Replace all illegal characters with underscore.
		for(int i=0; i<ret.length(); i++)
		{
			char c = ret.charAt(i);
			if (!Character.isLetterOrDigit(ret.charAt(i))
			 && c != '_' && c != '-')
				ret.setCharAt(i, '_');
		}
		return ret.toString();
	}

	/**
	* This creates a copy of this PlatformConfig.  The ConfigSensors
	* and DecodesScripts belonging to this are also copied, but the
	* EquipmentModel is not [cfm:  I don't know why.]
	  @return deep copy
	*/
	public PlatformConfig copy()
	{
//System.out.println("PlatformConfig copy");
		PlatformConfig ret = new PlatformConfig(configName);
		ret.copyFrom(this);
		try { ret.setId(getId()); }
		catch(DatabaseException ex) {} // won't happen.
		return ret;
	}

	/**
	 * Makes this object look like the passed one, except for the database
	 * and surrogate key fields. Does a deep copy.
	 * @param rhs the object top copy from.
	 */
	public void copyFrom(PlatformConfig rhs)
	{
		//This and input parameter could be the same reference if accessed via cache
		if (rhs == this)
		{
			return;
		}
		this.configName = rhs.configName;
		this.description = rhs.description;
		this.numPlatformsUsing = rhs.numPlatformsUsing;

		this.equipmentModel = rhs.equipmentModel;

		this.configSensors.clear();
		for(ConfigSensor cs : rhs.configSensors)
		{
			this.configSensors.add(cs.copy(this));
		}

		this.decodesScripts.clear();
		for(DecodesScript ds : rhs.decodesScripts)
		{
			this.addScript(ds.copy(this));
		}
	}

	/** 
	  Make a copy without the database ID 
	  @return copy with clear ID
	*/
	public PlatformConfig noIdCopy()
	{
		PlatformConfig ret = copy();
		ret.clearIds();
		return ret;
	}

	/**
	 * Recursively clear all database IDs (surrogate keys) associated
	 * with this object. Used to facilitate copying and importing.
	 */
	public void clearIds()
	{
		clearId();
		for(Iterator<DecodesScript> it = decodesScripts.iterator(); it.hasNext(); )
		{
			DecodesScript ds = it.next();
			ds.clearId();
			for (Iterator<ScriptSensor> ssit = ds.scriptSensors.iterator(); ssit.hasNext(); )
			{
				ScriptSensor ss = ssit.next();
				if (ss.rawConverter != null)
					ss.rawConverter.clearId();
			}
		}
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
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readConfig(this);
		lastReadTime = new Date();
	}

	/**
	* This overrides the DatabaseObject method.
	*/
	public void write()
		throws DatabaseException
	{
		myDatabase.getDbIo().writeConfig(this);
	}

	/** @return the name of this configuration. */
	public String getName()
	{
		return configName;
	}

	public String getDisplayName()
	{
		return configName;
	}
	
	/**
	  * Removes all sensors from this platformconfig	  
	*/
	public void removeAllSensors()
	{
		configSensors.removeAllElements();
	}

	/**
      Called when the config sensors have changed.  Makes sure that there is
      exactly one ScriptSensor for each config sensor, and no extras.
    */
    public void validateDecodingScriptSensors()
    {
        int nsensors = this.getNumSensors();
        int sensnums[] = new int[nsensors];

        for(Iterator<DecodesScript> dsit = this.decodesScripts.iterator(); dsit.hasNext(); )
        {
            DecodesScript ds = dsit.next();

            // build an array of all valid sensor numbers.
            int i = 0;
            for(Iterator<ConfigSensor> csit = this.getSensors(); csit.hasNext(); )
            {
                ConfigSensor cs = csit.next();
                sensnums[i++] = cs.sensorNumber;
            }

            for(Iterator<ScriptSensor> ssit = ds.scriptSensors.iterator(); ssit.hasNext(); )
            {
                // Make sure each script sensor still refers to a config sensor.
                ScriptSensor ss = ssit.next();
                for(i=0; i<nsensors; i++)
                    if (ss.sensorNumber == sensnums[i])
                    {
                        sensnums[i] = -1;
                        break;
                    }
                if (i == nsensors) // fell through - invalid sensor number.
                    ssit.remove();
            }

            // Add script sensors for each unrepresented config sensor.
            for(i=0; i < nsensors; i++)
                if (sensnums[i] != -1)
                {
                    ScriptSensor ss = new ScriptSensor(ds, sensnums[i]);
                    ss.rawConverter = new UnitConverterDb("raw", "raw");
                    ss.rawConverter.algorithm = Constants.eucvt_none;
                    ds.addScriptSensor(ss);
                }
        }
    }

	public ConfigSensor findSensorByName(String sensorName)
    {
        for(Iterator<ConfigSensor> csit = this.getSensors(); csit.hasNext();)
        {
            ConfigSensor cs = csit.next();
            if (cs.sensorName.equalsIgnoreCase(sensorName))
                return cs;
        }
        return null;
    }

	public void AddGoesParameters(ArrayList<String> pmNames, int startingSensorNumber) 
	{
		int sensorNum = startingSensorNumber;
	  for(String n : pmNames)
        {
            ConfigSensor cs = findSensorByName(n);
            if (cs != null)
            {
				Logger.instance().log(Logger.E_FAILURE, "There is already a sensor named '" + n + "' -- cannot add.");
                continue;
            }

            cs = new ConfigSensor(this, sensorNum++);
            cs.sensorName = n;
            cs.recordingMode = Constants.recordingModeVariable;
            if (n.equalsIgnoreCase(GoesPMParser.DCP_ADDRESS))
            {
                cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Code-DCPAddress"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YA"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.MESSAGE_LENGTH))
            {
                   cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Length-Message"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YL"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.SIGNAL_STRENGTH))
            {
                   cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Power-Signal"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YS"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.FAILURE_CODE))
            {
                   cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Code-Failure"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YF"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.FREQ_OFFSET))
            {
                   cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Freq-Offset"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YO"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.MOD_INDEX))
            {
                   cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Ratio-ModIndex"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YI"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.CHANNEL))
            {
                cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Code-Channel"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YC"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.SPACECRAFT))
            {
                cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Code-Spacecraft"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YP"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.BAUD))
            {
                   cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Freq-Baud"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YB"));
            }
            else if (n.equalsIgnoreCase(GoesPMParser.DCP_MSG_FLAGS))
            {
                cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Binary-Flags"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YG"));
            }
            else if (n.equalsIgnoreCase(IridiumPMParser.LATITUDE))
            {
                cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Rotation-Latitude"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YA"));
            }
            else if (n.equalsIgnoreCase(IridiumPMParser.LONGITUDE))
            {
                cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Rotation-Longitude"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YO"));
            }
            else if (n.equalsIgnoreCase(IridiumPMParser.CEP_RADIUS))
            {
                cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Rotation-CEPRadius"));
                cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YR"));
            }
            this.addSensor(cs);
        }
	}


}

