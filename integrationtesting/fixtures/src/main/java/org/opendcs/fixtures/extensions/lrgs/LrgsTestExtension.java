package org.opendcs.fixtures.extensions.lrgs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

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
            assertDoesNotThrow(() ->
            {
                // java.io.tmpdir is redirected to a project-local build directory by the
                // consuming test tasks (see opendcs-tests/build.gradle), so this isn't
                // landing in a shared, publicly writable system temp directory. The
                // directory is also created owner-only where the platform supports it.
                File lrgsHome = createPrivateTempDirectory().toFile();
                var testClass = context.getRequiredTestClass();
                var lrgsConfig = testClass.getAnnotation(LrgsConfig.class);
                return new LrgsTestInstance(lrgsHome, lrgsConfig != null ? lrgsConfig.value() : null);
            })
        );
    }

    /**
     * Creates the scratch directory handed to the LRGS instance under test.
     *
     * <p>On POSIX file systems the directory is created owner-only (rwx------) as part of the
     * create call, so it is never briefly readable or writable by other users on the machine even
     * if the configured temp root happens to be a shared directory.</p>
     */
    private static Path createPrivateTempDirectory() throws IOException
    {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
        {
            return Files.createTempDirectory("lrgshome", //NOSONAR - created owner-only, see javadoc above
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        }
        // Windows and friends: no POSIX view, and Files.createTempDirectory already restricts
        // the directory to the current user via the default ACL.
        return Files.createTempDirectory("lrgshome"); //NOSONAR - test fixture, tmpdir is redirected into the build dir
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
