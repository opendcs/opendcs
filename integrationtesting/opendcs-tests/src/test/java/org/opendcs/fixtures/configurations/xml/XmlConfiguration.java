/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.opendcs.fixtures.configurations.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.opendcs.fixtures.UserPropertiesBuilder;
import org.opendcs.spi.configuration.Configuration;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;

/**
 * Handles setup for XML Database based tests.
 */
public class XmlConfiguration implements Configuration
{
    private static final Logger logger = Logger.getLogger(XmlConfiguration.class.getName());

    public static final String NAME = "OpenDCS-XML";

    File userDir;
    File propertiesFile;
    private boolean started = false;

    public XmlConfiguration(File userDir) throws Exception
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
    public File getUserDir()
    {
        return this.userDir;
    }

    @Override
    public void start(SystemExit exit, EnvironmentVariables environment, SystemProperties properties) throws Exception
	{
        if (started)
        {
            return;
        }
        File editDb = new File(userDir,"edit-db");
        new File(userDir,"output").mkdir();
        editDb.mkdirs();
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        configBuilder.withDatabaseLocation("$DCSTOOL_USERDIR/edit-db");
        configBuilder.withEditDatabaseType("XML");
        configBuilder.withSiteNameTypePreference("CWMS");
        try(OutputStream out = new FileOutputStream(propertiesFile);)
        {
            FileUtils.copyDirectory(new File(System.getProperty("DCSTOOL_HOME")+"/edit-db"),editDb);
            FileUtils.copyDirectory(new File(System.getProperty("DCSTOOL_HOME")+"/schema"),new File(userDir,"/schema/"));
            configBuilder.build(out);
            started = true;
        }
    }

    @Override
    public boolean isRunning()
    {
        return started;
    }

    @Override
    public Map<Object, Object> getEnvironment()
    {
        return new HashMap<>();
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
