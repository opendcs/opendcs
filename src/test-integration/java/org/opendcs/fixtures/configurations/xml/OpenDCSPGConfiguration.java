package org.opendcs.fixtures.configurations.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.jdbi.v3.core.Jdbi;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.spi.configuration.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

public class OpenDCSPGConfiguration implements Configuration
{
    private static Logger log = Logger.getLogger(OpenDCSPGConfiguration.class.getName());

    private static String dbUrl = "jdbc:tc:postgresql:15.3///dcstest";
    private File userDir;
    private File propertiesFile;
    //private PostgreSQLContainer<?> db = null;

    public OpenDCSPGConfiguration(File userDir)
    {
        //this.db = new PostgreSQLContainer<>("postgres:15.3")
        //                .withDatabaseName("OpenDCSTest")
        //                .with                ;
        //db.withDatabaseName(null)
        //db.start();
        
        this.userDir = userDir;
        this.propertiesFile = new File(userDir,"/user.properties");
        File editDb = new File(userDir,"edit-db");
        new File(userDir,"output").mkdir();
        editDb.mkdirs();
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        configBuilder.withDatabaseLocation("jdbc:tc:postgresql:15.3///dcstest");
        configBuilder.withEditDatabaseType("OPENTSDB");
        configBuilder.withSiteNameTypePreference("CWMS");
        installDb();
        // set username/pw (env)
        try(OutputStream out = new FileOutputStream(propertiesFile);)
        {
            configBuilder.build(out);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Unable to setup configuration.",ex);
        }
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
    
    /**
     * Actually setup the database
     * @param dbUrl2
     * @throws Exception
    */
    private void installDb()
    {
        Jdbi jdbi = Jdbi.create(dbUrl);
        jdbi.useHandle(h->{
            
        });
    }
}
