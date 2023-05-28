package org.opendcs.fixtures;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import decodes.util.DecodesSettings;
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
        if (testCase.getConfiguration().getEnvironment() != null
         && !testCase.getConfiguration().getEnvironment().isEmpty())
        {
            environment.set(testCase.getConfiguration().getEnvironment());
        }
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
        DecodesSettings settings = DecodesSettings.instance();
        Class<?> clazz = settings.getClass();
        Field isLoaded = clazz.getDeclaredField("_isLoaded");
        Field theInstance = clazz.getDeclaredField("_instance");
        isLoaded.setAccessible(true);
        isLoaded.setBoolean(settings, false);

        theInstance.setAccessible(true);
        theInstance.set(settings,null);

    }

    public abstract DynamicNode tests(String baseName) throws Exception;

    public DynamicNode getTests(String baseName) throws Exception
    {
        return dynamicContainer(Toolkit.testName(baseName, this.getClass().getName()), Stream.of(
            dynamicTest(Toolkit.testName(baseName,"Configuration","start"),()->this.testCase.getConfiguration().start(exit,environment)),
            dynamicTest(Toolkit.testName(baseName,"Configuration","Setup"), () -> setup()),
            tests(baseName),
            dynamicTest(Toolkit.testName(baseName,"Configuration","Teardown"), () -> teardown())
        ));        
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
