package org.opendcs.fixtures.extensions.lrgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LrgsTestExtensionTest
{
    @Test
    void test_posix_temp_directory_is_owner_only() throws IOException
    {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                   "default file system has no POSIX view");
        Path dir = LrgsTestExtension.createPrivateTempDirectory("lrgshome", true);
        try
        {
            assertTrue(Files.isDirectory(dir), "directory was not created");
            assertTrue(dir.getFileName().toString().startsWith("lrgshome"), "prefix was not kept");
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(dir);
            assertEquals(PosixFilePermissions.fromString("rwx------"), perms,
                         "other users must not be able to read or write the fixture directory");
        }
        finally
        {
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void test_non_posix_temp_directory_falls_back_to_default_permissions() throws IOException
    {
        // The fallback taken on Windows, where the default ACL already restricts the directory.
        Path dir = LrgsTestExtension.createPrivateTempDirectory("lrgshome", false);
        try
        {
            assertTrue(Files.isDirectory(dir), "directory was not created");
            assertTrue(dir.getFileName().toString().startsWith("lrgshome"), "prefix was not kept");
        }
        finally
        {
            Files.deleteIfExists(dir);
        }
    }
}
