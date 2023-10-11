package org.opendcs.fixtures.configurations.xml;

import java.io.File;

import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

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
