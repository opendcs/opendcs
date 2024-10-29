package org.opendcs.fixtures.configuration.cwms;

import java.io.File;

import org.opendcs.fixtures.configuration.Configuration;
import org.opendcs.fixtures.configuration.ConfigurationProvider;

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
