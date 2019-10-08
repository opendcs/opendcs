/*
*  $Id$
*/
package decodes.comp;

import decodes.comp.Computation;
import decodes.comp.RatingTableReader;
import decodes.cwms.CwmsConstants;
import decodes.comp.LookupTable;
import decodes.comp.ComputationParseException;
import java.util.Enumeration;
import java.util.Date;
import decodes.db.ConfigSensor;
import ilex.util.Logger;
import ilex.var.IFlags;
import ilex.var.TimedVariable;
import ilex.var.NoConversionException;

/**
* Implements rating table computations.
* Holds the lookup table & shift values.
* Delegates table reads to supplied reader.
*/
public class RatingComputation 
	extends Computation
	implements HasLookupTable
{
	/**
	* If true, apply shifts to independent variable before lookup.
	*/
	private boolean applyShifts;
	
	/**
	* Sensor number in PlatformConfig for independent variable
	*/
	private int indepSensorNum;
	
	/**
	* Internal flag indicating whether tables
	* have been sorted.
	*/
	private boolean sorted;
	
	/**
	* The table of independent/dependent points for the rating.
	*/
	private LookupTable ratingTable;
	
	/**
	* A table of shift values.
	*/
	private LookupTable shiftTable;
	
	/**
	* The object used to read the table.
	*/
	private RatingTableReader tableReader;
	
	
	/**
	* Sensor number for the dependent (output) value.
	*/
	private int depSensorNum;
	
	
	/**
	* Beginning time for this rating. Only independent values after this time
	* will be processed.
	*/
	private Date beginTime;
	
	
	/**
	* Ending time for this rating. Only independent values before this time
	* will be processed.
	*/
	private Date endTime;
	
	/**
	  Adds a point to the table.
	  @param indep the independent value
	  @param dep the dependent value
	*/
	public void addPoint( double indep, double dep )
	{
		ratingTable.add(indep, dep);
	}
	
	/**
	  Adds a shift to the table.
	  @param indep the independent value
	  @param shift the shift value
	*/
	public void addShift( double indep, double shift )
	{
		shiftTable.add(indep, shift);
	}
	
	/**
	  Applies the rating to the data found in the passed
	  message. If successful, this will result in a new
	  TimeSeries containing the dependent variables.
	  @param msg the input data collection.
	*/
	public void apply( IDataCollection msg )
	{
		Logger.instance().debug3("Applying rating calculation");
		// Retrieve independent time series.
		ITimeSeries indepTs = msg.getITimeSeries(indepSensorNum);
		if (indepTs == null)
		{
			Logger.instance().warning(
				"Message does not contain independent sensor " +
				indepSensorNum);
			return;
		}

		String name = getProperty("DepName");
		if (name == null)
			name = "anon";

		ITimeSeries depTs = msg.getITimeSeries(depSensorNum);
		if (depTs != null)
		{
			name = depTs.getSensorName();
		}
		else
		{
			depTs = msg.newTimeSeries(depSensorNum, name);
			Logger.instance().debug3("Created dep sensor " + depSensorNum + ": " + name);
		}
		depTs.setDataOrder(indepTs.getDataOrder());
		depTs.setPeriodicity(indepTs.getRecordingMode(), 
			indepTs.getTimeOfFirstSample(), indepTs.getTimeInterval());
		String s = getProperty("DepEpaCode");
		if (s != null)
			depTs.addDataType("EPA-Code", s);
		s = getProperty("DepShefCode");
		if (s != null)
			depTs.addDataType("SHEF-PE", s);
//		s = getProperty("depCwmsParam");
//		if (s != null)
//			depTs.addDataType(CwmsConstants.CWMS_DATA_TYPE, s);

/////// THIS IS THE ONLY HOLE
//		ps.site = indepSensor.getSensorSite();
////////

		s = getProperty("DepUnits");
		if (s != null)
			depTs.setUnits(s);

		// For each indep sample, lookup dep value & add to new time series.
		int sz = indepTs.size();
		for(int i=0; i<sz; i++)
		{
			TimedVariable indepTv = indepTs.sampleAt(i);
			if ((indepTv.getFlags() & (IFlags.IS_ERROR|IFlags.IS_MISSING)) != 0)
				continue;
			Date d = indepTv.getTime();
			if (d.compareTo(beginTime) < 0
			 || d.compareTo(endTime) > 0)
			{
				Logger.instance().warning(
					"Skipping rating computation because sample time is"
					+ " outside the rating time range. Sample Time=" +d
					+ ", RatingStart=" + beginTime
					+ ", RatingEnd=" + endTime);
				continue;
			}
			try
			{
				double v = ratingTable.lookup(indepTv.getDoubleValue());
				TimedVariable depTv = new TimedVariable(v);
				depTv.setTime(d);
				depTs.addSample(depTv);
			}
			catch(NoConversionException ex)
			{
				Logger.instance().warning("Independent value not a number.");
			}
			catch(TableBoundsException ex)
			{
				Logger.instance().warning(ex.toString());
			}
		}

		Logger.instance().debug3("RatingComp produced " + depTs.size() +
			" " + name + " samples.");

		if (depTs.size() == 0)
			msg.rmTimeSeries(depTs);
	}
	
	/**
	  Called from the resolver.
	  @param reader the object used to read the table.
	*/
	public RatingComputation( RatingTableReader reader )
	{
		super();
		tableReader = reader;
		applyShifts = false;
		indepSensorNum = -1;
		sorted = false;
		ratingTable = new LookupTable();
		ratingTable.setLookupType(LookupTable.INTERP_LOG);
		shiftTable = new LookupTable();
		shiftTable.setLookupType(LookupTable.INTERP_TRUNC);
		setBoundsInterp(false, false);
		beginTime = new Date(0L);
		endTime = new Date(Long.MAX_VALUE);
	}
	
	/**
	* Sets the applyShifts flag.
	* @param yn the flag
	*/
	public void setApplyShifts( boolean yn )
	{
		applyShifts = yn;
	}
	
	/**
	* Sets the flags to interpolate above/below the table bounds.
	* @param below the below flag
	* @param above the above flag
	*/
	public void setBoundsInterp( boolean below, boolean above )
	{
		ratingTable.setExceedLowerBound(below);
		ratingTable.setExceedUpperBound(above);
		shiftTable.setExceedLowerBound(below);
		shiftTable.setExceedUpperBound(above);
	}
	
	/**
	* Sets sensor number for independent variable.
	* @param num the sensor number
	*/
	public void setIndepSensorNum( int num )
	{
		indepSensorNum = num;
		
	}

	/**
	* Sets the X offset used for logarithmic interpolation.
	* @param xo the X offset
	*/
	public void setXOffset(double xo)
	{
		ratingTable.setXOffset(xo);
	}
	
	/** 
	* Sets lookup type to one of the constantd defined in LookupTable.
	* @param t the type
	*/
	public void setLookupType( int t )
	{
		ratingTable.setLookupType(t);
		// Note shift table is always truncating.
	}

	/** Test method to spit contents onto stdout */
	public void dump()
	{
		System.out.println("RatingComputation");
		System.out.println("Properties:");
		for(Enumeration it = props.propertyNames(); it.hasMoreElements(); )
		{
			String n = (String)it.nextElement();
			System.out.println("\t" + n + "=" + props.getProperty(n));
		}
		System.out.println("indepSensorNum = " + indepSensorNum);
		System.out.println("applyShifts = " + applyShifts);
		System.out.println("sorted = " + sorted);
		System.out.println("Shift Table:");
		shiftTable.dump();
		System.out.println("Rating Table:");
		ratingTable.dump();
	}

	/**
	  Reads the rating table using the supplied reader.
	  @throws ComputationParseException if error reading table.
	*/
	public void read( ) 
		throws ComputationParseException
	{
		tableReader.readRatingTable(this);	
	}

	/**
	* Sets dependent sensor number
	* @param num the number
	*/
	public void setDepSensorNum( int num )
	{
		depSensorNum = num;
	}

	/**
	* Sets the begin time for this rating.
	* @param bt the begin time
	*/
	public void setBeginTime( Date bt )
	{
		beginTime = bt;
	}
	
	
	/**
	* Sets the end time for this rating.
	* @param et the end time
	*/
	public void setEndTime( Date et )
	{
		endTime = et;
	}

	public LookupTable getLookupTable() { return ratingTable; }

	public void clearTable()
	{
		ratingTable.clear();
	}
}
