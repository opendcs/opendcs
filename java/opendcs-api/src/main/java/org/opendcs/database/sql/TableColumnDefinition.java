package org.opendcs.database.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Used primarily with Column enums to provide a standard interface to return
 * information about a column. At this time we are only dealing with the column name.
 * Future work will likely start including additional informaiton, such as types.
 */
public interface TableColumnDefinition
{
    /**
     * Returns the column name.
     * @return
     */
    String column();

    static <T extends Enum<T> & TableColumnDefinition> Optional<Enum<T>> fromString(Class<T> enumClass, String input)
    {
        final var enums = (List<T>)Arrays.asList(enumClass.getEnumConstants());
        for (var e: enums)
        {
            if (e.column().equals(input))
            {
                return Optional.of(e);
            }
        }   
        return Optional.empty();
    }
}
