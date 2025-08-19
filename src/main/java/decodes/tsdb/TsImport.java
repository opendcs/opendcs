package decodes.tsdb;

import java.io.IOException;
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
                try
                {
                    Collection<CTimeSeries> dc = importer.readTimeSeriesFile(filename);
                    for(CTimeSeries cts : dc)
                    {
                        String tsid = cts.getTimeSeriesIdentifier().getUniqueString();
                        log.info("Saving time series {}", tsid);
                        try
                        {
                            timeSeriesDAO.saveTimeSeries(cts);
                        }
                        catch(DbIoException | BadTimeSeriesException ex)
                        {
                            log.atWarn()
                               .setCause(ex)
                               .log("Cannot save time series '{}'", tsid);
                        }
                    }
                }
                catch (IOException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Error reading {} -- skipping", filename);
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
