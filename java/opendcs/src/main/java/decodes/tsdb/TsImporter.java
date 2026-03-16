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
package decodes.tsdb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.util.functional.ThrowingFunction;

/**
 * Utility class to handle importing OpenDCS import/export time-series files
 */
public class TsImporter
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private Map<String, CTimeSeries> dc = new HashMap<>();
    private int lineNum = -1;
    private CTimeSeries currentTS = null;
    private String tsidStr = null;
    private String filename = null;
    private String siteNameType = null;
    private String units = null;
    private SimpleDateFormat dataSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    private final ThrowingFunction<String,TimeSeriesIdentifier, DbIoException> makeTsIdFunc;
    private Collection<String> selectedTsIds = null;

    public TsImporter(TimeZone tz, String siteNameType, ThrowingFunction<String, TimeSeriesIdentifier, DbIoException> makeTsFunc)
    {
        dataSdf.setTimeZone(tz);
        this.siteNameType = siteNameType;
        this.makeTsIdFunc = makeTsFunc;
    }

    public Collection<CTimeSeries> readTimeSeriesFile(String filename) throws FileNotFoundException, IOException
    {
        this.filename = filename;
        try (InputStream is = new FileInputStream(filename))
        {
            return readTimeSeriesFile(is);
        }
    }

    /**
     * Scan a file to extract all TSID identifiers without loading the data.
     * @param filename the file to scan
     * @return a collection of TSID strings found in the file
     */
    public static Collection<String> scanForTsIds(String filename) throws FileNotFoundException, IOException
    {
        Map<String, String> tsidsFound = new HashMap<>();
        try (LineNumberReader lnr = new LineNumberReader(new FileReader(filename)))
        {
            String line = null;
            while((line = lnr.readLine()) != null)
            {
                line = line.trim();
                if (TextUtil.startsWithIgnoreCase(line, "TSID:"))
                {
                    String tsidStr = line.substring(5).trim();
                    if (tsidStr.length() > 0)
                    {
                        tsidsFound.put(tsidStr, tsidStr);
                    }
                }
            }
        }
        return tsidsFound.values();
    }

    /**
     * Read time series file but only import the specified TSIDs.
     * @param filename the file to read
     * @param selectedTsIds the TSIDs to import (null or empty means import all)
     * @return a collection of CTimeSeries for the selected TSIDs
     */
    public Collection<CTimeSeries> readTimeSeriesFile(String filename, Collection<String> selectedTsIds)
        throws FileNotFoundException, IOException
    {
        this.filename = filename;
        try (InputStream is = new FileInputStream(filename))
        {
            return readTimeSeriesFile(is, selectedTsIds);
        }
    }

    public Collection<CTimeSeries> readTimeSeriesFile(InputStream stream) throws IOException
    {
        return readTimeSeriesFile(stream, null);
    }

    public Collection<CTimeSeries> readTimeSeriesFile(InputStream stream, Collection<String> selectedTsIds) throws IOException
    {
        if (this.filename == null)
        {
            this.filename = "Provided InputStream";
        }

        this.selectedTsIds = selectedTsIds;
        log.info("Processing '{}'", filename);
        LineNumberReader lnr = null;
        lineNum = 0;
        try (Reader reader = new InputStreamReader(stream))
        {
            lnr = new LineNumberReader(reader);
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
            log.warn("Error in Database Interface.", ex);
        }
        catch(IOException ex)
        {
            log.warn("I/O Error.", ex);
        }
        finally
        {
            if (lnr != null)
                try { lnr.close(); } catch(Exception ex) {}
        }
        return dc.values();
    }

    /**
     * Called after filename is successfully opened.
     */
    private void beginFile()
    {
        dc.clear();
    }

    /**
     * Called when EOF is reached.
     */
    private void endOfFile() throws DbIoException
    {
        currentTS = null;
        lineNum = -1;
    }

    /**
     * Process a line from the file
     * @param line with any termination char stripped
     */
    private void processLine(String line) throws DbIoException
    {
        line = line.trim();
        if (line.length() == 0 || line.charAt(0) == '#')
        {
            return; // blank  or comment line
        }
        if (TextUtil.startsWithIgnoreCase(line, "SET:"))
        {
            int idx = line.indexOf('=');
            if (idx == -1)
            {
                log.warn("'SET:' line with no '=' -- ignored.");
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
            {
                log.warn("TSID line with no Time Series Identifier -- ignored.");
            }
        }
        else if (!Character.isDigit(line.charAt(0)))
        {
            log.warn("File '{}:{}': expected data line but got '{}' -- skipped.", filename, lineNum, line);
        }
        else
        {
            if (currentTS == null)
            {
                instantiateTsId();
            }
            processDataLine(line);
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
            {
                currentTS.setUnitsAbbr(units);
            }
        }
        else if (paramName.equalsIgnoreCase("DATEFORMAT"))
        {
            dataSdf.applyPattern(paramValue);
        }
        else
        {
            log.warn("Unrecognized param name '{}' -- ignored.", paramName);
        }
    }

    private void instantiateTsId() throws DbIoException
    {
        // If selective import is enabled and this TSID is not selected, skip it
        if (selectedTsIds != null && !selectedTsIds.isEmpty() && !selectedTsIds.contains(tsidStr))
        {
            currentTS = null;
            return;
        }

        currentTS = dc.computeIfAbsent(tsidStr, (key) ->
        {
            try
            {
                TimeSeriesIdentifier tsId = makeTsIdFunc.accept(key);
                return new CTimeSeries(tsId);
            }
            catch (DbIoException ex)
            {
                log.atError()
                   .setCause(ex)
                   .log("Unable to Initialize current time series {}", key);
                throw new RuntimeException("Cannot find or create time series " + key, ex);
            }
        });
        if (currentTS != null && units != null)
        {
            currentTS.setUnitsAbbr(units);
        }
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
            log.warn("Unparsable data line '{}' -- ignored", line);
            return;
        }
        TimedVariable tv = new TimedVariable(0.0);
        try
        {
            tv.setTime(dataSdf.parse(x[0].trim()));
        }
        catch(Exception ex)
        {
            log.warn("Unparsable date field '{}' -- line ignored.", x[0] );
            return;
        }
        try
        {
            final String trimmed = x[1].trim();
            if (!"m".equalsIgnoreCase(trimmed))
            {
                tv.setValue(Double.parseDouble(x[1].trim()));    
            }
            else
            {
                tv.setValue(Double.NEGATIVE_INFINITY);
            }
            
        }
        catch(Exception ex)
        {
            log.warn("Unparsable value field '{}' -- line ignored.", x[1]);
            return;
        }
        if (x.length > 2)
        {
            String flags = x[2].trim();
            try
            {
                int numericFlags;
                if (TextUtil.startsWithIgnoreCase(flags, "0x"))
                {
                    numericFlags = Integer.parseInt(flags.substring(2), 16);
                }
                else if (TextUtil.startsWithIgnoreCase(flags, "0b"))
                {
                    numericFlags = Integer.parseInt(flags.substring(2), 2);
                }
                else
                {
                    numericFlags = (Integer.parseInt(flags));
                }
                tv.setFlags(numericFlags);
            }
            catch(Exception ex)
            {
                log.warn("Unparsable flags field '{}' -- flags assumed to be 0.", x[0]);
                return;
            }
        }
        VarFlags.setToWrite(tv);
        currentTS.addSample(tv);
    }

    /**
     * Create a function that will get or create a TimeSeriesIdentifier.
     * This is a helper method that can be used by both TsImport and GUI code.
     *
     * @param theDb the database connection
     * @param siteDAO the site DAO
     * @param timeSeriesDAO the time series DAO
     * @return a function that gets or creates TSIDs
     */
    public static ThrowingFunction<String, TimeSeriesIdentifier, DbIoException>
        makeGetOrCreateTsIdFunction(TimeSeriesDb theDb, SiteDAI siteDAO, TimeSeriesDAI timeSeriesDAO)
    {
        return (tsIdStr) ->
        {
            try
            {
                return timeSeriesDAO.getTimeSeriesIdentifier(tsIdStr);
            }
            catch (NoSuchObjectException ex)
            {
                log.warn("No existing time series '{}'. Will attempt to create.", tsIdStr);

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
                        log.info("Created new site: {}", tsId.getSiteName());
                    }
                    tsId.setSite(site);

                    log.info("Creating time series: {}", tsIdStr);
                    timeSeriesDAO.createTimeSeries(tsId);
                    log.info("Created time series with key = {}", tsId.getKey());
                    return tsId;
                }
                catch(Exception ex2)
                {
                    throw new DbIoException(String.format("No such time series and cannot create for '%s'", tsIdStr), ex2);
                }
            }
        };
    }

    /**
     * Import time series from a file with all necessary database operations.
     * This is a convenience method for GUI and other code to import time series.
     *
     * @param theDb the database connection
     * @param filename the file to import
     * @param selectedTsIds the TSIDs to import (null means import all)
     * @param tz the timezone for parsing dates
     * @param siteNameType the site name type preference
     * @return the number of time series successfully imported
     */
    public static int importTimeSeriesFile(TimeSeriesDb theDb, String filename,
        Collection<String> selectedTsIds, TimeZone tz, String siteNameType)
        throws IOException, DbIoException
    {
        int count = 0;
        try (SiteDAI siteDAO = theDb.makeSiteDAO();
             TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO())
        {
            TsImporter importer = new TsImporter(tz, siteNameType,
                makeGetOrCreateTsIdFunction(theDb, siteDAO, timeSeriesDAO));

            Collection<CTimeSeries> dc = (selectedTsIds != null && !selectedTsIds.isEmpty())
                ? importer.readTimeSeriesFile(filename, selectedTsIds)
                : importer.readTimeSeriesFile(filename);

            for(CTimeSeries cts : dc)
            {
                String tsid = cts.getTimeSeriesIdentifier().getUniqueString();
                log.info("Saving time series {}", tsid);
                try
                {
                    timeSeriesDAO.saveTimeSeries(cts);
                    count++;
                }
                catch(DbIoException | BadTimeSeriesException ex)
                {
                    log.warn("Cannot save time series '{}': {}", tsid, ex.getMessage());
                }
            }
        }
        return count;
    }
}
