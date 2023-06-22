package org.opendcs.utils.properties;

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
    
    @Override
    public String processValue(String value)
    {
        String envVar = value.substring(prefix.length());
        return System.getenv(envVar);
    }
    
}
