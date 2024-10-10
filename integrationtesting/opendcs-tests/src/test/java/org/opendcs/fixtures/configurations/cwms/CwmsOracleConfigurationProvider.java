package org.opendcs.fixtures.configurations.cwms;

import java.io.File;

import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class CwmsOracleConfigurationProvider implements ConfigurationProvider
{
    @Override
    public String getImplementation()
    {
        return CwmsOracleConfiguration.NAME;
    }

    @Override
    public Configuration getConfig(File tempDir) throws Exception
    {
        CwmsOracleConfiguration config = new CwmsOracleConfiguration(tempDir);
        return config;
    }   
}
