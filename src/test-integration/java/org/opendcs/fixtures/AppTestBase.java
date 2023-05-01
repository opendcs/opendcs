package org.opendcs.fixtures;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;


@ExtendWith(SystemStubsExtension.class)
public abstract class AppTestBase {

    private static File resourceDir = new File(System.getProperty("resource.dir"),"data");

    @SystemStub
    protected final EnvironmentVariables environment = new EnvironmentVariables();

    @SystemStub
    protected final SystemProperties properties = new SystemProperties();

    @SystemStub
    protected final SystemExit exit = new SystemExit();

    protected OpenDCSAppTestCase testCase = null;

    public AppTestBase(OpenDCSAppTestCase testCase)
    {
        this.testCase = testCase;
    }

    private void setup() throws Exception
    {
        File userDir = testCase.getConfiguration().getUserDir();
        System.out.println("DCSTOOL_USERDIR="+userDir);
        environment.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());
        properties.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());
        properties.set("INPUT_DATA",new File(resourceDir,"/shared").getAbsolutePath());
        properties.setup();
        environment.setup();
    }

    protected void assertExitNullOrZero()
    {
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0, "System.exit called with unexpected code.");
    }

    
    private void teardown() throws Exception
    {
        environment.teardown();
        properties.teardown();
    }

    public abstract DynamicNode tests();

    public DynamicNode getTests()
    {
        return dynamicContainer(this.getClass().getName(), Stream.of(
            dynamicTest("Setup", () -> setup()),
            tests(),
            dynamicTest("Teardown", () -> teardown())
        ));        
    }


    /**
     * Helper to make calling mains easier
     * @param arg
     * @return
     */
    public static String[] args(String... arg)
    {
        return arg;
    }

    /**
     * Helper to get a file from the resource directory
     * @param file file name under the data/ directory to reference
     * @return
     */
    public static String getResource(String file)
    {
        return new File(resourceDir,file).getAbsolutePath();
    }
}
