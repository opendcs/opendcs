package org.opendcs.fixtures.configurations.cwms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.MigrationManager;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.SimpleDataSource;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.spi.database.MigrationProvider;
import org.testcontainers.containers.output.OutputFrame;

import decodes.db.Database;
import decodes.launcher.Profile;
import decodes.sql.OracleSequenceKeyGenerator;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;
import mil.army.usace.hec.test.database.CwmsDatabaseContainer;
import mil.army.usace.hec.test.database.CwmsDatabaseContainers;
import opendcs.dao.CompDependsDAO;
import opendcs.dao.DaoBase;
import opendcs.dao.LoadingAppDao;
import opendcs.dao.XmitRecordDAO;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;

public class CwmsOracleConfiguration implements Configuration
{
    private static Logger log = Logger.getLogger(CwmsOracleConfiguration.class.getName());

    private static final String CWMS_ORACLE_IMAGE = System.getProperty("opendcs.cwms.oracle.image","registry-public.hecdev.net/cwms/database-ready-ora-23.5:latest-dev");
    private static final String CWMS_ORACLE_VOLUME = System.getProperty("opendcs.cwms.oracle.volume","cwms_opendcs_volume");
    private static final String CWMS_SCHEMA_IMAGE = System.getProperty("opendcs.cwms.schema.image","registry-public.hecdev.net/cwms/schema_installer:latest-dev");
    private static final String CWMS_BUILDUSER_PASSWORD = System.getProperty("opendcs.cwms.build.user.password","antbuildpassword");

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

    private void installDb(SystemExit exit,EnvironmentVariables environment, UserPropertiesBuilder configBuilder) throws Exception
    {

        if (!started)
        {
            cwmsDb = CwmsDatabaseContainers.createDatabaseContainer(CWMS_ORACLE_IMAGE)
                            .withSchemaImage(CWMS_SCHEMA_IMAGE)
                            .withVolumeName(CWMS_ORACLE_VOLUME)
                            .withOfficeId("SPK")
                            .withOfficeEroc("l2")
                            .withEnv("BUILDUSER_PASSWORD", CWMS_BUILDUSER_PASSWORD)
                            .withCreateContainerCmdModifier(cmd ->
                            {
                                cmd.getHostConfig()
                                   .withMemory(4L*1024*1024*1024)
                                   .withCpuCount(2L)
                                ;
                            })
                            .withLogConsumer(line -> {
                                log.info(((OutputFrame)line).getUtf8String());
                            });
            log.info("starting CWMS Database");
            cwmsDb.start();
            log.info("CWMS Database started.");
            try
            {
                cwmsDb.executeSQL("create tablespace CCP_DATA DATAFILE 'ccp_data.dbf' SIZE 100M REUSE AUTOEXTEND ON NEXT 1M MAXSIZE 2000M","sys");
            }
            catch (RuntimeException ex)
            {
                if (ex.getCause() instanceof SQLException)
                {
                    log.log(Level.WARNING, "Create tablespace failed", ex);
                }
                else
                {
                    throw ex;
                }
            }
            
            
            String createBuildUser = IOUtils.resourceToString("/database/admin_user.sql", StandardCharsets.UTF_8);
            cwmsDb.executeSQL(createBuildUser, "sys");
            SimpleDataSource ds = new SimpleDataSource(cwmsDb.getJdbcUrl(), "builduser", CWMS_BUILDUSER_PASSWORD);

            MigrationManager mm = new MigrationManager(ds, NAME);
            MigrationProvider mp = mm.getMigrationProvider();
            mp.setPlaceholderValue("CWMS_SCHEMA", "CWMS_20");
            mp.setPlaceholderValue("CCP_SCHEMA", "CCP");
            mp.setPlaceholderValue("DEFAULT_OFFICE_CODE", "44");
            mp.setPlaceholderValue("DEFAULT_OFFICE", "SPK");
            mp.setPlaceholderValue("TABLE_SPACE_SPEC", "tablespace CCP_DATA");
            mm.migrate();
            ArrayList<String> roles = new ArrayList<>();
            roles.add("CCP Mgr");
            roles.add("CCP Proc");
            roles.add("CWMS Users");
            this.dcsUser = "dcs_user";
            this.dcsUserPassword = "dcs_user";
            mm.createUser(dcsUser, dcsUserPassword, roles);
            this.dbUrl = cwmsDb.getJdbcUrl();
            createPropertiesFile(configBuilder, this.propertiesFile);
            profile = Profile.getProfile(this.propertiesFile);
            mp.loadBaselineData(profile, dcsUser, dcsUserPassword);
        }


        environment.set("DB_USERNAME",dcsUser);
        environment.set("DB_PASSWORD",dcsUserPassword);
        environmentVars.put("DB_USERNAME",dcsUser);
        environmentVars.put("DB_PASSWORD",dcsUserPassword);
        environmentVars.put("DB_OFFICE", cwmsDb.getOfficeId());
        environmentVars.put("DB_URL", cwmsDb.getJdbcUrl());

        started = true;
    }

    @Override
    public void start(SystemExit exit, EnvironmentVariables environment, SystemProperties properties) throws Exception
    {
        File editDb = new File(userDir,"edit-db");
        new File(userDir,"output").mkdir();
        editDb.mkdirs();
        String stageDir = System.getProperty("DCSTOOL_HOME");
        FileUtils.copyDirectory(new File(stageDir + "/edit-db"),editDb);
        FileUtils.copyDirectory(new File(stageDir + "/schema"),new File(userDir,"/schema/"));
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        installDb(exit, environment, configBuilder); // need files copied first.
        createPropertiesFile(configBuilder, this.propertiesFile);
        try (OutputStream out = new FileOutputStream(new File(userDir,"logfilter.txt")))
        {
            out.write("org.jooq".getBytes());
        }
    }

    private void createPropertiesFile(UserPropertiesBuilder configBuilder, File propertiesFile) throws Exception
    {
        configBuilder.withEditDatabaseType("CWMS");
        configBuilder.withDatabaseDriver("oracle.jdbc.driver.OracleDriver");
        configBuilder.withSiteNameTypePreference("CWMS");
        configBuilder.withDecodesAuth("env-auth-source:username=DB_USERNAME,password=DB_PASSWORD");
        configBuilder.withCwmsOffice(cwmsDb.getOfficeId());
        configBuilder.withDbOffice(cwmsDb.getOfficeId());
        configBuilder.withWriteCwmsLocations(true);
        configBuilder.withSqlKeyGenerator(OracleSequenceKeyGenerator.class);
        configBuilder.withDatabaseLocation(dbUrl);
        try (OutputStream out = new FileOutputStream(propertiesFile);)
        {
            configBuilder.build(out);
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
    public OpenDcsDatabase getOpenDcsDatabase() throws Throwable
    {
        synchronized (this)
        {
            if (databases == null)
            {
                buildDatabases();
            }
            return databases;
        }
    }
}