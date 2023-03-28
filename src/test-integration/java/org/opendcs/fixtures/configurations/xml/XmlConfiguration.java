package org.opendcs.fixtures.configurations.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.spi.configuration.Configuration;

/**
 * Handles setup for XML Database based tests.
 */
public class XmlConfiguration implements Configuration
{

    File userDir;
    File propertiesFile;

    public XmlConfiguration(File userDir) 
    {
        this.userDir = userDir;
        this.propertiesFile = new File(userDir,"user.properties");
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        configBuilder.withDatabaseLocation("$DCSTOOL_USERDIR/edit-db");
        configBuilder.withEditDatabaseType("XML");
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
    public boolean isSql()
    {
        return false;
    }
    
}
