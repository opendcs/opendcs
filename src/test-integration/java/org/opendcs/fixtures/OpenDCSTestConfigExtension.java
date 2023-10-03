package org.opendcs.fixtures;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opendcs.fixtures.helpers.TestResources;
import org.opendcs.spi.configuration.Configuration;
import org.opendcs.spi.configuration.ConfigurationProvider;

import decodes.util.DecodesSettings;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;

public class OpenDCSTestConfigExtension implements BeforeAllCallback, AfterAllCallback
{
    private static final Logger logger = Logger.getLogger(OpenDCSTestConfigExtension.class.getName());

    /**
     * Perform initial or per test environment setup.
     */
    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception
    {
        logger.info("Searching for 'opendcs.test.engine'.");
        final Configuration configuration = (Configuration)ctx.getRoot().getStore(Namespace.GLOBAL).getOrComputeIfAbsent("config", key -> 
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
                    return configProvider.getConfig(tmp);
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
        });
        

        ctx.getTestInstance().ifPresent(testInstance -> 
        {
            List<Field> fields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(),ConfiguredField.class);
            for (Field f: fields)
            {    
                try
                {            
                    if( f.getType().equals(Configuration.class))
                    {
                        f.set(testInstance,configuration);
                    }
                    
                }
                catch (Exception ex)
                {
                    throw new PreconditionViolationException("Unable to assigned configuration to field.", ex);
                }   
            }
        });

        SystemExit exit = (SystemExit)getStub(ctx,SystemExit.class);
        EnvironmentVariables environment = (EnvironmentVariables)getStub(ctx,EnvironmentVariables.class);
        SystemProperties properties = (SystemProperties)getStub(ctx,SystemProperties.class);

        if (!configuration.isRunning())
        {
            configuration.start(exit,environment);   
        }

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
