package org.opendcs.fixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class ConfigurationContextProvider implements TestTemplateInvocationContextProvider {

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext ctx) {
        final ArrayList<TestTemplateInvocationContext> contexts = new ArrayList<>();
        ServiceLoader<ConfigurationProvider> loader = ServiceLoader.load(ConfigurationProvider.class);
        Iterator<ConfigurationProvider> configs = loader.iterator();
        while(configs.hasNext())
        {
            ConfigurationProvider config = configs.next();
            try
            {
                contexts.add(
                    config.getConfig(
                        Files.createTempDirectory("configs").toFile()
                    )
                );
            }
            catch( IOException ex)
            {
                throw new RuntimeException("Unable to create temp directories for configuration",ex);
            }
        }
        return contexts.stream();
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext ctx) {
        return true;
    }
    
}
