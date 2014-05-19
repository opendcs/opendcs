/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.15  2013/02/15 17:26:53  mmaloney
*  Added 'failIfNoTable' property.
*
*  Revision 1.14  2012/11/06 16:11:08  mmaloney
*  Added tableProps with getter.
*  properties from the RDB file header are stored here.
*
*  Revision 1.13  2012/10/24 23:15:24  mmaloney
*  Try to build file name with USGS site number first.
*  If that fails, fall back to preferred site name.
*
*  Revision 1.12  2012/09/12 20:53:11  mmaloney
*  Added filePrefix and fileSuffix properties.
*
*  Revision 1.11  2011/05/03 15:22:49  mmaloney
*  Added 'interp' property to select between log and linear.
*  When table file doesn't exist, fail silently. Do not through DbCompException because
*  this leaves FAILED tasklist entries in the queue.
*
*  Revision 1.10  2011/02/15 14:48:13  mmaloney
*  On comp-init, if algorithm throws DbCompException, log a warning but do not put the
*  stack trace to stderr. This signifies an orderly failure, like a Rating unable to find its rating
*  file. Only extraordinary things should get a stack trace in nohup.out.
*
*  Revision 1.9  2010/08/18 14:04:36  gchen
*  *** empty log message ***
*
*  Revision 1.8  2010/08/17 19:11:58  mmaloney
*  Cleaned up imports. Added CVS header.
*
*  Revision 1.7  2010/08/17 18:48:52  mmaloney
*  Fail if a tableReader cannot be built, regardless of the reason.
*
*/
package decodes.tsdb.algo;

import java.util.Date;
import java.io.File;

import ilex.var.NamedVariable;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.var.TimedVariable;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;

//AW:IMPORTS
import decodes.comp.LookupTable;
import decodes.comp.RdbRatingReader;
import decodes.comp.TableBoundsException;
import decodes.comp.ComputationParseException;
import decodes.db.Constants;
import decodes.util.DecodesSettings;

import java.util.Properties;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements rating table computations.
Holds the lookup table & shift values.
Independent (e.g. STAGE) value is called "indep".
Dependent (e.g. FLOW) is called "dep".
<p>Properties include:
<ul>
  <li>applyShifts - true if you want algorithm to apply shifts.
      Usually unnecessary because RDB files are expanded.
  </li>
  <li>tableDir - Directory containing table files.</li>
</ul>
 */
//AW:JAVADOC_END
public class RdbRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
	implements decodes.comp.HasLookupTable
{
//AW:INPUTS
	double indep;	//AW:TYPECODE=i
	String _inputNames[] = { "indep" };
//AW:INPUTS_END

//AW:LOCALVARS
	LookupTable lookupTable = null;
	LookupTable shiftTable = null;
	RdbRatingReader tableReader = null;
	Date beginTime = null;
	Date endTime = null;
	Properties tableProps = new Properties();
	
	public void setProperty(String name, String value)
	{
		tableProps.setProperty(name, value);
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
	public Properties getTableProps() { return tableProps; }

//AW:LOCALVARS_END

//AW:OUTPUTS
	NamedVariable dep = new NamedVariable("dep", 0);
	String _outputNames[] = { "dep" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	boolean exceedLowerBound = false;
	String tableDir = "$DECODES_INSTALL_DIR/rdb";
	boolean exceedUpperBound = false;
	boolean applyShifts = false;
	boolean failIfNoTable = false;
	String interp = "log"; // possibilities are log and linear
	String filePrefix = "";
	String fileSuffix = ".rdb";
	String _propertyNames[] = { "exceedLowerBound", "tableDir", "exceedUpperBound", 
		"applyShifts", "interp", 
		"filePrefix", "fileSuffix", "failIfNoTable" };
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
		// Find the name for the input parameter.
		tableReader = null;
		String siteName = getSiteName("indep", Constants.snt_USGS);
		String tried = "";
		if (siteName != null)
		{
			String fn = tableDir + "/" + filePrefix + siteName + fileSuffix;
			tried = tried + " " + fn;
			File f = new File(EnvExpander.expand(fn));
debug1("trying '" + f.getPath() + "'");
			if (f.exists())
			{
				debug3("Constructing RDB reader for '" + fn + "'");
				tableReader = new RdbRatingReader(fn);
			}
		}
		if (tableReader == null)
		{
			String namePref = DecodesSettings.instance().siteNameTypePreference;
			if (namePref.equalsIgnoreCase(Constants.snt_USGS))
				throw new DbCompException("No usgs site name for independent "
					+ "variable with SDI=" + getSDI("indep"));
			siteName = getSiteName("indep", namePref);
			if (siteName != null)
			{
				String fn = tableDir + "/" + filePrefix + siteName + ".rdb";
				tried = tried + " " + fn;
				File f = new File(EnvExpander.expand(fn));
debug1("trying '" + f.getPath() + "'");
				if (f.exists())
				{
					debug3("Constructing RDB reader for '" + fn + "'");
					tableReader = new RdbRatingReader(fn);
				}
			}
		}
		
		// MJM regardless of the failure above, fail if we don't have a table-reader.
		if (tableReader == null)
			warning("No table file. Tried: " + tried);
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		if (tableReader == null)
		{
			if (failIfNoTable)
				throw new DbCompException("No rating table");
			return;
		}
		
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
			String msg = "Cannot read RDB rating table: " + ex;
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
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
//AW:TIMESLICE
		if (tableReader == null)
			return;
		try
		{
			setOutput(dep, lookupTable.lookup(indep));
			debug2("Stage = " + indep + ", discharge set to " + dep);
		}
		catch(TableBoundsException ex)
		{
			warning("Table bounds exceeded on indep value at site "
				+ getSiteName("indep", null));
		}
//GC comment at 2010/08/17
/*
		setOutputUnitsAbbr("dep", depUnits);
*/		
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
