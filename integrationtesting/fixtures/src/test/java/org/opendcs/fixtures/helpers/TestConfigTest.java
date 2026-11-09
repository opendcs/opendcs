package org.opendcs.fixtures.helpers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;
import org.opendcs.fixtures.helpers.ImporterHelper.TestConfig;

class TestConfigTest
{
    private static final String ENGINE_PROPERTY = "opendcs.test.engine";

    @TempDir
    Path dir;

    private String engineUnderTest;

    @BeforeEach
    void rememberEngine()
    {
        engineUnderTest = System.getProperty(ENGINE_PROPERTY);
    }

    @AfterEach
    void restoreEngine()
    {
        if (engineUnderTest == null)
        {
            System.clearProperty(ENGINE_PROPERTY);
        }
        else
        {
            System.setProperty(ENGINE_PROPERTY, engineUnderTest);
        }
    }

    private TestConfig read(String... lines) throws Exception
    {
        return TestConfig.read(Files.write(dir.resolve("aTest.config"), List.of(lines)).toFile());
    }

    @Test
    void empty_config_has_no_directives() throws Exception
    {
        TestConfig config = read();

        assertNull(config.enableOn());
        assertNull(config.expectError());
        assertEquals(List.of(), config.unassignedRoles());
    }

    @Test
    void reads_all_directives() throws Exception
    {
        TestConfig config = read("EnableOn: CWMS-Oracle",
                                 "ExpectError: Cannot read rating for 'A.B;C.D.E'",
                                 "UnassignedRoles: indep3,indep4");

        assertEquals("CWMS-Oracle", config.enableOn());
        assertEquals("Cannot read rating for 'A.B;C.D.E'", config.expectError());
        assertEquals(List.of("indep3", "indep4"), config.unassignedRoles());
    }

    @Test
    void unassigned_roles_tolerate_spaces_and_trailing_commas() throws Exception
    {
        TestConfig config = read("UnassignedRoles: indep3 , indep4,, indep5 ,");

        assertEquals(List.of("indep3", "indep4", "indep5"), config.unassignedRoles());
    }

    @Test
    void directives_are_found_anywhere_and_extra_text_is_ignored() throws Exception
    {
        TestConfig config = read("# engine gate", "", "  EnableOn:   OpenDCS-XML  ",
                                 "some other thing", "ExpectError: boom");

        assertEquals("OpenDCS-XML", config.enableOn());
        assertEquals("boom", config.expectError());
    }

    @Test
    void config_without_enable_on_runs_on_any_engine() throws Exception
    {
        System.setProperty(ENGINE_PROPERTY, "OpenDCS-Postgres");

        assertDoesNotThrow(() -> read("ExpectError: boom").assumeEnabledForCurrentEngine());
    }

    @Test
    void enable_on_skips_the_test_for_other_engines() throws Exception
    {
        TestConfig config = read("EnableOn: CWMS-Oracle");

        System.setProperty(ENGINE_PROPERTY, "OpenDCS-XML");
        assertThrows(TestAbortedException.class, () -> config.assumeEnabledForCurrentEngine());

        System.setProperty(ENGINE_PROPERTY, "CWMS-Oracle");
        assertDoesNotThrow(() -> config.assumeEnabledForCurrentEngine());
    }
}
