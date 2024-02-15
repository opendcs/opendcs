package org.opendcs.fixtures;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.helpers.TestResources;
import org.opendcs.spi.configuration.Configuration;

import ilex.util.EnvExpander;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;
import uk.org.webcompere.systemstubs.security.SystemExit;


@ExtendWith(SystemStubsExtension.class)
@ExtendWith(OpenDCSTestConfigExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class AppTestBase
{
    @SystemStub
    protected final EnvironmentVariables environment = new EnvironmentVariables();

    @SystemStub
    protected final SystemProperties properties = new SystemProperties();

    @SystemStub
    protected final SystemExit exit = new SystemExit();

    @ConfiguredField
    protected Configuration configuration;

    protected void assertExitNullOrZero()
    {
        assertTrue(exit.getExitCode() == null || exit.getExitCode()==0, "System.exit called with unexpected code.");
    }

    /**
     * Helper to make calling mains easier
     * @param arg
     * @return
     */
    public static String[] args(String... arg)
    {
        return arg;
    }
}
