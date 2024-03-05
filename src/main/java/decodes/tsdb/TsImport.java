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

import static org.slf4j.helpers.Util.getCallingClass;

import java.util.Collection;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import decodes.cwms.CwmsTimeSeriesDAO;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
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
	private static final Logger log = LoggerFactory.getLogger(TsImport.class.getName());
	public static final String module = "TsImport";
	/** One or more input files specified on end of command line */
	private StringToken filenameArg = new StringToken("", "input-file", "", 
		TokenOptions.optArgument|TokenOptions.optRequired
		|TokenOptions.optMultiple, "");
	private BooleanToken noUnitConvArg = new BooleanToken("U", "Pass file units directly to CWMS", "",
		TokenOptions.optSwitch, false);

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
		try (SiteDAI siteDAO = theDb.makeSiteDAO();
			 TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
			)
		{
			DecodesSettings settings = DecodesSettings.instance();
			CwmsTimeSeriesDAO.setNoUnitConv(noUnitConvArg.getValue());
			final TimeZone tz = TimeZone.getTimeZone(settings.sqlTimeZone);

			TsImporter importer = new TsImporter(tz, settings.siteNameTypePreference, (tsIdStr) ->
			{
				try
				{
					return timeSeriesDAO.getTimeSeriesIdentifier(tsIdStr);
				}
				catch (NoSuchObjectException ex)
				{
					log.warn("No existing time series. Will attempt to create.");
					
					try
					{
						TimeSeriesIdentifier tsId = theDb.makeEmptyTsId();
						tsId.setUniqueString(tsIdStr);
						Site site = theDb.getSiteById(siteDAO.lookupSiteID(tsId.getSiteName()));
						if (site == null)
						{
							site = new Site();
							site.addName(new SiteName(site, Constants.snt_CWMS, tsId.getSiteName()));
							siteDAO.writeSite(site);
						}
						tsId.setSite(site);
						
						log.info("Calling createTimeSeries");
						timeSeriesDAO.createTimeSeries(tsId);
						log.info("After createTimeSeries, ts key = {}", tsId.getKey());
						return tsId;
					}
					catch(Exception ex2)
					{
						throw new DbIoException(String.format("No such time series and cannot create for '%'", tsIdStr), ex);
					}
				}
			});
			for(int n = filenameArg.NumberOfValues(), i=0; i<n; i++)
			{

				final String filename = filenameArg.getValue(i);
				Collection<CTimeSeries> dc = importer.readTimeSeriesFile(filename);
				for(CTimeSeries cts : dc)
				{
					String tsid = cts.getTimeSeriesIdentifier().getUniqueString();
					info("Saving time series " + tsid);
					try
					{
						timeSeriesDAO.saveTimeSeries(cts);
					}
					catch(DbIoException | BadTimeSeriesException ex)
					{
						warning("Cannot save time series '" + tsid + "': " + ex);
					}
				}
			}
		}
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
}
