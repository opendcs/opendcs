package fixtures;

import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class GuiTest extends AssertJSwingJUnitTestCase
{
    @BeforeAll
    public static void setup_framework()
    {
        FailOnThreadViolationRepaintManager.install();
    }

    @BeforeEach
    public void setup_robot()
    {
        setUpRobot();
    }
    @Override
    protected void onSetUp()
    {
    }
}
