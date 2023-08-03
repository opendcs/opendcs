package org.opendcs.utils.properties;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.opendcs.spi.properties.PropertyValueProvider;

import ilex.util.FileUtil;

public class SecretsFilePropertyValueProvider implements PropertyValueProvider
{
    private static final String prefix = "file.";

    @Override
    public boolean canProcess(String value)
    {
        return value.startsWith(prefix);
    }


    /**
     * Retrieve property file named within the value.
     * @param value actual value to decipher.
     *
     * @param properties ignored in this implementation.
     * @param environment ignored in this implementation.
     *
     * @return the real value, or null if not found.
     */
    @Override
    public String processValue(String value, Properties props, Map<String,String> environment) throws IOException
    {
        final String fileName = value.substring(prefix.length());
        return FileUtil.getFileContents(new File(fileName));
    }

}
