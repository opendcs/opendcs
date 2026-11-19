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

package org.opendcs.imports;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Inspects import content without database access. */
public final class ImportPreviewParser
{
    private static final Set<String> DECODES_ROOTS = Set.of("Database", "EnumList", "Platform",
            "DataTypeEquivalenceList", "EngineeringUnitList", "RoutingSpec", "NetworkList",
            "PresentationGroup", "PlatformConfig", "EquipmentModel", "DataSource", "Site",
            "PlatformList", "IntervalList", "ScheduleEntry", "PlatformStatus");
    private static final List<String> DECODES_OBJECTS = List.of("Platform", "Site", "PlatformConfig",
            "EquipmentModel", "RoutingSpec", "NetworkList", "PresentationGroup", "DataSource",
            "ScheduleEntry", "PlatformStatus", "EngineeringUnit", "Enum", "DataTypeEquivalence", "Interval");
    private static final List<String> COMPUTATION_OBJECTS = List.of("Algorithm", "Computation",
            "LoadingApplication", "TsGroup");
    private static final int MAX_WARNINGS = 20;

    private ImportPreviewParser()
    {
    }

    public enum ImportType
    {
        DECODES("dbimport"),
        COMPUTATIONS("compimport"),
        TIMESERIES("tsimport");

        private final String importer;

        ImportType(String importer)
        {
            this.importer = importer;
        }

        public String importer()
        {
            return importer;
        }
    }

    public record ImportPreview(ImportType type, long sizeBytes, Map<String, Long> counts,
                                Instant firstSampleTime, Instant lastSampleTime, List<String> warnings)
    {
        public ImportPreview
        {
            counts = Collections.unmodifiableMap(new LinkedHashMap<>(counts));
            warnings = List.copyOf(warnings);
        }
    }

    public static final class InspectionException extends Exception
    {
        private static final long serialVersionUID = 1L;

        private InspectionException(String message)
        {
            super(message);
        }

        private InspectionException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /** Inspects decoded UTF-8 content using the supplied time zone until a SET:TZ record overrides it. */
    public static ImportPreview inspect(String content, TimeZone defaultTimeZone) throws InspectionException
    {
        ImportType type = detectImportType(content);
        long sizeBytes = content.getBytes(StandardCharsets.UTF_8).length;
        Map<String, Long> counts = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        if (type == ImportType.TIMESERIES)
        {
            return inspectTimeSeries(content, sizeBytes, defaultTimeZone, counts, warnings);
        }

        inspectXml(content, type == ImportType.COMPUTATIONS, counts, warnings);
        return new ImportPreview(type, sizeBytes, counts, null, null, warnings);
    }

    private static ImportType detectImportType(String content) throws InspectionException
    {
        if (content == null || content.isBlank())
        {
            throw new InspectionException("Missing import content.");
        }
        String first = content.lines().map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).findFirst().orElse("");
        if (!first.startsWith("<"))
        {
            if (first.regionMatches(true, 0, "TSID:", 0, 5)
                    || first.regionMatches(true, 0, "SET:", 0, 4))
            {
                return ImportType.TIMESERIES;
            }
            throw new InspectionException("Unsupported import content. Expected DECODES XML, CompMetaData XML, or TSID:/SET: records.");
        }
        try
        {
            XMLStreamReader reader = xmlReader(content);
            try
            {
                String root = null;
                while (reader.hasNext())
                {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT && root == null)
                    {
                        root = reader.getLocalName();
                    }
                }
                if ("CompMetaData".equalsIgnoreCase(root) || "LoadingApplication".equalsIgnoreCase(root))
                {
                    return ImportType.COMPUTATIONS;
                }
                if (root != null && DECODES_ROOTS.stream().anyMatch(root::equalsIgnoreCase))
                {
                    return ImportType.DECODES;
                }
                throw new InspectionException("Unsupported XML root element: " + root);
            }
            finally
            {
                reader.close();
            }
        }
        catch (XMLStreamException ex)
        {
            throw new InspectionException("Malformed XML import content.", ex);
        }
    }

    private static XMLStreamReader xmlReader(String content) throws XMLStreamException
    {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory.createXMLStreamReader(new StringReader(content));
    }

    private static void inspectXml(String content, boolean computations, Map<String, Long> counts,
            List<String> warnings) throws InspectionException
    {
        List<String> objects = computations ? COMPUTATION_OBJECTS : DECODES_OBJECTS;
        try
        {
            XMLStreamReader reader = xmlReader(content);
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
                                    counts.merge(object, 1L, Long::sum);
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
            throw new InspectionException("Malformed XML import content.", ex);
        }
        if (counts.isEmpty())
        {
            warn(warnings, "No recognized import object elements were found.");
        }
    }

    private static ImportPreview inspectTimeSeries(String content, long sizeBytes, TimeZone defaultTimeZone,
            Map<String, Long> counts, List<String> warnings) throws InspectionException
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        format.setTimeZone(defaultTimeZone);
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
                    throw new InspectionException("Empty time series identifier on line " + lineNumber + ".");
                }
                identifiers.add(currentId);
            }
            else if (line.regionMatches(true, 0, "SET:", 0, 4))
            {
                inspectTimeSeriesSetting(line, lineNumber, format, warnings);
            }
            else if (Character.isDigit(line.charAt(0)))
            {
                if (currentId == null)
                {
                    throw new InspectionException("Sample before a TSID on line " + lineNumber + ".");
                }
                int comment = line.indexOf('#');
                String[] fields = (comment >= 0 ? line.substring(0, comment) : line).split(",");
                Date time = sampleTime(fields, format);
                if (time == null)
                {
                    warn(warnings, "Invalid sample on line " + lineNumber + "; excluded from statistics.");
                    continue;
                }
                samples++;
                first = first == null || time.before(first) ? time : first;
                last = last == null || time.after(last) ? time : last;
            }
            else
            {
                warn(warnings, "Unrecognized record on line " + lineNumber + "; ignored.");
            }
        }
        if (identifiers.isEmpty())
        {
            throw new InspectionException("No time series identifiers found in import content.");
        }
        counts.put("timeSeries", (long) identifiers.size());
        counts.put("samples", samples);
        if (samples == 0)
        {
            warn(warnings, "No valid samples were found.");
        }
        return new ImportPreview(ImportType.TIMESERIES, sizeBytes, counts,
                first == null ? null : first.toInstant(), last == null ? null : last.toInstant(), warnings);
    }

    private static void inspectTimeSeriesSetting(String line, int lineNumber, SimpleDateFormat format,
            List<String> warnings) throws InspectionException
    {
        int equals = line.indexOf('=');
        if (equals < 0)
        {
            warn(warnings, "SET record without '=' on line " + lineNumber + "; ignored.");
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
                    throw new InspectionException("Invalid DATEFORMAT on line " + lineNumber + ".", ex);
                }
            }
            case "UNITS", "SITENAMETYPE" ->
            {
            }
            default -> warn(warnings, "Unknown setting on line " + lineNumber + "; ignored.");
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

    private static void warn(List<String> warnings, String message)
    {
        if (warnings.size() < MAX_WARNINGS)
        {
            warnings.add(message);
        }
        else if (warnings.size() == MAX_WARNINGS)
        {
            warnings.add("Further warnings omitted.");
        }
    }
}
