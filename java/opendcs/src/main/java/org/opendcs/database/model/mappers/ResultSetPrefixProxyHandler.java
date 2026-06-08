package org.opendcs.database.model.mappers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;

public final class ResultSetPrefixProxyHandler implements InvocationHandler
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


    /**
     * Return a ResultSet implementation that uses the provided parent ResultSet
     * and handles adding the prefix automatically.
     * @param parent
     * @return
     */
    public static ResultSet getResultSetProxy(ResultSet parent, String prefix)
    {
        Class<?> resultSetClass = parent.getClass();
        return (ResultSet)Proxy.newProxyInstance(resultSetClass.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new ResultSetPrefixProxyHandler(prefix, parent)
            );
    }
}