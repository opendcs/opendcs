package org.opendcs.utils.sql;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Predicate;

import org.jdbi.v3.core.statement.PreparedBatch;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

import decodes.sql.DbKey;

public final class PropertyBindings
{
    public static final String LAST_MODIFIED = "LastModified";
    public static final DateTimeFormatter LAST_MODIFIED_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                                                                    .withZone(ZoneOffset.UTC);

    private PropertyBindings()
    {
    }

    public static void addLastModified(PreparedBatch batch, String idColumnName, DbKey ownerId)
    {
        addProperty(batch, idColumnName, ownerId, LAST_MODIFIED, LAST_MODIFIED_FORMATTER.format(Instant.now()));
    }

    /** Adds text properties, converting null values to empty strings. Callers must format date/time values. */
    public static void addProperties(PreparedBatch batch, String idColumnName, DbKey ownerId, Map<?, ?> properties)
    {
        addProperties(batch, idColumnName, ownerId, properties, key -> true);
    }

    /** Adds properties identified by an owner ID and a second key, such as a sensor number. */
    public static void addProperties(PreparedBatch batch, String idColumnName, DbKey ownerId,
                                     String secondaryIdColumnName, int secondaryId, Map<?, ?> properties)
    {
        properties.forEach((key, value) ->
        {
            batch.bind(secondaryIdColumnName, secondaryId);
            addProperty(batch, idColumnName, ownerId, key.toString(), value);
        });
    }

    /** Adds only properties whose keys match includeProperty, using the same value conversion as addProperties. */
    public static <K> void addProperties(PreparedBatch batch, String idColumnName, DbKey ownerId,
                                         Map<K, ?> properties, Predicate<? super K> includeProperty)
    {
        properties.forEach((key, value) ->
        {
            if (includeProperty.test(key))
            {
                addProperty(batch, idColumnName, ownerId, key.toString(), value);
            }
        });
    }

    /** Adds one text property using the same conversion as addProperties. */
    private static void addProperty(PreparedBatch batch, String idColumnName, DbKey ownerId, String name, Object value)
    {
        batch.bind(idColumnName, ownerId)
             .bind(PropertiesMapper.Columns.NAME.column(), name)
             .bind(PropertiesMapper.Columns.VALUE.column(), value != null ? value.toString() : "")
             .add();
    }
}
