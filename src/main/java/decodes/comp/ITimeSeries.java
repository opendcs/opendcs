/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/12/10 21:43:21  mmaloney
*  dev
*
*  Revision 1.3  2004/08/24 14:31:28  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2004/08/11 21:40:57  mjmaloney
*  Improved javadocs
*
*  Revision 1.1  2004/06/30 20:01:50  mjmaloney
*  Isolated DECODES interface behind IDataCollection and ITimeSeries interfaces.
*
*/
package decodes.comp;

import java.util.Date;
import ilex.var.TimedVariable;

/**
Defines the interface that all time-series must support in order to use
the computation infrastructure.
*/
public interface ITimeSeries
{
	/** ascending code */
	public static final char DATA_ORDER_ASCENDING = 'A';
	/** descending code */
	public static final char DATA_ORDER_DESCENDING = 'D';
	/** undefined code */
	public static final char DATA_ORDER_UNDEFINED  = 'D';

	/** code for fixed-interval */
	public static final char MODE_FIXED      = 'F';
	/** code for variable-interval */
	public static final char MODE_VARIABLE   = 'V';
	/** code for undefined-interval */
	public static final char MODE_UNDEFINED  = 'U';

	/**
	* Finds the named property. 
	* TimeSeries must support properties. Property name lookup should be
	* case INsensitive.
	*
	* @param name  Name of property to find.
	* @return named property value, or null if not found.
	*/
	public String getProperty(String name);

	/**
	* @return a numeric identifier for sensor that produced this time series.
	* Identifiers must be unique within a site. That is, no two
	* ITimeSeries objects in the same ITimeSeriesData object may have the
	* same ID.
	* Examples would be the DECODES sensor number or the USGS DDNO.
	*/
	public int getSensorId();

	/**
	* For fixed interval time-series, this method returns the number of seconds
	* between samples.
	*
	* @return number of seconds between samples, or -1 if this is not a fixed 
	*         interval time series.
	*/
	public int getTimeInterval();

	/**
	* @return the number of samples stored in this time-series. 
	*/
	public int size();

	/**
	* @return sensor name associated with this time series.
	*/
	public String getSensorName();

	/**
	* @return the begin time for the time series, that is, the time of the 
	* earliest sample stored.
	*/
	public Date getBeginTime();

	/**
	* @return data order of values in this time series.
	*/
	public char getDataOrder();

	/**
	* Sets the data order of values in this time series.
	* @param dord  Data Order, should be one of the defined constants in this 
	*            class.
	*/
	public void setDataOrder(char dord);

	/**
	* Sets the engineering units associated with values in this time series.
	*
	* @param  eu   A string representing a standard engineering unit name or 
	*              abbreviation.
	*/
	public void setUnits(String eu);

	/**
	* Gets the engineering units associated with values in this time series.
	*
	* @return a string representing engineering units or the string "unknown"
	*         if units are not set.
	*/
	public String getUnits();

	/**
	* Sets mode and periodicity parameters for this sensor.
	* @param mode       should be one of the MODE constants defined herein.
	* @param firstSamp  Second-of-day of first sample recorded
	* @param sampInt    Sampling interval in seconds.
	*/
	public void setPeriodicity(char mode, int firstSamp, int sampInt);

	/**
	* @return time of first sample for FIXED mode sensor.
	*/
	public int getTimeOfFirstSample();

	/**
	* @return mode, one of the MODE constants defined herein.
	*/
	public char getRecordingMode();

	/**
	* Associates a data type with values in this time series.
	* A time series may have multiple data types but only one of each standard.
	* @param standard the standard of the data type
	* @param code the code of the data type
	*/
	public void addDataType(String standard, String code);

	/**
	* Returns the sample at the specified position in the Time Series.
	* @param idx the index of the sample to return.
	* @return ilex.var.TimedVariable or null if idx out of bounds.
	*/
	public TimedVariable sampleAt(int idx);

	/**
	* Adds a sample to this time series.
	* @param tv the timed variable to add
	*/
	public void addSample(TimedVariable tv);

}
