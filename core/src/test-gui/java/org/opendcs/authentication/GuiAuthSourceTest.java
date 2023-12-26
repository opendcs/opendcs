package org.opendcs.authentication;

import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;
import static org.assertj.swing.edt.GuiActionRunner.execute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ilex.gui.LoginDialog;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fixtures.GuiTest;

public class GuiAuthSourceTest extends GuiTest
{
    private DialogFixture dialog;
    //private FrameFixture frame;
    private LoginDialog loginDialog;
    //private JFrame rootFrame;
    private ExecutorService executor = Executors.newCachedThreadPool();

    @BeforeEach
    public void setup() throws Exception
    {
        loginDialog = GuiActionRunner.execute(() -> (LoginDialog)AuthSourceService.getFromString("gui-auth-source:Login"));
        dialog = new DialogFixture(loginDialog);
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
        pause(new Condition("Gui visible") {
            @Override
            public boolean test() {
                return execute(loginDialog::isVisible);
            }
        },timeout(500));

        dialog.textBox("username").setText(username);
        dialog.textBox("password").setText(password);
        dialog.button("ok").click();
        Properties creds = credentialsFuture.get();
        assertNotNull(creds, "no credentials returned.");
        assertEquals(username,creds.getProperty("username"), "Username did not match");
        assertEquals(password,creds.getProperty("password"), "Password did not match");
    }

    @Test
    public void login_cancelled_returns_null() throws Exception
    {
        Future<Properties> credentialsFuture = executor.submit(() -> loginDialog.getCredentials());
        assertTrue(loginDialog.isValid(), "Our Dialog isn't valid.");
        pause(new Condition("Gui visible") {
            @Override
            public boolean test() {
                return execute(loginDialog::isVisible);
            }
        },timeout(500));
        dialog.textBox("username").setText("doesn't matter");
        dialog.textBox("password").setText("bad password");
        dialog.button("cancel").click();
        Properties creds = credentialsFuture.get();
        assertNull(creds, "Actual values were returned during cancel.");
    }
}
