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
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Assumptions;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.fixtures.configurations.opendcs.pg.OpenDCSPGConfiguration;
import org.opendcs.spi.configuration.Configuration;
import org.python.antlr.PythonParser.try_stmt_return;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import mil.army.usace.hec.test.database.CwmsDatabaseContainer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

public class CwmsOracleConfiguration implements Configuration
{
    private static Logger log = Logger.getLogger(OpenDCSPGConfiguration.class.getName());

    private static final String CWMS_ORACLE_IMAGE = System.getProperty("opendcs.cwms.oracle.image","docker pull gvenzl/oracle-xe:21.3.0-faststart");
    private static final String CWMS_ORACLE_VOLUME = System.getProperty("opendcs.cwms.oracle.volume","cwms_opendcs_volume");
    private static final String CWMS_SCHEMA_IMAGE = System.getProperty("opendcs.cwms.schema.image","registry.hecdev.net/cwms/schema_installer:23.03.16");

    public static final String NAME = "CWMS-Oracle";

    private CwmsDatabaseContainer<?> cwmsDb = null;
    private String dbUrl = null;
    private File userDir;
    private File propertiesFile;
    private boolean started = false;
    private HashMap<Object,Object> environmentVars = new HashMap<>();
    private String dcsUser = null;
    private String dcsUserPassword = null;

    public CwmsOracleConfiguration(File userDir)
    {
        this.userDir = userDir;
        this.propertiesFile = new File(userDir,"/user.properties");
    }

    private void installDb(SystemExit exit,EnvironmentVariables environment) throws Exception
    {
        if (started)
        {
            return;
        }
        cwmsDb = new CwmsDatabaseContainer<>(CWMS_ORACLE_IMAGE)
                        .withSchemaImage(CWMS_SCHEMA_IMAGE)
                        .withVolumeName(CWMS_ORACLE_VOLUME);
        log.info("starting CWMS Database");
        cwmsDb.start();
        log.info("CWMS Database started.");
        this.dbUrl = cwmsDb.getJdbcUrl();
        dcsUser = System.getProperty("opendcs.cwms.dcsuser.name",cwmsDb.getUsername());
        dcsUserPassword = System.getProperty("opendcs.cwms.dcsuser.password",cwmsDb.getPassword());
        environment.set("DB_USERNAME",dcsUser);
        environment.set("DB_PASSWORD",dcsUserPassword);
        started = true;
        //TODO strip/reinstall schema
    }

    @Override
    public void start(SystemExit exit, EnvironmentVariables environment) throws Exception
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
        
        // set username/pw (env)
        try (OutputStream out = new FileOutputStream(propertiesFile);)
        {
            configBuilder.build(out);
            FileUtils.copyDirectory(new File("stage/edit-db"),editDb);
            FileUtils.copyDirectory(new File("stage/schema"),new File(userDir,"/schema/"));
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
        CwmsTimeSeriesDb db = new CwmsTimeSeriesDb();
        Properties credentials = new Properties();
        credentials.put("username",dcsUser);
        credentials.put("password",dcsUserPassword);
        db.connect("utility",credentials);
        return db;
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
}
