/*
 *  Copyright 2026 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.res;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import decodes.util.DecodesSettings;
import org.opendcs.odcsapi.beans.ApiImportResult;
import org.opendcs.odcsapi.errorhandling.WebAppException;

/** Inspects uploaded files without database access or staging files. */
final class ImportPreviewParser
{
    private static final List<String> DECODES_OBJECTS = List.of("Platform", "Site", "PlatformConfig",
            "EquipmentModel", "RoutingSpec", "NetworkList", "PresentationGroup", "DataSource",
            "ScheduleEntry", "PlatformStatus", "EngineeringUnit", "Enum", "DataTypeEquivalence", "Interval");
    private static final List<String> COMPUTATION_OBJECTS = List.of("Algorithm", "Computation",
            "LoadingApplication", "TsGroup");
    private static final int MAX_WARNINGS = 20;

    private ImportPreviewParser()
    {
    }

    static ApiImportResult inspect(String content) throws WebAppException
    {
        String type = ImportResources.detectImportType(content);
        ApiImportResult result = new ApiImportResult();
        result.setImportType(type);
        result.setImporter(switch (type)
        {
            case ImportResources.DECODES_IMPORT_TYPE -> "dbimport";
            case ImportResources.COMPUTATIONS_IMPORT_TYPE -> "compimport";
            default -> "tsimport";
        });
        result.setSizeBytes(content.getBytes(StandardCharsets.UTF_8).length);
        result.setValidateOnly(true);
        if (ImportResources.TIMESERIES_IMPORT_TYPE.equals(type))
        {
            inspectTimeSeries(content, result);
        }
        else
        {
            inspectXml(content, result);
        }
        result.setSuccess(true);
        result.setMessage("File inspected successfully.");
        return result;
    }

    private static void inspectXml(String content, ApiImportResult result) throws WebAppException
    {
        boolean computations = ImportResources.COMPUTATIONS_IMPORT_TYPE.equals(result.getImportType());
        List<String> objects = computations ? COMPUTATION_OBJECTS : DECODES_OBJECTS;
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try
        {
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(content));
            try
            {
                int depth = 0;
                while (reader.hasNext())
                {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT)
                    {
                        depth++;
                        if (!computations || depth <= 2)
                        {
                            for (String object : objects)
                            {
                                if (object.equalsIgnoreCase(reader.getLocalName()))
                                {
                                    result.getCounts().merge(object, 1L, Long::sum);
                                    break;
                                }
                            }
                        }
                    }
                    else if (event == XMLStreamConstants.END_ELEMENT)
                    {
                        depth--;
                    }
                }
            }
            finally
            {
                reader.close();
            }
        }
        catch (XMLStreamException ex)
        {
            throw new WebAppException(400, "Malformed XML import content.", ex);
        }
        if (result.getCounts().isEmpty())
        {
            warn(result, "No recognized import object elements were found.");
        }
    }

    private static void inspectTimeSeries(String content, ApiImportResult result) throws WebAppException
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().sqlTimeZone));
        Set<String> identifiers = new HashSet<>();
        String currentId = null;
        Date first = null;
        Date last = null;
        long samples = 0;
        int lineNumber = 0;
        for (var lines = content.lines().iterator(); lines.hasNext();)
        {
            String line = lines.next().trim();
            lineNumber++;
            if (line.isEmpty() || line.startsWith("#"))
            {
                continue;
            }
            if (line.regionMatches(true, 0, "TSID:", 0, 5))
            {
                currentId = line.substring(5).trim();
                if (currentId.isEmpty())
                {
                    throw new WebAppException(400, "Empty time series identifier on line " + lineNumber + ".");
                }
                identifiers.add(currentId);
            }
            else if (line.regionMatches(true, 0, "SET:", 0, 4))
            {
                inspectTimeSeriesSetting(line, lineNumber, format, result);
            }
            else if (Character.isDigit(line.charAt(0)))
            {
                if (currentId == null)
                {
                    throw new WebAppException(400, "Sample before a TSID on line " + lineNumber + ".");
                }
                int comment = line.indexOf('#');
                String[] fields = (comment >= 0 ? line.substring(0, comment) : line).split(",");
                Date time = sampleTime(fields, format);
                if (time == null)
                {
                    warn(result, "Invalid sample on line " + lineNumber + "; excluded from statistics.");
                    continue;
                }
                samples++;
                first = first == null || time.before(first) ? time : first;
                last = last == null || time.after(last) ? time : last;
            }
            else
            {
                warn(result, "Unrecognized record on line " + lineNumber + "; ignored.");
            }
        }
        if (identifiers.isEmpty())
        {
            throw new WebAppException(400, "No time series identifiers found in import content.");
        }
        result.getCounts().put("timeSeries", (long) identifiers.size());
        result.getCounts().put("samples", samples);
        result.setFirstSampleTime(first == null ? null : first.toInstant().toString());
        result.setLastSampleTime(last == null ? null : last.toInstant().toString());
        if (samples == 0)
        {
            warn(result, "No valid samples were found.");
        }
    }

    private static void inspectTimeSeriesSetting(String line, int lineNumber, SimpleDateFormat format,
            ApiImportResult result) throws WebAppException
    {
        int equals = line.indexOf('=');
        if (equals < 0)
        {
            warn(result, "SET record without '=' on line " + lineNumber + "; ignored.");
            return;
        }
        String name = line.substring(4, equals).trim().toUpperCase(Locale.ROOT);
        String value = line.substring(equals + 1).trim();
        switch (name)
        {
            case "TZ" -> format.setTimeZone(TimeZone.getTimeZone(value));
            case "DATEFORMAT" ->
            {
                try
                {
                    format.applyPattern(value);
                }
                catch (IllegalArgumentException ex)
                {
                    throw new WebAppException(400, "Invalid DATEFORMAT on line " + lineNumber + ".", ex);
                }
            }
            case "UNITS", "SITENAMETYPE" ->
            {
            }
            default -> warn(result, "Unknown setting on line " + lineNumber + "; ignored.");
        }
    }

    private static Date sampleTime(String[] fields, SimpleDateFormat format)
    {
        if (fields.length < 2)
        {
            return null;
        }
        Date time = format.parse(fields[0].trim(), new ParsePosition(0));
        if (time == null)
        {
            return null;
        }
        try
        {
            if (!"m".equalsIgnoreCase(fields[1].trim()))
            {
                Double.parseDouble(fields[1].trim());
            }
            if (fields.length > 2)
            {
                String flags = fields[2].trim();
                if (flags.regionMatches(true, 0, "0x", 0, 2))
                {
                    Integer.parseInt(flags.substring(2), 16);
                }
                else if (flags.regionMatches(true, 0, "0b", 0, 2))
                {
                    Integer.parseInt(flags.substring(2), 2);
                }
                else
                {
                    Integer.parseInt(flags);
                }
            }
            return time;
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    private static void warn(ApiImportResult result, String message)
    {
        if (result.getWarnings().size() < MAX_WARNINGS)
        {
            result.getWarnings().add(message);
        }
        else if (result.getWarnings().size() == MAX_WARNINGS)
        {
            result.getWarnings().add("Further warnings omitted.");
        }
    }
}
