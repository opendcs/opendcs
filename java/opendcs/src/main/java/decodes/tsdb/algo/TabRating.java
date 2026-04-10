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
package decodes.tsdb.algo;

import java.util.Date;

import ilex.util.EnvExpander;
import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;

import decodes.comp.LookupTable;
import decodes.comp.TabRatingReader;
import decodes.comp.TableBoundsException;
import decodes.comp.ComputationParseException;
import java.io.File;

import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description = "Implements rating table computations.\n" + 
"Holds the lookup table & shift values.\n" +
"Independent (e.g. STAGE) value is called \"indep\"." +
"Dependent (e.g. FLOW) is called \"dep\"." +
"<p>Properties include:\n" +
"<ul>\n" +
"  <li>tableDir - Directory containing table files</li>\n" +
"  <li>tableName - Overrides sitename.tab default</li>\n" +
"</ul>")
public class TabRating extends decodes.tsdb.algo.AW_AlgorithmBase implements decodes.comp.HasLookupTable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double indep;

	LookupTable lookupTable = null;
	LookupTable shiftTable = null;
	TabRatingReader tableReader = null;
	Date beginTime = null;
	Date endTime = null;
	public void setProperty(String name, String value)
	{
	}
	public void addPoint(double indep, double dep)
	{
		lookupTable.add(indep, dep);
	}
	public void addShift(double indep, double shift)
	{
		//shiftTable.add(indep, shift);
	}
	public void setXOffset(double xo)
	{
		lookupTable.setXOffset(xo);
	}
	public void setBeginTime( Date bt )
	{
		beginTime = bt;
	}
	public void setEndTime( Date et )
	{
		endTime = et;
	}

	public void clearTable()
	{
		lookupTable.clear();
	}

	@Output(type = Double.class)
	public NamedVariable dep = new NamedVariable("dep", 0);

	@PropertySpec(value = "false") 
	public boolean exceedLowerBound = false;
	@PropertySpec(value = "$DECODES_INSTALL_DIR/tab-files") 
	public String tableDir = "$DECODES_INSTALL_DIR/tab-files";
	@PropertySpec(value = "")
	public String tableName = "";
	@PropertySpec(value = "false")
	public boolean exceedUpperBound = false;
	@PropertySpec(value = ".tab")
	public String tableNameSuffix = ".tab";
	@PropertySpec(value = "log")
	String interp = "log"; // possibilities are log and linear
	@PropertySpec(value = "")
	public String nametype = "";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// Find the name for the input parameter.
		if (tableName.length() == 0)
		{
			if (nametype != null && nametype.length() > 0)
				tableName = getSiteName("indep", nametype);
			else // No name type specified, use the siteNameTypePreference from Decodes Settings.
				tableName = getSiteName("indep");
			if (tableName == null)
				throw new DbCompException("No site name for independent "
					+ "variable with SDI=" + getSDI("indep"));
			tableName += tableNameSuffix;
		}
		String p = tableDir + "/" + tableName;
		File f = new File(EnvExpander.expand(p));
		if (!f.exists())
		{
			log.warn("TabRating no table file '{}'", p);
			tableReader = null;
		}
		else
		{
			log.trace("Constructing Tab reader for '{}'", p);
			tableReader = new TabRatingReader(p);
		}

		if (tableReader == null)
			return;
		
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		lookupTable = new LookupTable();
		lookupTable.setExceedLowerBound(exceedLowerBound);
		lookupTable.setExceedUpperBound(exceedUpperBound);
		if (interp.equalsIgnoreCase("linear"))
			lookupTable.setLookupType(LookupTable.INTERP_LINEAR);
		else
			lookupTable.setLookupType(LookupTable.INTERP_LOG);
		try
		{
			tableReader.readRatingTable(this);
		}
		catch(ComputationParseException ex)
		{
			String msg = "Cannot read SIMPLE rating table.";
			throw new DbCompException(msg, ex);
		}
	}

	/**
	 * Do the algorithm for a single time slice.
	 * AW will fill in user-supplied code here.
	 * Base class will set inputs prior to calling this method.
	 * User code should call one of the setOutput methods for a time-slice
	 * output variable.
	 *
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
		if (tableReader == null)
			return;
		try { setOutput(dep, lookupTable.lookup(indep)); }
		catch(TableBoundsException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Table bounds exceeded on indep value at site {}, value was {} at time {}, indep units={}",
					getSiteName("indep", null), indep,
					_timeSliceBaseTime, this.getParmRef("indep").timeSeries.getUnitsAbbr());
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		lookupTable = null;
	}
}
