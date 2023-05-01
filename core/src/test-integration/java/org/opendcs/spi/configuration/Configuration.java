package org.opendcs.spi.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.Extension;

import decodes.tsdb.TimeSeriesDb;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

/**
 * Baseline of a test implementation configuration
 */
public interface Configuration
{
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

    /**
     * Close files, shutdown databases, etc
     * @throws Exception
     */
    public default void stop() throws Throwable
    {
        // nothing to do by default
    }

    public File getPropertiesFile();
    public File getUserDir();
    public boolean isSql();
    default public boolean isTsdb()
    {
        return false;
    }

    /**
     * Additional environment variables this test configuration requires
     * @return
     */
    public default Map<String,String> getEnvironment()
    {
        return null;
    }

    /**
     * If available return a valid instead of a TimeSeriesDb based on the current configuration.
     *
     * Default implementation returns null;
     * @return The timeseries database if it can be made.
     * @throws Throwable any issue with the creation of the TimeSeriesDb object
     */
    default public TimeSeriesDb getTsdb() throws Throwable
    {
        return null;
    }
}
