package org.opendcs.fixtures;

import static org.junit.jupiter.api.Assumptions.abort;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.TsdbAppRequired;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;
import org.opendcs.fixtures.helpers.Programs;
import org.opendcs.fixtures.helpers.TestResources;
import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;

public class OpenDCSTestConfigExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, TestExecutionListener
{
    private static final Logger logger = Logger.getLogger(OpenDCSTestConfigExtension.class.getName());

    private static Configuration configuration = null;
    private static Map<String,BackgroundTsDbApp<? extends TsdbAppTemplate>> runningApps = new HashMap<>();

    private SystemExit exit = null;
    private EnvironmentVariables environment = null;
    private SystemProperties properties = null;

    @Override
    public void beforeEach(ExtensionContext ctx) throws Exception
    {
        Method method = ctx.getRequiredTestMethod();
        Object testInstance = ctx.getRequiredTestInstance();
        setupStubs(testInstance,ctx);
        applyPerMethodConfig(testInstance,method,ctx);
    }

    /**
     * Perform initial or per test environment setup.
     */
    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception
    {
        Object testInstance = ctx.getRequiredTestInstance();
        setupStubs(testInstance,ctx);
        applyPerInstanceConfig(testInstance,ctx);
        assignFields(testInstance);
    }

    private void setupStubs(Object testInstance, ExtensionContext ctx) throws Exception
    {
        exit = (SystemExit)getStub(ctx,SystemExit.class);
        environment = (EnvironmentVariables)getStub(ctx,EnvironmentVariables.class);
        properties = (SystemProperties)getStub(ctx,SystemProperties.class);
    }

    private void applyPerInstanceConfig(Object testInstance, ExtensionContext ctx) throws Exception
    {
        if (!configuration.isRunning())
        {
            configuration.start(exit,environment);
        }

        logger.info("Initializing decodes.");
        DecodesSettings settings = DecodesSettings.instance();
        Properties props = new Properties();
        try(InputStream propStream = configuration.getPropertiesFile().toURI().toURL().openStream())
        {
            props.load(propStream);
        }
        settings.loadFromUserProperties(props);

        File userDir = configuration.getUserDir();
        logger.info("DCSTOOL_USERDIR="+userDir);
        environment.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());

        configuration.getEnvironment().forEach((k,v) -> environment.set(k,v));

        properties.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());
        properties.set("INPUT_DATA",new File(TestResources.resourceDir,"/shared").getAbsolutePath());
        properties.setup();
        environment.setup();
    }

    private void applyPerMethodConfig(Object testInstance, Method testMethod, ExtensionContext ctx) throws Exception
    {
        setupDecodesTestData(testInstance, testMethod, ctx, environment, exit);
        setupComputationTestData(testInstance, testMethod, ctx,environment, exit);
        startOrCheckApplications(testInstance, testMethod, ctx, environment, exit);
    }

    private void startOrCheckApplications(Object testInstance, Method testMethod, ExtensionContext ctx, EnvironmentVariables environment,
            SystemExit exit) throws Exception
    {
        logger.info("Starting or Checking required applications.");
        ArrayList<TsdbAppRequired> requiredApps = new ArrayList<>();
        TsdbAppRequired apps[] = testInstance.getClass().getAnnotationsByType(TsdbAppRequired.class);
        for (TsdbAppRequired app: apps)
        {
            requiredApps.add(app);
        }

        apps = testMethod.getAnnotationsByType(TsdbAppRequired.class);
        for (TsdbAppRequired app: apps)
        {
            requiredApps.add(app);
        }

        for (TsdbAppRequired app : apps)
        {
            BackgroundTsDbApp<? extends TsdbAppTemplate> runningApp = runningApps.get(app.appName());
            if (runningApp == null)
            {
                logger.info("Starting Application " + app.appName() + "{" + app.app().getName()+"}");
                runningApp = BackgroundTsDbApp.forApp(
                                app.app(),app.appName(),configuration.getPropertiesFile(),
                                setupLog(app.appName()+".log"),environment
                            );
            }
            else if (!runningApp.isRunning())
            {
                throw new PreconditionViolationException(
                    "Application " + app.appName() + "{" + app.app().getName() + "}"
                  + " has stopped running."
                );
            }
        }
    }

    /**
     * Handle assigning configured fields
     * @param testInstance
     * @throws Exception
     */
    private void assignFields(Object testInstance) throws Exception
    {
        List<Field> fields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(),ConfiguredField.class);
        for (Field f: fields)
        {
            try
            {
                if( f.getType().equals(Configuration.class))
                {
                    f.setAccessible(true);
                    f.set(testInstance,configuration);
                }
                else if (f.getType().equals(TimeSeriesDb.class) && configuration.isRunning())
                {
                    f.setAccessible(true);
                    f.set(testInstance,configuration.getTsdb());
                }
            }
            catch (Throwable ex)
            {
                throw new PreconditionViolationException("Unable to assign configuration to field.", ex);
            }
        }
    }

    /**
     * Gather and run dbimport on any required Decodes Test data.
     * @param testInstance actual instance of the test class
     * @param ctx Junit extension context, used to get test method
     * @param env EnvironmentVariables stub used to call DbImport
     * @param exit SystemExit stub used to call DbImport
     * @throws Exception
     */
    private void setupDecodesTestData(Object testInstance, Method testMethod, ExtensionContext ctx, EnvironmentVariables env, SystemExit exit) throws Exception
    {
        DecodesConfigurationRequired decodesConfig = testInstance.getClass().getAnnotation(DecodesConfigurationRequired.class);
        ArrayList<String> files = new ArrayList<>();
        if (decodesConfig != null)
        {
            for(String file: decodesConfig.value())
            {
                files.add(AppTestBase.getResource(file));
            }
        }
        decodesConfig = testMethod.getAnnotation(DecodesConfigurationRequired.class);
        if (decodesConfig != null)
        {
            for(String file: decodesConfig.value())
            {
                files.add(AppTestBase.getResource(file));
            }
        }

        if (!files.isEmpty())
        {
           Programs.DbImport(setupLog("decodes-setup.log"), configuration.getPropertiesFile(), env, exit, files.toArray(new String[0]));
        }
    }

    /**
     * Gather and run compimport on any required computation setup data Test data.
     * @param testInstance actual instance of the test class
     * @param ctx Junit extension context, used to get test method
     * @param env EnvironmentVariables stub used to call DbImport
     * @param exit SystemExit stub used to call DbImport
     * @throws Exception
     */
    private void setupComputationTestData(Object testInstance, Method testMethod, ExtensionContext ctx, EnvironmentVariables env, SystemExit exit) throws Exception
    {
        ComputationConfigurationRequired compConfig = testInstance.getClass().getAnnotation(ComputationConfigurationRequired.class);
        ArrayList<String> files = new ArrayList<>();
        if (compConfig != null)
        {
            for(String file: compConfig.value())
            {
                files.add(AppTestBase.getResource(file));
            }
        }

        compConfig = testMethod.getAnnotation(ComputationConfigurationRequired.class);
        if (compConfig != null)
        {
            for(String file: compConfig.value())
            {
                files.add(AppTestBase.getResource(file));
            }
        }

        if (!files.isEmpty())
        {
            Programs.CompImport(setupLog("computation-setup.log"), configuration.getPropertiesFile(), env, exit, files.toArray(new String[0]));
        }
    }
    /**
     * Save a little typing to generate a log name in the test data setup handlers.
     * @return
     */
    private File setupLog(String logName) {
        return new File(configuration.getUserDir(),"/" + logName);
    }

    /**
     * Reset environment, properties, DecodesSettings, etc;
     */
    @Override
    public void afterAll(ExtensionContext ctx) throws Exception
    {
        EnvironmentVariables environment = (EnvironmentVariables)getStub(ctx,EnvironmentVariables.class);
        SystemProperties properties = (SystemProperties)getStub(ctx,SystemProperties.class);
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

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan)
    {
        logger.info("All tests are starting.");
        if(configuration == null)
        {
            logger.warning("CREATING CONFIGURATION");
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
                    configuration = configProvider.getConfig(tmp);
                }
                catch (Exception ex)
                {
                    throw new PreconditionViolationException("Unable to initialize configuration.", ex);
                }
            }
            else
            {
                throw new PreconditionViolationException("No implementation found for engine '" + engine + "'.");
            }
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan)
    {
        try
        {
            for (String app: runningApps.keySet())
            {
                runningApps.get(app).stop();
                runningApps.remove(app);
            }
            configuration.stop();
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Unable to cleanup resources.",t);
        }
    }

    /**
     * Get the appropriate SystemStub instance from the extended class.
     *
     * It is up to the calling code to perform proper conversions
     *
     * @param ctx Junit ExtensionContext
     * @param stubClass which stub to get
     * @return The Stub Instance.
     * @throws Exception Stub can't be found for any reason
     */
    private Object getStub(ExtensionContext ctx, Class<?> stubClass) throws Exception
    {

        Optional<Object> exit = ctx.getTestInstance().map(testInstance ->
        {
            List<Field> fields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(),SystemStub.class);
            for (Field f: fields)
            {
                try
                {
                    Object obj = f.get(testInstance);
                    if (obj.getClass().equals(stubClass))
                    {
                        return obj;
                    }
                }
                catch (Exception ex)
                {
                    throw new PreconditionViolationException("Unable to acquire SystemExit Stub.", ex);
                }
            }
            return null;
        });

        return exit.orElseThrow(() -> new PreconditionViolationException("No SystemExit field annotated with @SystemStub are present."));
    }
}
