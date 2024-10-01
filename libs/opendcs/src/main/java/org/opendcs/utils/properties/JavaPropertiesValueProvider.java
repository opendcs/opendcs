package org.opendcs.utils.properties;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.opendcs.spi.properties.PropertyValueProvider;

public class JavaPropertiesValueProvider implements PropertyValueProvider
{
    private static final String prefix = "java.";

    @Override
    public boolean canProcess(String value)
    {
        return value.toLowerCase().startsWith(prefix);
    }

    /**
     * Retrieve property from the provided property map
     * @param value actual value to decipher.
     *
     * @param properties Properties to use for the given request.
     * @param environment ignored in this implementation.
     *
     * @return the real value, or null if not found.
     */
    @Override
    public String processValue(String value, Properties props, Map<String,String> environment) throws IOException
    {
        String propVar = value.substring(prefix.length());
        return props.getProperty(propVar);
    }
}
