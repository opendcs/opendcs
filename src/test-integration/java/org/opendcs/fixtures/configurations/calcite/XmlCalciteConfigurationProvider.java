package org.opendcs.fixtures.configurations.calcite;

import java.io.File;

import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class XmlCalciteConfigurationProvider implements ConfigurationProvider
{

    @Override
    public String getImplementation()
    {
        return "OpenDCS-XML-Calcite";
    }

    @Override
    public Configuration getConfig(File tempDir) throws Exception
    {
        final XmlCalciteConfiguration config = new XmlCalciteConfiguration(tempDir);
        return config;
    }

}
