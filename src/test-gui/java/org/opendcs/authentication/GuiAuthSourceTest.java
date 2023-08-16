package org.opendcs.authentication;

import ilex.gui.LoginDialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.spi.authentication.AuthSource;

import com.github.dockerjava.api.model.Frame;


public class GuiAuthSourceTest
{
    private DialogFixture dialog;
    //private FrameFixture frame;
    private LoginDialog loginDialog;
    //private JFrame rootFrame;
    private ExecutorService executor = Executors.newCachedThreadPool();

    @BeforeEach
    public void setup() throws Exception
    {
        loginDialog = (LoginDialog)AuthSourceService.getFromString("gui-auth-source:Login");
        //rootFrame = GuiActionRunner.execute(() -> new JFrame("Root Window"));
        //frame = new FrameFixture(rootFrame);
        dialog = new DialogFixture(loginDialog);
        //frame.show();
    }

    @AfterEach
    public void tearDown()
    {        
        dialog.cleanUp();
    }

    @Test
    public void login_accepted() throws Exception
    {        
        final String username = "user";
        final String password = "password";
    
        Future<Properties> credentialsFuture = executor.submit(() -> loginDialog.getCredentials());
        assertTrue(loginDialog.isValid(), "Our Dialog isn't valid.");

        Thread.sleep(500); // give the dialog a bit of time to actually start

        dialog.textBox("username").setText(username);
        dialog.textBox("password").setText(password);
        dialog.button("ok").click();
        Properties creds = credentialsFuture.get();
        assertNotNull(creds, "no credentials returned.");
        assertEquals(username,creds.getProperty("username"), "Username did not match");
        assertEquals(password,creds.getProperty("password"), "Password did not match");
    }
}
