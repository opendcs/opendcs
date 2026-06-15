package org.opendcs.database.model.mappers;

import java.sql.SQLException;
import java.util.EnumSet;

import org.jdbi.v3.core.mapper.RowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

/**
 * Helper class for row mappers to take a prefix.
 * If the provided prefix does not start end with an underscore (_)
 * one will be added.
 * 
 * A enum of defined columns must also be provided. At this time only
 * the column name is handled. Future work will allow building a
 * <pre> table.column prefix_column</pre> list for joins.
 * 
 * @param <T> The type that the mapper will return
 * @param <E> A enum containing all columns. This enum must implement the {@link TableColumnDefinition} interface.
 */
public abstract class PrefixRowMapper<T,E extends Enum<E> & TableColumnDefinition> implements RowMapper<T>
{
    protected final String prefix;
    
    private final EnumSet<E> columns;

    protected PrefixRowMapper(String prefix, EnumSet<E> columns)
    {
        this.prefix = addUnderscoreIfMissing(prefix);
        this.columns = columns;
    }

    protected PrefixRowMapper(String prefix, Class<E> enumClass)
    {
        this(prefix, EnumSet.allOf(enumClass));
    }

    public static String addUnderscoreIfMissing(String prefix)
    {
        var tmp = (prefix == null || prefix.isBlank()) ? "" : prefix;
        if ("".equals(tmp))
        {
            return tmp;
        }
        else
        {
            return tmp.endsWith("_") ? tmp : (tmp + "_");
        }
    }    

    /**
     * Return the requested column with the provided name
     * @param column the desired column.
     * @return the prefixed column name
     */
    public String column(E column) throws SQLException
    {
        if (!columns.contains(column))
        {
            throw new SQLException("Column " + column + " does not exist for this table.");
        }
        return prefix + column.column();
    }
}
