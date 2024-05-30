package org.opendcs.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.opendcs.spi.properties.PropertyValueProvider;
import org.opendcs.utils.properties.PropertySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Property
{
    private static ServiceLoader<PropertyValueProvider> loader = ServiceLoader.load(PropertyValueProvider.class);
    private static final Logger log = LoggerFactory.getLogger(Property.class);
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
        else
        {
            Iterator<PropertyValueProvider> providers = loader.iterator();
            while(providers.hasNext())
            {
                PropertyValueProvider p = providers.next();
                if (p.canProcess(value)) {
                    String realValue = p.processValue(value, props, env);
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
            /*
             * We really only need this when debugging.
             */
            if ( PropertySettings.TRACE_PROPERTY_PROVIDERS == true && log.isTraceEnabled())
            {
                log.atTrace()
                   .setCause(new Exception("Unable to find variable processor."))
                   .log("Unable to find processor for variable: {} original value.", value);
            }
        }
        return value;
    }
}
