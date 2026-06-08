package org.opendcs.database.model.mappers;

import java.sql.ResultSet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


import org.jdbi.v3.core.mapper.RowMapper;

/**
 * Helper class for row mappers to take a prefix.
 * If the provided prefix does not start end with and underscore (_)
 * one will be added.
 */
public abstract class PrefixRowMapper<T> implements RowMapper<T>
{
    protected final String prefix;

    protected PrefixRowMapper(String prefix)
    {
        this.prefix = addUnderscoreIfMissing(prefix);
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
     * Return a ResultSet implementation that uses the provided parent ResultSet
     * and handles adding the prefix automatically.
     * @param parent
     * @return
     */
    public ResultSet getResultSetProxy(ResultSet parent)
    {
        Class<?> resultSetClass = parent.getClass();
        return (ResultSet)Proxy.newProxyInstance(resultSetClass.getClassLoader(),
                new Class<?>[]{resultSetClass},
                new ResultSetPrefixProxyHandler(prefix)
            );
    }
    

    public static final class ResultSetPrefixProxyHandler implements InvocationHandler
    {
        final String prefix;

        public ResultSetPrefixProxyHandler(String prefix)
        {
            this.prefix = prefix;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if (args.length == 1 && (String.class.equals(args[0].getClass())))
            {
                String name = prefix + (String)args[0];
                return method.invoke(proxy, name);
            }
            else
            {
                return method.invoke(proxy, args);
            }
        }
    }
}
