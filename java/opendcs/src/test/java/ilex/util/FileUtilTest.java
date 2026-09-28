package ilex.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileUtilTest
{
    @TempDir
    Path tmp;

    @ParameterizedTest
    @CsvSource({
        "file.txt,             true",
        "sub/../file.txt,      true",
        "../evil.txt,          false",
        "sub/../../evil.txt,   false",
        "/evil.txt,            false"
    })
    void test_unzip(String entryName, boolean allowed) throws Exception
    {
        File zip = makeZip(entryName);
        Path target = Files.createDirectory(tmp.resolve("target"));
        RecordingMonitor monitor = new RecordingMonitor();

        FileUtil.unzip(zip.getPath(), target.toString(), monitor);

        assertEquals(allowed, monitor.complete);
        if (allowed)
        {
            assertNull(monitor.failure);
            assertEquals("hello", new String(Files.readAllBytes(target.resolve("file.txt")), StandardCharsets.UTF_8));
        }
        else
        {
            assertNotNull(monitor.failure);
            assertFalse(Files.exists(tmp.resolve("evil.txt")));
        }
    }

    private File makeZip(String entryName) throws Exception
    {
        File zip = tmp.resolve("test.zip").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip)))
        {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write("hello".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return zip;
    }

    private static class RecordingMonitor implements ZipMonitor
    {
        boolean complete = false;
        Exception failure = null;

        @Override public void setZipStatus(String msg) {}
        @Override public void setNumZipEntries(int num) {}
        @Override public void setZipProgress(int num) {}
        @Override public void zipComplete() { complete = true; }
        @Override public void zipFailed(Exception ex) { failure = ex; }
    }
}
