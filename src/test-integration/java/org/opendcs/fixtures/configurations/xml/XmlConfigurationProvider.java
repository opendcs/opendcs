package org.opendcs.fixtures.configurations.xml;

import java.io.File;

import org.opendcs.fixtures.OpenDCSAppTestCase;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class XmlConfigurationProvider implements ConfigurationProvider
{

    @Override
    public String getImplementation() {
        return "XML";
    }

    @Override
    public OpenDCSAppTestCase getConfig(File tempDir) {
        final XmlConfiguration config = new XmlConfiguration(tempDir);
        return new OpenDCSAppTestCase(getImplementation(), config);
    }
    
}
