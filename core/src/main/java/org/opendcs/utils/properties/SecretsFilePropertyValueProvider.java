package org.opendcs.utils.properties;

import java.io.File;
import java.io.IOException;

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

    @Override
    public String processValue(String value) throws IOException
    {
        final String fileName = value.substring(prefix.length());
        return FileUtil.getFileContents(new File(fileName));
    }
    
}
