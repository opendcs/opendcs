package decodes.tsdb.algo;

import java.util.Date;
import java.io.File;
import java.io.FileNotFoundException;

import ilex.var.NamedVariable;
import ilex.util.EnvExpander;
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
    double indep;    //AW:TYPECODE=i
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
				try
				{
                	tableReader = new RdbRatingReader(fn);
				}
				catch (FileNotFoundException ex)
				{
					throw new DbCompException(String.format("Cannot read %s", fn), ex);
				}
            }
        }
        if (tableReader == null)
        {
            String namePref = DecodesSettings.instance().siteNameTypePreference;
            if (namePref.equalsIgnoreCase(Constants.snt_USGS))
                throw new DbCompException("No usgs site name for independent "
                    + "variable with SDI=" + getSDI("indep"));
            siteName = getSiteName("indep");
            if (siteName != null)
            {
                String fn = tableDir + "/" + filePrefix + siteName + ".rdb";
                tried = tried + " " + fn;
                File f = new File(EnvExpander.expand(fn));
					debug1("trying '" + f.getPath() + "'");
                if (f.exists())
                {
                    debug3("Constructing RDB reader for '" + fn + "'");
                    try
					{
						tableReader = new RdbRatingReader(fn);
					}
					catch (FileNotFoundException ex)
					{
						throw new DbCompException(String.format("Cannot read %s", fn), ex);
					}
                }
            }
        }

        // MJM regardless of the failure above, fail if we don't have a table-reader.
        if (tableReader == null)
        {
            String msg = "No table file. Tried: " + tried;
            warning(msg);
            if (failIfNoTable)
                throw new DbCompException(msg);
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
                + getSiteName("indep", null) + ", value was " + indep + " at time "
                + debugSdf.format(_timeSliceBaseTime) + ", indep units="
                + this.getParmRef("indep").timeSeries.getUnitsAbbr());
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
