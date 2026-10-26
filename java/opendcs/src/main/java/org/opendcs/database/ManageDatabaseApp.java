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
import java.io.IOException;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ManageDatabaseApp.class);

    public static void main(String args[]) throws IOException, NoMigrationProvider, SQLException
    {
        StringToken implementation = new StringToken("I","which database type are we installing? Examples: 'CWMS-Oracle', 'OpenDCS-Oracle',  'OpenDCS-Postgres' ",
                                                     "",TokenOptions.optRequired,"");
        StringToken username = new StringToken("username",
                                       "administrator user name for the database",
                          "",
                                               TokenOptions.optRequired,
                                               null);
        StringToken password = new StringToken("password",
                                       "administrator password for the database",
                          "",
                                               TokenOptions.optRequired,
                                               null);
        StringToken appUsername = new StringToken("appUsername",
                                       "user name for user of the database",
                          "",
                                              TokenOptions.optRequired,
                                              null);
        StringToken appPassword = new StringToken("appPassword",
                                        "password for user of the database",
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

        log.info("Migrating database");
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
            log.info("Installing fresh database");

            mm.migrate();
            Pair<String,String> credentials = createInitialUser(mm, appUsername.getValue(), appPassword.getValue(), console);
            log.info("Now loading baseline data.");
            mp.loadBaselineData(profile, credentials.first, credentials.second);
            log.info("Baseline data has been imported. You may now begin using the software.");
            log.info("If you will be running background apps such as CompProc and the RoutingScheduler,");
            log.info("you should create a separate user. This is not currently covered in this application.");
        }
        else
        {
            log.info("Applying migrations to existing database. Current version is:");
            for (int i = applied.length -1; i >= 0; i--)
            {
                if (applied[i].isVersioned())
                {
                    log.info("{}", applied[i].getVersion());
                    break; // exit the loop, we're done.
                }
            }

            MigrationInfo[] pending = mm.pendingUpdates();
            log.info("The following migrations will be performed (only versioned migrations listed):");
            if (pending.length > 0 )
            {
                for (MigrationInfo mi: pending)
                {
                    log.info("{} - {}", mi.getVersion(), mi.getDescription());
                }
                final String doMigration = console != null ? console.readLine("Proceed? (y/N)") : "y";
                if (doMigration.toLowerCase().startsWith("y"))
                {
                    log.info("Performing migration.");
                    mm.migrate();
                }
                else
                {
                    log.info("Exiting application.");
                    System.exit(0);
                }
            }
            else
            {
                log.info("Database is already up-to-date.");
            }
            AfterUpdateRunner.run(getDataSourceForAfterUpdates(
                    profile, console, username.getValue(), password.getValue()));
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
        log.info("A default admin username will be created to allow initial data import and GUI configuration.");
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

    public static DataSource getDataSourceFromProfileAndUserInfo(Profile p, Console c, String username, String password) throws IOException
    {
        return getDataSource(p, c, username, password, false);
    }

    /** Schema migrations use a raw connection; after-updates also need CWMS session context. */
    public static DataSource getDataSourceForAfterUpdates(Profile p, Console c, String username, String password)
            throws IOException
    {
        return getDataSource(p, c, username, password, true);
    }

    private static DataSource getDataSource(Profile p, Console c, String username, String password,
            boolean initializeCwmsSession) throws IOException
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

        if (initializeCwmsSession && settings.editDatabaseTypeCode == DecodesSettings.DB_CWMS)
        {
            return new CwmsSessionDataSource(settings.editDatabaseLocation, username, password, settings.CwmsOfficeId);
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
                log.error("Not all placeholder values have been configured.");
                log.error("Please define the following:");
                remaining.forEach(p ->
                {
                    log.error("{}: {}", p.name, p.description);
                });
                throw new IllegalStateException("Incomplete placeholder configuration.");
            }
        }

    }
}
