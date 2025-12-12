package org.opendcs.fixtures.spi;

import java.io.File;
import java.util.Map;

import org.opendcs.database.api.OpenDcsDatabase;

import decodes.db.Database;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import opendcs.dao.DaoBase;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
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
     * @param exit SystemExit stub for configurations needing to use various OpenDCS functions
     *             that may call System.exit.
     * @param environment The System.getenv environment to hold appropriate values.
     * @param properties The System.getProperty map to hold appropriate values.
     * @throws Exception thrown if error occurs
     */
    void start(SystemExit exit, EnvironmentVariables environment, SystemProperties properties) throws Exception;

    /**
     *
     * @return true if required services/content are configured and running as appropriate
     */
    boolean isRunning();

    /**
     * Close files, shutdown databases, etc
     * @throws Exception if error occurs
     */
    default void stop() throws Throwable
    {
        // nothing to do by default
    }

    File getPropertiesFile();
    File getUserDir();
    boolean isSql();

    default boolean isTsdb()
    {
        return false;
    }

    /**
     * Additional environment variables this test configuration requires
     * @return map of environment variables/values
     */
    Map<Object,Object> getEnvironment();

    /**
     * If available return a valid instead of a TimeSeriesDb based on the current configuration.
     *
     * Default implementation returns null;
     * @return The timeseries database if it can be made.
     * @throws Throwable any issue with the creation of the TimeSeriesDb object
     */
    default TimeSeriesDb getTsdb() throws Throwable
    {
        return null;
    }

    /**
     * Returns an independent instance of the {@link decodes.db.Database} Decodes Database for this configuration.
     *
     * @return Instance of the Decodes Database for this run/test.
     * @throws Throwable if an error occurs when getting the Database
     */
    public Database getDecodesDatabase() throws Throwable;    

    default boolean implementsSupportFor(Class<? extends TsdbAppTemplate> appClass)
    {
        return false;
    }

    /**
     * Returns true if this Database implementation supports a given dataset.
     * @param dao Class that extends from {@link opendcs.dao.DaoBase}
     * @return true if the Database implementation supports the given dataset, false otherwise.
     */
    default boolean supportsDao(Class<? extends DaoBase> dao)
    {
        return false;
    };

    /* The name of this configuration
    * @return
    */
    String getName();

    OpenDcsDatabase getOpenDcsDatabase() throws Throwable;

    /**
     * Does this configuration expect the Rest API to function.
     * @return
     */
    default boolean supportsRestApi()
    {
        return false;
    }
}
