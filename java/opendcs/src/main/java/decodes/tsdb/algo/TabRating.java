/**
 * $Id$
 * 
 * $Log$
 * Revision 1.2  2015/01/15 19:25:46  mmaloney
 * RC01
 *
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

import decodes.comp.LookupTable;
import decodes.comp.RatingTableReader;
import decodes.comp.TabRatingReader;
import decodes.comp.TableBoundsException;
import decodes.comp.ComputationParseException;
import decodes.tsdb.ParmRef;
import decodes.tsdb.algo.AWAlgoType;
import java.io.File;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

import java.util.Date;

@Algorithm(description = "Implements rating table computations.\n" + 
"Holds the lookup table & shift values.\n" +
"Independent (e.g. STAGE) value is called \"indep\"." +
"Dependent (e.g. FLOW) is called \"dep\"." +
"<p>Properties include:\n" +
"<ul>\n" +
"  <li>tableDir - Directory containing table files</li>\n" +
"  <li>tableName - Overrides sitename.tab default</li>\n" +
"</ul>")
public class TabRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
	implements decodes.comp.HasLookupTable
{
	@Input
	public double indep;	//AW:TYPECODE=i

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
	@Override
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
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
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
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
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		lookupTable = null;
		//shiftTable = null;
	}
}
