package opendcs.opentsdb.hydrojson.beans;

import java.io.Serializable;
import java.util.HashMap;

public class SiteData
	implements Serializable
{
	private static final long serialVersionUID = -6972622560035423939L;
	private String location_id = "";
	private String HUC = "";
	private char active_flag = 'T';
	private Coordinates coordinates = new Coordinates();
	private Elevation elevation = new Elevation();
	private String location_type = "";
	private String name = "";
	
	private String responsibility = "";
	private String time_format = "%Y-%m-%dT%H:%M:%S%z";
	
	
	/** Maps TSID string to TimeSeries data. Will be empty for catalog queries. */
	private HashMap<String, TimeSeries> timeseries = new HashMap<String,TimeSeries>();
	
	private String timezone = "";
	private double tz_offset = 0.0; // # hours

	public SiteData()
	{
	}
	
	public SiteData(String location_id)
	{
		this.setLocation_id(location_id);
	}

	public String getHUC()
	{
		return HUC;
	}

	public void setHUC(String hUC)
	{
		HUC = hUC;
	}

	public char isActive_flag()
	{
		return active_flag;
	}

	public void setActive_flag(char active_flag)
	{
		this.active_flag = active_flag;
	}

	public String getLocation_type()
	{
		return location_type;
	}

	public void setLocation_type(String location_type)
	{
		this.location_type = location_type;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getResponsibility()
	{
		return responsibility;
	}

	public void setResponsibility(String responsibility)
	{
		this.responsibility = responsibility;
	}

	public String getTime_format()
	{
		return time_format;
	}

	public void setTime_format(String time_format)
	{
		this.time_format = time_format;
	}

	public String getTimezone()
	{
		return timezone;
	}

	public void setTimezone(String timezone)
	{
		this.timezone = timezone;
	}

	public double getTz_offset()
	{
		return tz_offset;
	}

	public void setTz_offset(double tz_offset)
	{
		this.tz_offset = tz_offset;
	}

	public Coordinates getCoordinates()
	{
		return coordinates;
	}

	public Elevation getElevation()
	{
		return elevation;
	}

	public HashMap<String, TimeSeries> getTimeseries()
	{
		return timeseries;
	}

	public String getLocation_id()
	{
		return location_id;
	}

	public void setLocation_id(String location_id)
	{
		this.location_id = location_id;
	}

}
