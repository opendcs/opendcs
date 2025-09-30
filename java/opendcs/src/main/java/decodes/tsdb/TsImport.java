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

import java.io.IOException;
import java.util.Collection;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.cwms.CwmsTimeSeriesDAO;
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
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    public static final String module = "TsImport";
    /** One or more input files specified on end of command line */
    private StringToken filenameArg = new StringToken("", "input-file", "",
        TokenOptions.optArgument|TokenOptions.optRequired
        |TokenOptions.optMultiple, "");
    private BooleanToken noUnitConvArg = new BooleanToken("U", "Pass file units directly to CWMS", "",
        TokenOptions.optSwitch, false);

    /** Optional list of TSIDs to import - if set, only these will be imported */
    private Collection<String> selectedTsIds = null;

    public TsImport()
    {
        super("util.log");
        DecodesInterface.silent = true;
    }

    /**
     * Set the list of TSIDs to import. If null or empty, all TSIDs in the file will be imported.
     * @param selectedTsIds the list of TSIDs to import
     */
    public void setSelectedTsIds(Collection<String> selectedTsIds)
    {
        this.selectedTsIds = selectedTsIds;
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
        DecodesSettings settings = DecodesSettings.instance();
        CwmsTimeSeriesDAO.setNoUnitConv(noUnitConvArg.getValue());
        final TimeZone tz = TimeZone.getTimeZone(settings.sqlTimeZone);

        for(int n = filenameArg.NumberOfValues(), i=0; i<n; i++)
        {
            final String filename = filenameArg.getValue(i);
            try
            {
                int count = TsImporter.importTimeSeriesFile(theDb, filename, selectedTsIds,
                    tz, settings.siteNameTypePreference);
                log.info("Successfully imported {} time series from {}", count, filename);
            }
            catch (IOException ex)
            {
                log.atWarn()
                   .setCause(ex)
                   .log("Error reading {} -- skipping", filename);
            }
            catch (DbIoException ex)
            {
                log.atWarn()
                   .setCause(ex)
                   .log("Database error processing {} -- skipping", filename);
            }
        }
    }

    @Override
    public void initDecodes()
        throws DecodesException
    {
    }


    public static void main(String args[])
        throws Exception
    {
        TsImport app = new TsImport();
        app.execute(args);
    }
}
