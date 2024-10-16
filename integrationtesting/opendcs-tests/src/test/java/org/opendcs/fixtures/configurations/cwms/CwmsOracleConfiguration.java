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
import org.opendcs.database.MigrationManager;
import org.opendcs.database.SimpleDataSource;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.fixtures.configurations.opendcs.pg.OpenDCSPGConfiguration;
import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.database.MigrationProvider;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.sql.OracleSequenceKeyGenerator;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
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
    private static Logger log = Logger.getLogger(OpenDCSPGConfiguration.class.getName());

    private static final String CWMS_ORACLE_IMAGE = System.getProperty("opendcs.cwms.oracle.image","gvenzl/oracle-free:23.5-full-faststart");
    private static final String CWMS_ORACLE_VOLUME = System.getProperty("opendcs.cwms.oracle.volume","cwms_opendcs_volume");
    private static final String CWMS_SCHEMA_IMAGE = System.getProperty("opendcs.cwms.schema.image","registry.hecdev.net/cwms/schema_installer:latest-dev");

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
        if (!started)
        {
            cwmsDb = CwmsDatabaseContainers.createDatabaseContainer(CWMS_ORACLE_IMAGE)
                            .withSchemaImage(CWMS_SCHEMA_IMAGE)
                            .withVolumeName(CWMS_ORACLE_VOLUME);
            log.info("starting CWMS Database");
            cwmsDb.start();
            log.info("CWMS Database started.");
            cwmsDb.executeSQL("create user CCP no authentication", "sys");
            cwmsDb.executeSQL("alter user CCP GRANT CONNECT through " + cwmsDb.getUsername(), "sys");
            cwmsDb.executeSQL("GRANT CWMS_USER TO CCP","sys");
            cwmsDb.executeSQL("GRANT ALTER ANY TABLE,CREATE ANY TABLE,CREATE ANY INDEX,CREATE ANY SEQUENCE,"
                            + "CREATE ANY VIEW,CREATE ANY PROCEDURE,CREATE ANY TRIGGER,CREATE ANY JOB,"
                            + "CREATE ANY SYNONYM,DROP ANY SYNONYM,CREATE PUBLIC SYNONYM,DROP PUBLIC SYNONYM"
                            + " TO CCP");
            cwmsDb.executeSQL("GRANT CREATE ANY CONTEXT,ADMINISTER DATABASE TRIGGER TO CCP");
            cwmsDb.executeSQL("begin\n" + //
                                "  -- Grant the aq object permissions to the CCP\n" + //
                                "  GRANT SELECT ON dba_scheduler_jobs to CCP;\n" + //
                                "  GRANT SELECT ON dba_queue_subscribers to CCP;\n" + //
                                "  GRANT SELECT ON dba_subscr_registrations to CCP;\n" + //
                                "  GRANT SELECT ON dba_queues to CCP;\n" + //
                                "  GRANT EXECUTE ON dbms_aq TO CCP;\n" + //
                                "  GRANT EXECUTE ON dbms_aqadm TO CCP;\n" + //
                                "  -- Grant the vpd privileges to the CCP.\n" + //
                                "  GRANT EXECUTE ON DBMS_SESSION to CCP;\n" + //
                                "  GRANT EXECUTE ON DBMS_RLS to CCP;\n" + //
                                "  -- Grant the aqadm privileges to the CCP.\n" + //
                                "\n" + //
                                "    sys.dbms_aqadm.grant_system_privilege (\n" + //
                                "      privilege    => 'enqueue_any',\n" + //
                                "      grantee      => 'CCP',\n" + //
                                "      admin_option => false);\n" + //
                                "    sys.dbms_aqadm.grant_system_privilege (\n" + //
                                "      privilege    => 'dequeue_any',\n" + //
                                "      grantee      => 'CCP',\n" + //
                                "      admin_option => false);\n" + //
                                "    sys.dbms_aqadm.grant_system_privilege (\n" + //
                                "      privilege    => 'manage_any',\n" + //
                                "      grantee      => 'CCP',\n" + //
                                "      admin_option => false);\n" + //
                                "  -- Grant the permissions on cwms tables, views, and packages to the CCP\n" + //
                                "  GRANT SELECT ON cwms_v_loc TO CCP WITH GRANT OPTION;\n" + //
                                "  GRANT SELECT ON cwms_v_ts_id TO CCP WITH GRANT OPTION;\n" + //
                                "  GRANT SELECT ON cwms_v_tsv TO CCP;\n" + //
                                "  GRANT SELECT ON cwms_20.cwms_seq TO CCP;\n" + //
                                "  GRANT SELECT ON cwms_20.cwms_seq TO CCP_USERS;\n" + //
                                "\n" + //
                                "  GRANT EXECUTE ON cwms_t_date_table TO CCP;\n" + //
                                "  GRANT EXECUTE ON cwms_t_jms_map_msg_tab TO CCP;\n" + //
                                "\n" + //
                                "  GRANT EXECUTE ON CWMS_20.cwms_ts TO CCP;\n" + //
                                "  GRANT EXECUTE ON CWMS_20.cwms_msg TO CCP;\n" + //
                                "  GRANT EXECUTE ON CWMS_20.cwms_util TO CCP;\n" + //
                                "  GRANT EXECUTE ON CWMS_20.cwms_sec TO CCP;\n" + //
                                "\n" + //
                                "  GRANT EXECUTE ON CWMS_20.cwms_env TO CCP;\n" + //
                                "  GRANT EXECUTE ON CWMS_20.cwms_env TO CCP_USERS;\n" + //
                                "\n" + //
                                "  -- Grant the permissions on cwms tables to the CCP for multiple office\n" + //
                                "  -- GRANT SELECT ON CWMS_20.at_sec_user_office TO CCP;\n" + //
                                "\n" + //
                                "  ALTER USER CCP DEFAULT ROLE ALL;\n" + //
                                "END;","sys");

            SimpleDataSource ds = new SimpleDataSource(cwmsDb.getJdbcUrl(),
                                                       cwmsDb.getUsername()+"[CCP]",
                                                       cwmsDb.getPassword());
            MigrationManager mm = new MigrationManager(ds, NAME);
            MigrationProvider mp = mm.getMigrationProvider();
            mp.setPlaceholderValue("CWMS_SCHEMA", "CWMS_20");
            mp.setPlaceholderValue("CCP_SCHEMA", "CCP");
            mp.setPlaceholderValue("DEFAULT_OFFICE_CODE", "1");
            mm.migrate();
        }

        this.dbUrl = cwmsDb.getJdbcUrl();
        dcsUser = System.getProperty("opendcs.cwms.dcsuser.name",cwmsDb.getUsername());
        dcsUserPassword = System.getProperty("opendcs.cwms.dcsuser.password",cwmsDb.getPassword());
        environment.set("DB_USERNAME",dcsUser);
        environment.set("DB_PASSWORD",dcsUserPassword);
        environmentVars.put("DB_USERNAME",dcsUser);
        environmentVars.put("DB_PASSWORD",dcsUserPassword);

        started = true;
    }

    @Override
    public void start(SystemExit exit, EnvironmentVariables environment, SystemProperties properties) throws Exception
    {        
        File editDb = new File(userDir,"edit-db");
        new File(userDir,"output").mkdir();
        editDb.mkdirs();
        FileUtils.copyDirectory(new File("stage/edit-db"),editDb);
        FileUtils.copyDirectory(new File("stage/schema"),new File(userDir,"/schema/"));        
        installDb(exit,environment); // need files copied first.
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
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
        
        
        try (OutputStream out = new FileOutputStream(new File(userDir,"logfilter.txt")))
        {
            out.write("org.jooq\n".getBytes());
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
