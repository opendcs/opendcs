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

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Supplier;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsImporter;

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
    public static ImportPreview inspect(String content, TimeZone defaultTimeZone,
            Supplier<TimeSeriesIdentifier> makeEmptyTsId) throws InspectionException
    {
        ImportType type = detectImportType(content);
        long sizeBytes = content.getBytes(StandardCharsets.UTF_8).length;
        Map<String, Long> counts = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        if (type == ImportType.TIMESERIES)
        {
            return inspectTimeSeries(content, sizeBytes, defaultTimeZone, makeEmptyTsId, counts, warnings);
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
            warnings.add("No recognized import object elements were found.");
        }
    }

    private static ImportPreview inspectTimeSeries(String content, long sizeBytes, TimeZone defaultTimeZone,
            Supplier<TimeSeriesIdentifier> makeEmptyTsId, Map<String, Long> counts, List<String> warnings)
            throws InspectionException
    {
        TsImporter importer = new TsImporter(defaultTimeZone, makeEmptyTsId);
        Collection<CTimeSeries> series;
        try
        {
            series = importer.readTimeSeriesFile(new StringReader(content));
        }
        catch (IOException | IllegalArgumentException ex)
        {
            throw new InspectionException("Unable to read time series import content.", ex);
        }
        Date first = null;
        Date last = null;
        long samples = 0;
        for (CTimeSeries timeSeries : series)
        {
            samples += timeSeries.size();
            if (timeSeries.size() > 0)
            {
                Date start = timeSeries.sampleAt(0).getTime();
                Date end = timeSeries.sampleAt(timeSeries.size() - 1).getTime();
                first = first == null || start.before(first) ? start : first;
                last = last == null || end.after(last) ? end : last;
            }
        }
        counts.put("timeSeries", (long) series.size());
        counts.put("samples", samples);
        if (samples == 0)
        {
            warnings.add("No valid samples were found.");
        }
        return new ImportPreview(ImportType.TIMESERIES, sizeBytes, counts,
                first == null ? null : first.toInstant(), last == null ? null : last.toInstant(), warnings);
    }
}
