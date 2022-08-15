package opendcs.opentsdb.hydrojson.beans;

import java.util.Properties;

public class DecodesPlatformSensor
{
	private int sensorNum = 0;
	
	/** Null means this sensor is at same site as Platform */
	private Long actualSiteId = null;
	
	/** Null means no limit is set */
	private Double min = null;
	private Double max = null;

	private Integer usgsDdno = null;
	private Properties sensorProps = new Properties();
	public int getSensorNum()
	{
		return sensorNum;
	}
	public void setSensorNum(int sensorNum)
	{
		this.sensorNum = sensorNum;
	}
	public Long getActualSiteId()
	{
		return actualSiteId;
	}
	public void setActualSiteId(Long actualSiteId)
	{
		this.actualSiteId = actualSiteId;
	}
	public Double getMin()
	{
		return min;
	}
	public void setMin(Double min)
	{
		this.min = min;
	}
	public Double getMax()
	{
		return max;
	}
	public void setMax(Double max)
	{
		this.max = max;
	}
	public Integer getUsgsDdno()
	{
		return usgsDdno;
	}
	public void setUsgsDdno(Integer usgsDdno)
	{
		this.usgsDdno = usgsDdno;
	}
	public Properties getSensorProps()
	{
		return sensorProps;
	}
	public void setSensorProps(Properties sensorProps)
	{
		this.sensorProps = sensorProps;
	}
}
