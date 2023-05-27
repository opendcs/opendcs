package org.opendcs.fixtures.configurations.opendcs.pg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdbi.v3.core.Jdbi;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.spi.configuration.Configuration;

public class OpenDCSPGConfiguration implements Configuration
{
    private static Logger log = Logger.getLogger(OpenDCSPGConfiguration.class.getName());

    private static String dbUrl = "jdbc:tc:postgresql:15.3:///dcstest?TC_DAEMON=true&TC_REUSABLE=true";
    private File userDir;
    private File propertiesFile;
    private HashMap<String,String> environmentVars = new HashMap<>();

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
    public Map<String,String> getEnvironment()
    {
        return this.environmentVars;
    }

    /**
     * Actually setup the database
     * @throws Exception
    */
    private void installDb() throws IOException
    {
        Jdbi jdbi = Jdbi.create(dbUrl);
        jdbi.useHandle(h->{
            String groups = FileUtils.readFileToString(
                                new File(
                                    "stage/schema/opendcs-pg/group_roles.sql"),
                                    Charset.forName("UTF8"));
            String opendcs = FileUtils.readFileToString(
                                new File(
                                    "stage/schema/opendcs-pg/opendcs.sql"),
                                    Charset.forName("UTF8")
                                    );
            h.createScript(groups).executeAsSeparateStatements();
            h.createScript(opendcs).executeAsSeparateStatements();
            h.createScript("insert into DecodesDatabaseVersion values(68, '');"
                         + "insert into tsdb_database_version values(68, '');")                         
             .executeAsSeparateStatements();
            h.execute("create user dcs_proc with password 'dcs_proc'");
            h.execute("GRANT \"OTSDB_ADMIN\" TO dcs_proc");
            h.execute("GRANT \"OTSDB_MGR\" TO dcs_proc");
            this.environmentVars.put("DB_USERNAME","dcs_proc");
            this.environmentVars.put("DB_PASSWORD","dcs_proc");
        });
    }

    @Override
    public void start() throws Exception
    {
        new File(userDir,"output").mkdir();        
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        configBuilder.withDatabaseLocation(dbUrl);
        configBuilder.withEditDatabaseType("OPENTSDB");
        configBuilder.withSiteNameTypePreference("CWMS");
        configBuilder.withDecodesAuth("env-auth-source:username=DB_USERNAME,password=DB_PASSWORD");
        // set username/pw (env)
        try(OutputStream out = new FileOutputStream(propertiesFile);)
        {
            installDb();
            configBuilder.build(out);
        }
    }

    @Override
    public void stop() throws Exception
    {
        System.out.println("Stopping.");
    }
}
