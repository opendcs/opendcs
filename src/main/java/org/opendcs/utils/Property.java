package org.opendcs.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.opendcs.spi.properties.PropertyValueProvider;

import ilex.util.Logger;

public class Property
{
    private static ServiceLoader<PropertyValueProvider> loader = ServiceLoader.load(PropertyValueProvider.class);

    /**
     * Retrieve the real value of a property from a location that may be the value itself
     * or perhaps the environment as determined by a given provider.
     *
     * If the value cannot be retrieved null is returned.
     *
     * For properties and environment System.getProperties and System.getenv are used.
     *
     * @param value value from a source such as the database or xml files
     * @return expanded value
     * @throws IOException any error with the definition of the value source or retrieving it from it's real source.
     */
    public static String getRealPropertyValue(String value) throws IOException
    {
        return getRealPropertyValue(value, null);
    }

    /**
     * Retrieve the real value of a property from a location that may be the value itself
     * or perhaps the environment as determined by a given provider.
     *
     * If the value cannot be retrieved null is returned.
     *
     * @param value value from a source such as the database or xml files
     * @param defaultValue value returned if none founded
     * @return expanded value
     * @throws IOException any error with the definition of the value source or retrieving it from it's real source.
     */
    public static String getRealPropertyValue(String value, String defaultValue) throws IOException
    {
        return getRealPropertyValue(value, defaultValue, System.getProperties(), System.getenv());
    }

    /**
     * Retrieve the real value of a property from a location that may be the value itself
     * or perhaps the environment as determined by a given provider.
     *
     * If the value cannot be retrieved null is returned.
     *
     * @param value value from a source such as the database or xml files
     * @param defaultValue value returned if none founded
     * @param props Java Properties object to use for properties source
     * @param env env map to use for retrieving environment values.
     * @return expanded value
     * @throws IOException any error with the definition of the value source or retrieving it from it's real source.
     */
    public static String getRealPropertyValue(String value, Properties props, Map<String,String> env) throws IOException
    {
        return getRealPropertyValue(value, null, props, env);
    }

    /**
     * Retrieve the real value of a property from a location that may be defined in the provided the value itself
     * or perhaps the environment as determined by a given provider.
     *
     * If the value cannot be retrieved the defaultValue is returned.
     *
     * If called directly should only have the contents of the parts between ${ and }
     * for an individual value. Otherwise this is intended to be called from EnvExpander
     *
     * @param value value from a source such as the database or xml files
     * @param defaultValue
     * @param props Java Properties object to use for properties source
     * @param env env map to use for retrieving environment values.
     * @return expanded value
     * @throws IOException any error with the definition of the value or retrieving it from it's real source.
     */
    public static String getRealPropertyValue(String value, String defaultValue, Properties props, Map<String,String> env) throws IOException
    {
        if(value == null)
        {
            return defaultValue;
        }
        else if(!value.startsWith("${"))
        {
            return value;
        }
        else if(value.startsWith("\\${"))
        {
            return value.substring(1);
        }
        else
        {
            Iterator<PropertyValueProvider> providers = loader.iterator();
            int closingCurly = value.lastIndexOf("}");
            if(closingCurly <= 2)
            {
                throw new IOException("A property value to be expanded must be a string enclosed by ${ and }. Value was " + value);
            }
            String valueStr = value.substring(2,closingCurly);
            while(providers.hasNext())
            {
                PropertyValueProvider p = providers.next();
                if (p.canProcess(valueStr)) {
                    String realValue = p.processValue(valueStr, props, env);
                    if (realValue == null)
                    {
                        return defaultValue;
                    }
                    else
                    {
                        return realValue;
                    }
                }
            }
            Logger.instance().warning("Unable to find processor for variable: " + value + " returning default.");
        }
        return defaultValue;
    }
}
