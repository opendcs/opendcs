/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.opendcs.spi.configuration;

import java.io.File;
import java.util.Map;

import decodes.db.DatabaseIO;
import decodes.util.DecodesSettings;

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
     * @throws Exception
     */
    public void start(SystemExit exit, EnvironmentVariables environment, SystemProperties properties) throws Exception;

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
    public default boolean isTsdb()
    {
        return false;
    }

    /**
     * Additional environment variables this test configuration requires
     * @return
     */
    public Map<Object,Object> getEnvironment();

    /**
     * If available return a valid instead of a TimeSeriesDb based on the current configuration.
     *
     * Default implementation returns null;
     * @return The timeseries database if it can be made.
     * @throws Throwable any issue with the creation of the TimeSeriesDb object
     */
    public default TimeSeriesDb getTsdb() throws Throwable
    {
        return null;
    }

    /**
     * Returns an independent instance of the {@link decodes.db.Database} Decodes Database for this configuration.
     *
     * @return Instance of the Decodes Database for this run/test.
     * @throws Throwable
     */
    default public Database getDecodesDatabase() throws Throwable
    {
        DecodesSettings settings = DecodesSettings.instance();
        Database db = new Database(true);
        DatabaseIO dbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode, settings.editDatabaseLocation);
        db.setDbIo(dbio);
        db.read();
        Database.setDb(db);
        return db;
    }

    public default boolean implementsSupportFor(Class<? extends TsdbAppTemplate> appClass)
    {
        return false;
    }

    /**
     * Returns true if this Database implementation supports a given dataset.
     * @param dao Class that extends from {@link opendcs.dao.DaoBase}
     * @return
     */
    default public boolean supportsDao(Class<? extends DaoBase> dao)
    {
        return false;
    }

    /* The name of this configuration
    * @return
    */
    public String getName();
}
