package org.opendcs.fixtures.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The directives a test directory's .config file can carry. Lines naming no known
 * directive are ignored.
 *
 * @param enableOn    the one engine this test runs on, or null to run on any engine.
 * @param expectError text the computation is expected to fail with, or null when the
 *                    test asserts on the computation's output instead.
 * @param unassignedRoles algorithm roles that must have a cp_comp_ts_parm record which
 *                    names no time series, so the computation finds the role declared but
 *                    unassigned rather than absent.
 */
public record TestConfig(String enableOn, String expectError, List<String> unassignedRoles)
{
    private static final String ENABLE_ON = "EnableOn:";
    private static final String EXPECT_ERROR = "ExpectError:";
    private static final String UNASSIGNED_ROLES = "UnassignedRoles:";

    /** What a test directory with no .config file is treated as having. */
    public static final TestConfig EMPTY = new TestConfig(null, null, List.of());

    public static TestConfig read(File configFile) throws IOException
    {
        String enableOn = null;
        String expectError = null;
        List<String> unassignedRoles = List.of();
        for (String line : Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8))
        {
            String setting = line.trim();
            if (setting.startsWith(ENABLE_ON))
            {
                enableOn = setting.substring(ENABLE_ON.length()).trim();
            }
            else if (setting.startsWith(EXPECT_ERROR))
            {
                expectError = setting.substring(EXPECT_ERROR.length()).trim();
            }
            else if (setting.startsWith(UNASSIGNED_ROLES))
            {
                unassignedRoles = Arrays.stream(
                        setting.substring(UNASSIGNED_ROLES.length()).split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .toList();
            }
        }
        return new TestConfig(enableOn, expectError, unassignedRoles);
    }

    /**
     * Skip the calling test unless the engine under test is the one the config names.
     */
    public void assumeEnabledForCurrentEngine()
    {
        if (enableOn == null)
        {
            return;
        }
        final String testEngine = System.getProperty("opendcs.test.engine", "").trim();
        assumeFalse(!enableOn.equals(testEngine), "Test is disabled by config file for: " + enableOn);
    }
}
