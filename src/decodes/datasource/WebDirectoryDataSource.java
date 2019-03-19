package decodes.datasource;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.util.PropertySpec;
import hec.util.TextUtil;
import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.var.Variable;

import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.html.test.SimpleUserAgentContext;


/**
 * Designed for MB Hydro use of the d.weatheroffice.gc.ca web site.
 * Provide a directory URL with embedded times in it. Build the URL and list the files
 * contained in the directory. Parse the file names for mediumIDs I'm interested in.
 * File names also contain the message time stamp.
 */
public class WebDirectoryDataSource extends DataSourceExec
{
	public final String module = "WebDirectory";
	
	private String directoryUrl = null;
	private String urlFieldDelimiter = "_";
	private TimeZone urlTimeZone = TimeZone.getTimeZone("UTC");
	private String urlTimeFormat = "ddHHmm";
	
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
					+ "' (must be integer)");
			}
		}
		if ((s = PropertiesUtil.getIgnoreCase(rsProps, "urlIdPos")) != null)
		{
			try { urlIdPos = Integer.parseInt(s.trim()); }
			catch(Exception ex)
			{
				throw new DataSourceException(module + ": bad urlIdPos property '" + s 
					+ "' (must be integer)");
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
				throw new DataSourceException(module + ": bad since time '" + since + "': " + ex);
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
				throw new DataSourceException(module + ": bad until time '" + until + "': " + ex);
			}
		}
		else // default to 'now'
			dUntil = new Date();
		
//		if ((s = PropertiesUtil.getIgnoreCase(rsProps, "urlDirectoryInterval")) != null)
//		{
//			try { urlDirectoryInterval = Integer.parseInt(s.trim()); }
//			catch(Exception ex)
//			{
//				throw new DataSourceException(module + ": bad urlDirectoryInterval property '" + s 
//					+ "' (must be integer)");
//			}
//		}

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
		
		Logger.instance().debug1(module + " since=" + debugSdf.format(dSince) + ", next=" 
			+ debugSdf.format(nextDirectoryCal.getTime()));
		
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
			Logger.instance().debug1(module + " filename '" + filename + "'");
			
			// Parse the ID and date/time from the file name
			String fields[] = filename.split(urlFieldDelimiter);
			if (fields == null || fields.length < urlIdPos)
			{
				Logger.instance().warning(module + " bad filename in directory '" + filename
					+ "' -- no id field in position " + urlIdPos);
				continue;
			}
			String id = fields[urlIdPos - 1];
			if (fields == null || fields.length < urlTimePos)
			{
				Logger.instance().warning(module + " bad filename in directory '" + filename
					+ "' -- no time field in position " + urlTimePos);
				continue;
			}
			Date fileTime = null;
			try
			{
				fileTime = fnSdf.parse(fields[urlTimePos-1]);
			}
			catch (ParseException e)
			{
				Logger.instance().warning(module + " filename '" + filename 
					+ "' has unparsable time in position " + urlTimePos);
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
Logger.instance().debug1("\tparsed fileTime=" + debugSdf.format(fileTime) + ", corrected fileTime=" 
+ debugSdf.format(fileTimeCal.getTime()));
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
				Logger.instance().debug1(module + " filename '" + filename 
					+ "' skipped because ID '" + id + "' is not in network lists.");
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
						Logger.instance().debug1(module + " url '" + fileUrl 
							+ "' resulted in an empty file -- skipped.");
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
				Logger.instance().warning(module + " bad URL '" + fileUrl + "': " + ex);
				continue;
			}
			catch (IOException ex)
			{
				Logger.instance().warning(module + " Error reading URL '" + fileUrl + "': " + ex);
				continue;
			}
			catch (DatabaseException ex)
			{
				Logger.instance().warning(module + " Error looking up platform for TM " 
						+ mediumType + "':" + id + ": " + ex);
				continue;
			}
			catch (UnknownPlatformException ex)
			{
				throw ex;
			}
			catch (Exception ex)
			{
				Logger.instance().warning(module + " Unexpected exception reading URL '" 
						+ fileUrl + "':" + ex);
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
		
		Logger.instance().debug1(module + " reading directory for time " + debugSdf.format(nextDirectoryTime));
		
		Properties urlProps = new Properties();
		urlProps.setProperty("TZ", urlTimeZone.getID());
		currentDirUrl = EnvExpander.expand(directoryUrl, urlProps, nextDirectoryTime);
		InputStream istrm = null;
		Reader ireader = null;
		try
		{
			Logger.instance().debug1(module + " reading URL '" + currentDirUrl + "'");
			URL dirUrl = new URL(currentDirUrl);
			URLConnection urlCon = dirUrl.openConnection();
			istrm = urlCon.getInputStream();
			ireader = new InputStreamReader(istrm);
			InputSource isrc = new InputSourceImpl(ireader, currentDirUrl);
			UserAgentContext uaCtx = new SimpleUserAgentContext();
			DocumentBuilderImpl docBldr = new DocumentBuilderImpl(uaCtx);
			Document dirDoc = docBldr.parse(isrc);
			isrc.getClass();
			
			NodeList anchorNodes = dirDoc.getElementsByTagName("a");
			for (int idx = 0; idx < anchorNodes.getLength(); idx++)
			{
				Node node = anchorNodes.item(idx);
				Node hrefAttr = node.getAttributes().getNamedItem("href");
				if (hrefAttr == null)
				{
//Logger.instance().debug1(module + " skipping anchor '" + node.getNodeValue() 
//+ "' because no href attr.");
					continue;
				}
				
				// For the file nodes we want, the href attribute is the same as the anchor content.
				if (TextUtil.equals(node.getTextContent(), hrefAttr.getNodeValue()))
					fileList.add(node.getTextContent());
//else
//Logger.instance().debug1(module + " skipping anchor with href='" + hrefAttr.getNodeValue() 
//+ "' diff from content '" + node.getTextContent() + "'");
			}

//Logger.instance().debug1("Read new file list: ");
//if (fileList.isEmpty()) Logger.instance().debug1("\tEmpty");
//else for(String s : fileList) Logger.instance().debug1("\t" + s);
			return true;
		}
		catch(MalformedURLException ex)
		{
			Logger.instance().warning(module + " Bad URL '" + currentDirUrl + "': " + ex);
			return false;
		}
		catch(FileNotFoundException ex)
		{
			Logger.instance().warning(module + " FileNotFound reading URL '" + currentDirUrl + "': " + ex);
			// Sometimes the depot skips an hour. Keep going unless we're past the until time.
			return nextDirectoryTime.before(dUntil);
		}
		catch(IOException ex)
		{
			Logger.instance().warning(module + " Error reading URL '" + currentDirUrl + "': " + ex);
		} 
		catch (SAXException ex)
		{
			Logger.instance().warning(module + " Error parsing data from URL '" + currentDirUrl + "': " + ex);
		}
		catch(Exception ex)
		{
			String msg = module + " Unexpected exception reading URL '" + currentDirUrl + "': " + ex;
			Logger.instance().warning(msg);
			PrintStream ps = Logger.instance().getLogOutput();
			if (ps != null)
				ex.printStackTrace(ps);
			
		}
		finally
		{
			if (ireader != null)
				try { ireader.close(); } catch(Exception ex) {}
			if (istrm != null)
				try { istrm.close(); } catch(Exception ex) {}
		}
		
		return false;
	}
	
	@Override
	public boolean supportsTimeRanges()
	{
		return true;
	}


}
