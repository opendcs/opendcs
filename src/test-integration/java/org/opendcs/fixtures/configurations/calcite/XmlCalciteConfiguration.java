package org.opendcs.fixtures.configurations.calcite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.spi.configuration.Configuration;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

/**
 * Handles setup for XML Database based tests.
 */
public class XmlCalciteConfiguration implements Configuration
{
    private static final Logger logger = Logger.getLogger(XmlCalciteConfiguration.class.getName());

    File userDir;
    File propertiesFile;
    private boolean started = false;

    public XmlCalciteConfiguration(File userDir) throws Exception
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
    public boolean isSql()
    {
        return false;
    }

    @Override
    public File getUserDir() {
        return this.userDir;
    }

    @Override
    public void start(SystemExit exit, EnvironmentVariables environment) throws Exception {
        File editDb = new File(userDir,"edit-db");
        new File(userDir,"output").mkdir();
        editDb.mkdirs();
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        configBuilder.withDatabaseLocation("$DCSTOOL_USERDIR/edit-db");
        configBuilder.withEditDatabaseType("XML-Calcite");
        configBuilder.withSiteNameTypePreference("CWMS");
        try(OutputStream out = new FileOutputStream(propertiesFile);)
        {
            FileUtils.copyDirectory(new File("stage/edit-db"),editDb);
            FileUtils.copyDirectory(new File("stage/schema"),new File(userDir,"/schema/"));
            configBuilder.build(out);
            started = true;
        }
    }

    @Override
    public boolean isRunning() {
        return started;
    }
}
