package org.opendcs.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.spi.authentication.AuthSource;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * At this time the library required only supports Java <= 11 due to some
 * changes in java.
 */
@EnabledForJreRange(max = JRE.JAVA_11)
@ExtendWith(SystemStubsExtension.class)
public class EnvironmentAuthSourceTest
{


    @Test
    public void test_environment_auth_source(EnvironmentVariables environment) throws Exception
    {
        final String TEST_USER = "opendcs_user";
        final String TEST_PASSWORD ="opendcs_password";

        environment.set("OPENDCS_USERNAME",TEST_USER);
        environment.set("OPENDCS_PASSWORD",TEST_PASSWORD);

        final String authSourceUrl = "env-auth-source:username=OPENDCS_USERNAME,password=OPENDCS_PASSWORD";
        final AuthSource authSource = AuthSourceService.getFromString(authSourceUrl);
        final Properties credentials = authSource.getCredentials();
        assertEquals(TEST_USER,credentials.getProperty("username"));
        assertEquals(TEST_PASSWORD,credentials.getProperty("password"));
    }
}
