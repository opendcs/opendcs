/**
 * $Id: TsImport.java,v 1.12 2020/05/08 13:18:54 mmaloney Exp $
 * 
 * $Log: TsImport.java,v $
 * Revision 1.12  2020/05/08 13:18:54  mmaloney
 * Handle bad data line better.
 *
 * Revision 1.11  2020/05/07 13:54:14  mmaloney
 * Lazy creation of tsids. This allows more flexibility in setting units.
 *
 * Revision 1.10  2020/03/02 13:55:24  mmaloney
 * Final bug fixes for OpenTSDB Computations
 *
 * Revision 1.9  2019/05/07 18:47:06  mmaloney
 * dev
 *
 * Revision 1.8  2017/10/10 18:25:03  mmaloney
 * Added support for TsdbFormatter
 *
 * Revision 1.7  2016/09/29 18:54:37  mmaloney
 * CWMS-8979 Allow Database Process Record to override decodes.properties and
 * user.properties setting. Command line arg -Dsettings=appName, where appName is the
 * name of a process record. Properties assigned to the app will override the file(s).
 *
 * Revision 1.6  2016/06/07 22:02:17  mmaloney
 * app name defaults to "utility" this is necessary for HDB.
 *
 * Revision 1.5  2015/05/14 13:52:18  mmaloney
 * RC08 prep
 *
 * Revision 1.4  2015/01/16 16:11:04  mmaloney
 * RC01
 *
 * Revision 1.3  2015/01/15 19:25:45  mmaloney
 * RC01
 *
 * Revision 1.2  2014/08/22 17:23:04  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.8  2012/09/17 19:59:03  mmaloney
 * dev
 *
 * Revision 1.7  2012/09/17 19:50:08  mmaloney
 * Added SET:UNITS command.
 *
 * Revision 1.6  2012/08/13 17:56:44  mmaloney
 * dev
 *
 * Revision 1.5  2012/08/13 17:49:11  mmaloney
 * dev
 *
 * Revision 1.4  2012/08/13 15:32:21  mmaloney
 * dev
 *
 * Revision 1.3  2012/08/13 15:22:03  mmaloney
 * Needed makeEmptyTsId method so that TsImport can create a time-series if it doesn't
 * already exist.
 *
 * Revision 1.2  2012/08/09 15:41:57  mmaloney
 * Use makeTimeSeries in TSDB.
 *
 * Revision 1.1  2012/08/08 19:35:49  mmaloney
 * Created.
 *
 * 
 * Open Source Software
 */
package decodes.tsdb;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import decodes.cwms.CwmsTimeSeriesDAO;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.hdb.HdbTsId;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;
import lrgs.gui.DecodesInterface;

/**
 * Import time series data from a file.
 * 
 * This utility provides an easy way of importing data into the time-series
 * database without setting up DECODES.
 * <p>
 * The file must have the following format:
 * <pre>
 *   File := Line*   (zero or more lines)
 *   Line := TsIdLine | SetLine | DataLine | CommentLine
 *   SetLine := 'SET:' ParmName '=' ParmValue
 *   ParmName := 'TZ' | 'SITENAMETYPE' | 'UNITS'
 *   ParmValue := STRING (appropriate data type for the ParmName being set) 
 *   TsIdLine := 'TSID:' TimeSeriesID
 *   TimeSeriesID := STRING (defined by underlying database)
 *   DataLine := DateTime ',' Value ',' Flags TrailingComment
 *   DateTime := (date and time in format yyyy/MM/dd-HH:mm:ss
 *   Value := NUMBER
 *   Flags := INTEGER  (flag bit-fields appropriate for underlying database)
 *   TrailingComment := '#' STRING
 *   CommentLine := '#' STRING   (blank and comment lines are ignored)
 * </pre>
 * <p>
 * TsIdLines and SetLines apply to all the following data lines.
 * Example: SET:TZ=MST   This means that all the DateTimes in the following
 * DataLines are to be interpreted in MST. (Without this, all date/times are
 * interpreted in the "sqlTimeZone" setting in decodes.properties.
 * <p>
 * Example: Set:SITENAMETYPE=NWHB5 means that all site names in subsequent
 * Time Series Identifiers are of the specified type.
 * <p>
 * Each file is processed separately, all data read, all time series written
 * to database, etc., before continuing to the next file.
 * 
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class TsImport extends TsdbAppTemplate
{
	public static final String module = "TsImport";
	/** One or more input files specified on end of command line */
	private StringToken filenameArg = new StringToken("", "input-file", "", 
		TokenOptions.optArgument|TokenOptions.optRequired
		|TokenOptions.optMultiple, "");
	private BooleanToken noUnitConvArg = new BooleanToken("U", "Pass file units directly to CWMS", "",
		TokenOptions.optSwitch, false);

	private SimpleDateFormat dataSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	
	private String filename = null;
	private int lineNum = 0;
	private ArrayList<CTimeSeries> timeSeries = new ArrayList<CTimeSeries>();
	private String siteNameType = null;
	private String units = null;
	private CTimeSeries currentTS = null;
	private SiteDAI siteDAO = null;
	TimeSeriesDAI timeSeriesDAO = null;
	private String tsidStr = null;


	public TsImport()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}

	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(noUnitConvArg);
		cmdLineArgs.addToken(filenameArg);
		appNameArg.setDefaultValue("utility");
	}

	@Override
	protected void runApp()
	{
		siteDAO = theDb.makeSiteDAO();
		timeSeriesDAO = theDb.makeTimeSeriesDAO();
		dataSdf.setTimeZone(TimeZone.getTimeZone(
			DecodesSettings.instance().sqlTimeZone));
		siteNameType = DecodesSettings.instance().siteNameTypePreference;
		CwmsTimeSeriesDAO.setNoUnitConv(noUnitConvArg.getValue());
		
		for(int n = filenameArg.NumberOfValues(), i=0; i<n; i++)
		{
			filename = filenameArg.getValue(i);
			info("Processing '" + filename + "'");
			LineNumberReader lnr = null;
			lineNum = 0;
			try
			{
				lnr = new LineNumberReader(new FileReader(filename));
				String line = null;
				beginFile();
				while((line = lnr.readLine()) != null)
				{
					lineNum = lnr.getLineNumber();
					processLine(line);
				}
				endOfFile();
			}
			catch(DbIoException ex)
			{
				warning("Error in Database Interface: " + ex);
			}
			catch(IOException ex)
			{
				warning("I/O Error: " + ex);
			}
			finally
			{
				if (lnr != null)
					try { lnr.close(); } catch(Exception ex) {}
			}
		}
		siteDAO.close();
		timeSeriesDAO.close();
	}

	/**
	 * Called after filename is successfully opened.
	 */
	private void beginFile()
	{
		timeSeries.clear();
	}
	
	/**
	 * Process a line from the file
	 * @param line with any termination char stripped
	 */
	private void processLine(String line)
		throws DbIoException
	{
		line = line.trim();
		if (line.length() == 0 || line.charAt(0) == '#')
			return; // blank  or comment line
		if (TextUtil.startsWithIgnoreCase(line, "SET:"))
		{
			int idx = line.indexOf('=');
			if (idx == -1)
			{
				warning("'SET:' line with no '=' -- ignored.");
				return;
			}
			String nm = line.substring(4, idx).trim();
			String val = line.substring(idx+1).trim();
			paramSet(nm, val);
		}
		else if (TextUtil.startsWithIgnoreCase(line, "TSID:"))
		{
			currentTS = null;
			tsidStr = line.substring(5).trim();
			if (tsidStr.length() == 0)
				warning("TSID line with no Time Series Identifier -- ignored.");
		}
		else if (!Character.isDigit(line.charAt(0)))
		{
			warning("File '" + filename + "' line " + lineNum + ": expected data line but got '"
				+ line + "' -- skipped.");
		}
		else
		{
			if (currentTS == null)
				instantiateTsid();
			processDataLine(line);
		}
	}

	/**
	 * Called when EOF is reached.
	 */
	private void endOfFile()
		throws DbIoException
	{
		lineNum = -1;
		for(CTimeSeries cts : timeSeries)
		{
			String tsid = cts.getTimeSeriesIdentifier().getUniqueString();
			info("Saving time series " + tsid);
			try { timeSeriesDAO.saveTimeSeries(cts); }
			catch(BadTimeSeriesException ex)
			{
				warning("Cannot save time series '" + tsid + "': " + ex);
			}
		}
	}
	
	private void paramSet(String paramName, String paramValue)
	{
		if (paramName.equalsIgnoreCase("TZ"))
		{
			TimeZone tz = TimeZone.getTimeZone(paramValue);
			if (tz != null)
				dataSdf.setTimeZone(tz);
		}
		else if (paramName.equalsIgnoreCase("SITENAMETYPE"))
			siteNameType = paramValue;
		else if (paramName.equalsIgnoreCase("UNITS"))
		{
			units = paramValue;
			if (currentTS != null)
				currentTS.setUnitsAbbr(units);
		}
		else if (paramName.equalsIgnoreCase("DATEFORMAT"))
		{
			dataSdf.applyPattern(paramValue);
		}
		else
			warning("Unrecognized param name '" + paramName + "' -- ignored.");
	}

	private void instantiateTsid()
		throws DbIoException
	{
		try
		{
			timeSeries.add(currentTS = theDb.makeTimeSeries(tsidStr));
		}
		catch (NoSuchObjectException ex)
		{
			warning("No existing time series. Will attempt to create.");
			TimeSeriesIdentifier tsid = theDb.makeEmptyTsId();
			try
			{
				tsid.setUniqueString(tsidStr);
				Site site = theDb.getSiteById(siteDAO.lookupSiteID(tsid.getSiteName()));
				if (site == null)
				{
					site = new Site();
					site.addName(new SiteName(site, Constants.snt_CWMS, tsid.getSiteName()));
					siteDAO.writeSite(site);
				}
				tsid.setSite(site);
				if (units != null)
					tsid.setStorageUnits(units);
				info("Calling createTimeSeries");
				timeSeriesDAO.createTimeSeries(tsid);
				info("After createTimeSeries, ts key = " + tsid.getKey());
				timeSeries.add(currentTS = theDb.makeTimeSeries(tsidStr));
			}
			catch(Exception ex2)
			{
				String msg = "No such time series and cannot create for '" + tsidStr + "': " + ex2;
				failure(msg);
				PrintStream out = Logger.instance().getLogOutput();
				if (out != null)
					ex2.printStackTrace(out);
				System.err.println(msg);
				ex2.printStackTrace();
				currentTS = null;
			}
		}
		if (currentTS != null && units != null)
			currentTS.setUnitsAbbr(units);
	}
	
	private void processDataLine(String line)
	{
		// If there was a problem parsing the TSID, there will be no currentTS.
		// Then just skip the data.
		if (currentTS == null)
			return;
		
		int hash = line.indexOf('#');
		if (hash != -1)
			line = line.substring(0, hash).trim();

		String x[] = line.split(",");
		if (x.length < 2)
		{
			warning("Unparsable data line '" + line + "' -- ignored");
			return;
		}
		TimedVariable tv = new TimedVariable(0.0);
		try { tv.setTime(dataSdf.parse(x[0].trim())); }
		catch(Exception ex)
		{
			warning("Unparsable date field '" + x[0] + "' -- line ignored.");
			return;
		}
		try { tv.setValue(Double.parseDouble(x[1].trim())); }
		catch(Exception ex)
		{
			warning("Unparsable value field '" + x[1] + "' -- line ignored.");
			return;
		}
		if (x.length > 2)
		{
			String flags = x[2].trim();
			try
			{
				if (TextUtil.startsWithIgnoreCase(flags, "0x"))
					tv.setFlags(Integer.parseInt(flags.substring(2), 16));
				else
					tv.setFlags(Integer.parseInt(flags));
			}
			catch(Exception ex)
			{
				warning("Unparsable flags field '" + x[0] + "' -- flags assumed to be 0.");
				return;
			}
		}
		VarFlags.setToWrite(tv);
		currentTS.addSample(tv);
	}

	@Override
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.initDecodes(cmdLineArgs.getPropertiesFile());
		Database.getDb().presentationGroupList.read();
	}

	
	public static void main(String args[])
		throws Exception
	{
		TsImport app = new TsImport();
		app.execute(args);
	}
	
	public void info(String msg)
	{
		Logger.instance().info(module + " " + msg);
	}
	
	public void warning(String msg)
	{
		super.warning(" " + filename 
			+ (lineNum <= 0 ? " " : "(" + lineNum + ") ") + msg);
	}
}
