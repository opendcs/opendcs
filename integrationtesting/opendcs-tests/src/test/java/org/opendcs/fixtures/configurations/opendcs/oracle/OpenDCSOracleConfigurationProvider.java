package org.opendcs.fixtures.configurations.opendcs.oracle;

import java.io.File;

import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class OpenDCSOracleConfigurationProvider implements ConfigurationProvider
{

    @Override
    public String getImplementation() {
        return OpenDCSOracleConfiguration.NAME;
    }

    @Override
    public Configuration getConfig(File tempDir) throws Exception
    {
        final OpenDCSOracleConfiguration config = new OpenDCSOracleConfiguration(tempDir);
        return config;
    }

}
