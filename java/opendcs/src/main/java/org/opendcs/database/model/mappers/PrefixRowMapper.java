package org.opendcs.database.model.mappers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;

import org.jdbi.v3.core.mapper.RowMapper;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.sql.TableColumnDefinition;

import ilex.util.Pair;

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
    protected final String tableName;

    protected final EnumSet<E> columns;

    protected PrefixRowMapper(String prefix, String table, EnumSet<E> columns)
    {
        this.prefix = addUnderscoreIfMissing(prefix);
        this.columns = columns;
        this.tableName = table;
    }

    /**
     * Placeholder until each Mapper is updated.
     * @param prefix
     * @param enumClass
     */
    protected PrefixRowMapper(String prefix, Class<E> enumClass)
    {
        this(prefix, null, EnumSet.allOf(enumClass));
    }

    protected PrefixRowMapper(String prefix, String table, Class<E> enumClass)
    {
        this(prefix, table, EnumSet.allOf(enumClass));
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

    /**
     * Create the basic set of columns and required join definition to pull in data for this mapper.
     * @param joinType type of join, without the word "join", can be null
     * @param idColumn
     * @param otherTable
     * @param otherIdColumn
     * @return
     */
    public String columnsForSelect()
    {
        final ArrayList<String> columnList = new ArrayList<>();
        final String prefixNoUnderscore = prefix.substring(0, prefix.length() - 1);
        columns.forEach(c ->
        {
            try
            {
                columnList.add(String.format("%s.%s %s", prefixNoUnderscore, c.column(), column(c)));
            }
            catch (SQLException ex)
            {
                throw new OpenDcsDataRuntimeException("A very unlikely situtation has happened.", ex);
            }
        });

        return String.join(",", columnList);
    }

    public String joinStatement(String joinType, E idColumn, String otherTable, String otherIdColumn)
    {
        if (tableName == null || tableName.isBlank())
        {
            throw new OpenDcsDataRuntimeException("Table name was not provided to this Mapper, we cannot build the join information.");
        }
        final String prefixNoUnderscore = prefix.substring(0, prefix.length() - 1);
        // example "left join transportmedium tm on tm.platformid = otherTable.otherIdColumn"
        return String.format("%s join %s %s on %s.%s = %s.%s",
                joinType != null ? joinType : "", tableName, prefixNoUnderscore, prefixNoUnderscore, idColumn.column(), otherTable, otherIdColumn);
    }
}
