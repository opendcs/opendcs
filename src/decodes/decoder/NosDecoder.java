package decodes.decoder;

import ilex.util.Logger;
import ilex.var.Variable;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.PlatformConfig;

public abstract class NosDecoder
	extends DecodesFunctionOperation
{
	public static final String PM_STATION_ID = "NOS_STATION_ID";
	public static final String PM_DCP_NUM = "NOS_DCP_NUM";
	/** Performance Measurement Label */
	public static final String PM_DATUM_OFFSET = "DATUM_OFFSET";
	/** Performance Measurement Label */
	public static final String PM_SENSOR_OFFSET = "SENSOR_OFFSET";
	/** System Status */
	public static final String PM_SYSTEM_STATUS = "SYSTEM_STATUS";
	/** Performance Measurement Label */
	public static final String PM_NOS_BATTERY = "BATTERY_VOLTAGE";
	public static final String PM_STATION_TIME = "NOS_STATION_TIME";
	
	public static final String module = "NosDecoder";
	
	/** This var-flag indicates that the value is redundant. */
	public static final int FLAG_REDUNDANT = 0x10;
	
	protected GregorianCalendar cal = new GregorianCalendar();
	protected NumberParser numberParser = new NumberParser();
	protected transient DataOperations dataOps = null;
	
	protected int sensorIndex[] = new int[26];
	protected int dcpNum = 0;
	protected int primaryDcpNum = 0;



	protected NosDecoder()
	{
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		initSensorIndeces();
	}
	
	/** Sets all the sensor indeces to zero */
	protected void initSensorIndeces()
	{
		for(int i=0; i<26; i++)
			sensorIndex[i] = 0;
	}
	


	/** Initialize the internal calendar to the Windows (9210) Epcoh */
	private void initCalToEpoch()
	{
		cal.set(Calendar.YEAR, 1984);
		cal.set(Calendar.MONTH, Calendar.DECEMBER);
		cal.set(Calendar.DAY_OF_MONTH, 31);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}
	
	/**
	 * Sets an arbitrary day number since the epoch.
	 * Day number is in range 0...4095, which is a little more than 11 years.
	 * So the day number wraps.
	 * The strategy here is to add 4096 until the calendar is after the current
	 * time, then back off 4096.
	 * Thus we find the latest possible day-num that is before the current time.
	 * @param daynum 12-bit day number decoded from message (0...4095)
	 */
	protected void setDayNumber(int daynum)
	{
		initCalToEpoch();
		cal.add(Calendar.DAY_OF_YEAR, daynum);
		Date now = new Date();
		while(cal.getTime().before(now))
			cal.add(Calendar.DAY_OF_YEAR, 4096);
		cal.add(Calendar.DAY_OF_YEAR, -4096);
	}

	/**
	 * Given a char sensor type, use the sensorIndex array and DCP
	 * Number to determine the correct sensor number to save a value
	 * to.
	 * @param sensorType 1st char of sensor type code, e.g. S=SAE
	 * @return Sensor Number to save value to, or -1 if error.
	 */
	protected int getSensorNumber(char sensorType, PlatformConfig config)
	{
		// Find the first Data Type for this sensor type.
		// Example, for 'S', find S1. Then add sensor number offsets
		// for DCP number and sensor number.
		for(Iterator<ConfigSensor> csit = config.getSensors();
			csit.hasNext(); )
		{
			ConfigSensor cs = csit.next();
			DataType dt = cs.getDataType(Constants.datatype_NOS);
			if (dt == null)
				continue;
			if (dt.getCode().charAt(0) == sensorType)
			{
				return cs.sensorNumber 
					+ (dcpNum-1)*100
					+ (sensorIndex[(int)sensorType - (int)'A']++);
				
			}
		}
		Logger.instance().warning(module + " Unknown sensor type '"
			+ sensorType + "' dcpNum=" + dcpNum);
		return -1;
	}

	protected Variable getNumber(int nbytes, boolean signed)
		throws DecoderException
	{
		int startPos = dataOps.getBytePos();
		try
		{
			numberParser.setDataType(signed ? NumberParser.SIGNED_PBINARY_FMT 
				: NumberParser.PBINARY_FMT);
			byte []field = dataOps.getField(nbytes, null);
			Variable result = numberParser.parseDataValue(field);
			Logger.instance().debug3(module + " parsing field '"
				+ new String(field) + "' result = " + result);
			return result;
		}
		catch(Exception ex)
		{
			throw new DecoderException(module + " cannot get number len=" + nbytes
				+ " at position " + startPos + ": " + ex);
		}
	}

	protected int getInt(int nbytes, boolean signed)
		throws DecoderException
	{
		int startPos = dataOps.getBytePos();
		try
		{
			return getNumber(nbytes, signed).getIntValue();
		}
		catch(Exception ex)
		{
			throw new DecoderException(module + " cannot get integer len=" + nbytes
				+ " at position " + startPos + ": " + ex);
		}
	}
	
	protected double getDouble(int nbytes, boolean signed)
		throws DecoderException
	{
		int startPos = dataOps.getBytePos();
		try
		{
			return getNumber(nbytes, signed).getDoubleValue();
		}
		catch(Exception ex)
		{
			throw new DecoderException(module + " cannot get double len=" + nbytes
				+ " at position " + startPos + ": " + ex);
		}
	}

}
