package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.opendcs.algorithms.update.AlgorithmUpdateApp;
import org.opendcs.database.ManageDatabaseApp;
import org.opendcs.fixtures.AppTestBase;

import decodes.launcher.Profile;

/** Exercises the updater's own connections against real CWMS row-level security. */
@EnabledIfSystemProperty(named = "opendcs.test.engine", matches = "CWMS-Oracle")
public class AlgorithmUpdateCwmsTestIT extends AppTestBase
{
    @Test
    public void standalonePreviewDoesNotRequireAnAlgorithmFilter()
    {
        String username = (String)configuration.getEnvironment().get("DB_USERNAME");
        String password = (String)configuration.getEnvironment().get("DB_PASSWORD");
        assertNotNull(username);
        assertNotNull(password);
        AlgorithmUpdateApp.main(args("-P", configuration.getPropertiesFile().getAbsolutePath(),
                "-username", username, "-password", password));
        assertExitNullOrZero();
    }

    @Test
    public void managerAfterUpdateConnectionAndStandalonePreviewHaveCwmsContext() throws Exception
    {
        String username = (String)configuration.getEnvironment().get("DB_USERNAME");
        String password = (String)configuration.getEnvironment().get("DB_PASSWORD");
        assertNotNull(username);
        assertNotNull(password);
        Profile profile = Profile.getProfile(configuration.getPropertiesFile());
        DataSource source = ManageDatabaseApp.getDataSourceForAfterUpdates(profile, null, username, password);
        String algorithm;
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT ALGORITHM_NAME FROM CP_ALGORITHM WHERE ROWNUM = 1");
             ResultSet rows = statement.executeQuery())
        {
            assertTrue(rows.next(), "The CWMS baseline must contain an algorithm for the preview");
            algorithm = rows.getString(1);
        }
        // No -apply: this checks the actual command entry point without changing baseline data.
        AlgorithmUpdateApp.main(args("-P", configuration.getPropertiesFile().getAbsolutePath(),
                "-username", username, "-password", password, "-algorithm", algorithm));
        assertExitNullOrZero();
    }
}
