/**
 * @(#) AreaRatingCompResolver.java
 */

package decodes.comp;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import decodes.comp.CompResolver;
import decodes.comp.Computation;
import decodes.db.RoutingSpec;
import decodes.util.PropertySpec;
import decodes.util.SensorPropertiesOwner;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

/**
* Tries to find Area Files for computations to be applied to the passed 
* message.
*/
public class AreaRatingCompResolver 
	extends CompResolver
	implements SensorPropertiesOwner
{
	/**
	 * These properties can be set on Platform or Config sensors.
	 */
	private PropertySpec sensorProperties[] = 
	{
		new PropertySpec("AreaFile", PropertySpec.FILENAME, 
			"Name of area rating file. The presence of this property " +
			"on the independent (stage) senosr triggers the execution " +
			"of an Area Rating."),
		new PropertySpec("DischargeShef", PropertySpec.STRING,
			"For an Area Rating, this will be the SHEF code of the output " +
			"(discharge) sensor. Set this property on the independent (stage) " +
			"sensor."),
		new PropertySpec("DischargeName", PropertySpec.STRING,
			"For an Area Rating, this will be the name code of the output " +
			"(discharge) sensor. Set this property on the independent (stage) " +
			"sensor."),
		new PropertySpec("DischargeEU", PropertySpec.STRING,
			"For an Area Rating, this will be the engineering units of the output " +
			"(discharge) sensor. Set this property on the independent (stage) " +
			"sensor."),
		new PropertySpec("velocitySensor", PropertySpec.STRING,
			"For an Area Rating, this will be the name of the independent " +
			"velocity sensor, which must be present in the same message. " +
			"Set this property on the independent (stage) sensor."),
	};

	/**
	* The directory containing Area files. PlatformSensor properties need
	* not contain the entire path.
	* If a 'dir' properties is not supplied, assume current directory.
	*/
	private File dir;

	private String module = "AreaRatingCompResolver";
	
	/**
	* This class looks for parameters that can be processed with a
	* USGS Area Rating File.
	*/
	public AreaRatingCompResolver( )
	{
		super();
	}
	
	/**
	* Resolves Area Rating Computations that can be done on this message.
	* The current algorithm simply looks for PlatformSensors that contain
	* a property called "AreaFile" and value is a file name. If a message
	* has such a sensor, attempt to open the named Area file, construct a
	* new AreaComputation object with a AreaRatingReader helper,
	* and initialize it with the file name.
	* @param msg the data collection
	*/
	public Computation[] resolve( IDataCollection msg )
	{
		AreaComputation rc = null;
		Vector<AreaComputation> v = new Vector<AreaComputation>();
		String velocitySensor = null;
		for(Iterator<ITimeSeries> it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			ITimeSeries ts = it.next();
			String areafn = ts.getProperty("AreaFile");
			if (areafn != null)
			{	//This is the HG sensor - which contains AreaFile and 
				//AreaShef properties
				Logger.instance().debug1(module + " Found AreaFile property='" 
					+ areafn + "'");
				File areaf = new File(dir, areafn);
				AreaRatingReader arr = new AreaRatingReader(areaf.getPath());

				rc = new AreaComputation(arr);
				String sh = ts.getProperty("DischargeShef");
				if (sh != null)
					rc.setProperty("DepShefCode", sh);
				sh = ts.getProperty("DischargeName");
				if (sh != null)
					rc.setProperty("DepName", sh);
				sh = ts.getProperty("DischargeEU");
				if (sh != null)
					rc.setProperty("DepUnits", sh);
				velocitySensor = ts.getProperty("velocitySensor");
			
				rc.setIndepSensorNum(ts.getSensorId());
				rc.setApplyShifts(false);
				rc.setDepSensorNum(findFreeSensorNum(msg));
				try 
				{
					rc.read();
					v.add(rc);
				}
				catch(ComputationParseException ex)
				{
					Logger.instance().warning("Cannot read '" + areafn
						+ "': " + ex);
				}
			}
		}
		// No sensor have an "AreaFile property
		if (v.size() == 0)
			return null;
		else
		{
			//Find the Mean Velocity sensor "XV" should be WV - water velocity
			boolean foundXVSensor = false;
			if (velocitySensor != null)
			{
				for(Iterator<ITimeSeries> it = msg.getAllTimeSeries(); it.hasNext(); )
				{
					ITimeSeries ts = it.next();
					//Find the velocity sensor
					String sensorName = ts.getSensorName();
					if (sensorName != null && 
							(sensorName.equalsIgnoreCase(velocitySensor)))
					{
						Logger.instance().debug1(module + 
							" Found velocity sensor " + sensorName);
						rc.setXVSensorNum(ts.getSensorId());
						foundXVSensor = true;
						break;
					}
				}	
			}
			if (foundXVSensor == false)
			{
				Logger.instance().warning(
						"Did not find a velocity sensor. Can not" +
						" perform the velocity rating calculations");
				return null; //The XV Sensor not found - cannot do the velocity
								//ratings calculations
			}
			Computation[] ret = new Computation[v.size()];
			for(int i=0; i<v.size(); i++)
				ret[i] = (Computation)v.get(i);
			return ret;
		}
	}



	/**
	* Initializes the Area Resolver.
	*/
	public void init(RoutingSpec routingSpec )
	{
		// Look for the 'dir' property telling me where Area files are located.
		String d = PropertiesUtil.getIgnoreCase(props, "dir");
		if (d != null)
			dir = new File(EnvExpander.expand(d));
		else
			dir = new File(".");
		Logger.instance().debug1("AreaRatingCompResolver will look in directory '"
			+ dir.getPath() + "'");
	}

	@Override
	public PropertySpec[] getSensorProps()
	{
		return sensorProperties;
	}
}
