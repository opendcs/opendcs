package org.opendcs.database.model.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jdbi.v3.core.mapper.RowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

/**
 * Helper class for row mappers to take a prefix.
 * If the provided prefix does not start end with and underscore (_)
 * one will be added.
 */
public abstract class PrefixRowMapper<T,K> implements RowMapper<T>
{
    protected final String prefix;
    
    private final Set<TableColumnDefinition> columns;

    protected PrefixRowMapper(String prefix, Set<TableColumnDefinition> columns)
    {
        this.prefix = addUnderscoreIfMissing(prefix);
        this.columns = columns;
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
    protected <E extends Enum<E> & TableColumnDefinition> String column(E column) throws SQLException
    {
        if (!columns.contains(column))
        {
            throw new SQLException("Column " + column + " does not exist for this table.");
        }
        return prefix + column.column();
    }

    /**
     * Return a ResultSet implementation that uses the provided parent ResultSet
     * and handles adding the prefix automatically.
     * @param parent
     * @return
     */
    public ResultSet getResultSetProxy(ResultSet parent)
    {
        Class<?> resultSetClass = parent.getClass();
        return (ResultSet)Proxy.newProxyInstance(resultSetClass.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new ResultSetPrefixProxyHandler(prefix, parent)
            );
    }
    

    public static final class ResultSetPrefixProxyHandler implements InvocationHandler
    {
        final String prefix;
        final ResultSet instance;

        public ResultSetPrefixProxyHandler(String prefix, ResultSet instance)
        {
            this.prefix = prefix;
            this.instance = instance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            // This may not be totally valid (e.g. there might be a few method where it's a single String input)
            // but quick a review of ResultSet didn't have any stand out.
            // we will add exceptions as we find them.
            if (args != null && args.length == 1 && (String.class.equals(args[0].getClass())))
            {
                String name = prefix + (String)args[0];
                return method.invoke(instance, name);
            }
            else
            {
                return method.invoke(instance, args);
            }
        }
    }
}
