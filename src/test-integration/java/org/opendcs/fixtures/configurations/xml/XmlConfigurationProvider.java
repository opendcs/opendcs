package org.opendcs.fixtures.configurations.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.opendcs.fixtures.OpenDCSAppTestCase;
import org.opendcs.fixtures.StoredResource;
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
                extensions.add(new BeforeAllCallback() {
                    @Override
                    public void beforeAll(ExtensionContext ctx) throws Exception {
                        final ExtensionContext.Store store = ctx.getStore(ExtensionContext.Namespace.GLOBAL);
                        store.getOrComputeIfAbsent("tmpdir"+testCase.getConfigurationName(), (key) -> {
                            return new StoredResource<>(tempDir,(f) -> {
                                System.out.println("In tempdir check for " + f.getAbsolutePath());
                                Boolean haveFailedTests = store.getOrDefault("failedTests"+testCase.getConfigurationName(), Boolean.class, false);
                                if (haveFailedTests == true)
                                {
                                    try
                                    {
                                        System.out.println("Removing temp directory.");
                                        FileUtils.deleteDirectory(f);
                                    }
                                    catch(IOException ex)
                                    {
                                        throw new RuntimeException("Unable to cleanup temp directory.",ex);
                                    }
                                }
                            });
                        });
                    }
                });
                extensions.add(new AfterTestExecutionCallback() {
                    @Override
                    public void afterTestExecution(ExtensionContext ec) throws Exception {
                        Boolean testResult = ec.getExecutionException().isPresent();
                        if (testResult == true) {
                            ec.getStore(Namespace.GLOBAL).put("failedTests"+getImplementation(),"true");
                        }
                    }
                });
                extensions.addAll(config.getExtensions());
                return extensions;
            }
        };
    }
    
}
