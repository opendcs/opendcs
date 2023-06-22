package org.opendcs.utils;

import java.io.IOException;
import java.util.Iterator;
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
     * @param value value from a source such as the database or xml files
     * @param defaultValue 
     * @return expanded value
     * @throws IOException any error with the definition of the value or retrieving it from it's real source.
     */
    public static String getRealPropertyValue(String value) throws IOException
    {
        return getRealPropertyValue(value, null);
    }

    /**
     * Retrieve the real value of a property from a location that may be the value itself
     * or perhaps the environment as determined by a given provider.
     * 
     * If the value cannot be retrieved the defaultValue is returned
     * 
     * @param value value from a source such as the database or xml files
     * @param defaultValue 
     * @return expanded value
     * @throws IOException any error with the definition of the value or retrieving it from it's real source.
     */
    public static String getRealPropertyValue(String value, String defaultValue) throws IOException
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
                    String realValue = p.processValue(valueStr);
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
