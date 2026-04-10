/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package ilex.util;


public class Location
{
	private double latitude = 0.0;
	private double longitude = 0.0;
	private double radius = 0.0;

	public String toString()
	{
		return "Loc: lat=" + latitude + ", lon=" + longitude + ", rad=" + radius;
	}

	/**
	 * Accept a string in decimal or deg/min/sec notation and return
	 * a decimal number representing the latitude.
	 * Southern latitudes are negative numbers
	 * @param lat the String representation of the latitude
	 * @return latitude as a number
	 * @throws ParseException
	 */
	public static double parseLatitude(String lat)
		throws NumberFormatException
	{
		if (lat == null || lat.trim().length() == 0)
			return 0.0;

		double ret = 0.0;
		// Try to trim any trailing characters like deg or N
		int n = lat.length()-1;
		while(n > 0
		 && !Character.isDigit(lat.charAt(n)) && lat.charAt(n) != '.')
			n--;
		try
		{
			ret = Double.parseDouble(lat.substring(0, n+1));
		}
		catch(NumberFormatException ex)
		{
			try { ret = degMinSec(lat); }
			catch(NumberFormatException ex2)
			{

				NumberFormatException toThrow = new NumberFormatException("Cannot parse latitude '" + lat + "'");
				toThrow.addSuppressed(ex);
				toThrow.addSuppressed(ex2);
				throw toThrow;
			}
		}
		// Default to NORTH latitude
		if (lat.toUpperCase().indexOf('S') >= 0 && ret > 0)
			ret = -ret;
		return ret;
	}

	/**
	 * Accept a string in decimal or deg/min/sec notation and return
	 * a decimal number representing the longitude.
	 * West longitudes are negative numbers
	 * @param lon the String representation of the latitude
	 * @return longitude as a number
	 * @throws ParseException
	 */
	public static double parseLongitude(String lon)
		throws NumberFormatException
	{
		if (lon == null || lon.trim().length() == 0)
			return 0.0;
		double ret = 0.0;

		// Try to trim any trailing characters like deg or N
		int n = lon.length()-1;
		while(n > 0
		 && !Character.isDigit(lon.charAt(n)) && lon.charAt(n) != '.')
			n--;
		try
		{
			ret = Double.parseDouble(lon.substring(0, n+1));
		}
		catch(NumberFormatException ex)
		{
			try { ret = degMinSec(lon); }
			catch(NumberFormatException ex2)
			{
				NumberFormatException toThrow = new NumberFormatException("Cannot parse longitude '" + lon + "'");
				toThrow.addSuppressed(ex);
				toThrow.addSuppressed(ex2);
				throw toThrow;
			}
		}
		// Default to WEST longitude as a negative number.
		if (lon.toUpperCase().indexOf('E') == -1 && ret > 0)
			ret = -ret;
		return ret;
	}

	/**
	 * Try to convert a string in deg min sec format to a double precision.
	 */
	private static double degMinSec(String dms)
		throws NumberFormatException
	{
		// Look for 3 integers separated by non integers
		int nstart = 0;
		for(nstart = 0; nstart < dms.length()
			&& !Character.isDigit(dms.charAt(nstart)); nstart++);
		if (nstart == dms.length())
			throw new NumberFormatException("No degrees");
		int nend = nstart+1;
		for(; nend < dms.length()
			&& Character.isDigit(dms.charAt(nend)); nend++);
		int deg = Integer.parseInt(dms.substring(nstart, nend));

		for(nstart = nend; nstart < dms.length()
			&& !Character.isDigit(dms.charAt(nstart)); nstart++);
		if (nstart == dms.length())
			throw new NumberFormatException("No minutes");
		nend = nstart+1;
		for(; nend < dms.length()
			&& Character.isDigit(dms.charAt(nend)); nend++);
		int min = Integer.parseInt(dms.substring(nstart, nend));

		for(nstart = nend; nstart < dms.length()
			&& !Character.isDigit(dms.charAt(nstart)); nstart++);
		if (nstart == dms.length())
			throw new NumberFormatException("No seconds");
		nend = nstart+1;
		for(; nend < dms.length()
			&& Character.isDigit(dms.charAt(nend)); nend++);
		int sec = Integer.parseInt(dms.substring(nstart, nend));

		return (double)deg + (double)min/60.0 + (double)sec/3600.0;
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

	public double getRadius()
	{
		return radius;
	}

	public void setRadius(double radius)
	{
		this.radius = radius;
	}
}