package org.opendcs.fixtures.configuration.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import decodes.db.DatabaseException;
import decodes.db.PlatformStatus;
import decodes.db.ScheduleEntryStatus;
import org.apache.commons.io.FileUtils;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.launcher.Profile;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import decodes.xml.CreatePlatformXref;
import ilex.util.Pair;
import org.opendcs.fixtures.configuration.Configuration;
import org.opendcs.fixtures.configuration.UserPropertiesBuilder;
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

    private File userDir;
    private File propertiesFile;
    private boolean started = false;
    private Profile profile = null;
    private OpenDcsDatabase databases = null;

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
    public void start(SystemExit exit, EnvironmentVariables environment, SystemProperties properties) throws Exception {
        if (started)
        {
            return;
        }
        File editDb = new File(userDir,"edit-db");
        new File(userDir,"output").mkdir();
        editDb.mkdirs();
        UserPropertiesBuilder configBuilder = new UserPropertiesBuilder();
        configBuilder.withDatabaseLocation("jdbc:xml:$DCSTOOL_USERDIR/edit-db");
        configBuilder.withEditDatabaseType("XML");
        configBuilder.withSiteNameTypePreference("CWMS");
        configBuilder.withDecodesAuth("noop:nothing");
        try(OutputStream out = new FileOutputStream(propertiesFile);)
        {
            FileUtils.copyDirectory(new File(System.getProperty("DCSTOOL_HOME")+"/edit-db"),editDb);
            FileUtils.copyDirectory(new File(System.getProperty("DCSTOOL_HOME")+"/schema"),new File(userDir,"/schema/"));
            configBuilder.build(out);
            started = true;
        }
        profile = Profile.getProfile(propertiesFile);
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


    public Database getDecodesDatabase() throws Throwable
    {
        return getOpenDcsDatabase().getLegacyDatabase(Database.class).get();
    }

    @Override
    public OpenDcsDatabase getOpenDcsDatabase() throws Throwable
    {
        synchronized(this)
        {
            if (databases == null)
            {
                final DecodesSettings settings = DecodesSettings.fromProfile(profile);
                databases = DatabaseService.getDatabaseFor(NAME, settings);
            }
            return databases;
        }
    }

    @Override
    public void loadXMLData(String[] files, SystemExit exit, SystemProperties properties)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void storeScheduleEntryStatus(ScheduleEntryStatus status)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void storePlatformStatus(PlatformStatus status) throws DatabaseException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
