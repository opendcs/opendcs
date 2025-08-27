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
package decodes.datasource;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.util.PropertySpec;
import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.PropertiesUtil;
import ilex.var.Variable;


/**
 * Designed for MB Hydro use of the dd.weatheroffice.gc.ca web site.
 * Provide a directory URL with embedded times in it. Build the URL and list the files
 * contained in the directory. Parse the file names for mediumIDs I'm interested in.
 * File names also contain the message time stamp.
 * 
 * https://dd.weather.gc.ca/bulletins/alphanumeric/20240326/CA/CWAO/00/ was used to verify behavior
 * of this source as best as possible. We suspect the agency has changed the directory structure of 
 * the data and we are not currently aware of expectation. Please contact the OpenDCS team if you 
 * need this working and can communicate the current expectations whether it's for the above link
 * or another source of data following a similar design.
 */
public class WebDirectoryDataSource extends DataSourceExec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public final String module = "WebDirectory";
	
	private String directoryUrl = null;
	private String urlFieldDelimiter = "_";
	private TimeZone urlTimeZone = TimeZone.getTimeZone("UTC");
	private String urlTimeFormat = "ddHHmm";
	private static final Pattern HTML_LINK_PATTERN = Pattern.compile(".*<a.*href=\"(?<link>.*?)\".*/?>.*");
	
	/** Position of the time within the file name (0=no time, 1=1st position) */
	private int urlTimePos = 3;
	/** Position of the station ID within the file name (0=no ID, 1=1st position) */
	private int urlIdPos = 5;
	
	private static PropertySpec[] myPropSpecs =
	{
		new PropertySpec("directoryUrl", PropertySpec.STRING,
			"(required) URL of the directory that lists the file names containing messages"),
		new PropertySpec("urlFieldDelimiter", PropertySpec.STRING,
			"(default = underscore) Delimiter for fields within the filenames."),
		new PropertySpec("urlTimePos", PropertySpec.INT,
			"(default=3) Position of time within the delimited file name (1=first pos)"),
		new PropertySpec("urlIdPos", PropertySpec.INT,
			"(default=5) Position of the transport medium ID within the file name (1=first pos)"),
		new PropertySpec("urlTimeFormat", PropertySpec.STRING,
				"(default = HHmmss) SimpleDateFormat format string for time in the filename"),
		new PropertySpec("urlTimeZone", PropertySpec.STRING,
				"(default = UTC) Time Zone for the time within the directory and file names")
	};
	
	private SimpleDateFormat fnSdf = new SimpleDateFormat(urlTimeFormat);
	private Date dSince = null, dUntil = null;
	private ArrayList<NetworkList> rsNetlists = new ArrayList<NetworkList>();
	private static final long MS_PER_DAY = 3600L * 24L * 1000L;
	private String currentDirUrl = null;
	private LinkedList<String> fileList = new LinkedList<String>();
	private Calendar nextDirectoryCal = Calendar.getInstance();
	private Calendar fileTimeCal = Calendar.getInstance();
	private SimpleDateFormat debugSdf = new SimpleDateFormat("yyyy/MMM/dd-HH:mm:ss");

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param source data source
	 * @param db database
	 */
	public WebDirectoryDataSource(DataSource source, Database db) {
		super(source, db);
	}


	@Override
	public void processDataSource() 
		throws InvalidDatabaseException
	{
	}

	@Override
	public void init(Properties rsProps, String since, String until, Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		directoryUrl = PropertiesUtil.getIgnoreCase(rsProps, "directoryUrl");
		if (directoryUrl == null)
		{
			directoryUrl = PropertiesUtil.getIgnoreCase(dbDataSource.arguments, "directoryUrl");
			if (directoryUrl == null)
				throw new DataSourceException(module + ": missing required 'directoryUrl' property.");
		}
		String s;
		if ((s = PropertiesUtil.getIgnoreCase(rsProps, "urlFieldDelimiter")) != null)
		{
			urlFieldDelimiter = s;
		}
		if ((s = PropertiesUtil.getIgnoreCase(rsProps, "urlTimePos")) != null)
		{
			try { urlTimePos = Integer.parseInt(s.trim()); }
			catch(Exception ex)
			{
				throw new DataSourceException(module + ": bad urlTimePos property '" + s 
					+ "' (must be integer)", ex);
			}
		}
		if ((s = PropertiesUtil.getIgnoreCase(rsProps, "urlIdPos")) != null)
		{
			try { urlIdPos = Integer.parseInt(s.trim()); }
			catch(Exception ex)
			{
				throw new DataSourceException(module + ": bad urlIdPos property '" + s 
					+ "' (must be integer)", ex);
			}
		}
		if ((s = PropertiesUtil.getIgnoreCase(rsProps, "urlTimeFormat")) != null)
		{
			urlTimeFormat = s;
			fnSdf = new SimpleDateFormat(urlTimeFormat);
		}
		if ((s = PropertiesUtil.getIgnoreCase(rsProps, "urlTimeZone")) != null)
		{
			urlTimeZone = TimeZone.getTimeZone(s);
		}
		
		if (since != null)
		{
			try { dSince = IDateFormat.parse(since); }
			catch(Exception ex)
			{
				throw new DataSourceException(module + ": bad since time '" + since + "'", ex);
			}
		}
		else // default to last 24 hours
		{
			dSince = new Date(System.currentTimeMillis() - MS_PER_DAY);
		}
		if (until != null)
		{
			try { dUntil = IDateFormat.parse(until); }
			catch(Exception ex)
			{
				throw new DataSourceException(module + ": bad until time '" + until + "'", ex);
			}
		}
		else // default to 'now'
			dUntil = new Date();
		
		nextDirectoryCal.setTimeZone(urlTimeZone);
		fileTimeCal.setTimeZone(urlTimeZone);
		fnSdf.setTimeZone(urlTimeZone);
		debugSdf.setTimeZone(urlTimeZone);
		
		nextDirectoryCal.setTimeInMillis(dSince.getTime());
		// Truncate to hour.
		nextDirectoryCal.set(Calendar.MINUTE, 0);
		nextDirectoryCal.set(Calendar.SECOND, 0);
		nextDirectoryCal.set(Calendar.MILLISECOND, 0);
		// Subtract an hour because readNextDirectory increments before reading.
		nextDirectoryCal.add(Calendar.HOUR_OF_DAY, -1);
		
		log.debug("since={}, next={}" , dSince, nextDirectoryCal.getTime());
		
		if (networkLists != null)
			for(NetworkList nl : networkLists)
				rsNetlists.add(nl);
	}

	@Override
	public void close()
	{
		// No resources left open.
	}

	@Override
	public RawMessage getRawMessage() 
		throws DataSourceException
	{
		String filename;
		while ((filename = getNextFile()) != null)
		{
			log.debug("filename '{}'", filename);
			
			// Parse the ID and date/time from the file name
			String fields[] = filename.split(urlFieldDelimiter);
			if (fields == null || fields.length < urlIdPos)
			{
				log.warn("bad filename in directory '{}' -- no id field in position {}", filename, urlIdPos);
				continue;
			}
			String id = fields[urlIdPos - 1];
			if (fields == null || fields.length < urlTimePos)
			{
				log.warn("Bad filename in directory '{}' -- no time field in position {}", filename, urlTimePos);
				continue;
			}
			Date fileTime = null;
			try
			{
				fileTime = fnSdf.parse(fields[urlTimePos-1]);
			}
			catch (ParseException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Filename '{}' has unparsable time in position ",
					    filename, urlTimePos);
				continue;
			}
			
			// File contains day hour minute. Construct full time from directory year/month.
			fileTimeCal.setTime(fileTime);
			fileTimeCal.set(Calendar.YEAR, nextDirectoryCal.get(Calendar.YEAR));
			fileTimeCal.set(Calendar.MONTH, nextDirectoryCal.get(Calendar.MONTH));
			// A file might arrive late, just after midnight. If file hour is 23 and directory hour is 00,
			// then subtract a day to the previous day.
			if (fileTimeCal.get(Calendar.HOUR_OF_DAY) > nextDirectoryCal.get(Calendar.HOUR_OF_DAY))
				fileTimeCal.add(Calendar.DAY_OF_YEAR, -1);
			log.debug("\tparsed fileTime={}, corrected fileTime={}", fileTime, fileTimeCal.getTime()); 
			fileTime = fileTimeCal.getTime();
			
			// Check to see if this ID is in one of my network lists if not, continue;
			boolean found = false;
			String mediumType = null;;
			for(NetworkList netlist : rsNetlists)
				if (netlist.getEntry(id) != null)
				{
					found = true;
					mediumType = netlist.transportMediumType;
					break;
				}
			if (!found)
			{
				log.debug("Filename '{}' skipped because ID '{}' is not in network lists.", filename, id);
				continue;
			}
			
			// Read the file into memory and build RawMessage
			String fileUrl = currentDirUrl 
				+ (currentDirUrl.endsWith("/") ? "" : "/")
				+ filename;
			InputStream istrm = null;
			BufferedInputStream bis = null;
			ByteArrayOutputStream baos = null;
			try
			{
				URL url = new URL(fileUrl);
				bis = new BufferedInputStream(url.openStream());
				baos = new ByteArrayOutputStream();
				byte buf[] = new byte[4096];
				int len;
				while ((len = bis.read(buf)) > 0)
					baos.write(buf, 0, len);
				
				if ((len = baos.size()) == 0)
				{
					if (!found)
					{
						log.debug("Url '{}' resulted in an empty file -- skipped.", fileUrl);
						continue;
					}
				}
				RawMessage ret = new RawMessage(baos.toByteArray(), len);
				
				// Set the Performance Measurements
				ret.setPM(GoesPMParser.MESSAGE_TIME, new Variable(fileTime));
				ret.setPM(GoesPMParser.DCP_ADDRESS, new Variable(id));
				ret.setTimeStamp(fileTime);
				ret.setMediumId(id);

				Platform platform = 
					Database.getDb().platformList.getPlatform(mediumType, id, fileTime);
				if (platform != null)
				{
					ret.setPlatform(platform);
					ret.setTransportMedium(platform.getTransportMedium(mediumType));
				}
				else if (!getAllowNullPlatform())
				{
					throw new UnknownPlatformException(module + " " + mediumType + ":" + id);
				}
				
				return ret;
			}
			catch (MalformedURLException ex)
			{
				log.atWarn().setCause(ex).log("bad URL '{}'", fileUrl);
				continue;
			}
			catch (IOException ex)
			{
				log.atWarn().setCause(ex).log("Error reading URL '{}'", fileUrl);
				continue;
			}
			catch (DatabaseException ex)
			{
				log.atWarn().setCause(ex).log("Error looking up platform for TM '{}:{}'", mediumType, id);
				continue;
			}
			catch (UnknownPlatformException ex)
			{
				throw ex;
			}
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log(" Unexpected exception reading URL '{}'", fileUrl);
				continue;
			}
			finally
			{
				if (baos != null)
					try { baos.close(); } catch(Exception ex) {}
				if (bis != null)
					try { bis.close(); } catch(Exception ex) {}
				if (istrm != null)
					try { istrm.close(); } catch(Exception ex) {}
			}
		}
		throw new DataSourceEndException("No more files to read.");
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return myPropSpecs;
	}

	/**
	 * Cycle through the list in memory. If empty read the next directory URL.
	 * Account for directory URLs that may contain no files (skip them).
	 * @return
	 */
	private String getNextFile()
	{
		while (fileList.isEmpty())
			if (!readNextDirectory())
				return null;
		return fileList.isEmpty() ? null : fileList.removeFirst();
	}
	
	/**
	 * Increment next Directory Time and build directory URL. Read it into memory
	 * and parse the anchor tags that contain file names. Place these
	 * @return false if end of archive is reached, true if a directory was read.
	 */
	private boolean readNextDirectory()
	{
		nextDirectoryCal.add(Calendar.HOUR_OF_DAY, 1);
		Date nextDirectoryTime = nextDirectoryCal.getTime();
		if (nextDirectoryTime.after(dUntil))
			return false;
		
		log.debug("Reading directory for time {}", nextDirectoryTime);
		
		
		Properties urlProps = new Properties();
		urlProps.setProperty("TZ", urlTimeZone.getID());
		currentDirUrl = EnvExpander.expand(directoryUrl, urlProps, nextDirectoryTime);
		try
		{
			log.debug("Reading URL '{}'", currentDirUrl);
			URL dirUrl = new URL(currentDirUrl);
			
			try (InputStream input = dirUrl.openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(input)))
			{
				String line = null;
				while((line = reader.readLine()) != null)
				{
					Matcher m = HTML_LINK_PATTERN.matcher(line);
					if (m.matches())
					{
						fileList.add(m.group("link"));
					}
				}
			}

			return true;
		}
		catch(MalformedURLException ex)
		{
			log.atWarn().setCause(ex).log("Bad URL '{}", currentDirUrl);
			return false;
		}
		catch(FileNotFoundException ex)
		{
			log.atWarn().setCause(ex).log("FileNotFound reading URL '{}'", currentDirUrl);
			// Sometimes the depot skips an hour. Keep going unless we're past the until time.
			return nextDirectoryTime.before(dUntil);
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Error reading URL '{}'", currentDirUrl);
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Unexpected exception reading URL '{}'", currentDirUrl);
		}
		
		return false;
	}
	
	@Override
	public boolean supportsTimeRanges()
	{
		return true;
	}


}
