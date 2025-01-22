package org.opendcs.fixtures.configuration.xml;

import java.io.File;

import org.opendcs.fixtures.configuration.Configuration;
import org.opendcs.fixtures.configuration.ConfigurationProvider;

public class XmlConfigurationProvider implements ConfigurationProvider
{

    @Override
    public String getImplementation()
    {
        return XmlConfiguration.NAME;
    }

    @Override
    public Configuration getConfig(File tempDir) throws Exception
    {
        final XmlConfiguration config = new XmlConfiguration(tempDir);
        return config;
    }

}
