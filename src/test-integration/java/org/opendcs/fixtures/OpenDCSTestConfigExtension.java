package org.opendcs.fixtures;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.opendcs.fixtures.helpers.TestResources;
import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import lrgs.gui.DecodesInterface;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;

public class OpenDCSTestConfigExtension implements BeforeAllCallback, AfterAllCallback, TestExecutionListener
{
    private static final Logger logger = Logger.getLogger(OpenDCSTestConfigExtension.class.getName());

    private static Configuration configuration = null;

    /**
     * Perform initial or per test environment setup.
     */
    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception
    {
        logger.info("Searching for 'opendcs.test.engine'.");
        // Store Configuration object for other extensions.
        ctx.getRoot().getStore(Namespace.GLOBAL).put("config",configuration);

        ctx.getTestInstance()
           .ifPresent(testInstance ->
        {
            try
            {
                SystemExit exit = (SystemExit)getStub(ctx,SystemExit.class);
                EnvironmentVariables environment = (EnvironmentVariables)getStub(ctx,EnvironmentVariables.class);
                SystemProperties properties = (SystemProperties)getStub(ctx,SystemProperties.class);

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
                if (configuration.getEnvironment() != null
                && !configuration.getEnvironment().isEmpty())
                {
                    environment.set(configuration.getEnvironment());
                }
                properties.set("DCSTOOL_USERDIR",userDir.getAbsolutePath());
                properties.set("INPUT_DATA",new File(TestResources.resourceDir,"/shared").getAbsolutePath());
                properties.setup();
                environment.setup();
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
            catch(Exception ex)
            {
                throw new PreconditionViolationException("Unable to setup environment or configuration.",ex);
            }
        });
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
