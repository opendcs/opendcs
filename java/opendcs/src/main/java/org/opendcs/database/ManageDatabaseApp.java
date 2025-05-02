package org.opendcs.database;

import decodes.launcher.Profile;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Pair;

import javax.sql.DataSource;

import org.flywaydb.core.api.MigrationInfo;
import org.opendcs.spi.database.MigrationProvider;
import org.opendcs.spi.database.MigrationProvider.MigrationProperty;

import java.io.Console;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Simple Terminal application to prompt user for required information
 * and install/update the schema.
 *
 * For simplicity and modernization this app forgoes previously implemented
 * helpers, such as extending from TsdbAppTemplate that would hinder it's
 * intended operation.
 *
 */
public class ManageDatabaseApp
{

    public static void main(String args[]) throws Exception
    {
        StringToken implementation = new StringToken("I","which database type are we installing",
                                                     "",TokenOptions.optRequired,"");
        StringToken username = new StringToken("username",
                                       "user name for user manage the database",
                          "",
                                               TokenOptions.optRequired,
                                               null);
        StringToken password = new StringToken("password",
                                       "password for user manage the database",
                          "",
                                               TokenOptions.optRequired,
                                               null);
        StringToken appUsername = new StringToken("appUsername",
                                       "user name for user manage the database",
                          "",
                                              TokenOptions.optRequired,
                                              null);
        StringToken appPassword = new StringToken("appPassword",
                                        "password for user manage the database",
                            "",
                                                TokenOptions.optRequired,
                                                null);
        CmdLineArgs cliArgs = new CmdLineArgs(true, "migrate.log");
        cliArgs.addToken(implementation);
        cliArgs.addToken(username);
        cliArgs.addToken(password);
        cliArgs.addToken(appPassword);
        cliArgs.addToken(appUsername);
        cliArgs.parseArgs(args);

        System.out.println("Migrating Database:");
        String impl = implementation.getValue();
        Profile profile = cliArgs.getProfile();
        Console console = System.console();
        DataSource ds = getDataSourceFromProfileAndUserInfo(profile,console, username.getValue(), password.getValue());
        MigrationManager mm = new MigrationManager(ds, impl);

        final MigrationProvider mp = mm.getMigrationProvider();
        final Properties props = new Properties(System.getProperties());

        setPlaceholders(props, console, mp);

        MigrationInfo[] applied = mm.currentVersion();
        if (applied.length == 0)
        {
            System.out.println("Installing fresh database");

            mm.migrate();
            Pair<String,String> credentials = createInitialUser(mm, appUsername.getValue(), appPassword.getValue(), console);
            System.out.printf("Now loading baseline data.%s", System.lineSeparator());
            mp.loadBaselineData(profile, credentials.first, credentials.second);
            System.out.printf("Base line data has been imported. You may now begin using the software.%s", System.lineSeparator());
            System.out.printf("If you will be running background apps such as CompProc and the RoutingScheduler,%s", System.lineSeparator());
            System.out.printf("you should create a separate user. This is not currently covered in this application.%s", System.lineSeparator());
        }
        else
        {
            System.out.printf("Applying migrations to existing database. Current version is:");
            for (int i = applied.length -1; i >= 0; i--)
            {
                if (applied[i].isVersioned())
                {
                    System.out.println(applied[i].getVersion());
                    break; // exit the loop, we're done.
                }
            }

            MigrationInfo[] pending = mm.pendingUpdates();
            System.out.println("The following migrations will be performed (only versioned migrations listed):");
            if (pending.length > 0 )
            {
                for (MigrationInfo mi: pending)
                {
                    System.out.printf("%s - %s%s", mi.getVersion(), mi.getDescription(), System.lineSeparator());
                }
                System.out.println();
                String doMigration = console.readLine("Proceed? (y/N)");
                if (doMigration.toLowerCase().startsWith("y"))
                {
                    System.out.println("Performing migration.");
                    mm.migrate();
                }
                else
                {
                    System.out.println("Exiting application.");
                    System.exit(0);
                }
            }
            else
            {
                System.out.println("Database is already up-to-date.");
            }
        }
    }

    /**
     * Create the initial user defaulting to the passed in credentials if not null.
     * @param mm migration manager instance
     * @param appUsername configured application username
     * @param appPassword configured application password
     * @param console console instance to prompt user for the credentials
     * @return
     */
    private static Pair<String,String> createInitialUser(MigrationManager mm, String appUsername, String appPassword, Console console)
    {
        System.out.printf("A default admin username will be created to allow initial data import and GUI configuration.%s",
        System.lineSeparator());
        String user = appUsername;
        String password = appPassword;
        if (user == null)
        {
            user = console.readLine("username:");

            boolean match = true;
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
        }
        List<String> roles = mm.getMigrationProvider().getAdminRoles();


        mm.createUser(user, password, roles);
        return Pair.of(user, password);
    }

    public static DataSource getDataSourceFromProfileAndUserInfo(Profile p, Console c, String username, String password) throws IOException, FileNotFoundException
    {
        DecodesSettings settings = DecodesSettings.fromProfile(p);
        if (username == null)
        {
            c.printf("Please enter the schema owning username and password for database at %s,%s",
                     settings.editDatabaseLocation,System.lineSeparator());
            c.printf("username:");
            username = c.readLine();
            char[] pw = c.readPassword("password:");
            password = new String(pw);
        }

        return new SimpleDataSource(settings.editDatabaseLocation,username,password);
    }

    private static void setPlaceholders(Properties props, Console c, MigrationProvider mp)
    {

        List<MigrationProperty> requiredPlaceholders = mp.getPlaceHolderDescriptions();
        List<MigrationProperty> remaining = new ArrayList<>();
        if (!requiredPlaceholders.isEmpty())
        {
            // get from props first
            for (MigrationProperty placeholder: requiredPlaceholders)
            {
                String value = props.getProperty(placeholder.name);
                if (value != null || props.containsKey(placeholder.name))
                {
                    mp.setPlaceholderValue(placeholder.name, value);
                }
                else
                {
                    remaining.add(placeholder);
                }
            }

            if (!remaining.isEmpty() && c != null)
            {
                c.writer().println("Please provide values for each of the presented properties.");
                remaining.forEach(p ->
                {
                    String value = c.readLine("%s (desc = %s) = ", p.name, p.description);
                    mp.setPlaceholderValue(p.name, value);
                });
            }
            else if (!remaining.isEmpty() && c == null)
            {
                System.err.println("Not all placeholder values have been configured.");
                System.err.println("Please define the following:");
                remaining.forEach(p ->
                {
                    System.err.printf("%s: %s%s", p.name, p.description, System.lineSeparator());
                });
                throw new RuntimeException("Incomplete Placeholder configuration.");
            }
        }

    }
}
