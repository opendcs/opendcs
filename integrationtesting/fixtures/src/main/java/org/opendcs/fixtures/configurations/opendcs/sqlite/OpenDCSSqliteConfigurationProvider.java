package org.opendcs.fixtures.configurations.opendcs.sqlite;

import java.io.File;

import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.fixtures.spi.ConfigurationProvider;

public class OpenDCSSqliteConfigurationProvider implements ConfigurationProvider
{

    @Override
    public String getImplementation() {
        return OpenDCSSqlite.NAME;
    }

    @Override
    public Configuration getConfig(File tempDir) throws Exception
    {
        final OpenDCSSqlite config = new OpenDCSSqlite(tempDir);
        return config;
    }

}
