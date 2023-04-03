package org.opendcs.fixtures;

import java.util.Objects;

import org.opendcs.spi.configuration.Configuration;

public class OpenDCSAppTestCase
{
    protected String configurationName;
    protected Configuration config;

    @SuppressWarnings("unused")
    private OpenDCSAppTestCase() {}

    public OpenDCSAppTestCase(String configurationName, Configuration config)
    {
        Objects.requireNonNull(configurationName, "Configuration Context must have a name.");
        Objects.requireNonNull(config, "Configuration must be provided.");
        this.configurationName = configurationName;
        this.config = config;
    }

    public String getDisplayName()
    {
        return this.configurationName + "-" + config.isSql() + "-" + config.getPropertiesFile().getAbsolutePath();
    }

    public Configuration getConfiguration()
    {
        return config;
    }
}
