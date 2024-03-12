package org.opendcs.gui;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.FlowLayout;

import javax.swing.JFrame;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fixtures.GuiTest;

public class PasswordWithShowTest extends GuiTest
{

    private HoldingFrame frame;
    private FrameFixture fixture;

    @BeforeEach
    public void setup()
    {
        frame  = GuiActionRunner.execute(() -> new HoldingFrame());
        frame.setVisible(true);
        fixture = new FrameFixture(robot(), frame);
        assertTrue(frame.isValid(), "Our Password Panel isn't valid.");
        pause(new Condition("Gui visible") {
            @Override
            public boolean test() {
                return execute(frame::isVisible);
            }
        },timeout(500));
    }

    @AfterEach
    public void teardown()
    {
        frame.setVisible(false);
        fixture.cleanUp();
    }

    @Test
    public void test_password_field() throws Exception
    {
        JTextComponentFixture pwFieldText = fixture.textBox("pw.showablePassword");
        assertTrue( pwFieldText.text() == null || pwFieldText.text().isEmpty());
    }

    private static class HoldingFrame extends JFrame
    {
        public HoldingFrame()
        {
            super("Password Test");
            setLayout(new FlowLayout());
            PasswordWithShow pwField = new PasswordWithShow(GuiConstants.DEFAULT_PASSWORD_WITH);
            pwField.setName("pw");
            add(pwField);
        }
    }
}
