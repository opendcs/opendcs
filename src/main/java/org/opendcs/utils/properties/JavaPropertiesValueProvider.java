package org.opendcs.utils.properties;

import java.io.IOException;

import org.opendcs.spi.properties.PropertyValueProvider;

public class JavaPropertiesValueProvider implements PropertyValueProvider
{
    private static final String prefix = "java.";

    @Override
    public boolean canProcess(String value)
    {
        return value.toLowerCase().startsWith(prefix);
    }

    @Override
    public String processValue(String value) throws IOException
    {
        String propVar = value.substring(prefix.length());
        return System.getProperty(propVar);
    }
}
