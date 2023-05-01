/**
 * @(#) RdbRatingCompResolver.java
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
import ilex.util.TextUtil;

/**
* Tries to find RDB Files for computations to be applied to the passed message.
*/
public class RdbRatingCompResolver 
	extends CompResolver
	implements SensorPropertiesOwner
{
	/**
	* The directory containing RDB files. PlatformSensor properties need
	* not contain the entire path.
	* If a 'dir' properties is not supplied, assume current directory.
	*/
	File dir;

	/**
	 * These properties can be set on Platform or Config sensors.
	 */
	private PropertySpec sensorProperties[] = 
	{
		new PropertySpec("RdbFile", PropertySpec.FILENAME, 
			"Name of RDB Rating file. May include environment settings" +
			" like $HOME, or $DCSTOOL_HOME."),
		new PropertySpec("RdbShef", PropertySpec.STRING, 
			"Optional way to set the SHEF code for the dependent variable."),
		new PropertySpec("ExceedBounds", PropertySpec.BOOLEAN, 
			"Set to true to allow interpolation if independent variable " +
			"is above or below the table bounds."),
		new PropertySpec("depSensorNumber", PropertySpec.BOOLEAN, 
			"Set to the sensor number for the dependent param. If this is not set,"
			+ "DECODES will create a new sensor with the SHEF code assigned in 'RdbShef'."),
	};
	
	/**
	* This class looks for parameters that can be processed with a
	* USGS RDB Rating File.
	*/
	public RdbRatingCompResolver( )
	{
		super();
	}
	
	/**
	* Resolves RDB Rating Computations that can be done on this message.
	* The current algorithm simply looks for PlatformSensors that contain
	* a property called "RdbFile" and value is a file name. If a message
	* has such a sensor, attempt to open the named RDB file, construct a
	* new RatingComputation object with a RdbRatingReader helper,
	* Property: ExceedBounds- true/false (default=false), allows lookup to
	*      exceed the upper/lower bound by linear extension.
	* and initialize it with the file name.
	* @param msg the data collection
	*/
	@Override
	public Computation[] resolve( IDataCollection msg )
	{
		Vector<Computation> v = new Vector<Computation>();
		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			ITimeSeries ts = (ITimeSeries)it.next();
			String rdbfn = ts.getProperty("RdbFile");
			if (rdbfn != null)
			{
				Logger.instance().debug1("Found RdbFile property='" + rdbfn + "'");
				File rdbf = new File(dir, EnvExpander.expand(rdbfn));
				RdbRatingReader rrr = new RdbRatingReader(rdbf.getPath());
				RatingComputation rc = new RatingComputation(rrr);
				String sh = ts.getProperty("RdbShef");
				if (sh == null)
					sh = ts.getProperty("DepShefCode");
				if (sh != null)
					rc.setProperty("DepShefCode", sh);
				sh = ts.getProperty("ExceedBounds");
				if (sh != null)
				{
					boolean tf = TextUtil.str2boolean(sh);
					rc.setBoundsInterp(tf, tf);
				}
				rc.setIndepSensorNum(ts.getSensorId());
				rc.setApplyShifts(false);
				
				int depSensorNumber = -1;
				sh = ts.getProperty("depSensorNumber");
				if (sh != null)
				{
					String mediumId = this.getPlatformContext(msg);
					try
					{
						depSensorNumber = Integer.parseInt(sh);
					}
					catch(NumberFormatException ex)
					{
						Logger.instance().warning("Platform " + mediumId + " RDB Rating computation for sensor "
							+ ts.getSensorId() + " has invalid 'depSensorNumber' property '" + sh 
							+ "' -- ignoring computation.");
						return null;
					}
				}
				else
					depSensorNumber = findFreeSensorNum(msg);
				rc.setDepSensorNum(depSensorNumber);

				try 
				{
					rc.read();
					v.add(rc);
				}
				catch(ComputationParseException ex)
				{
					Logger.instance().warning("Cannot read '" + rdbfn
						+ "': " + ex);
				}
			}
		}
		if (v.size() == 0)
			return null;
		else
		{
			Computation[] ret = new Computation[v.size()];
			for(int i=0; i<v.size(); i++)
				ret[i] = (Computation)v.get(i);
			return ret;
		}
	}

	@Override
	public void init(RoutingSpec routingSpec )
	{
		// Look for the 'dir' property telling me where RDB files are located.
		String d = PropertiesUtil.getIgnoreCase(props, "dir");
		if (d != null)
			dir = new File(EnvExpander.expand(d));
		else
			dir = new File(".");
		Logger.instance().debug1("RdbRatingCompResolver will look in directory '"
			+ dir.getPath() + "'");
	}

	@Override
	public PropertySpec[] getSensorProps()
	{
		return sensorProperties;
	}
}
