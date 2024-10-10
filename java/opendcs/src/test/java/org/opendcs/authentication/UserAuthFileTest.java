package org.opendcs.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opendcs.spi.authentication.AuthSource;

import ilex.util.AuthException;
import ilex.util.UserAuthFile;

public class UserAuthFileTest
{
    @Test
    public void test_user_auth_file_default(@TempDir Path configDir) throws Exception
    {
        final String USERNAME = "Testuser";
        final String PASSWORD = "Testpassword";
        File uaf = configDir.resolve("uaf.txt").toFile();
        UserAuthFile ua = new UserAuthFile(uaf);
        ua.write(USERNAME, PASSWORD);

        final AuthSource asSPI = AuthSourceService.getFromString(uaf.getAbsolutePath());
        final Properties props = asSPI.getCredentials();
        assertEquals(USERNAME,props.getProperty("username"));
        assertEquals(PASSWORD,props.getProperty("password"));

        final AuthSource asSpiWithType = AuthSourceService.getFromString("UserAuthFile:"+uaf.getAbsolutePath());
        final Properties propsFromType = asSpiWithType.getCredentials();
        assertEquals(USERNAME,propsFromType.getProperty("username"));
        assertEquals(PASSWORD,propsFromType.getProperty("password"));
    }

    @Test
    @SuppressWarnings("unused")
    public void unknown_type_throws_error()
    {
        final String badAuthTypeName = "BAD_AUTH_TYPE_NAME:location_doesn't_matter";
        assertThrows( AuthException.class, () -> {
            final AuthSource source = AuthSourceService.getFromString(badAuthTypeName);
        });
    }
}
