package org.opendcs.database;

import decodes.dbimport.DbImport;
import decodes.launcher.Profile;
import decodes.tsdb.ImportComp;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;

import javax.sql.DataSource;

import org.flywaydb.core.api.MigrationInfo;
import org.opendcs.spi.database.MigrationProvider;
import org.opendcs.spi.database.MigrationProvider.MigrationProperty;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Simple Terminal application to prompt user for required information
 * and install/update the schema.
 *
 * For simplicity and modernization this app forgoes previously implemented
 * helpers, such as extending from TsdbAppTemplate that would hinder it's
 * intended operation.
 */
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
        MigrationInfo[] applied = mm.currentVersion();
        if (applied.length == 0)
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
            String user = console.readLine("Please provided an admin user:");
            // TODO: lo
            boolean match = true;
            String password;
            do
            {
                if(!match)
                {
                    console.writer().println("Passwords did not match, try again.");
                }
                char[] pw_chars =  console.readPassword("Please provide a password:");
                char[] pw2_chars = console.readPassword("Please repeat the password:");
                String pw = new String(pw_chars);
                String pw2 = new String(pw2_chars);
                password = pw;
                match = pw.equals(pw2);
            } while (!match);
            List<String> roles = new ArrayList<>();
            roles.add("OTSDB_MGR");
            roles.add("OTSDB_ADMIN");
            mp.createUser(mm.getJdbiHandle(), user, password, roles);

            /* Initializing database with default data */
            List<File> decodesFiles = mp.getDecodesData();
            if (!decodesFiles.isEmpty())
            {
                console.writer().println("Loading baseline decodes data.");
                ArrayList<String> theArgs = new ArrayList<>();
                theArgs.add("-P"); theArgs.add(profile.getFile().getAbsolutePath());
                theArgs.add("-d3");
                theArgs.addAll(
                    decodesFiles.stream()
                            .map(f->f.getAbsolutePath())
                            .collect(Collectors.toList())
                            );
                DbImport.main(theArgs.toArray(new String[0]));
            }
            /*List<File> compFiles = mp.getComputationData();
            theArgs.clear();
            theArgs.add("-P"); theArgs.add(profile.getFile().getAbsolutePath());
            theArgs.add("-d3");
            theArgs.add("-C");
            theArgs.addAll(
                compFiles.stream()
                        .map(f->f.getAbsolutePath())
                        .collect(Collectors.toList())
                        );                        
            ImportComp.main(theArgs.toArray(new String[0]));*/
        }
        else
        {
            console.printf("Applying migrations to existing database. Current version is:");
            for (int i = applied.length -1; i >= 0; i--)
            {
                if (applied[i].isVersioned())
                {
                    console.writer().println(applied[i].getVersion());
                    break; // exit the loop, we're done.
                }
            }

            MigrationInfo[] pending = mm.pendingUpdates();
            console.writer().println("The following migrations will be performed (only versioned migrations listed):");
            if (pending.length > 0 )
            {
                for (MigrationInfo mi: pending)
                {
                    console.printf("%s - %s%s", mi.getVersion(), mi.getDescription(), System.lineSeparator());
                }
                console.writer().println();
                String doMigration = console.readLine("Proceed? (y/N)");
                if (doMigration.toLowerCase().startsWith("y"))
                {
                    console.writer().println("Performing migration.");
                    mm.migrate();
                }
                else
                {
                    console.writer().println("Exiting application.");
                    System.exit(0);
                }
            }
            else
            {
                console.writer().println("Database is already up-to-date.");
            }
        }
    }

    public static DataSource getDataSourceFromProfileAndUserInfo(Profile p, Console c) throws IOException, FileNotFoundException
    {
        c.printf("username:");
        
        DecodesSettings settings = DecodesSettings.fromProfile(p);
        
        String username = c.readLine();
        char[] pw = c.readPassword("password:");
        String password = new String(pw);
        c.printf("Using jdbc URL: %s%s",settings.editDatabaseLocation,System.lineSeparator());
        return new SimpleDataSource(settings.editDatabaseLocation,username,password);
    }
}
