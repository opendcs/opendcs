/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.16  2004/08/27 12:23:11  mjmaloney
*  Added javadocs
*
*  Revision 1.15  2002/10/04 13:28:49  mjmaloney
*  *** empty log message ***
*
*  Revision 1.14  2002/08/28 13:11:36  chris
*  SQL database I/O development.
*
*  Revision 1.13  2002/08/26 04:53:46  chris
*  Major SQL Database I/O development.
*
*  Revision 1.12  2001/12/02 22:02:34  mike
*  dev
*
*  Revision 1.11  2001/10/26 20:01:52  mike
*  Implemented PlatformConfig editor.
*
*  Revision 1.10  2001/10/22 01:34:58  mike
*  Added equals() method to several entities, for use by DB editor to detect
*  editing changes.
*
*  Revision 1.9  2001/10/20 14:41:08  mike
*  Work on Config Editor Panel
*
*  Revision 1.8  2001/08/12 17:36:54  mike
*  Slight architecture change for unit converters. The UnitConverterDb objects
*  are now full-fledged DatabaseObjects and not derived from UnitConverter.
*  This necessitated changes to DB parsing code and prepareForExec code.
*
*  Revision 1.7  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.6  2001/04/12 12:30:43  mike
*  dev
*
*  Revision 1.5  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.4  2001/01/20 02:54:00  mike
*  dev
*
*  Revision 1.3  2001/01/12 21:53:38  mike
*  Renamed UnitConverter to UnitConverterDb
*
*  Revision 1.2  2000/12/31 23:12:49  mike
*  dev
*
*  Revision 1.1  2000/12/29 02:42:52  mike
*  Created.
*
*
*/
package decodes.db;

import decodes.sql.DbKey;

/**
This class holds the information about sensors that are specific to a
script. This includes the EU assignment and coefficients.
*/
public class ScriptSensor extends DatabaseObject
{
  	/**
   	* A sequence number for this ScriptSensor.  This corresponds to the
   	* sensorNumber of the ConfigSensor associated, via the DecodesScript
   	* and PlatformConfig, to this ScriptSensor.
   	*/
	public int sensorNumber;

  	/**
   	* The DecodesScript to which this ScriptSensor belongs.  This should
   	* never be null.
   	*/
	public DecodesScript decodesScript;

	/**
	  Unit converter used to convert from raw data to specified units.
	  The from component will always be "raw", the to component will
	  specify the target units.
	*/
    public UnitConverterDb rawConverter;

	/** Executable unit converter after preparation */
	public UnitConverter execConverter;
	
	private DbKey unitConverterId = DbKey.NullKey;


  	/**
   	  Construct with the parent DecodesScript and a sequence number.
	  @param decodesScript the owner of this ScriptSensor
	  @param sensorNumber the sensor number -- must match a ConfigSensor.
   	*/
	public ScriptSensor(DecodesScript decodesScript, int sensorNumber)
	{
		this.sensorNumber = sensorNumber;
		this.decodesScript = decodesScript;
		rawConverter = null;
	}

  	/**
   	  This overrides the DatabaseObject method; this always returns
   	  "ScriptSensor".
	  @return "ScriptSensor"
   	*/
	public String getObjectType() {
        return "ScriptSensor";
    }

  	/**
   	  Returns the name of the ConfigSensor corresponding to this ScriptSensor.
   	  If no ConfigSensor can be found, this returns the empty string.
	  @return name of ConfigSensor or null if no association.
   	*/
	public String getSensorName()
	{
		if (decodesScript == null)
			return "";
		if (decodesScript.platformConfig == null)
			return "";

        ConfigSensor cs =
            decodesScript.platformConfig.getSensor(sensorNumber);
		if (cs == null)
			return "";

        return cs.sensorName;
	}

  	/**
   	  This compares two ScriptSensors.
	  @param ob the other Script Sensor
	  @return true if equal
   	*/
	public boolean equals(Object ob)
	{
		if (!(ob instanceof ScriptSensor))
			return false;
		ScriptSensor cs = (ScriptSensor)ob;
		if (this == cs)
			return true;

		if (sensorNumber != cs.sensorNumber)
			return false;
		if (rawConverter == null)
		{
			if (cs.rawConverter != null)
				return false;
		}
		else if (cs.rawConverter == null)
			return false;
		else if (!rawConverter.equals(cs.rawConverter))
			return false;
		return true;
	}

  	/**
   	* This overrides the DatabaseObject method.
   	* If this object has a rawConverter, and that rawConverter is not
   	* already prepared, then this method will prepare that rawConverter,
   	* and then set execConverter to refer to the execConverter of that
   	* rawConverter.
   	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		if (rawConverter == null)
			execConverter = null;
		else if (!rawConverter.isPrepared())
		{
			rawConverter.prepareForExec();
			execConverter = rawConverter.execConverter;
		}
	}

  	/**
   	  This overrides the DatabaseObject method.
	  @return true if prepared
   	*/
	public boolean isPrepared()
	{
		if (rawConverter == null)
			return true;
		else
			return rawConverter.isPrepared();
	}

  	/**
   	* This overrides the DatabaseObject method.
   	* This does nothing.
   	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

  	/**
   	* This overrides the DatabaseObject method.
   	* This does nothing.  I/O for this class is handled by the DecodesScript
   	* parent.
   	*/
	public void read()
		throws DatabaseException
	{
	}

  	/**
   	* This overrides the DatabaseObject method.
   	* This does nothing.  I/O for this class is handled by the DecodesScript
   	* parent.
   	*/
	public void write()
		throws DatabaseException
	{
	}

	public DbKey getUnitConverterId()
	{
		return unitConverterId;
	}

	public void setUnitConverterId(DbKey unitConverterId)
	{
		this.unitConverterId = unitConverterId;
	}
}

