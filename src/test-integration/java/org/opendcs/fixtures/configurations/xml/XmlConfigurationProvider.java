package org.opendcs.fixtures.configurations.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.opendcs.fixtures.OpenDCSAppTestCase;
import org.opendcs.fixtures.TypedParameterResolver;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class XmlConfigurationProvider implements ConfigurationProvider
{

    @Override
    public String getImplementation() {
        return "XML";
    }

    @Override
    public TestTemplateInvocationContext getConfig(File tempDir) {
        final XmlConfiguration config = new XmlConfiguration(tempDir);
        final OpenDCSAppTestCase testCase = new OpenDCSAppTestCase(this.getImplementation(), config);

        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationContext) {
                return testCase.getDisplayName();
            }

            @Override
            public List<Extension> getAdditionalExtensions()
            {
                List<Extension> extensions = new ArrayList<>();
                extensions.add(new TypedParameterResolver<OpenDCSAppTestCase>(testCase));
                extensions.addAll(config.getExtensions());
                return extensions;
            }
        };
    }
    
}
