package org.opendcs.utils.properties;

import java.util.Map;
import java.util.Properties;

import org.opendcs.spi.properties.PropertyValueProvider;

/**
 * Get the real value of a property from the environment.
 */
public class EnvironmentPropertyValueProvider implements PropertyValueProvider
{
    private static final String prefix = "env.";

    @Override
    public boolean canProcess(String value)
    {
        return value.toLowerCase().startsWith(prefix);
    }

    /**
     * Retrieve property from the provided envrionment map
     * @param value actual value to decipher.
     *
     * @param properties ignored in this implementation.
     * @param environment Environment to use for the given request.
     *
     * @return the real value, or null if not found.
     */
    @Override
    public String processValue(String value, Properties props, Map<String,String> environment)
    {
        String envVar = value.substring(prefix.length());
        return environment.get(envVar);
    }

}
