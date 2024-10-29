package org.opendcs.fixtures.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opendcs.fixtures.configuration.Configuration;

import ilex.util.EnvExpander;

public class TestResources
{
    public static final File resourceDir = new File(System.getProperty("resource.dir"),"data");

    /**
     * Helper to get a file from the resource directory
     * @param config Configuration used to retrieve substitution information.
     * @param file file name under the data/ directory to reference
     * @return
     */
    public static String getResource(Configuration config, String fileName)
    {
        if (!fileName.startsWith("$"))
        {
            String configName = config.getName();
            String newName = fileName.replace("${impl}",configName);
            File file = new File(TestResources.resourceDir,newName);
            if (file.exists())
            {
                return file.getAbsolutePath();
            }
            else
            {
                return new File(TestResources.resourceDir,
                                fileName.replace("${impl}",""))
                            .getAbsolutePath();
            }
        }
        else
        {
            return new File(EnvExpander.expand(fileName,System.getProperties())).getAbsolutePath();
        }
    }

    public static InputStream getResourceAsStream(Configuration config, String fileName) throws IOException
    {
        final String resourceFileName = getResource(config, fileName);
        InputStream stream = ClassLoader.getSystemResourceAsStream(resourceFileName);
        if (stream == null)
        {
            File f = new File(resourceFileName);
            stream = new FileInputStream(f);
            
        }
        return stream;
    }
}
