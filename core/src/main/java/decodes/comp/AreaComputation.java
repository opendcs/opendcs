/*
*  $Id$
*/
package decodes.comp;

import ilex.util.Logger;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.util.Date;
import java.util.Enumeration;

/**
* Implements area table computations.
* Holds the lookup table &amp; shift values.
* Delegates table reads to supplied reader.
*/
public class AreaComputation 
	extends Computation
	implements HasLookupTable
{
	private String module = "AreaComputation";

	/**
	* If true, apply shifts to independent variable before lookup.
	*/
	private boolean applyShifts;
	
	/**
	* Sensor number in PlatformConfig for independent variable
	*/
	private int indepSensorNum;
	
	/**
	 * Sensor number in PlatformConfig for Mean Velocity (XV)
	 */
	private int xvSensorNum;
	
	/**
	 * 
	 */
	private double xvScale;
	
	
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
		Logger.instance().debug3("Applying area rating calculation");
		// Retrieve independent time series.
		ITimeSeries indepTs = msg.getITimeSeries(indepSensorNum);
		if (indepTs == null)
		{
			Logger.instance().warning(module + 
				" Message does not contain independent sensor " +
				indepSensorNum);
			return;
		}

		String name = getProperty("DepName");
		if (name == null)
			name = "anon";

		ITimeSeries depTs = msg.newTimeSeries(depSensorNum, name);
		Logger.instance().debug3("Created dep sensor " + depSensorNum + ": " + name);
		depTs.setDataOrder(indepTs.getDataOrder());
		depTs.setPeriodicity(indepTs.getRecordingMode(), 
			indepTs.getTimeOfFirstSample(), indepTs.getTimeInterval());
		String s = getProperty("DepEpaCode");
		if (s != null)
			depTs.addDataType("EPA-Code", s);
		s = getProperty("DepShefCode");
		if (s != null)
			depTs.addDataType("SHEF-PE", s);

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
					"Skipping area computation because sample time is"
					+ " outside the rating time range.");
				continue;
			}
			try
			{
				//Find area for the HG value - comes from the area files
				double area = ratingTable.lookup(indepTv.getDoubleValue());
				Logger.instance().info(module + " Area for " + 
						indepTv.getDoubleValue() + " is " + area);
				//multiply the area by the XV value sensor. Note the XV value
				//has a scale value, this value is multiplied by the XV value
				//before doing this equation
				double xvValue = 0;//Mean velocity
				//Find the XV value for this timestamp
				ITimeSeries xvTs = msg.getITimeSeries(xvSensorNum);
				if (xvTs == null)
				{
					Logger.instance().warning(module + 
						" Message does not contain xv (mean velocity) sensor " +
						xvSensorNum);
					return;
				}
				int xvz = xvTs.size();
				for(int x=0; x<xvz; x++)
				{
					TimedVariable xvTv = xvTs.sampleAt(x);
					if ((xvTv.getFlags() & (IFlags.IS_ERROR|IFlags.IS_MISSING)) != 0)
						continue;
					Date xvd = xvTv.getTime();
					if (xvd.getTime() == d.getTime())
					{
						xvValue = xvTv.getDoubleValue();
						break;
					}
				}
				//Calculate avg velocity
				double outputV = area * xvValue;

				Logger.instance().debug1(module + " area = " + area + 
						" VelocityValue = "
						+ xvValue + " output = " + outputV );

				TimedVariable depTv = new TimedVariable(outputV);
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

		Logger.instance().debug3("AreaComp produced " + depTs.size() +
			" " + name + " samples.");

		if (depTs.size() == 0)
			msg.rmTimeSeries(depTs);
	}
	
	/**
	  Called from the resolver.
	  @param reader the object used to read the table.
	*/
	public AreaComputation( RatingTableReader reader )
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
		xvScale = 1.0;
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
	 * Sets the Mean Velocity Sensor number
	 * 
	 * @param num the sensor number
	 */
	public void setXVSensorNum( int num)
	{
		xvSensorNum = num;
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
		System.out.println("AreaComputation");
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

	/**
	 * This is the scale value used to get mean velocity from
	 * the XV Sensor value
	 * 
	 * @return xv scale
	 */
	public double getXvScale() 
	{
		return xvScale;
	}

	/**
	 * This is the scale value used to get mean velocity from
	 * the XV Sensor value
	 * 
	 * @param xvScale value used to get the mean velocity
	 */
	public void setXvScale(double xvScale) 
	{
		this.xvScale = xvScale;
	}

	public void clearTable()
	{
		ratingTable.clear();
	}
}
