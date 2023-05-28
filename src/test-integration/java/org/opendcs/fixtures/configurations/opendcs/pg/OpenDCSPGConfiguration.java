package org.opendcs.fixtures.configurations.opendcs.pg;

import static org.opendcs.fixtures.Toolkit.args;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdbi.v3.core.Jdbi;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.spi.configuration.Configuration;

import decodes.dbimport.DbImport;
import uk.org.webcompere.systemstubs.SystemStubs;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

public class OpenDCSPGConfiguration implements Configuration
{
    private static Logger log = Logger.getLogger(OpenDCSPGConfiguration.class.getName());

    private static String dbUrl = "jdbc:tc:postgresql:15.3:///dcstest?TC_DAEMON=true&TC_TMPFS=/pg_tmpfs:rw";
    private File userDir;
    private File propertiesFile;
    private boolean started = false;
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
    private void installDb(SystemExit exit,EnvironmentVariables environment) throws Exception
    {
        if (started)
        {
            return;
        }
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
            String sequences = FileUtils.readFileToString(
                                new File(
                                    "stage/schema/opendcs-pg/sequences.sql"),
                                    Charset.forName("UTF8")
                                    );
            h.createScript(groups).executeAsSeparateStatements();
            h.createScript(opendcs).executeAsSeparateStatements();
            h.createScript(sequences).executeAsSeparateStatements();
            h.createScript("insert into DecodesDatabaseVersion values(68, '');"
                         + "insert into tsdb_database_version values(68, '');")                         
             .executeAsSeparateStatements();
            h.execute("create user dcs_proc with password 'dcs_proc'");
            h.execute("GRANT \"OTSDB_ADMIN\" TO dcs_proc");
            h.execute("GRANT \"OTSDB_MGR\" TO dcs_proc");
            environment.set("DB_USERNAME","dcs_proc");
            environment.set("DB_PASSWORD","dcs_proc");

            environment.execute(() ->
                exit.execute(() ->
                    SystemStubs.tapSystemErrAndOut(() -> {
                        ArrayList<String> theArgs = new ArrayList<>();
                        theArgs.add("-l"); theArgs.add(new File(this.getUserDir(),"/db-install.log").getAbsolutePath());
                        theArgs.add("-P"); theArgs.add(propertiesFile.getAbsolutePath());
                        theArgs.add("-d3");
                        theArgs.addAll(
                            FileUtils.listFiles(new File("stage/edit-db/enum"),null,true)
                                     .stream()
                                     .map(f->f.getAbsolutePath())
                                     .collect(Collectors.toList())
                                    );
                        DbImport.main(theArgs.toArray(new String[0]));
                    })
                )
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
    public void stop() throws Exception
    {
        System.out.println("Stopping.");
    }
}
