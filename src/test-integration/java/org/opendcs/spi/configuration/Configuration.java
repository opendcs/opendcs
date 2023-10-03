package org.opendcs.spi.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.Extension;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

/**
 * Baseline of a test implementation configuration
 */
public interface Configuration {
    /**
     * Do any configuration or initialization options that affect the system.
     * Such as:
     *  - copying require files
     *  - creating the user.properties file
     *  - starting and installing database schemas 
     * @param exit SystemExit stub for configurations needing to use various opendcs functions
     *             that may call System.exit.
     * @throws Exception
     */
    public void start(SystemExit exit, EnvironmentVariables environment) throws Exception;

    /**
     *
     * @return true if required services/content are configured and running as appropriate
     */
    public boolean isRunning();

    public default void stop() throws Exception
    {
        // nothing to do by default
    }
    public File getPropertiesFile();
    public File getUserDir();
    public boolean isSql();
    public default List<Extension> getExtensions()
    {
        return new ArrayList<>();
    }

    /**
     * Additional environment variables this test configuration requires
     * @return
     */
    public default Map<String,String> getEnvironment()
    {
        return null;
    }
}
