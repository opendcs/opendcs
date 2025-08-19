package org.opendcs.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.spi.authentication.AuthSource;

import fixtures.FileUtils;

public class SecretsFileAuthSourceTest
{
    final String TEST_USER = "test_user";
    final String TEST_PASSWORD ="test_password";

    File username;
    File password;

    @BeforeEach
    public void create_files() throws Exception
    {
        username = File.createTempFile("username", null);
        username.deleteOnExit();

        password = File.createTempFile("password",null);
        password.deleteOnExit();
        FileUtils.writeFile(username, TEST_USER);
        FileUtils.writeFile(password, TEST_PASSWORD);
    }

    @Test
    public void test_environment_auth_source() throws Exception
    {
        
        
        final String authSourceUrl = 
            String.format("secrets-auth-source:username=%s,password=%s",
                          username.getAbsolutePath(),
                          password.getAbsolutePath()
                          );
        final AuthSource authSource = AuthSourceService.getFromString(authSourceUrl);
        final Properties credentials = authSource.getCredentials();
        assertEquals(TEST_USER,credentials.getProperty("username"));
        assertEquals(TEST_PASSWORD,credentials.getProperty("password"));
        
    }
}
