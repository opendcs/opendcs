package fixtures;

import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.junit.jupiter.api.BeforeAll;

public class GuiTest
{

    @BeforeAll
    public static void setup_framework()
    {
        FailOnThreadViolationRepaintManager.install();
    }
}
