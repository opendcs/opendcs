package decodes.excel;

import ilex.util.Logger;
import ilex.var.TimedVariable;

import java.util.Comparator;
import java.util.Date;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.decoder.TimeSeries;

/**
 * This class stores a representation of a sensor column in the
 * excel workbook.
 *
 */
public class ExcelColumn
{
	private String preferredDataType = Constants.datatype_SHEF;
	private String module;
	TimeSeries timeSeries;
	int colWidth;
	int curSampleNum;
	String blankSample;
	String sensorName;
	String dataType;
	String euAbbr;
	int dotPos;
	String siteName;
	
	Site site;
	String siteNameCell;//the B PART
	String fPart;//raw or rev - comes from sensor properties
				//default to raw
	String type;//INST-VAL for all except PC which is PER-CUM
	int sensorNumber;
	
	/** Initialize the ExcelColumn constructor class */
	public ExcelColumn(TimeSeries ts, String nameOfSite)
	{
		module = "ExcelColumn";
		timeSeries = ts;
		curSampleNum = 0;
		colWidth = 0;
		siteNameCell = nameOfSite;
		siteName = ts.getSensor().getSensorSiteName();

		sensorName = ts.getSensor().getName();
		sensorNumber = ts.getSensor().getNumber();
		
		//Get the fpart from sensor properties - raw or rev
		fPart = ts.getSensor().getProperty("fpart");
		if (fPart == null)
		{
			fPart = "raw";
		}
		
		if (sensorName == null)
			sensorName = "unknown";

		euAbbr = ts.getEU().abbr;

		DataType dt = ts.getSensor().getDataType(preferredDataType);
		if (dt == null)
		{
			// Doesn't have preferred, get whatever is defined.
			dt = ts.getSensor().getDataType();
			if (dt == null)
			{
				Logger.instance().log(Logger.E_WARNING,
					" " + module + " Site '" + siteName +"' Sensor "
					+ ts.getSensor().configSensor.sensorNumber 
					+ " has unknown data type!");
				dt = DataType.getDataType("UNKNOWN", "UNKNOWN");
			}
		}
		dataType = dt.getCode();

		colWidth = sensorName.length();
		if (dataType.length() > colWidth)
			colWidth = dataType.length();
		if (euAbbr.length() > colWidth)
			colWidth = euAbbr.length();

		//Set the type row for this sensor
		if (dataType.equalsIgnoreCase("PC"))
			type = "PER-CUM";
		else
			type = "INST-VAL";
		
		initColumnData();
	}

	/**
	 * Add more TimeSeries samples to this sensor
	 * 
	 * @param ts
	 */
	public void appendTimeSeries(TimeSeries ts)
	{
		for(int i=0; i<ts.size(); i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			boolean found = false;
			//Verify if we already have this timeseries
			//with this sample - check by timestamp
			for(int y=0; y<timeSeries.size(); y++)
			{
				TimedVariable tvIn = timeSeries.sampleAt(y);
				if (tvIn.getTime().getTime() == tv.getTime().getTime())
				{	//we already have this value - do not add it
					found = true;
					break;
				}
			}
			if (!found)
				timeSeries.addSample(tv);
		}
		initColumnData();
	}
	
	/**
	 * 
	 *
	 */
	private void initColumnData()
	{
		dotPos = -1;
		for(int i=0; i<timeSeries.size(); i++)
//		for(Iterator it = timeSeries.formattedSamplesIterator(); 
//			it.hasNext(); )
		{
			String s = timeSeries.formattedSampleAt(i);
//			String s = (String)it.next();
			if (s.length() > colWidth)
				colWidth = s.length();
			int dp = s.indexOf('.');
			if (dp == -1)
			{
				String trimmed = s.trim();
				if (trimmed.length() > 0 && 
						!Character.isDigit(trimmed.charAt(0)))
					dp = 0;
				else
					dp = s.length();
			}
			if (dp > dotPos)
				dotPos = dp;
		}
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<colWidth; i++)
			sb.append(' ');
		blankSample = sb.toString();

		// sort time series into ascending order.
		timeSeries.setDataOrder(Constants.dataOrderAscending);
		//timeSeries.setDataOrder(Constants.dataOrderDescending);
		timeSeries.sort();
	}
	
	public String getBlankSample()
	{
		return blankSample;
	}

	// Return date of next sample in series or null if at end of series.
	public Date nextSampleTime()
	{
		if (curSampleNum < timeSeries.size())
			return timeSeries.timeAt(curSampleNum);
		return null;
	}

	public String nextSample()
	{
		if (curSampleNum >= timeSeries.size())
			return blankSample;
		String s = timeSeries.formattedSampleAt(curSampleNum++);
		int dp = s.indexOf('.');
		if (dp == -1)
			dp = s.length();
		StringBuffer sb = new StringBuffer(s);
		if (dp < dotPos)
		{
			while(dp++ < dotPos)
			{
				sb.insert(0, ' ');
				if (sb.charAt(sb.length() - 1) == ' ')
					sb.deleteCharAt(sb.length() - 1);
			}
		}
		while(sb.length() < colWidth-2)
			sb.append(' ');
		
		s = sb.toString();
		return s;
	}
}

//Sort the sensors by sensor numbers
class SensorColumnComparator implements Comparator
{
	public SensorColumnComparator()
	{	
	}
	public int compare(Object se1, Object se2)
	{
		if (se1 == se2)
			return 0;
		ExcelColumn s1 = (ExcelColumn) se1;
		ExcelColumn s2 = (ExcelColumn) se2;
		int i1 = s1.sensorNumber;
		int i2 = s2.sensorNumber;
		return i1 - i2;
	}
}
