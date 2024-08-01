/*
 jWeather(TM) is a Java library for parsing raw weather data
 Copyright (C) 2004 David Castro

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 For more information, please email arimus@users.sourceforge.net
 */
package decodes.decoder.jweather.metar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

/*
 * examples:
 *  KCNO 060653Z 32004KT 10SM BKN043 13/11 A2993 RMK AO2 SLP133 T01280106
 *  KCNO 070353Z AUTO 29009KT 10SM CLR 13/11 A2991 RMK AO2 SLP127 T01280106
 *  KCNO 071853Z 24010KT 10SM BKN038 OVC048 17/09 A2998 RMK AO2 SLP147 T01670089
 *  KCNO 231653Z VRB04KT 1 3/4SM HZ BKN010 18/15 A2997 RMK AO2 SLP145 HZ FEW000 T01780150
 *  KCNO 291753Z 26006KT 4SM HZ CLR A2991 RMK AO2 SLPNO 57007
 *
 * Body of report:
 *  (1)  Type of report - METAR/SPECI
 *  (2)  Station Identifier - CCCC
 *  (3)  Date and Time of Report (UTC) - YYGGggZ
 *  (4)  Report Modifier - AUTO/COR
 *  (5)  Wind - ddff(f)Gf f (f )KT_d d d Vd d d
 *                       m m  m     n n n  x x x
 *  (6)  Visibility - VVVVVSM
 *  (7)  Runaway Visual Range - RD D /V V V V FT  or  RD D /V V V V VV V V V FT
 *                                r r  r r r r          r r  n n n n  x x x x
 *  (8)  Present Weather - w'w'
 *  (9)  Sky Condition - N N N h h h  or  VVh h h  or  SKC/CLR
 *                        s s s s s s        s s s
 *  (10) Temperature and Dew Point - T'T'/T' T'
 *                                          d  d
 *  (11) Altimeter - AP P P P
 *                     h h h h
 * Remarks section of report:
 *  (1)  Automated, Manual, Plain Language
 *  (2)  Additive and Maintenance Data
 *
 * *note: '_' denotes a required space
 *
 *
 * Table 12-2 Present Weather
 *
 * _________________________________________________________________________________
 * | Intensity  |   Descriptor  |   Precipitation  |   Obscuration |   Other       |
 * +------------+---------------+------------------+---------------+---------------+
 * | - Light    | MI Shallow    | DZ Drizzle       | BR Mist       | PO Well-      |
 * |   Moderate | PR Partial    | RA Rain          | FG Fog        |    Developed  |
 * | + Heavy    | BC Patches    | SN Snow          | FU Smoke      |    Dust/Sand  |
 * |            | DR Low        | SG Snow Grains   | VA Volcanic   |    Whirls     |
 * |            |    Drifting   | IC Ice Crystals  |    Ash        | SQ Squalls    |
 * |            | BL Blowing    | PL Ice Pellets   | DU Widespread | FC Funnel     |
 * |            | SH Shower(s)  | GR Hail          |    Dust       |    Cloud,     |
 * |            | TS Thunder-   | GS Small Hail    | SA Sand       |    Tornado,   |
 * |            |    storm      |    and/or        | HZ Haze       |    Waterspout |
 * |            | FZ Freezing   |    Snow Pellets  | PY Spray      | SS Sandstorm  |
 * |            |               | UP Unknown       |               | DS Duststorm  |
 * |            |               |    Precipitation |               |               |
 * +------------+---------------+------------------+---------------+---------------+
 *
 * up to 3 weather groups can be reported
 */

/**
 * Responsible for parsing raw METAR data and providing methods for accessing
 * the data
 * 
 * @author David Castro, dcastro@apu.edu
 * @version $Revision: 1.18 $
 * @see <a href="Weather.html">Weather </a>
 * @see <a href="Obscuration.html">Obscuration </a>
 * @see <a href="RunwayVisualRange.html">RunwayVisualRange </a>
 * @see <a href="SkyCondition.html">SkyCondition </a>
 * @see <a href="WeatherCondition.html">WeatherCondition </a>
 */
public class MetarParser {
	private static org.slf4j.Logger logger = LoggerFactory.getLogger(MetarParser.class);

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	private static TimeZone utcZone = TimeZone.getTimeZone("UTC");

	public static void main(String[] args) {
		try {
			// Directory path to read
	        String directoryPath = "C:\\work\\metar\\metarFiles";
			
			// Create a File object representing the directory
	        File directory = new File(directoryPath);
	        
	        // Verify if the directory exists
	        if (!directory.exists() || !directory.isDirectory()) {
	            System.out.println("Directory does not exist: " + directoryPath);
	            return;
	        }
	        
	        // List all files in the directory
	        File[] files = directory.listFiles();
	        int count = files.length;
	        System.out.println(count);
	        
	        // Loop through each file in the directory
	        int num = 0;
	        for (File file : files) {
	            if (file.isFile()) {
	                // Process each file
	                System.out.println("File: " + file.getAbsolutePath());
	                MetarParser.parse(file.getAbsolutePath(), num);
	                num++;
	            }
	        }
			// BISMTRDIK
//			MetarParser.parse("C:\\work\\metar\\metarFiles\\DSMMTRCID2", 0);// CYSMTRBYG
		} catch (MetarParseException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Kept for backwards compatibility.
	 * 
	 * @param metarData The date string / metar report lines to parse
	 * @return The Metar representing this report
	 * @throws MetarParseException Error parsing the report
	 * @deprecated MetarParser.parse(String) deprecated, please use
	 *             MetarParser.parseRecord(String metarData)
	 */
	public static Metar parse(String metarData, int count) throws MetarParseException {
		Metar met = MetarParser.parseRecord(metarData);
		if(null != met) {
			met.print();
			System.out.println("----- Count ----- " + count);
		} else {
			System.out.println("-------------- Null Metar Record ------------ " + count + "\n" + metarData);
		}
		return met;
	}

	/**
	 * Parse both the date string and metar report as extracted from a METAR file.
	 * This method accepts the two values in a single string separated by a newline.
	 * This might be called if parsing the METAR source file using an empty line as
	 * a delimiter.
	 * 
	 * Example:
	 * 
	 * 2003/10/29 02:45 <-- Date String KUVA 290245Z AUTO 14003KT 7SM CLR 14/06
	 * A2984 RMK AO2 <-- Metar report
	 * 
	 * NOTE: The report date/time provided in the resulting object will be created
	 * using the following: 1) The day and time will be retrieved from the DDHHMMZ
	 * string. 2) If the dayOfMonth is greater than the current dayOfMonth - We'll
	 * assume the report was from last month and roll back to last month. If this
	 * involves rolling the year, we'll also do that. 3) Otherwise Year and Month
	 * will be current.
	 * 
	 * passed in the 'dateString' arg will be used to determine the year and month
	 * of this report. The day and time will be retreived from the the actual report
	 * string. Ex: 290245Z above will be 0245GMT on 29 Oct 2003
	 * 
	 * @param metarData The string containing both the date and the report.
	 * @return The Metar object describing the report
	 */
	public static Metar parseRecord(String metarData) throws MetarParseException {
		String[] dateString = null;
		ArrayList<String[]> datString = new ArrayList<String[]>();
		String metarString = null;
		StringBuffer metData = new StringBuffer();
		StringBuffer infoData = new StringBuffer();
		// Read all bytes from a file into a byte array
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(metarData));
			metarString = new String(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(metarString.contains("\n")) {
			StringTokenizer tokenizer = new StringTokenizer(metarString, "\n");
			boolean flag = false;
			while (tokenizer.hasMoreTokens()) {
				String line = tokenizer.nextToken();
				if (line.startsWith("METAR") || flag) {
					metData.append(line);
					String[] data = line.split(" ");
					datString.add(data);
//					System.out.println(line);
					flag = true;
				}
			}
			dateString = metData.toString().split(" ");
			Object[] info = datString.toArray();
			
			for (Object[] array : datString) {
	            for (Object element : array) {
	            	infoData.append(element.toString()).append(" "); // Convert element to string and append
	            }
	        }
			
			return parseReport(dateString[2], infoData.toString());
		}
		return null;
	}

	/**
	 * Parse the record date string that is found on the line above the Metar record
	 * in the standardd NOAA Metar data file.
	 * 
	 * 
	 * @param dateString The record date string
	 * @return The Date object representing the source date string
	 * @throws ParseException      Error parsing the date
	 * @throws MetarParseException A necessary component of the metar record was not
	 *                             parsed correctly
	 */
	public static Date parseRecordDateString(String dateString) throws MetarParseException {
		// Parse the date string
		// Set the timezone to UTC (Zulu time)
		// Get current year and month
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH); // Note: Calendar.MONTH is zero-based

		// Extract day, hour, minute from dateString
		int day = Integer.parseInt(dateString.substring(0, 2));
		int hour = Integer.parseInt(dateString.substring(2, 4));
		int minute = Integer.parseInt(dateString.substring(4, 6));

		// Create a Calendar instance for the specified date
		cal.clear(); // Clear all fields
		cal.setTimeZone(TimeZone.getTimeZone("UTC")); // Set time zone to UTC (Zulu time)
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, 0); // Optional: Set seconds to 0 if needed

		// Get the Date object from Calendar
		Date date = cal.getTime();

		// Output the parsed date
//		System.out.println("Parsed Date: " + date);

		// Format the Date object to display in UTC time
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'");
//        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//        String formattedDate = sdf.format(date);

		// Output the formatted date
//        System.out.println("Parsed Date in UTC: " + formattedDate);

		return date;
	}

	/**
	 * Parse both the date string and metar report as extracted from a METAR file.
	 * Example:
	 * 
	 * 2003/10/29 02:45 <-- Date String KUVA 290245Z AUTO 14003KT 7SM CLR 14/06
	 * A2984 RMK AO2 <-- Metar report
	 * 
	 * NOTE: The date passed in the 'dateString' arg will be used to determine the
	 * year and month of this report. The day and time will be retreived from the
	 * the actual report string. Ex: 290245Z above will be 0245GMT on 29 Oct 2003
	 * 
	 * @param dateString  The date string as found above each metar report in the
	 *                    NOAA METAR file.
	 * @param metarString The metar report as found in the NOAA METAR file.
	 * @return The Metar object describing the report
	 * @throws MetarParseException Unable to parse report
	 */
	public static Metar parseReport(String dateString, String metarString) throws MetarParseException {
		Date recordDate = parseRecordDateString(dateString);
		Metar metar = parseReport(metarString);
		metar.setDate(recordDate);
		metar.setDateString(dateString);
		return metar;
	}

	/**
	 * Parse metar report (no DateString line parsed) as extracted from a METAR
	 * file. Example:
	 * 
	 * KUVA 290245Z AUTO 14003KT 7SM CLR 14/06 A2984 RMK AO2 <-- Metar report
	 * 
	 * NOTE: The report date/time provided in the resulting object will be created
	 * using the following: 1) The day and time will be retrieved from the DDHHMMZ
	 * string. 2) If the dayOfMonth is greater than the current dayOfMonth - We'll
	 * assume the report was from last month and roll back to last month. If this
	 * involves rolling the year, we'll also do that. 3) Otherwise Year and Month
	 * will be current.
	 * 
	 * NOTE: If you later provide a date string to the Metar using setDate(), then
	 * that value will overwrite the value parsed here.
	 * 
	 * @param metarString A standard NOAA Metar report. This should be a single line
	 *                    of data. It should not include the record date as found
	 *                    one line above the metar report in the data file.
	 * @return The Metar object describing the report
	 * @throws MetarParseException Unable to parse report
	 */
	public static Metar parseReport(String metarString) throws MetarParseException {
		try {
			int index = 0;
			int numTokens = 0;
			String temp = null;
			Pattern pattern = Pattern.compile("\\d{2}");

			logger.debug("MetarParser: instantiated");

			if (metarString == null) {
				throw new MetarParseException("empty metar data");
			}

			logger.debug("MetarParser: raw: " + metarString);

			Metar metar = new Metar();

			// First action is to save the report string
			metar.setReportString(metarString);
			metarString = metarString.substring(metarString.indexOf("METAR") + 5, metarString.length());
			// Convert array to ArrayList
			List<String> tokens = new ArrayList<>(Arrays.asList(metarString.trim().split(" ")));

			// split the second line, the METAR data, on whitespace into tokens for processing
//            try {
//                utility.split(tokens, metarString);
//            } catch (Exception e) {
//                logger
//                        .error("MetarParser: error spliting metar data on whitespace: "
//                                + e);
//                throw new MetarParseException(
//                        "error spliting metar data on whitespace:", e);
//            }

			// the number of tokens we have
			numTokens = tokens.size();
			logger.debug("MetarParser: have '" + numTokens + "' tokens");

			// type of report should be present (METAR/SPECI)???

			// station id will always be present in
			// format: CCCC
			// CCCC - alphabetic characters only [a-zA-Z]
			metar.setStationID((String) tokens.get(index++));
			logger.debug("MetarParser: stationID: " + metar.getStationID());

			logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

			// date and time of the report
			// format: YYGGggZ
			// YY - date
			// GG - hours
			// gg - minutes
			// Z - Zulu (UTC)
			if (((String) tokens.get(index)).endsWith("Z")) {
				// Parse report date (Mandatory in Metar report)
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeZone(utcZone);

				String day = ((String) tokens.get(index)).substring(0, 2);
				String hour = ((String) tokens.get(index)).substring(2, 4);
				String minute = ((String) tokens.get(index)).substring(4, 6);

				int dayInt = -1;
				int hourInt = -1;
				int minuteInt = -1;
				try {
					dayInt = Integer.parseInt(day);
					hourInt = Integer.parseInt(hour);
					minuteInt = Integer.parseInt(minute);
				} catch (NumberFormatException nfExc) {
					String errMsg = "Unable to parse Metar date value: " + nfExc;
					logger.error(errMsg);
					throw new MetarParseException(errMsg, nfExc);
				}

				// Case where the month may have rolled. In this case, the
				// calendar should be
				// rolled back one day
				if (dayInt > calendar.get(Calendar.DAY_OF_MONTH)) {
					calendar.roll(Calendar.DAY_OF_MONTH, false);
				}

				calendar.set(Calendar.DAY_OF_MONTH, dayInt);
				calendar.set(Calendar.HOUR_OF_DAY, hourInt);
				calendar.set(Calendar.MINUTE, minuteInt);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);

				metar.setDate(calendar.getTime());

				// on to the next token
				if (index < numTokens - 1) {
					index++;
				}
				logger.debug("MetarParser: date: " + metar.getDate());
			} else {
				logger.debug("MetarParser: date: no date found");
				// unexpected token...should have been data in Zulu (UTC)
			}

			logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

			// report modifier
			// format: (AUTO or COR)
			// AUTO - fully automated with no human intervention or oversight
			// COR - corrected report
			if (((String) tokens.get(index)).equals(MetarConstants.METAR_AUTOMATED)
					|| ((String) tokens.get(index)).equals(MetarConstants.METAR_CORRECTED)) {
				metar.setReportModifier((String) tokens.get(index));
				// on to the next token
				if (index < numTokens - 1) {
					index++;
				}
				logger.debug("MetarParser: report modifier: " + metar.getReportModifier());
			} else {
				logger.debug("MetarParser: no report modifier");
			}

			logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

			// wind group (speed and direction)
			// format: dddff(f)Gff(f)KT_dddVddd
			// m m m n n n x x x
			// ddd - wind direction (may be VRB (variable))
			// ff(f) - wind speed
			// Gff(f) - wind gust speed
			// m m m
			// KT/KTS (or) MPS - knots (or) meters per second
			// d d d Vd d d - variable wind direction > 6 knots, degree=>degree
			// n n n x x x e.g. 180V210 => variable from 180deg to 210deg

			temp = (String) tokens.get(index); // Wind info
			if (temp.endsWith("KT") || temp.endsWith("KTS") || temp.endsWith("MPS") || temp.startsWith("VRB")) {
				int pos = 0;
				boolean windInKnots = false;

				// Note: There have been cases where wind started with VRB
				// and did not end with KT. This seems to only happen in the
				// US, so assuming knots.
				if (temp.endsWith("KT") || temp.endsWith("KTS") || (temp.startsWith("VRB") && !temp.endsWith("MPS"))) 
				{
					logger.debug("MetarParser: wind speed in knots");
					windInKnots = true;
				} else 
				{
					logger.debug("MetarParser: wind speed in meters per second");
				}

				if (!((String) tokens.get(index)).substring(0, 3).equals("VRB")) 
				{
					// we have gusts
					Integer windDirection = Integer.parseInt((tokens.get(index)).substring(0, 3));
					metar.setWindDirection(windDirection);
				} else 
				{
					logger.debug("MetarParser: variable wind direction <= 6 knots");
					metar.setWindDirectionIsVariable(true);
				}

				temp = (String) tokens.get(index);

				// Three digit wind only if not variable and has digit in
				// 5th position
				if (!metar.getWindDirectionIsVariable() && temp.length() >= 6 && Character.isDigit(temp.charAt(5))) 
				{
					// have three-digit wind speed
					logger.debug("MetarParser: have three-digit wind speed");
					if (windInKnots) {
						metar.setWindSpeed(Float.parseFloat((tokens.get(index)).substring(3, 6)));
					} else {
						metar.setWindSpeedInMPS(Float.parseFloat((tokens.get(index)).substring(3, 6)));
					}
					pos = 6;
				} else 
				{
					// have two-digit wind speed
					logger.debug("MetarParser: have two-digit wind speed");
					if (windInKnots) {
						metar.setWindSpeed(Float.parseFloat((tokens.get(index)).substring(3, 5)));
					} else {
						metar.setWindSpeedInMPS(Float.parseFloat((tokens.get(index)).substring(3, 5)));
					}
					pos = 5;
				}

				temp = (String) tokens.get(index);
				if (temp.length() > pos && temp.charAt(pos) == 'G') 
				{
					// we have wind gusts
					logger.debug("MetarParser: wind gusts");
					pos++;
					temp = temp.substring(pos, pos + 2); // Extracts 2 digits for gust
//					Matcher matcher = pattern.matcher(temp);
//					boolean isDigit = matcher.matches();
						try {
							temp = (String) tokens.get(index);
							temp = temp.substring(pos, pos + 4);
							Pattern pattern3 = Pattern.compile("\\d{3}");// match only 3 didgits
							Matcher matcher3 = pattern3.matcher(temp);
							boolean isMatch3 = matcher3.find();
							if (isMatch3) {
								System.out.println("Found three-digit wind speed: " + matcher3.group());
								// have three-digit wind speed
								logger.debug("MetarParser: have three-digit wind speed");
								if (windInKnots) {
									metar.setWindGusts(Float.parseFloat((tokens.get(index)).substring(pos, pos + 3)));
								} else {
									metar.setWindGustsInMPS(Float.parseFloat((tokens.get(index)).substring(pos, pos + 3)));
								}
							}
							temp = (String) tokens.get(index);
							temp = temp.substring(pos, pos + 4);
							Pattern pattern2 = Pattern.compile("\\d{2}");// match only 2 digits
							Matcher matcher2 = pattern2.matcher(temp);
							boolean isMatch2 = matcher2.find();
							if (isMatch2) {
								// have two-digit wind speed
								logger.debug("MetarParser: have two-digit wind speed");
								if (windInKnots) {
									metar.setWindGusts(Float.parseFloat((tokens.get(index)).substring(pos, pos + 2)));
								} else {
									metar.setWindGustsInMPS(Float.parseFloat((tokens.get(index)).substring(pos, pos + 2)));
								}
							}
						} catch (Exception e) {
							logger.error("MetarParser: error matching wind gusts: " + e);
						}
					} else {
						// we don't have gusts
						logger.debug("MetarParser: no gusts");
					}

					logger.debug("MetarParser: wind direction: " + metar.getWindDirection());
					if (windInKnots) {
						logger.debug("MetarParser: wind speed: " + metar.getWindSpeedInKnots());
					} else {
						logger.debug("MetarParser: wind speed: " + metar.getWindSpeedInMPS());
					}
					logger.debug("MetarParser: wind gusts: " + metar.getWindGustsInKnots());

					// on to the next token
					if (index < numTokens - 1) {
						index++;
					}
					logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))="
							+ ((String) tokens.get(index)));

					// if we have variable wind direction
					temp = ((String) tokens.get(index));
					try {
						Pattern patternVRB = Pattern.compile(".*\\d\\d\\dV\\d\\d\\d");
				        Matcher matcherVRB = patternVRB.matcher(temp);
						boolean ismatcherVRB = matcherVRB.find();
//                    if (matcher.matches(temp, new Perl5Compiler().compile(".*\\d\\d\\dV\\d\\d\\d")))
						if (matcherVRB.matches())
						{
							metar.setWindDirectionIsVariable(true);
							metar.setWindDirectionMin(Integer.parseInt((tokens.get(index)).substring(0, 3)));
							metar.setWindDirectionMax(Integer.parseInt((tokens.get(index)).substring(4, 7)));

							logger.debug("MetarParser: variable wind direction min: " + metar.getWindDirectionMin());
							logger.debug("MetarParser: variable wind direction max: " + metar.getWindDirectionMax());

							// on to the next token
							if (index < numTokens - 1) {
								index++;
							}
						}
					} catch (Exception e) {
						logger.error("MetarParser: error matching variable wind speed: " + e);
					}
				}//TODO ends the if 492
				else 
				{
					// unexpected token...should have been wind speed
					logger.debug("MetarParser: wind speed: not found");
				}

				logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))=" + (tokens.get(index)));

				// CAVOK
				//
				// Visibility greater than 10Km, no cloud below 5000 ft or
				// minimum
				// sector altitude, whichever is the lowest and no CB
				// (Cumulonimbus) or
				// over development and no significant weather.
				if ((tokens.get(index)).equals(MetarConstants.METAR_CAVOK)) {
					metar.setIsCavok(true);

					// on to the next token
					if (index < numTokens - 1) {
						index++;
					}
					// Horizontal visibility in meters
				} else if ((tokens.get(index)).equals("9999")) {
					metar.setVisibilityInKilometers(Float.valueOf(10));

					// on to the next token
					if (index < numTokens - 1) {
						index++;
					}

					// get visibility
					// format: (M)VVVVVSM
					// (M) - used to indicate less than
					// VVVVV - miles (00001SM)
					// SM - statute miles
				} else if ((tokens.get(index)).endsWith("SM")
						|| ((index + 1 < numTokens) && (tokens.get(index + 1)).endsWith("SM"))
						|| (tokens.get(index)).endsWith("KM")
						|| ((index + 1 < numTokens) && (tokens.get(index + 1)).endsWith("KM"))) 
				{
					logger.debug("MetarParser: visibility");

					String whole, fraction = "";
					Float visibility = null;
					boolean isLessThan = false;
					String token = (String) tokens.get(index);
					boolean visibilityInStatuteMiles = false;

					if ((tokens.get(index)).endsWith("SM") || ((index + 1 < numTokens) && (tokens.get(index + 1)).endsWith("SM"))) 
					{
						visibilityInStatuteMiles = true;
					}

					if (token.startsWith("M")) 
					{
						logger.debug("MetarParser: visibility: less than");
						isLessThan = true;
						token = token.substring(1, token.length());
					}

					if (token.endsWith("SM") || token.endsWith("KM")) 
					{
						if (token.indexOf('/') == -1) 
						{
							// no fractions to deal with
							whole = token.substring(0, token.length() - 2);
						} else 
						{
							whole = "0";
							fraction = token.substring(0, token.length() - 2);
						}
					} 
					else 
					{
						whole = token;
						// next token is the fraction part
						index++;
						fraction = ((String) tokens.get(index)).substring(0, ((String) tokens.get(index)).length() - 2);
					}

					visibility =  Float.parseFloat(whole);

                if (!fraction.equals("")) 
                {
                    // we have a fraction to convert
                    ArrayList<String> frac = new ArrayList<String>();
                    try {
                        String regex = "/"; 
                        Pattern patternFrac = Pattern.compile(regex);
                        String[] parts = patternFrac.split(fraction);

                        for (String part : parts) {
                            frac.add(part);
                        }
                    } catch (Exception e) {
                        logger.error("MetarParser: error spliting fraction on /: " + e);
                        throw new MetarParseException("error spliting fraction on /:", e);
                    }

                    visibility = Float.valueOf(visibility.floatValue()
                            + Float.valueOf(frac.get(0)).floatValue()
                            / Float.valueOf(frac.get(1)).floatValue());
                }

				if (visibilityInStatuteMiles) 
				{
						metar.setVisibility(visibility);
				} 
				else 
				{
						metar.setVisibilityInKilometers(visibility);
				}
					metar.setVisibilityLessThan(isLessThan);

					// on to the next token
				if (index < numTokens - 1) 
				{
						index++;
				}
					logger.debug("MetarParser: visibility: " + metar.getVisibility() + " M");
			} 
				else 
			{
					String token = (String) tokens.get(index);
					boolean isLessThan = false;
					
					Pattern patternVis = Pattern.compile("/^M?\\d+(N|NE|E|SE|S|SW|W|NW)?$/");
			        Matcher matcherVis = patternVis.matcher(token);
					if (matcherVis.matches()) {
//                if (utility.match("/^M?\\d+(N|NE|E|SE|S|SW|W|NW)?$/", token)) {
                    logger.debug("MetarParser: visibility");
                    
                    if (token.startsWith("M")) 
                    {
                        logger.debug("MetarParser: visibility: less than");
                        isLessThan = true;
                        token = token.substring(1, token.length());
                    }
                    
                    // Catch case where a direction is attached. This is 
                    // done in some countries.
                    // TODO: For now we don't actually store the direction
                    while(!Character.isDigit(token.charAt(token.length()-1)))
                    {
                      token = token.substring(0,token.length()-1);
                    }

                    metar.setVisibilityInMeters(Float.parseFloat(token));
                    metar.setVisibilityLessThan(isLessThan);

                    // on to the next token
                    if (index < numTokens - 1) 
                    {
                        index++;
                    }
                } else {
                    // unexpected token...should have been visibility
                    logger.debug("MetarParser: visibility: not found");
                }
			}

				logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

				// see if we have a Runaway Visual Range Group token
				// format: RD D /V V V V FT or RD D /V V V V VV V V V FT
				// r r r r r r r r n n n n x x x x
				// R - runway number follows
				// D D - runway number
				// r r
				// (D ) - runway approach directions
				// r L (left), R (right), C (center)
				// (M/P) - M (less than 0600FT), P (greater than 6000FT)
				// V V V V - (lowest) visual range, constant reportable value
				// r r r r
				// V - separates lowest/highest visual range
				// V V V V - (highest) visual range, constant reportable value
				// x x x x
				// FT - feet
				//
				while (((String) tokens.get(index)).startsWith("R")) {
					// check that first character after the R is a digit. this helps
					// qualify this as a real RVR. Otherwise we could be grabbing
					// the
					// wx descriptor 'RA'
					if (!Character.isDigit(((String) tokens.get(index)).charAt(1))) 
					{
						break;
					}

					logger.debug("MetarParser: found RVR");

					// we have a runway visual range
					RunwayVisualRange runwayVisualRange = new RunwayVisualRange();

					// get our runway number
					runwayVisualRange.setRunwayNumber(Integer.parseInt((tokens.get(index)).substring(1, 3)));
					logger.debug("MetarParser: RVR runway number: " + Integer.parseInt((tokens.get(index)).substring(1, 3)));

					int pos = 3;
					if (((String) tokens.get(index)).charAt(pos) != '/') 
					{
						runwayVisualRange.setApproachDirection(((String) tokens.get(index)).charAt(pos));
						logger.debug("MetarParser: RVR runway approach direction: "
								+ ((String) tokens.get(index)).charAt(pos));
						pos += 2; // increment past the '/'
					} 
					else 
					{
						pos++;
					}

					// determine if we have a modifier for above 6000ft or below
					// 600ft
					switch (((String) tokens.get(index)).charAt(pos)) {
					case 'P': // below 600ft
					case 'M': // above 6000ft
						runwayVisualRange.setReportableModifier(((String) tokens.get(index)).charAt(pos));
						logger.debug("MetarParser: RVR modifier: " + ((String) tokens.get(index)).charAt(pos));
						pos++;
					}
					runwayVisualRange.setLowestReportable(Integer.parseInt((tokens.get(index)).substring(pos, pos + 4)));
					logger.debug("MetarParser: RVR lowest reportable: " + Integer.parseInt((tokens.get(index)).substring(pos, pos + 4)));
					pos += 4;
					// if we are using the format with highest reportable
					if (((String) tokens.get(index)).charAt(pos) == 'V') {
//						char y = ((String) tokens.get(index)).charAt(pos);
						pos++; // increment past V
						pos++; //added this so it only gets the 4 digits
//						String x = (tokens.get(index)).substring(pos, pos + 4);
						runwayVisualRange.setHighestReportable(Integer.parseInt((tokens.get(index)).substring(pos, pos + 4)));
						logger.debug("MetarParser: RVR highest reportable: " + Integer.parseInt((tokens.get(index)).substring(pos, pos + 4)));
					}

					metar.addRunwayVisualRange(runwayVisualRange);

					// on to the next token
					if (index < numTokens - 1) {
						index++;
					} else if (index == numTokens - 1)
						return metar;

				}

				logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

				// weather groups
				// format: (+/-)ddpp
				// (+/-) - intensity, light (-), moderate (default), heavy (+)
				// dd - descriptor, qualifier/adjective for phenomena
				// pp - phenomena (rain, hail, tornado, etc.)
				// we know we have a weather group if the token starts with one of:
				// _________________________________________________________________________________
				// | Intensity | Descriptor | Precipitation | Obscuration | Other |
				// +------------+---------------+------------------+---------------+---------------+
				// | - Light | MI Shallow | DZ Drizzle | BR Mist | PO Well- |
				// | Moderate | PR Partial | RA Rain | FG Fog | Developed |
				// | + Heavy | BC Patches | SN Snow | FU Smoke | Dust/Sand |
				// | | DR Low | SG Snow Grains | VA Volcanic | Whirls |
				// | | Drifting | IC Ice Crystals | Ash | SQ Squalls |
				// | | BL Blowing | PL Ice Pellets | DU Widespread | FC Funnel |
				// | | SH Shower(s) | GR Hail | Dust | Cloud, |
				// | | TS Thunder- | GS Small Hail | SA Sand | Tornado, |
				// | | storm | and/or | HZ Haze | Waterspout |
				// | | FZ Freezing | Snow Pellets | PY Spray | SS Sandstorm |
				// | | | UP Unknown | | DS Duststorm |
				// | | | Precipitation | | |
				// +------------+---------------+------------------+---------------+---------------+
				while (((String) tokens.get(index)).startsWith(MetarConstants.METAR_HEAVY)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_LIGHT)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SHALLOW)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_PARTIAL)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_PATCHES)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_LOW_DRIFTING)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_BLOWING)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SHOWERS)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_THUNDERSTORMS)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_FREEZING)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_DRIZZLE)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_RAIN)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SNOW)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SNOW_GRAINS)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_ICE_CRYSTALS)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_ICE_PELLETS)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_HAIL)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SMALL_HAIL)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_UNKNOWN_PRECIPITATION)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_MIST)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_FOG)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SMOKE)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_VOLCANIC_ASH)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_WIDESPREAD_DUST)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SAND)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_HAZE)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SPRAY)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_DUST_SAND_WHIRLS)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SQUALLS)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_FUNNEL_CLOUD)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SAND_STORM)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_DUST_STORM)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_NO_SIGNIFICANT_CHANGE)) {
					logger.debug("MetarParser: found weather groups");

                int pos = 0;

					// we have a weather condition
					WeatherCondition weatherCondition = new WeatherCondition();

					if (((String) tokens.get(index)).startsWith(MetarConstants.METAR_HEAVY)
							|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_LIGHT)) {
						weatherCondition.setIntensity(String.valueOf(((String) tokens.get(index)).charAt(0)));
						logger.debug(
								"MetarParser: weather group: intensity: " + ((String) tokens.get(index)).charAt(0));
						pos++;
					} else {
						logger.debug("MetarParser: weather group: intensity: moderate");
					}

					// if we have a descriptor
					if (((String) tokens.get(index)).substring(pos, pos + 2).startsWith(MetarConstants.METAR_SHALLOW)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.startsWith(MetarConstants.METAR_PARTIAL)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.startsWith(MetarConstants.METAR_PATCHES)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.startsWith(MetarConstants.METAR_LOW_DRIFTING)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.startsWith(MetarConstants.METAR_BLOWING)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.startsWith(MetarConstants.METAR_SHOWERS)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.startsWith(MetarConstants.METAR_THUNDERSTORMS)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.startsWith(MetarConstants.METAR_FREEZING)) 
					{
						weatherCondition.setDescriptor(((String) tokens.get(index)).substring(pos, pos + 2));
						logger.debug("MetarParser: weather group: descriptor: "
								+ ((String) tokens.get(index)).substring(pos, pos + 2));
						pos += 2;
					} 
					else 
					{
						logger.debug("MetarParser: weather group: descriptor: no descriptor");
					}

					if (((String) tokens.get(index)).length() < pos + 2)
						logger.debug("MetarParser: weather group: no phenomena");
					else
					// if we have phenomena (we should always!)
					if (((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_DRIZZLE)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_RAIN)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_SNOW)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_SNOW_GRAINS)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_ICE_CRYSTALS)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_ICE_PELLETS)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_HAIL)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_SMALL_HAIL)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_UNKNOWN_PRECIPITATION)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_MIST)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_FOG)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_SMOKE)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_VOLCANIC_ASH)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_WIDESPREAD_DUST)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_SAND)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_HAZE)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_SPRAY)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_DUST_SAND_WHIRLS)
							|| ((String) tokens.get(index)).substring(pos, pos + 2).equals(MetarConstants.METAR_SQUALLS)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_FUNNEL_CLOUD)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_SAND_STORM)
							|| ((String) tokens.get(index)).substring(pos, pos + 2)
									.equals(MetarConstants.METAR_DUST_STORM)) 
					{
						weatherCondition.setPhenomena(((String) tokens.get(index)).substring(pos, pos + 2));
						logger.debug("MetarParser: weather group: phenomena: "
								+ ((String) tokens.get(index)).substring(pos, pos + 2));
						metar.addWeatherCondition(weatherCondition);
						logger.debug("MetarParser: " + weatherCondition.getNaturalLanguageString());
					} 
					else 
					{
						logger.debug("MetarParser: weather group: no phenomena");
					}

					// on to the next token
					if (index < numTokens - 1) 
					{
						index++;
					} else if (index == numTokens - 1)
						return metar;
				}

				logger.debug(
						"MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

				// sky condition
				// format: NNNhhh or VVhhh or CLR/SKC
				// NNN - amount of sky cover
				// hhh - height of layer (in hundreds of feet above the surface)
				// VV - vertical visibility, indefinite ceiling
				// SKC - clear skies (reported by manual station)
				// CLR - clear skies (reported by automated station)
				while (((String) tokens.get(index)).startsWith(MetarConstants.METAR_VERTICAL_VISIBILITY)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SKY_CLEAR)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_CLEAR)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_FEW)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SCATTERED)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_BROKEN)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_OVERCAST)
						|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_NO_SIGNIFICANT_CLOUDS)) 
				{
					 logger.debug("MetarParser: found sky conditions");

					// we have a sky condition
					SkyCondition skyCondition = new SkyCondition();

					if (((String) tokens.get(index)).startsWith(MetarConstants.METAR_FEW)
							|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_SCATTERED)
							|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_BROKEN)
							|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_OVERCAST)) 
					{
						skyCondition.setContraction(((String) tokens.get(index)).substring(0, 3));
						logger.debug("MetarParser: sky condition: contraction: "
								+ ((String) tokens.get(index)).substring(0, 3));
						skyCondition.setHeight(Integer.parseInt((tokens.get(index)).substring(3, 6)));
						logger.debug(
								"MetarParser: sky condition: height: " + ((String) tokens.get(index)).substring(3, 6));
						if (((String) tokens.get(index)).length() > 6) {
							// we have a modifier
							skyCondition.setModifier(
									((String) tokens.get(index)).substring(6, ((String) tokens.get(index)).length()));
							logger.debug("MetarParser: sky condition: modifier: "
									+ ((String) tokens.get(index)).substring(6, ((String) tokens.get(index)).length()));
						}
					} else if (((String) tokens.get(index)).startsWith(MetarConstants.METAR_SKY_CLEAR)
							|| ((String) tokens.get(index)).startsWith(MetarConstants.METAR_CLEAR)) 
					{
						skyCondition.setContraction(((String) tokens.get(index)).substring(0, 3));
						// logger.debug("MetarParser: sky condition: clear: "
						// + ((String) tokens.get(index)).substring(0, 3));
					} else if (((String) tokens.get(index)).startsWith(MetarConstants.METAR_VERTICAL_VISIBILITY)) 
					{
						skyCondition.setContraction(((String) tokens.get(index)).substring(0, 2));
						logger.debug("MetarParser: sky condition: contraction: "
								+ ((String) tokens.get(index)).substring(0, 2));
						skyCondition.setHeight(Integer.parseInt((tokens.get(index)).substring(2, 5)));
						logger.debug(
								"MetarParser: sky condition: height: " + ((String) tokens.get(index)).substring(2, 5));
					} else if (((String) tokens.get(index)).startsWith(MetarConstants.METAR_NO_SIGNIFICANT_CLOUDS)) 
					{
						skyCondition.setContraction(((String) tokens.get(index)).substring(0, 3));
						logger.debug("MetarParser: sky condition: No Significant Clouds: "
								+ ((String) tokens.get(index)).substring(0, 3));
					} else 
					{
						logger.debug("MetarParser: sky condition: couldn't determine which");
					}

					metar.addSkyCondition(skyCondition);
					// logger.debug("MetarParser: "
					// + skyCondition.getNaturalLanguageString());

					// on to the next token
					if (index < numTokens - 1) 
					{
						index++;
					} else if (index == numTokens - 1)
						return metar;
				}

				logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

				// temperature / dew point
				// format: (M)T'T'/(M)T' T'
				// d d
				// (M) - sub-zero temperature
				// T'T' - temerature (in celsius)
				// T' T' - dew point (in celsius)
				// d d
				//
				// TF = ( 9 / 5 ) x TC + 32 (conversion from celsius to fahrenheit)
				if (((String) tokens.get(index)).indexOf("/") != -1) {
					logger.debug("MetarParser: found temperature");
					ArrayList<String> temps = new ArrayList<String>();

                try {
                	Pattern pat = Pattern.compile("/");
                	Matcher matcherT = pat.matcher(tokens.get(index));
//                	String[] parts = pat.split(tokens.get(index));
                	int start = 0;
                    while (matcherT.find()) {
                        String part = tokens.get(index).substring(start, matcherT.start());
                        temps.add(part);
                        start = matcherT.end();
                    }
                    // Add the last part of the string
                    temps.add(tokens.get(index).substring(start));
//                    utility.split(temps, "/\\//", ((String) tokens.get(index)));
                } 
                catch (Exception e) 
                {
                    logger.error("MetarParser: error spliting temperature on /: " + e);
                    throw new MetarParseException("error spliting temperature on /: " + e, e);
                }

					// we have a sub-zero temperature
					Float temperature = null;

					// Temperature is missing from report
					if (temps.size() == 0 || ((String) temps.get(0)).length() == 0) {
						metar.setTemperature(null);
					} else if (((String) temps.get(0)).startsWith("M")) {
						temperature = Float.parseFloat((temps.get(0)).substring(1, 3));
						temperature = (temperature.floatValue() - temperature.floatValue() * 2); // negate
						metar.setTemperature(temperature);
					} else {
						temperature = Float.parseFloat(((String) temps.get(0)));
						metar.setTemperature(temperature);
					}

					if (temperature != null)
						logger.debug("MetarParser: temperature: " + temperature + " C, "
								+ (temperature.floatValue() * 9 / 5 + 32) + " F");

					// Investigate dewpoint
					Float dewPoint = null;

					// DewPoint is missing from report
					if (temps.size() < 2 || ((String) temps.get(1)).length() == 0) {
						metar.setDewPoint(null);
					} else if (((String) temps.get(1)).startsWith("M")) {
						dewPoint = Float.parseFloat((temps.get(1)).substring(1, 3));
						dewPoint = (dewPoint.floatValue() - dewPoint.floatValue() * 2); // negate
						metar.setDewPoint(dewPoint);
					} else {
						dewPoint = Float.parseFloat((temps.get(1)));
						metar.setDewPoint(dewPoint);
					}

					if (dewPoint != null)
						logger.debug("MetarParser: dew point: " + dewPoint + " C, "
								+ (dewPoint.floatValue() * 9 / 5 + 32) + " F");

					// on to the next token
					if (index < numTokens - 1) {
						index++;
					}
				} else {
					logger.debug("MetarParser: temperature/dew point not found");
					metar.setTemperature(null);
					metar.setDewPoint(null);
				}

				logger.debug(
						"MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

				// altimeter
				// get pressure, which is reported in hundreths
				//
				// format: AP P P P
				// h h h h
				// A - altimeter in inches of mercury
				// P P P P - tens, units, tenths and hundreths inches mercury
				// h h h h (no decimal point coded)
				if (((String) tokens.get(index)).startsWith("A")) {
					Float pressure = Float.parseFloat((tokens.get(index)).substring(1, 5));
					// correct for no decimal point
					pressure = (pressure.floatValue() / 100);
					metar.setPressure(pressure);

					logger.debug("MetarParser: pressure: " + metar.getPressure() + " Hg");

					// on to the next token
					if (index < numTokens - 1) {
						index++;
					} else if (index == numTokens - 1)
						return metar;
				}
				// Alternative pressure (HPa/mB) (HectoPascal/Millbar)
				// QPPPP - QNH
				// Format: "QPPPP" -> Q - indicator for QNH, PPPP - Pressure value.
				// Measured in hecto Pascal (HPa), 1 Hpa = 1 mB(millibar)
				else if (((String) tokens.get(index)).startsWith("Q")) {
					Float pressure = Float.parseFloat((tokens.get(index)).substring(1, 5));

					logger.debug("MetarParser: pressure: " + pressure + " hPa");

					// Convert to inHg
					pressure = (pressure.floatValue() * .02953F);
					metar.setPressure(pressure);

					logger.debug("MetarParser: pressure: " + metar.getPressure() + " Hg");

					// on to the next token
					if (index < numTokens - 1) {
						index++;
					}

				} else {
					logger.debug("MetarParser: pressure not found");
				}

				logger.debug(
						"MetarParser: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index)));

				// remarks
				if (!((String) tokens.get(index)).equals(MetarConstants.METAR_REMARKS)) {
					// we have no remarks
					logger.debug("MetarParser: we have no remarks");
				} else {
					logger.debug("MetarParser: we have remarks");
					index++;
				}

				// ---------------------------------------------------------------
				// "BECMG" Section
				// Desc: Some non-NOAA reports use the identifier "BECMG" as a
				// method to describe a future trend. Almost like bringing
				// some TAF data into a METAR. Some countries seem to have
				// this section well defined, while others seem to be free
				// form.
				// Pre Contract:
				// None, we're just going to search for "BECMG" directly
				// after the altimeter setting
				// Post Contract:
				// We will accumulate all tokens until either end of report
				// or until we encounter a "RMK" token.
				// TODO: Determine method for safely parsing the BECMG section. It
				// seems to be too freeform to do it?
				// ---------------------------------------------------------------
				if (index < numTokens && MetarConstants.METAR_BECOMING.equalsIgnoreCase((String) tokens.get(index))) {
					StringBuffer sb = new StringBuffer();

					logger.debug("MetarParser: processing \"BECMG\" section");
					// BECMG seems to be consistently terminated by a remark
					while (index < numTokens
							&& !MetarConstants.METAR_REMARKS.equalsIgnoreCase((String) tokens.get(index))) {

						if (sb.length() > 0)
							sb.append(" ");

						logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))="
								+ ((String) tokens.get(index)));
						sb.append((String) tokens.get(index));
						++index;
					}

					metar.setBecoming(sb.toString());
				}

				// remarks
				// -------
				// volcanic eruptions
				// funnel cloud
				// type of automated station (A01/A02)
				// A01 - stations without a precipitation descriminator
				// A02 - stations with a precipitation descriminator
				// peak wind, PK_WND_dddff(f)/(hh)mm
				// wind shift, WSHFT_(hh)mm (FROPA)
				// tower or surface visibility
				// variable prevailing visbility
				// sector visbility
				// visbility at second location
				// lightning
				// beginning and ending of precipitation
				// beginning and ending of thunderstorms
				// thunderstorm location
				// hailstone size
				// virga
				// variable ceiling height
				// obscurations
				// variable sky condition
				// significant cloud types
				// ceiling height at second location
				// pressure rising or falling rapidly
				// sea-level pressure
				// aircraft mishap
				// no SPECI reports taken
				// snow increasing rapidly
				// other significant information

				// additive data
				// -------------
				// precipitation
				// cloud types
				// duration of sunshine

				// hourly temperature and dewpoint
				// format: Ts T'T'T's T' T' T'
				// n n d d d
				// T - group indicator
				// s - sign of the temperature (1=sub-zero, 0=zero+)
				// n
				// T'T'T' - temperature
				// T' T' T' - dew point
				// d d d
				//
				// see if we have hourly temperature
				while (index < numTokens) {
					logger.debug("MetarParser: processing ((String)tokens.get(" + index + "))="
							+ ((String) tokens.get(index)));

					// if we have temperature
					temp = (String) tokens.get(index);
                try {
                	Pattern patternTemp = Pattern.compile("T\\d{8}");
			        Matcher matcherTemp = patternTemp.matcher(temp);
					if (matcherTemp.matches()) {
                        logger.debug("MetarParser: found detailed temp: " + (tokens.get(index)));
                        // we have a sub-zero temperature
                        Float temperaturePrecise = Float.parseFloat((tokens.get(index)).substring(2, 5));
                        if (((String) tokens.get(index)).charAt(1) == '1') {
                            temperaturePrecise = (temperaturePrecise.floatValue() - temperaturePrecise.floatValue() * 2); // negate
                        }
                        // it is in tenths
                        temperaturePrecise = (temperaturePrecise.floatValue() / 10);
                        metar.setTemperaturePrecise(temperaturePrecise);

                        // we have a sub-zero dew point
                        Float dewPointPrecise = Float.parseFloat((tokens.get(index)).substring(6, 9));
                        if (((String) tokens.get(index)).charAt(5) == '1') {
                            dewPointPrecise = (dewPointPrecise.floatValue() - dewPointPrecise.floatValue() * 2); // negate
                        }
                        // it is in tenths
                        dewPointPrecise = (dewPointPrecise.floatValue() / 10);
                        metar.setDewPointPrecise(dewPointPrecise);

                        logger.debug("MetarParser: temperature precise: "
                                        + temperaturePrecise
                                        + " C, "
                                        + (temperaturePrecise.floatValue() * 9 / 5 + 32)
                                        + " F");
                        logger.debug("MetarParser: dew point precise: "
                                + dewPointPrecise + " C, "
                                + (dewPointPrecise.floatValue() * 9 / 5 + 32)
                                + " F");
                        // if we have an obscuration
                    } else if (((String) tokens.get(index))
                            .equals(MetarConstants.METAR_MIST)
                            || ((String) tokens.get(index))
                                    .equals(MetarConstants.METAR_FOG)
                            || ((String) tokens.get(index))
                                    .equals(MetarConstants.METAR_SMOKE)
                            || ((String) tokens.get(index))
                                    .equals(MetarConstants.METAR_VOLCANIC_ASH)
                            || ((String) tokens.get(index))
                                    .equals(MetarConstants.METAR_WIDESPREAD_DUST)
                            || ((String) tokens.get(index))
                                    .equals(MetarConstants.METAR_SAND)
                            || ((String) tokens.get(index))
                                    .equals(MetarConstants.METAR_HAZE)
                            || ((String) tokens.get(index))
                                    .equals(MetarConstants.METAR_SPRAY)) {
                        // we have an obscuration
                        Obscuration obscuration = new Obscuration();
                        obscuration.setPhenomena(((String) tokens.get(index)));
                        logger.debug("MetarParser: weather group: phenomena: " + (tokens.get(index)));

                        // move to quantity and height token
                        index++;

                        // we have a quantity and height too
                        if (((String) tokens.get(index))
                                .startsWith(MetarConstants.METAR_FEW)
                                || ((String) tokens.get(index))
                                        .startsWith(MetarConstants.METAR_SCATTERED)
                                || ((String) tokens.get(index))
                                        .startsWith(MetarConstants.METAR_BROKEN)
                                || ((String) tokens.get(index))
                                        .startsWith(MetarConstants.METAR_OVERCAST)) {
                            obscuration.setContraction(((String) tokens.get(index)).substring(0, 3));
                            obscuration.setHeight(Integer.parseInt(((tokens.get(index)).substring(3, 6))));

                            metar.addObscuration(obscuration);
                            logger.debug("MetarParser: " + obscuration.getNaturalLanguageString());
                        }

                        index++;
                        // there has been no significant change in weather
                    } else if (((String) tokens.get(index))
                            .equals(MetarConstants.METAR_NO_SIGNIFICANT_CHANGE)) {
                        // have no significant change
                        metar.setIsNoSignificantChange(true);
                    }
                } catch (Exception e) {
                    logger
                            .error("MetarParser: error matching additional remarks: "
                                    + e);
				}

				index++;
			}

			logger.debug("MetarParser: done processing metar data");

			// 6-hourly maximum temperature
			// 6-hourly minimum temperature
			// 24-hour maximum and minimum temperature
			// 3-hourly pressure tendency

			return metar;
		} catch (Exception exc) {
			String err = "Uncaught Exception during parse. Report:  " + metarString + " Cause: " + exc.toString();
			logger.error(err);
			throw new MetarParseException(err, exc);
		}
	}

	protected static String getTokenString(ArrayList tokens, int index) {
		return "Metar: processing ((String)tokens.get(" + index + "))=" + ((String) tokens.get(index));
	}
}
