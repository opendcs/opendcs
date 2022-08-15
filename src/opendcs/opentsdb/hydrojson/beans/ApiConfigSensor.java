package opendcs.opentsdb.hydrojson.beans;

import java.util.HashMap;
import java.util.Properties;

import decodes.db.Constants;

public class ApiConfigSensor
{
	private int sensorNumber = 0;

	private String sensorName = null;

	/** V=Variable, F=Fixed, U=undefined */
	private char recordingMode = Constants.recordingModeUndefined;

	private int recordingInterval = 3600;

	/** The second-of-day of the first sample, in seconds since midnight. */
	private int timeOfFirstSample = 0;

	private Double absoluteMin = null;

	private Double absoluteMax = null;

	private Properties properties = new Properties();

	private HashMap<String, String> dataTypes = new HashMap<String, String>();

	private String usgsStatCode = null;

	public int getSensorNumber()
	{
		return sensorNumber;
	}

	public void setSensorNumber(int sensorNumber)
	{
		this.sensorNumber = sensorNumber;
	}

	public String getSensorName()
	{
		return sensorName;
	}

	public void setSensorName(String sensorName)
	{
		this.sensorName = sensorName;
	}

	public char getRecordingMode()
	{
		return recordingMode;
	}

	public void setRecordingMode(char recordingMode)
	{
		this.recordingMode = recordingMode;
	}

	public int getRecordingInterval()
	{
		return recordingInterval;
	}

	public void setRecordingInterval(int recordingInterval)
	{
		this.recordingInterval = recordingInterval;
	}

	public int getTimeOfFirstSample()
	{
		return timeOfFirstSample;
	}

	public void setTimeOfFirstSample(int timeOfFirstSample)
	{
		this.timeOfFirstSample = timeOfFirstSample;
	}

	public Double getAbsoluteMin()
	{
		return absoluteMin;
	}

	public void setAbsoluteMin(Double absoluteMin)
	{
		this.absoluteMin = absoluteMin;
	}

	public Double getAbsoluteMax()
	{
		return absoluteMax;
	}

	public void setAbsoluteMax(Double absoluteMax)
	{
		this.absoluteMax = absoluteMax;
	}

	public Properties getProperties()
	{
		return properties;
	}

	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}

	public HashMap<String, String> getDataTypes()
	{
		return dataTypes;
	}

	public void setDataTypes(HashMap<String, String> dataTypes)
	{
		this.dataTypes = dataTypes;
	}

	public String getUsgsStatCode()
	{
		return usgsStatCode;
	}

	public void setUsgsStatCode(String usgsStatCode)
	{
		this.usgsStatCode = usgsStatCode;
	}

}
