package org.opendcs.database;

import decodes.launcher.Profile;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;

import javax.sql.DataSource;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefSettings.LogSeverity;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefCallback;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefLoadHandler.ErrorCode;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefKeyboardHandler.CefKeyEvent;
import org.cef.handler.CefKeyboardHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.cef.handler.CefRequestHandler;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.cef.network.CefRequest.TransitionType;
import org.flywaydb.core.api.MigrationInfo;
import org.opendcs.spi.database.MigrationProvider;
import org.opendcs.spi.database.MigrationProvider.MigrationProperty;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        CefAppBuilder builder = new CefAppBuilder();

        //Configure the builder instance
        builder.setInstallDir(new File("jcef-bundle")); //Default
        builder.setProgressHandler(new ConsoleProgressHandler()); //Default        
        builder.addJcefArgs("--disable-gpu"); //Just an example
        builder.getCefSettings().windowless_rendering_enabled = true; //Default - select OSR mode
        builder.getCefSettings().log_severity = LogSeverity.LOGSEVERITY_VERBOSE;

        //Set an app handler. Do not use CefApp.addAppHandler(...), it will break your code on MacOSX!
        builder.setAppHandler(new MavenCefAppHandlerAdapter(){
            @Override
            public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                // Shutdown the app if the native CEF part is terminated
                if (state == CefAppState.TERMINATED) System.exit(0);
            }
        });

        //Build a CefApp instance using the configuration above
        CefApp app = builder.build();
        CefClient client = app.createClient();

        CefMessageRouter msgRouter = CefMessageRouter.create();
        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
                    boolean persistent, CefQueryCallback callback)
            {
                System.out.println("OnQuery: " + request);
                
                return false;
            }

        }, true);
        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request,
                    boolean persistent, CefQueryCallback callback)
            {
                System.out.println("EX:" + request);
                
                return false;
            }
        }, false);
        client.addMessageRouter(msgRouter);
        client.addFocusHandler(new CefFocusHandlerAdapter() {
            private boolean browserFocus = true;
            @Override
            public void onGotFocus(CefBrowser browser) {
                if (browserFocus) return;
                browserFocus = true;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                browser.setFocus(true);
            }

            @Override
            public void onTakeFocus(CefBrowser browser, boolean next) {
                browserFocus = false;
            }
        });
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                System.out.println(">>>>>>>>>>>>>>>>>>>> load done, cleared");
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                browser.setFocus(false);
                browser.getUIComponent().getParent().requestFocus();
                //..requestFocus();
            }
        });
        client.addKeyboardHandler(new CefKeyboardHandlerAdapter() {
            @Override
            public boolean onKeyEvent(CefBrowser thisBrowserInstance, CefKeyEvent event) {
               thisBrowserInstance.setFocus(false);
               KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
               thisBrowserInstance.getUIComponent().getParent().requestFocus();
               return false;
            }
         });
        client.addDisplayHandler(new CefDisplayHandlerAdapter(){
                @Override
                public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                    System.out.println("addr change " + url);
                    super.onAddressChange(browser, frame, url);
                }
        });
        client.addRequestHandler(new CefRequestHandler() {

            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture,
                    boolean is_redirect) {
                System.out.println("before browse:" + request.getURL());
                return false;
            }

            @Override
            public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String target_url,
                    boolean user_gesture) {
                System.out.println("open: " + target_url);
                        return false;
            }

            @Override
            public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame,
                    CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator,
                    BoolRef disableDefaultHandling) {
                System.out.println("create request handler." + request.getURL());
                return null;
            }

            @Override
            public boolean getAuthCredentials(CefBrowser browser, String origin_url, boolean isProxy, String host,
                    int port, String realm, String scheme, CefAuthCallback callback) {
                System.out.println("Get auth.");
                return true;
            }

            @Override
            public boolean onCertificateError(CefBrowser browser, ErrorCode cert_error, String request_url,
                    CefCallback callback) {
                System.out.println("Cert Error: " + cert_error);
                return true;
            }

            @Override
            public void onRenderProcessTerminated(CefBrowser browser, TerminationStatus status, int error_code,
                    String error_string) {
                System.out.println("Error " + error_string);
                
            }
            
        });
        String url = "https://opendcs-env.readthedocs.io";        
        CefBrowser browser = client.createBrowser(url, false, true);
        final Component browserUI = browser.getUIComponent();
    
        SwingUtilities.invokeLater(() -> {
            // Create the main frame
            JFrame frame = new JFrame("Simple Swing App");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600); // Set initial size

            // Add the button to the frame's content pane
            frame.getContentPane().add(browserUI);
            frame.requestFocus();
            
            // Make the frame visible
            frame.setVisible(true);
            browser.openDevTools();
        });
/*
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

        MigrationInfo[] applied = mm.currentVersion();
        if (applied.length == 0)
        {
            console.writer().println("Installing fresh database");
            
            mm.migrate();
            console.printf("A default admin username will be created to allow initial data import and GUI configuration.%s",
                           System.lineSeparator());
            String user = console.readLine("username:");

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
            console.printf("Now loading baseline data.%s", System.lineSeparator());
            mp.loadBaselineData(profile, user, password);
            console.printf("Base line data has been imported. You may now begin using the software.%s", System.lineSeparator());
            console.printf("If you will be running background apps such as CompProc and the RoutingScheduler,%s", System.lineSeparator());
            console.printf("you should create a separate user. This is not currently covered in this application.%s", System.lineSeparator());
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
            */
    }

    public static DataSource getDataSourceFromProfileAndUserInfo(Profile p, Console c) throws IOException, FileNotFoundException
    {
        DecodesSettings settings = DecodesSettings.fromProfile(p);
        c.printf("Please enter the schema owning username and password for database at %s,%s",
                 settings.editDatabaseLocation,System.lineSeparator());
        c.printf("username:");
        String username = c.readLine();
        char[] pw = c.readPassword("password:");
        String password = new String(pw);
        return new SimpleDataSource(settings.editDatabaseLocation,username,password);
    }
}
