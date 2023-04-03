package org.opendcs.fixtures;

import java.io.File;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;


@ExtendWith({ConfigurationContextProvider.class,SystemStubsExtension.class})
@TestInstance(Lifecycle.PER_CLASS)
public class AppTestBase {

    @SystemStub
    protected EnvironmentVariables environment = new EnvironmentVariables();

    @SystemStub
    protected SystemProperties properties = new SystemProperties();

    private static File resourceDir = new File(System.getProperty("resource.dir"),"data");

    @TestTemplate
    @Order(-1000)
    public void baseline(OpenDCSAppTestCase testCase) throws Exception
    {
        File userDir = testCase.getConfiguration().getUserDir();
        environment.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());
        properties.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());
        properties.set("INPUT_DATA",new File(resourceDir,"/shared").getAbsolutePath());
        properties.setup();
        environment.setup();

        


    }

    @AfterAll
    public void end_test() throws Exception
    {
        environment.teardown();
        properties.teardown();
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
