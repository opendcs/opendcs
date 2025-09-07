package org.opendcs.fixtures.configurations.opendcs.sqlite;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.jdbi.v3.core.Jdbi;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.MigrationManager;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.SimpleDataSource;
import org.opendcs.database.impl.opendcs.OpenDcsPgProvider;
import org.opendcs.database.impl.opendcs.OpenDcsSqliteProvider;
import org.opendcs.database.impl.opendcs.sqlite.RandomKeyGenerator;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.spi.database.MigrationProvider;
import org.testcontainers.containers.PostgreSQLContainer;

import decodes.db.Database;
import decodes.launcher.Profile;
import decodes.sql.SequenceKeyGenerator;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;
import ilex.util.FileLogger;
import opendcs.dao.CompDependsDAO;
import opendcs.dao.DaoBase;
import opendcs.dao.LoadingAppDao;
import opendcs.dao.XmitRecordDAO;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
/**
 * Handles setup of an OpenDCS Postgres SQL Database instance.
 *
 */
public class OpenDCSSqlite implements Configuration
{
    private static Logger log = Logger.getLogger(OpenDCSSqlite.class.getName());

    public static final String NAME = "OpenDCS-SQLite";

    private File userDir;
    private File propertiesFile;
    private static AtomicBoolean started = new AtomicBoolean(false);
    private HashMap<Object,Object> environmentVars = new HashMap<>();
    private Profile profile = null;
    private OpenDcsDatabase databases = null;
    private String jdbcUrl = null;

    // FUTURE work: allow passing of override values to bypass the test container creation
    // ... OR setup a separate testcontainer library like USACE did for CWMS.
    private static final String DATABASE_NAME = "dcs";
    private static final String SCHEMA_OWNING_USER = "dcs_owner";
    

    public OpenDCSSqlite(File userDir) throws Exception
    {
        this.userDir = userDir;
        this.propertiesFile = new File(userDir,"/user.properties");

    }

    @Override
    public File getPropertiesFile()
    {
        return this.propertiesFile;
    }

    @Override
    public File getUserDir()
    {
        return this.userDir;
    }

    @Override
    public boolean isSql()
    {
        return true;
    }

    @Override
    public boolean isTsdb()
    {
        return true;
    }

    @Override
    public Map<Object,Object> getEnvironment()
    {
        return this.environmentVars;
    }

    /**
     * Actually setup the database
     * @throws Exception
    */
    private void installDb(SystemExit exit,EnvironmentVariables environment, SystemProperties properties, UserPropertiesBuilder configBuilder) throws Exception
    {
        // These should always be set.
        if (isRunning())
        {
            return;
        }
        this.jdbcUrl = String.format("jdbc:sqlite:%s/test.db?busy_timeout=60000&foreign_keys=true", this.propertiesFile.getParent());
        environmentVars.put("DB_URL", jdbcUrl);
        environment.set("DB_URL", jdbcUrl);
        createPropertiesFile(configBuilder, this.propertiesFile);
        profile = Profile.getProfile(this.propertiesFile);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        
        config.setLeakDetectionThreshold(10000L);
        
        HikariDataSource ds = new HikariDataSource(config);
        

        MigrationManager mm = new MigrationManager(ds,OpenDcsSqliteProvider.NAME);
        MigrationProvider mp = mm.getMigrationProvider();
        mp.setPlaceholderValue("NUM_TS_TABLES", "1");
        mp.setPlaceholderValue("NUM_TEXT_TABLES","1");
        mm.migrate();
        Jdbi jdbi = mm.getJdbiHandle();
        log.info("Setting authentication environment vars.");
        ilex.util.Logger originalLog = ilex.util.Logger.instance();
        ilex.util.FileLogger fl = null;
        try
        {
            
            mp.loadBaselineData(profile, "", "");
        }
        finally
        {
            if (fl != null)
            {
                ilex.util.Logger.setLogger(originalLog);
                fl.close();
            }
        }
        setStarted();
    }

    @Override
    public void start(SystemExit exit, EnvironmentVariables environment, SystemProperties properties) throws Exception
    {
        File editDb = new File(userDir,"edit-db");
        new File(userDir,"output").mkdir();
        editDb.mkdirs();
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        // set username/pw (env)
        String stageDir = System.getProperty("DCSTOOL_HOME");
        FileUtils.copyDirectory(new File(stageDir+"/edit-db"),editDb);
        FileUtils.copyDirectory(new File(stageDir+"/schema"),new File(userDir,"/schema/"));
        installDb(exit, environment, properties, configBuilder);
        createPropertiesFile(configBuilder, this.propertiesFile);
    }

    private void createPropertiesFile(UserPropertiesBuilder configBuilder, File propertiesFile) throws Exception
    {
        try (OutputStream out = new FileOutputStream(propertiesFile);)
        {
            configBuilder.withDatabaseLocation(this.jdbcUrl);
            configBuilder.withEditDatabaseType("OPENTSDB");
            configBuilder.withDatabaseDriver("org.postgresql.Driver");
            configBuilder.withSiteNameTypePreference("CWMS");
            configBuilder.withSqlKeyGenerator(RandomKeyGenerator.class);
            configBuilder.withDecodesAuth("noop:");
            configBuilder.build(out);
        }
    }

    private void setStarted()
    {
        synchronized(started)
        {
            started.set(true);
        }
    }

    @Override
    public boolean isRunning()
    {
        synchronized(started)
        {
            return started.get();
        }
    }

    @Override
    public TimeSeriesDb getTsdb() throws Throwable
    {
        return getOpenDcsDatabase().getLegacyDatabase(TimeSeriesDb.class).get();
    }

    @Override
    public Database getDecodesDatabase() throws Throwable
    {
        return getOpenDcsDatabase().getLegacyDatabase(Database.class).get();
    }

    private void buildDatabases() throws Exception
    {
        Properties credentials = new Properties();
        databases = DatabaseService.getDatabaseFor("utility", DecodesSettings.fromProfile(profile), credentials);
    }

    @Override
    public boolean implementsSupportFor(Class<? extends TsdbAppTemplate> appClass)
    {
        Objects.requireNonNull(appClass, "You must specify a valid class, not null.");
        if (appClass.equals(ComputationApp.class))
        {
            return true;
        } // add more cases here.
        return false;
    }

    /**
     * 
     * @param dao Class that extends from {@link opendcs.dao.DaoBase}
     * @return true if this Database implementation supports a given dataset.
     */
    @Override
    public boolean supportsDao(Class<? extends DaoBase> dao)
    {
        Objects.requireNonNull(dao, "You must specifiy a valid class, not null.");
        /**
         * Extends this list as specific tests and requirements are added in the future.
         */
        if (dao.equals(XmitRecordDAO.class))
        {
            return true;
        }
        else if(dao.equals(CompDependsDAO.class))
        {
            return true;
        }
        else if(dao.equals(LoadingAppDao.class))
        {
            return true;
        }
        return false;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public OpenDcsDatabase getOpenDcsDatabase() throws Throwable
    {
        synchronized(this)
        {
            if (databases == null)
            {
                buildDatabases();
            }
            return databases;
        }
    }
}