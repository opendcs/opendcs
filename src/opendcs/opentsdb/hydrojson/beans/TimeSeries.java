package opendcs.opentsdb.hydrojson.beans;

import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.io.Serializable;
import java.text.SimpleDateFormat;

import decodes.cwms.CwmsTsId;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesIdentifier;

public class TimeSeries
	implements Serializable
{
	private static final long serialVersionUID = -5276282694853583639L;
	private char active_flag = 'T';
	private int count = 0;
	private String duration = "";
	private String end_timestamp = null;
	private String interval = "";
	private Double max_value = null;
	private Double min_value = null;

	private String notes = "";
	private String param = "";
	private int sigfig = 3;
	
	/** for future dev? Is this supposed to be a string rep of the
	 * quality flags in each value?
	 */
	private String site_quality[] = null;
	
	private String start_timestamp = null;
	private String units="";
	
	private Object[] values = null;

	public TimeSeries()
	{
	}
	
	public void fillFromCTimeSeries(CTimeSeries cts, SimpleDateFormat sdf)
		throws BadTimeSeriesException
	{
		TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
		if (tsid == null)
			throw new BadTimeSeriesException(
				"Cannot set HydrJSON time series without a TSID.");
		if (tsid instanceof CwmsTsId)
		{
			CwmsTsId ctsid = (CwmsTsId)tsid;
			active_flag = ctsid.isActive() ? 'T' : 'F';
			duration = ctsid.getDuration();
			param = ctsid.getPart("Param");
		}
		interval = tsid.getInterval();
		
		// Make sure sorted in ascending order
		cts.sort();
		count = cts.size();
		if (count > 0)
		{
			start_timestamp = sdf.format(cts.sampleAt(0).getTime());
			end_timestamp = sdf.format(cts.sampleAt(count - 1).getTime());
			
			values = new Object[count];
			for(int idx = 0; idx < count; idx++)
			{
				TimedVariable tv = cts.sampleAt(idx);
				try
				{
					double d = tv.getDoubleValue();
					Object v[] = new Object[3];
					v[0] = sdf.format(tv.getTime());
					v[1] = new Double(d);
					v[2] = new Integer(tv.getFlags());
					values[idx] = v;
						
					if (max_value == null || d > max_value)
						max_value = d;
					if (min_value == null || d < min_value)
						min_value = d;
				}
				catch(NoConversionException ex){} // Not a numeric value?
			}
		}
		
		units = cts.getUnitsAbbr();
		notes = tsid.getDisplayName();
		
		System.out.println("setFromCTimeSeries successful with " + count 
			+ " values, arraylen=" + (values==null?0:values.length) 
			+ ", tsid.units=" + tsid.getStorageUnits() + ", cts.units=" + cts.getUnitsAbbr());
	}
	
	public int getCount() { return count; }

	public char isActive_flag()
	{
		return active_flag;
	}

	public void setActive_flag(char active_flag)
	{
		this.active_flag = active_flag;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	public int getSigfig()
	{
		return sigfig;
	}

	public void setSigfig(int sigfig)
	{
		this.sigfig = sigfig;
	}

	public String[] getSite_quality()
	{
		return site_quality;
	}

	public void setSite_quality(String[] site_quality)
	{
		this.site_quality = site_quality;
	}

	public String getDuration()
	{
		return duration;
	}

	public String getEnd_timestamp()
	{
		return end_timestamp;
	}

	public String getInterval()
	{
		return interval;
	}

	public Double getMax_value()
	{
		return max_value;
	}

	public Double getMin_value()
	{
		return min_value;
	}

	public String getParam()
	{
		return param;
	}

	public String getStart_timestamp()
	{
		return start_timestamp;
	}

	public String getUnits()
	{
		return units;
	}

//	public TimeSeriesValue[] getValues()
	public Object[] getValues()
	{
		return values;
	}

}
