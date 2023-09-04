package org.opendcs.fixtures;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.PreconditionViolationException;
import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

import decodes.util.DecodesSettings;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;


@ExtendWith(SystemStubsExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class AppTestBase {

    private static File resourceDir = new File(System.getProperty("resource.dir"),"data");

    @SystemStub
    protected final EnvironmentVariables environment = new EnvironmentVariables();

    @SystemStub
    protected final SystemProperties properties = new SystemProperties();

    @SystemStub
    protected final SystemExit exit = new SystemExit();

    protected final Configuration configuration;

    public AppTestBase()
    {
        String engine = System.getProperty("opendcs.test.engine");
        if (engine == null)
        {
            throw new PreconditionViolationException("You must set the provide 'opendcs.test.engine' in a system property to run the tests against");
        }

        ServiceLoader<ConfigurationProvider> loader = ServiceLoader.load(ConfigurationProvider.class);
        Iterator<ConfigurationProvider> configs = loader.iterator();

        ConfigurationProvider configProvider = null;
        while(configs.hasNext())
        {
            ConfigurationProvider configProviderTmp = configs.next();
            if (engine.equals(configProviderTmp.getImplementation()))
            {
                configProvider = configProviderTmp;
            }
        }

        if (configProvider != null)
        {
            try
            {
                File tmp = Files.createTempDirectory("configs-"+configProvider.getImplementation()).toFile();
                this.configuration = configProvider.getConfig(tmp);
            }
            catch (Exception ex)
            {
                throw new PreconditionViolationException("Unable to load load configuration for OpenDCS engine '" + engine + "'. Provider failed to initialize");
            }
        }
        else
        {
            throw new PreconditionViolationException("No implementation found for engine '" + engine + "'.");
        }
    }

    protected void assertExitNullOrZero()
    {
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0, "System.exit called with unexpected code.");
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

    @BeforeAll
    public void beforeAll() throws Exception {
        configuration.start(exit,environment);
        File userDir = configuration.getUserDir();
        System.out.println("DCSTOOL_USERDIR="+userDir);
        environment.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());
        if (configuration.getEnvironment() != null
         && !configuration.getEnvironment().isEmpty())
        {
            environment.set(configuration.getEnvironment());
        }
        properties.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());
        properties.set("INPUT_DATA",new File(resourceDir,"/shared").getAbsolutePath());
        properties.setup();
        environment.setup();
    }

    @AfterAll
    public void afterAll() throws Exception {
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

    /**
     * Helper to make calling mains easier
     * @param arg
     * @return
     */
    public static String[] args(String... arg)
    {
        return arg;
    }
}
