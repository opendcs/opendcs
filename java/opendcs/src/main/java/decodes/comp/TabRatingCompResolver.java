/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.comp;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;

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
public class TabRatingCompResolver extends CompResolver implements SensorPropertiesOwner
{
	private static final org.slf4j.Logger log = OpenDcsLoggerFactory.getLogger();
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
		Vector<Computation> v = new Vector<Computation>();
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
				if (sh == null)
					sh = ts.getProperty("DepShefCode");
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

				int depSensorNumber = -1;
				sh = ts.getProperty("depSensorNumber");
				if (sh != null)
				{
					try
					{
						depSensorNumber = Integer.parseInt(sh);
					}
					catch(NumberFormatException ex)
					{
						String mediumId = this.getPlatformContext(msg);
						log.atWarn()
						   .setCause(ex)
						   .log("Platform {} RDB Rating computation for sensor "
							  + "{} has invalid 'depSensorNumber' property '{}'"
							  + " -- ignoring computation.", mediumId, ts.getSensorId(), sh);
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
					log.atWarn()
					   .setCause(ex)
					   .log("Cannot read '{}'", fn);
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
