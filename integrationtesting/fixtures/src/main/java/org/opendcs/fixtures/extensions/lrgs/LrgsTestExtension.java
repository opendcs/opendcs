package org.opendcs.fixtures.extensions.lrgs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.PreconditionViolationException;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;

public final class LrgsTestExtension implements BeforeAllCallback, ParameterResolver
{
    private static final Namespace LRGS_INSTANCE = Namespace.create("lrgs", "instance");

    @Override
    public void beforeAll(ExtensionContext context) throws Exception
    {
        var ns = context.getStore(LRGS_INSTANCE);
        ns.computeIfAbsent(LrgsTestInstance.class, t ->
        {
            try
            {
                File lrgsHome = createOwnerOnlyTempDirectory("lrgshome");
                var testClass = context.getRequiredTestClass();
                var lrgsConfig = testClass.getAnnotation(LrgsConfig.class);
                return new LrgsTestInstance(lrgsHome, lrgsConfig != null ? lrgsConfig.value() : null);
            }
            catch (Exception ex)
            {
                throw new PreconditionViolationException("Unable to create LRGS test instance.", ex);
            }
        });
    }

    /**
     * Creates a temp directory restricted to the owner, rather than relying on the
     * platform default (which, on some systems, leaves the publicly writable temp
     * directory readable/writable by other users).
     */
    private static File createOwnerOnlyTempDirectory(String prefix) throws IOException
    {
        try
        {
            return Files.createTempDirectory(prefix,
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")))
                    .toFile();
        }
        catch (UnsupportedOperationException ex)
        {
            // POSIX permissions aren't supported (e.g. Windows). Fall back to the
            // platform default, still isolated by the JVM's unique temp directory name.
            return Files.createTempDirectory(prefix).toFile();
        }
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
