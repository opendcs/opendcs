package org.opendcs.fixtures.configurations.opendcs.pg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.fixtures.helpers.Programs;
import org.opendcs.spi.configuration.Configuration;

import decodes.tsdb.TimeSeriesDb;
import opendcs.opentsdb.OpenTsdb;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

/**
 * Handles setup of an OpenDCS Postgres SQL Database instance.
 *
 */
public class OpenDCSPGConfiguration implements Configuration
{
    private static Logger log = Logger.getLogger(OpenDCSPGConfiguration.class.getName());

    private static String dbUrl = "jdbc:tc:postgresql:15.3:///dcstest?TC_DAEMON=true&TC_TMPFS=/pg_tmpfs:rw";
    private File userDir;
    private File propertiesFile;
    private boolean started = false;
    private HashMap<Object,Object> environmentVars = new HashMap<>();

    public OpenDCSPGConfiguration(File userDir) throws Exception
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
    private void installDb(SystemExit exit,EnvironmentVariables environment) throws Exception
    {
        if (started)
        {
            return;
        }
        HashMap<String,String> placeHolders = new HashMap<>();
        placeHolders.put("NUM_TS_TABLES","1");
        placeHolders.put("NUM_TEXT_TABLES","1");

        Flyway flyway = Flyway.configure()
                              .dataSource(dbUrl,null,null)
                              .placeholders(placeHolders)
                              .locations("db/opendcs-pg")
                              .load();

        flyway.migrate();
        Jdbi jdbi = Jdbi.create(dbUrl);
        jdbi.useHandle(h->{
            log.info("Creating application user.");
            h.execute("create user dcs_proc with password 'dcs_proc'");
            h.execute("GRANT \"OTSDB_ADMIN\" TO dcs_proc");
            h.execute("GRANT \"OTSDB_MGR\" TO dcs_proc");
            log.info("Setting authentication environment vars.");
            environmentVars.put("DB_USERNAME","dcs_proc");
            environmentVars.put("DB_PASSWORD","dcs_proc");

            environment.set("DB_USERNAME","dcs_proc");
            environment.set("DB_PASSWORD","dcs_proc");

            log.info("Loading base data.");
            Programs.DbImport(new File(this.getUserDir(),"/db-install.log"),
                              propertiesFile,
                              environment,exit,
                              "stage/edit-db/enum",
                              "stage/edit-db/eu/EngineeringUnitList.xml",
                              "stage/edit-db/datatype/DataTypeEquivalenceList.xml",
                              "stage/edit-db/presentation",
                              "stage/edit-db/loading-app"
            );
        });
        started = true;
    }

    @Override
    public void start(SystemExit exit, EnvironmentVariables environment) throws Exception
    {
        File editDb = new File(userDir,"edit-db");
        new File(userDir,"output").mkdir();
        editDb.mkdirs();
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        configBuilder.withDatabaseLocation(dbUrl);
        configBuilder.withEditDatabaseType("OPENTSDB");
        configBuilder.withDatabaseDriver("org.postgresql.Driver");
        configBuilder.withSiteNameTypePreference("CWMS");
        configBuilder.withDecodesAuth("env-auth-source:username=DB_USERNAME,password=DB_PASSWORD");
        // set username/pw (env)
        try (OutputStream out = new FileOutputStream(propertiesFile);)
        {
            configBuilder.build(out);
            FileUtils.copyDirectory(new File("stage/edit-db"),editDb);
            FileUtils.copyDirectory(new File("stage/schema"),new File(userDir,"/schema/"));
            installDb(exit,environment);
        }
    }

    @Override
    public boolean isRunning() {
        return started;
    }

    @Override
    public TimeSeriesDb getTsdb() throws Throwable
    {
        OpenTsdb db = new OpenTsdb();
        Properties credentials = new Properties();
        credentials.put("username","dcs_proc");
        credentials.put("password","dcs_proc");
        db.connect("utility",credentials);
        return db;
    }
}
