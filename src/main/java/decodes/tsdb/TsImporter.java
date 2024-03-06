package decodes.tsdb;

import static org.slf4j.helpers.Util.getCallingClass;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ilex.util.TextUtil;
import ilex.var.TimedVariable;
import opendcs.util.functional.ThrowingFunction;

/**
 * Utility class to handler importing OpenDCS import/export files
 */
public class TsImporter
{
    private static final Logger log = LoggerFactory.getLogger(getCallingClass());

    private Map<String, CTimeSeries> dc = new HashMap<>();
    private int lineNum = -1;
    private CTimeSeries currentTS = null;
    private String tsidStr = null;
    private String filename = null;
    private String siteNameType = null;
    private String units = null;
    private SimpleDateFormat dataSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    private final ThrowingFunction<String,TimeSeriesIdentifier, DbIoException> makeTsIdFunc;

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

    public Collection<CTimeSeries> readTimeSeriesFile(InputStream stream) throws IOException
    {
        if (this.filename == null)
        {
            this.filename = "Provided InputStream";
        }

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
                currentTS.setUnitsAbbr(units);
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
                   .log("Unable to Initialize current timeseries.", ex);
                return null;
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
            tv.setValue(Double.parseDouble(x[1].trim()));
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
                if (TextUtil.startsWithIgnoreCase(flags, "0x"))
                    tv.setFlags(Integer.parseInt(flags.substring(2), 16));
                else
                    tv.setFlags(Integer.parseInt(flags));
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
}
