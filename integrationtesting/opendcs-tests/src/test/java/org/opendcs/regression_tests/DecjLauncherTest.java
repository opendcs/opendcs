package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.helpers.ArgumentEchoApp;

public class DecjLauncherTest extends AppTestBase
{
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    static Stream<Arguments> windowsArguments()
    {
        return Stream.of(
            Arguments.of(
                new String[]{"-d1", "-lc:\\tmp\\abc.log", "-DLOG_DIR=c:\\tmp"},
                new String[]{"LOG_LEVEL=DEBUG", "LOG_FILE=c:\\tmp\\abc.log", "LOG_DIR=c:\\tmp"}
            ),
            Arguments.of(
                new String[]{"-d7", "-lc:\\tmp\\abc.log", "-DLOG_DIR=c:\\tmp"},
                new String[]{"LOG_LEVEL=TRACE", "LOG_FILE=c:\\tmp\\abc.log", "LOG_DIR=c:\\tmp"}
            ),
            Arguments.of(
                new String[]{"-d", "1", "-l", "c:\\tmp\\abc.log", "-DLOG_DIR=c:\\tmp"},
                new String[]{"LOG_LEVEL=DEBUG", "LOG_FILE=c:\\tmp\\abc.log", "LOG_DIR=c:\\tmp"}
            )
        );
    }

    static Stream<Arguments> unixArguments()
    {
        return Stream.of(
            Arguments.of(
                new String[]{"-d1", "-l/tmp/abc.log", "-DLOG_DIR=/tmp"},
                new String[]{"LOG_LEVEL=DEBUG", "LOG_FILE=/tmp/abc.log", "LOG_DIR=/tmp"}
            ),
            Arguments.of(
                new String[]{"-d", "1", "-l", "/tmp/abc.log", "-DLOG_DIR=/tmp"},
                new String[]{"LOG_LEVEL=DEBUG", "LOG_FILE=/tmp/abc.log", "LOG_DIR=/tmp"}
            )
        );
    }

    @ParameterizedTest
    @MethodSource("windowsArguments")
    void testDecjWindows(String[] inputArgs, String[] expectedOutputs) throws Exception
    {
        assumeTrue(IS_WINDOWS, "Skipping Windows test on non-Windows platform");

        String output = run(inputArgs);

        for (String expected : expectedOutputs)
        {
            assertTrue(output.contains(expected), "Expected '" + expected + "' in output");
        }
    }

    @ParameterizedTest
    @MethodSource("unixArguments")
    void testDecjUnix(String[] inputArgs, String[] expectedOutputs) throws Exception
    {
        assumeTrue(!IS_WINDOWS, "Skipping Unix test on Windows platform");

        String output = run(inputArgs);

        for (String expected : expectedOutputs)
        {
            assertTrue(output.contains(expected), "Expected '" + expected + "' in output");
        }
    }

    /**
     * Launches ArgumentEchoApp via decj.bat (Windows) or decj (Unix/Linux)
     * and captures the output.
     * @param args the command-line arguments to pass to ArgumentEchoApp
     * @return the captured standard output from the process
     * @throws Exception if an error occurs during execution
     */
    public String run(String... args) throws Exception
    {
        setupTestClassOnClasspath();

        List<String> command = new ArrayList<>();

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String decjScript = isWindows ? "decj.bat" : "decj";
        File binDir = new File(configuration.getUserDir(), "bin");
        File scriptFile = new File(binDir, decjScript);

        command.add(scriptFile.getAbsolutePath());
        command.add("org.opendcs.fixtures.helpers.ArgumentEchoApp");
        for (String arg : args)
        {
            command.add(arg);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())))
        {
            output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        process.waitFor();

        return output;
    }

    /**
     * Copy ArgumentEchoApp class to the dep/ directory
     * so that decj.bat can find it on the classpath.
     */
    private void setupTestClassOnClasspath() throws Exception
    {
        File depDir = new File(configuration.getUserDir(), "dep");
        depDir.mkdirs();

        URL classLocation = ArgumentEchoApp.class.getProtectionDomain()
                                                  .getCodeSource()
                                                  .getLocation();
        Path classesDir = Path.of(classLocation.toURI());
        Path classFile = classesDir.resolve("org/opendcs/fixtures/helpers/ArgumentEchoApp.class");
        Path targetPackageDir = depDir.toPath().resolve("org/opendcs/fixtures/helpers");
        Files.createDirectories(targetPackageDir);
        Files.copy(classFile, targetPackageDir.resolve("ArgumentEchoApp.class"),
                   StandardCopyOption.REPLACE_EXISTING);
    }
}
