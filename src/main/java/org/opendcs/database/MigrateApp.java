package org.opendcs.database;

import decodes.launcher.Profile;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.TTYEcho;

import javax.sql.DataSource;

import org.opendcs.spi.database.MigrationProvider;
import org.opendcs.spi.database.MigrationProvider.MigrationProperty;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

public class MigrateApp
{    

    public static void main(String args[]) throws Exception
    {
        StringToken implementation = new StringToken("I","which database type are we installing",
                                                     "",TokenOptions.optRequired,"");
        CmdLineArgs cliArgs = new CmdLineArgs(true, "migrate.log");
        cliArgs.addToken(implementation);
        cliArgs.parseArgs(args);

        System.out.println("Migrating Database:");
        String impl = implementation.getValue();
        Profile profile = cliArgs.getProfile();
        Console console = System.console();
        DataSource ds = getDataSourceFromProfileAndUserInfo(profile,console);
        MigrationManager mm = new MigrationManager(ds, impl);
        if (mm.getVersion() == MigrationManager.DatabaseVersion.NOT_INSTALLED)
        {
            console.writer().println("Installing fresh database");
            final MigrationProvider mp = mm.getMigrationProvider();
            List<MigrationProperty> requiredPlaceholders = mp.getPlaceHolderDescriptions();
            if (!requiredPlaceholders.isEmpty())
            {
                console.writer().println("Please provide values for each of the presented properties.");
                requiredPlaceholders.forEach(p ->
                {
                    String value = console.readLine("%s (desc = %s) = ", p.name, p.description);
                    mp.setPlaceholderValue(p.name, value);
                });
            }
            mm.migrate();
        }
    }

    public static DataSource getDataSourceFromProfileAndUserInfo(Profile p, Console c) throws IOException, FileNotFoundException
    {
        c.printf("username:");
        try(InputStream propStream = new FileInputStream(p.getFile());)
        {
            Properties props = new Properties();
            props.load(propStream);
            DecodesSettings settings = new DecodesSettings();
            settings.loadFromProperties(props);
            String username = c.readLine();
            char[] pw = c.readPassword("password:");
            String password = new String(pw);
            c.printf("Using jdbc URL: %s",settings.editDatabaseLocation);
            return new SimpleDataSource(settings.editDatabaseLocation,username,password);
        }
    }
}
