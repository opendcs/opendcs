package decodes.launcher;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.ActionFailedException;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.fixture.JTableCellFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import decodes.util.DecodesSettings;
import fixtures.GuiTest;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

@ExtendWith(SystemStubsExtension.class)
public class ProfileManagerFrameTest extends GuiTest
{
    @SystemStub
    private static SystemProperties properties = new SystemProperties();

    @TempDir
    private static File userDir;

    ProfileManagerFrame pmf;
    private FrameFixture frame;

    @BeforeEach
    public void setup() throws Exception
    {
        String resourceDir = System.getProperty("resource.dir");
        properties.setup();
        DecodesSettings.instance().rememberScreenPosition = false;
        properties.set("DCSTOOL_USERDIR",resourceDir+"/decodes/launcher/profiles");
        pmf = GuiActionRunner.execute(() -> new ProfileManagerFrame());
        pmf.setExitOnClose(false);
        frame = new FrameFixture(pmf);
        frame.show();
        pause(new Condition("Gui visible") {
            @Override
            public boolean test() {
                return execute(pmf::isVisible);
            }
        },timeout(500));
    }

    @Test
    @EnabledIfSystemProperty(named = "opendcs.gui-test.run.profile",
                             matches = "true",
                             disabledReason = "Test flaky during github actions. Only run if told.")
    public void test_profile_manager() throws Exception
    {
        JTableFixture jtf = frame.table("profileTable");
        JTableCellFixture tc = jtf.cell("cwms");
        tc.select();
        frame.button("copyProfile").click();
        JOptionPaneFixture opf = frame.optionPane(Timeout.timeout(2, TimeUnit.SECONDS));
        opf.requireMessage(Pattern.compile("Enter name for copy:"));
        opf.textBox().setText("cwms2");
        opf.okButton().focus().click();
        JTableCellFixture cwms2 = jtf.cell("cwms2").requireNotEditable();

        frame.button("copyProfile").click();
        opf = frame.optionPane(Timeout.timeout(2, TimeUnit.SECONDS));
        opf.requireMessage(Pattern.compile("Enter name for copy:"));
        opf.textBox().setText("cwms2");
        opf.okButton().focus().click();

        opf = frame.optionPane(Timeout.timeout(2, TimeUnit.SECONDS));
        opf.requireTitle("Error!");
        opf.requireMessage(Pattern.compile("A profile with that name already.*"));
        opf.okButton().focus().click();

        cwms2.select();
        frame.button("deleteProfile").click();
        opf = frame.optionPane(Timeout.timeout(2, TimeUnit.SECONDS));
        opf.requireMessage(Pattern.compile("Are you sure you want to delete the.*"));
        opf.yesButton().click();
        assertThrows(ActionFailedException.class, () ->
        {
            jtf.cell("cwms2").requireNotEditable();
        });

        jtf.tableHeader().clickColumn(0);
        JTableCellFixture userProfileCell = jtf.cell("(default)");
        assertEquals(0,userProfileCell.row());
        userProfileCell.select();
        frame.button("deleteProfile").click();
        opf = frame.optionPane(Timeout.timeout(2, TimeUnit.SECONDS));
        opf.requireMessage(Pattern.compile("Cannot delete the default profile!"));
        opf.okButton().click();
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        frame.cleanUp();
        properties.teardown();
    }
}
