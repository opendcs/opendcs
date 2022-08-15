package opendcs.opentsdb.hydrojson.beans;

import java.io.Serializable;

public class Elevation
	implements Serializable
{
	private static final long serialVersionUID = -5057938869365960814L;
	private double accuracy = 0.0;
	private String datum = "NGVD29";
	private String method = "";
	private double value = 0.0;
	
	public Elevation()
	{
	}

	public double getAccuracy()
	{
		return accuracy;
	}

	public void setAccuracy(double accuracy)
	{
		this.accuracy = accuracy;
	}

	public String getDatum()
	{
		return datum;
	}

	public void setDatum(String datum)
	{
		this.datum = datum;
	}

	public String getMethod()
	{
		return method;
	}

	public void setMethod(String method)
	{
		this.method = method;
	}

	public double getValue()
	{
		return value;
	}

	public void setValue(double value)
	{
		this.value = value;
	}

}
