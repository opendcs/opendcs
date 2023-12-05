package org.opendcs.odcsapi.beans;

import java.io.Serializable;

public class Coordinates
	implements Serializable
{
	private static final long serialVersionUID = -7977889608062582257L;
	private String datum = "WGS84";
	private double latitude = 0.0;
	private double longitude = 0.0;

	public Coordinates()
	{
	}

	public String getDatum()
	{
		return datum;
	}

	public void setDatum(String datum)
	{
		this.datum = datum;
	}

	public double getLatitude()
	{
		return latitude;
	}

	public void setLatitude(double latitude)
	{
		this.latitude = latitude;
	}

	public double getLongitude()
	{
		return longitude;
	}

	public void setLongitude(double longitude)
	{
		this.longitude = longitude;
	}

}
