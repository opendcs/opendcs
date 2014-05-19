/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2008/08/09 21:50:56  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/11 21:40:59  mjmaloney
*  Improved javadocs
*
*  Revision 1.2  2004/07/01 14:23:59  mjmaloney
*  RDB & Tab working with generic interfaces
*
*  Revision 1.1  2004/06/30 20:01:52  mjmaloney
*  Isolated DECODES interface behind IDataCollection and ITimeSeries interfaces.
*
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
* Tries to find Simple Table Files for computations to be applied to the 
* passed message.
*/
public class TabRatingCompResolver 
	extends CompResolver
	implements SensorPropertiesOwner
{
	/**
	* The directory containing TABLE files. PlatformSensor properties need
	* not contain the entire path.
	* If a 'dir' properties is not supplied, assume current directory.
	*/
	File dir;

	/**
	 * These properties can be set on Platform or Config sensors.
	 */
	private PropertySpec sensorProperties[] = 
	{
		new PropertySpec("TabFile", PropertySpec.FILENAME, 
			"Name of Table Rating file. May include environment settings" +
			" like $HOME, or $DCSTOOL_HOME."),
		new PropertySpec("TabShef", PropertySpec.STRING, 
			"Optional way to set the SHEF code for the dependent variable."),
		new PropertySpec("TabName", PropertySpec.STRING, 
			"Optional way to set the Sensor Name for the dependent variable."),
		new PropertySpec("TabEU", PropertySpec.STRING, 
			"Engineering Units for the dependent variable."),
		new PropertySpec("ExceedBounds", PropertySpec.BOOLEAN, 
			"Set to true to allow interpolation if independent variable " +
			"is above or below the table bounds."),
	};

	/**
	* This class looks for parameters that can be processed with a
	* simple table file containing INDEP, DEP pairs, one per line.
	*/
	public TabRatingCompResolver( )
	{
		super();
	}
	
	/**
	* Resolves simple Table rating Computations that can be done on this 
	* message.
	* Looks for PlatformSensors that contain the following properties 
	* <ul>
	*  <li>TabFile - name of the table file in my directory (required)</li>
	*  <li>TabShef - SHEF code for output parameter (required)</li>
	*  <li>TabEU - Engineering Units for output parameter (required)</li>
	*  <li>ExceedBounds- true/false (default=false), allows lookup to
	*      exceed the upper/lower bound by linear extension.</li>
	* </ul>
	* @param msg the data collection
	*/
	public Computation[] resolve( IDataCollection msg )
	{
		Vector v = new Vector();
		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			ITimeSeries ts = (ITimeSeries)it.next();
			String fn = ts.getProperty("TabFile");
			if (fn != null)
			{
				File f = new File(dir, EnvExpander.expand(fn));
				TabRatingReader rr = new TabRatingReader(f.getPath());
				RatingComputation rc = new RatingComputation(rr);
				String sh = ts.getProperty("TabShef");
				if (sh != null)
					rc.setProperty("DepShefCode", sh);
				sh = ts.getProperty("TabName");
				if (sh != null)
					rc.setProperty("DepName", sh);
				sh = ts.getProperty("TabEU");
				if (sh != null)
					rc.setProperty("DepUnits", sh);
				sh = ts.getProperty("ExceedBounds");
				if (sh != null)
				{
					boolean tf = TextUtil.str2boolean(sh);
					rc.setBoundsInterp(tf, tf);
				}
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
					Logger.instance().warning("Cannot read '" + fn
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



	/**
	* Initializes the RDB Resolver.
	*/
	public void init(RoutingSpec routingSpec )
	{
		// Look for the 'dir' property telling me where RDB files are located.
		String d = PropertiesUtil.getIgnoreCase(props, "dir");
		if (d != null)
			dir = new File(EnvExpander.expand(d));
		else
			dir = new File(".");
	}

	@Override
	public PropertySpec[] getSensorProps()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
