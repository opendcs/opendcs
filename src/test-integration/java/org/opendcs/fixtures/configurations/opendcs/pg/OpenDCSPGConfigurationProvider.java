package org.opendcs.fixtures.configurations.opendcs.pg;

import java.io.File;

import org.opendcs.fixtures.OpenDCSAppTestCase;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class OpenDCSPGConfigurationProvider implements ConfigurationProvider
{

    @Override
    public String getImplementation() {
        return "OpenDCS-Postgres";
    }

    @Override
    public OpenDCSAppTestCase getConfig(File tempDir) throws Exception
    {
        final OpenDCSPGConfiguration config = new OpenDCSPGConfiguration(tempDir);
        return new OpenDCSAppTestCase(getImplementation(), config);
    }
    
}
