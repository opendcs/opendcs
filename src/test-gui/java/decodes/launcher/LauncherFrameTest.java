package decodes.launcher;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.timing.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;



@ExtendWith(SystemStubsExtension.class)
public class LauncherFrameTest
{

    @SystemStub
    private static SystemProperties properties = new SystemProperties(System.getProperties());

    LauncherFrame lf;
    private FrameFixture frame;    
    private ExecutorService executor = Executors.newCachedThreadPool();

    @BeforeEach
    public void setup() throws Exception
    {
        String resourceDir = System.getProperty("resource.dir");
        properties.set("DCSTOOL_USERDIR",resourceDir+"/decodes/launcher/profiles");
        lf = GuiActionRunner.execute(() -> new LauncherFrame(new String[0]));
        lf.setExitOnClose(false);
        lf.checkForProfiles();
        frame = new FrameFixture(lf);
        frame.show();
        pause(new Condition("Gui visible") {
            @Override
            public boolean test() {
                return execute(lf::isVisible);
            }
        },timeout(500));
    }

    @AfterEach
    public void tearDown()
    {        
        frame.cleanUp();
    }


    @Test
    public void profile_comboActivatesButtons()
    {
        JComboBoxFixture profileCombo = frame.comboBox("profileCombo");
        assertTrue(profileCombo.isEnabled(),"Profiles were not found by the launcher.");
        assertNotNull(profileCombo.contents(),"Profile combo was not correctly created");
        assertEquals(3,profileCombo.contents().length,"Specified Profiles are not in the Profile ComboBox.");
        assertNull(lf.getSelectedProfile(), "Profile combo did not start on default");
        assertFalse(frame.button("computations").isEnabled(),"The computation button is enabled but shouldn't be.");
        profileCombo.selectItem("cwms");
        assertTrue(frame.button("computations").isEnabled(),"The computation button was not enabled when it should've been.");

    }

}
