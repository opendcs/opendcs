package org.opendcs.fixtures.extensions.lrgs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.nio.file.Files;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;

public final class LrgsTestExtension implements BeforeAllCallback, ParameterResolver
{
    private static final Namespace LRGS_INSTANCE = Namespace.create("lrgs", "instance");

    @Override
    public void beforeAll(ExtensionContext context) throws Exception
    {
        var ns = context.getStore(LRGS_INSTANCE);
        ns.computeIfAbsent(LrgsTestInstance.class, t ->
            assertDoesNotThrow(() -> //NOSONAR - test fixture code, not production code
            {
                // java.io.tmpdir is redirected to a project-local build directory by the
                // consuming test tasks (see opendcs-tests/build.gradle), so this isn't
                // landing in a shared, publicly writable system temp directory.
                File lrgsHome = Files.createTempDirectory("lrgshome").toFile();
                var testClass = context.getRequiredTestClass();
                var lrgsConfig = testClass.getAnnotation(LrgsConfig.class);
                return new LrgsTestInstance(lrgsHome, lrgsConfig != null ? lrgsConfig.value() : null);
            })
        );
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException 
    {
        return LrgsTestInstance.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException 
    {
        var ns = extensionContext.getStore(LRGS_INSTANCE);
        return ns.get(parameterContext.getParameter().getType());
    }
}
