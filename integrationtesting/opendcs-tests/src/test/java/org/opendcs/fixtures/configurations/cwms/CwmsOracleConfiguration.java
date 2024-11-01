package org.opendcs.fixtures.configurations.cwms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.fixtures.configurations.opendcs.pg.OpenDCSPGConfiguration;
import org.opendcs.spi.configuration.Configuration;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Database;
import decodes.launcher.Profile;
import decodes.sql.OracleSequenceKeyGenerator;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;
import ilex.util.Pair;
import mil.army.usace.hec.test.database.CwmsDatabaseContainer;
import opendcs.dao.CompDependsDAO;
import opendcs.dao.DaoBase;
import opendcs.dao.LoadingAppDao;
import opendcs.dao.XmitRecordDAO;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;

public class CwmsOracleConfiguration implements Configuration
{
    private static Logger log = Logger.getLogger(OpenDCSPGConfiguration.class.getName());

    private static final String CWMS_ORACLE_IMAGE = System.getProperty("opendcs.cwms.oracle.image","docker pull gvenzl/oracle-xe:21.3.0-faststart");
    private static final String CWMS_ORACLE_VOLUME = System.getProperty("opendcs.cwms.oracle.volume","cwms_opendcs_volume");
    private static final String CWMS_SCHEMA_IMAGE = System.getProperty("opendcs.cwms.schema.image","<must be see on commandline>");

    public static final String NAME = "CWMS-Oracle";

    private CwmsDatabaseContainer<?> cwmsDb = null;
    private String dbUrl = null;
    private File userDir;
    private File propertiesFile;
    private boolean started = false;
    private HashMap<Object,Object> environmentVars = new HashMap<>();
    private String dcsUser = null;
    private String dcsUserPassword = null;
    private Profile profile = null;
    private OpenDcsDatabase databases = null;

    public CwmsOracleConfiguration(File userDir)
    {
        this.userDir = userDir;
        this.propertiesFile = new File(userDir,"/user.properties");
    }

    private void installDb(SystemExit exit,EnvironmentVariables environment) throws Exception
    {
        if (!started)
        {
            cwmsDb = new CwmsDatabaseContainer<>(CWMS_ORACLE_IMAGE)
                            .withSchemaImage(CWMS_SCHEMA_IMAGE)
                            .withVolumeName(CWMS_ORACLE_VOLUME);
            log.info("starting CWMS Database");
            cwmsDb.start();
        }
        log.info("CWMS Database started.");
        this.dbUrl = cwmsDb.getJdbcUrl();
        dcsUser = System.getProperty("opendcs.cwms.dcsuser.name",cwmsDb.getUsername());
        dcsUserPassword = System.getProperty("opendcs.cwms.dcsuser.password",cwmsDb.getPassword());
        environment.set("DB_USERNAME",dcsUser);
        environment.set("DB_PASSWORD",dcsUserPassword);
        environmentVars.put("DB_USERNAME",dcsUser);
        environmentVars.put("DB_PASSWORD",dcsUserPassword);
        started = true;
        //TODO strip/reinstall schema
    }

    @Override
    public void start(SystemExit exit, EnvironmentVariables environment, SystemProperties properties) throws Exception
    {       
        if (!started) 
        {
            File editDb = new File(userDir,"edit-db");
            new File(userDir,"output").mkdir();
            editDb.mkdirs();
            installDb(exit,environment);
            UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
            configBuilder.withDatabaseLocation(dbUrl);
            configBuilder.withEditDatabaseType("CWMS");
            configBuilder.withDatabaseDriver("oracle.jdbc.driver.OracleDriver");
            configBuilder.withSiteNameTypePreference("CWMS");
            configBuilder.withDecodesAuth("env-auth-source:username=DB_USERNAME,password=DB_PASSWORD");
            configBuilder.withCwmsOffice(cwmsDb.getOfficeId());
            configBuilder.withDbOffice(cwmsDb.getOfficeId());
            configBuilder.withWriteCwmsLocations(true);
            configBuilder.withSqlKeyGenerator(OracleSequenceKeyGenerator.class);
            // set username/pw (env)
            try (OutputStream out = new FileOutputStream(propertiesFile);)
            {
                configBuilder.build(out);
                FileUtils.copyDirectory(new File("stage/edit-db"),editDb);
                FileUtils.copyDirectory(new File("stage/schema"),new File(userDir,"/schema/"));
            }
            try (OutputStream out = new FileOutputStream(new File(userDir,"logfilter.txt")))
            {
                out.write("org.jooq\n".getBytes());
            }
            profile = Profile.getProfile(propertiesFile);
        }
    }

    @Override
    public boolean isRunning()
    {
        return started;
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
    public TimeSeriesDb getTsdb() throws Throwable
    {
        synchronized (this)
        {
            if (databases == null)
            {
                buildDatabases();
            }
            return databases.getLegacyDatabase(TimeSeriesDb.class).get();
        }
    }

    @Override
    public Database getDecodesDatabase() throws Throwable
    {
        synchronized (this)
        {
            if (databases == null)
            {
                buildDatabases();
            }
            return databases.getLegacyDatabase(Database.class).get();
        }
    }

    private void buildDatabases() throws Exception
    {
        DecodesSettings settings = DecodesSettings.fromProfile(profile);
        Properties credentials = new Properties();
        
        credentials.put("username",dcsUser);
        credentials.put("password",dcsUserPassword);
        databases = DatabaseService.getDatabaseFor("utility", settings, credentials);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Map<Object, Object> getEnvironment()
    {
        return environmentVars;
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
     * Returns true if this Database implementation supports a given dataset.
     * @param dao Class that extends from {@link opendcs.dao.DaoBase}
     * @return
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
}
