package org.opendcs.fixtures.configurations.opendcs.pg;

import java.io.File;

import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class OpenDCSPGConfigurationProvider implements ConfigurationProvider
{

    @Override
    public String getImplementation() {
        return OpenDCSPGConfiguration.NAME;
    }

    @Override
    public Configuration getConfig(File tempDir) throws Exception
    {
        final OpenDCSPGConfiguration config = new OpenDCSPGConfiguration(tempDir);
        return config;
    }

}
