/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.7  2012/09/18 15:57:06  mmaloney
 * Use EnvExpander on path name.
 *
 * Revision 1.6  2012/08/28 16:42:21  mmaloney
 * Fixed import to be compatible with template.
 *
 * Revision 1.5  2011/05/03 15:17:39  mmaloney
 * Added 'interp' property to select between log and linear.
 * When table file doesn't exist, fail silently. Do not through DbCompException because
 * this leaves FAILED tasklist entries in the queue.
 *
 */
package decodes.tsdb.algo;

import java.util.Date;

import ilex.util.EnvExpander;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;

//AW:IMPORTS
import decodes.comp.LookupTable;
import decodes.comp.RatingTableReader;
import decodes.comp.TabRatingReader;
import decodes.comp.TableBoundsException;
import decodes.comp.ComputationParseException;
import decodes.tsdb.ParmRef;
import decodes.tsdb.algo.AWAlgoType;
import java.io.File;

import java.util.Date;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements rating table computations.
Holds the lookup table & shift values.
Independent (e.g. STAGE) value is called "indep".
Dependent (e.g. FLOW) is called "dep".
<p>Properties include:
<ul>
  <li>tableDir - Directory containing table files</li>
  <li>tableName - Overrides sitename.tab default</li>
</ul>
 */
//AW:JAVADOC_END
public class TabRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
	implements decodes.comp.HasLookupTable
{
//AW:INPUTS
	public double indep;	//AW:TYPECODE=i
	String _inputNames[] = { "indep" };
//AW:INPUTS_END

//AW:LOCALVARS
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
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable dep = new NamedVariable("dep", 0);
	String _outputNames[] = { "dep" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean exceedLowerBound = false;
	public String tableDir = "$DECODES_INSTALL_DIR/tab-files";
	public String tableName = "";
	public boolean exceedUpperBound = false;
	public String tableNameSuffix = ".tab";
	String interp = "log"; // possibilities are log and linear
	public String _propertyNames[] = { "exceedLowerBound", "tableDir", "tableName", 
			"exceedUpperBound", "tableNameSuffix", "interp" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		// Find the name for the input parameter.
		if (tableName.length() == 0)
		{
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
			warning("TabRating no table file '" + p + "'");
			tableReader = null;
		}
		else
		{
			debug3("Constructing Tab reader for '" + p + "'");
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
		//shiftTable = new LookupTable();
		//shiftTable.setLookupType(LookupTable.INTERP_TRUNC);
		//shiftTable.setExceedLowerBound(false);
		//shiftTable.setExceedUpperBound(false);
		try
		{
			tableReader.readRatingTable(this);
		}
		catch(ComputationParseException ex)
		{
			String msg = "Cannot read SIMPLE rating table: " + ex;
			warning(msg);
			throw new DbCompException(msg);
		}
//AW:BEFORE_TIMESLICES_END
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
//AW:TIMESLICE
		if (tableReader == null)
			return;
		try { setOutput(dep, lookupTable.lookup(indep)); }
		catch(TableBoundsException ex)
		{
			warning("Table bounds exceeded on indep value at site "
				+ getSiteName("indep", null) + ", value was " + indep + " at time " 
				+ debugSdf.format(_timeSliceBaseTime) + ", indep units="
				+ this.getParmRef("indep").timeSeries.getUnitsAbbr());
		}
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		lookupTable = null;
		//shiftTable = null;
//AW:AFTER_TIMESLICES_END
	}

	/**
	 * Required method returns a list of all input time series names.
	 */
	public String[] getInputNames()
	{
		return _inputNames;
	}

	/**
	 * Required method returns a list of all output time series names.
	 */
	public String[] getOutputNames()
	{
		return _outputNames;
	}

	/**
	 * Required method returns a list of properties that have meaning to
	 * this algorithm.
	 */
	public String[] getPropertyNames()
	{
		return _propertyNames;
	}
}
