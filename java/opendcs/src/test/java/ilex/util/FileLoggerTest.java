package ilex.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

public class FileLoggerTest
{
    private static org.slf4j.Logger log = LoggerFactory.getLogger(FileLoggerTest.class);
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    static Path tempDir;

    private Path myLog;
    private Path myOldLog;

    @BeforeEach
    void create_log_files() throws Exception
    {
        myLog = Files.createFile(tempDir.resolve("test.log"));
        assertNotNull(myLog);
        myOldLog = Files.createFile(tempDir.resolve("test.log.old"));
        assertNotNull(myOldLog);
        log.info("Test Logger output will be at: {}", myLog);
        Files.deleteIfExists(myOldLog);
        Files.deleteIfExists(myLog);
    }

    @Test
    void test_file_logger_rotation() throws Exception
    {
        
        // The strings below are 20 characters, repeated 25 times and 24 respective to force, and then prevent a rotation.
        final String testMsg1 = String.join("", Collections.nCopies(25,"This is test1string."));
        final String testMsg2 = String.join("", Collections.nCopies(24,"This is test2string."));

        // 500 bytes to force rotation
        final FileLogger loggerUnderTest = new FileLogger("test", myLog.toString(), 700);
        loggerUnderTest.setMinLogPriority(Logger.E_DEBUG3);
        loggerUnderTest.doLog(Logger.E_INFORMATION, testMsg1);
        loggerUnderTest.doLog(Logger.E_WARNING, testMsg1);
        loggerUnderTest.doLog(Logger.E_DEBUG1, testMsg2);

        Thread.sleep(5000); // wait 5 seconds for all operations to complete.
        
        assertTrue(myOldLog.toFile().exists(), "Log was not rotated.");

        String oldLog = FileUtil.getFileContents(myOldLog.toFile());
        String curLog = FileUtil.getFileContents(myLog.toFile());
        assertNotNull(curLog, "Current log does not exist");
        assertNotNull(oldLog, "Log was never rotated.");

        assertTrue(oldLog.contains(testMsg1));
        assertFalse(oldLog.contains(testMsg2));
        assertTrue(curLog.contains(testMsg2));
        loggerUnderTest.close();
    }
}
