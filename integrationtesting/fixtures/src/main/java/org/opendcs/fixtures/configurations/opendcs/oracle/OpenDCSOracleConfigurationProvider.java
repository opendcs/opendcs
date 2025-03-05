package org.opendcs.fixtures.configurations.opendcs.oracle;

import java.io.File;

import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.fixtures.spi.ConfigurationProvider;

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
