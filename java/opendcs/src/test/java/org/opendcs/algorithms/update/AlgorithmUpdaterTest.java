package org.opendcs.algorithms.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.opendcs.database.SimpleDataSource;

class AlgorithmUpdaterTest
{
    @Test
    void movesResEvapMappingsWithoutReplacingValuesAndCanRunTwice() throws Exception
    {
        var source = new SimpleDataSource("jdbc:derby:memory:algorithm_update;create=true", "test", "test");
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement())
        {
            statement.executeUpdate("CREATE TABLE CP_ALGORITHM (ALGORITHM_ID BIGINT, ALGORITHM_NAME VARCHAR(64), EXEC_CLASS VARCHAR(240), CMMNT VARCHAR(1000))");
            statement.executeUpdate("CREATE TABLE CP_ALGO_TS_PARM (ALGORITHM_ID BIGINT, ALGO_ROLE_NAME VARCHAR(24), PARM_TYPE VARCHAR(24), PRIMARY KEY (ALGORITHM_ID, ALGO_ROLE_NAME))");
            statement.executeUpdate("CREATE TABLE CP_ALGO_PROPERTY (ALGORITHM_ID BIGINT, PROP_NAME VARCHAR(48), PROP_VALUE VARCHAR(240), PRIMARY KEY (ALGORITHM_ID, PROP_NAME))");
            statement.executeUpdate("CREATE TABLE CP_COMPUTATION (COMPUTATION_ID BIGINT, ALGORITHM_ID BIGINT, DATE_TIME_LOADED BIGINT)");
            statement.executeUpdate("CREATE TABLE CP_COMP_TS_PARM (COMPUTATION_ID BIGINT, ALGO_ROLE_NAME VARCHAR(24), SITE_DATATYPE_ID BIGINT, PRIMARY KEY (COMPUTATION_ID, ALGO_ROLE_NAME))");
            statement.executeUpdate("CREATE TABLE CP_COMP_PROPERTY (COMPUTATION_ID BIGINT, PROP_NAME VARCHAR(48), PROP_VALUE VARCHAR(240), PRIMARY KEY (COMPUTATION_ID, PROP_NAME))");
            statement.executeUpdate("INSERT INTO CP_ALGORITHM VALUES (1, 'Custom ResEvap', 'decodes.cwms.algo.ResEvapAlgo', 'Keep this comment')");
            statement.executeUpdate("INSERT INTO CP_ALGO_TS_PARM VALUES (1, 'hourlyEvap', 'o')");
            statement.executeUpdate("INSERT INTO CP_ALGO_TS_PARM VALUES (1, 'dailyEvap', 'o')");
            statement.executeUpdate("INSERT INTO CP_COMPUTATION VALUES (10, 1, 1)");
            statement.executeUpdate("INSERT INTO CP_COMP_TS_PARM VALUES (10, 'hourlyEvap', 123)");
            statement.executeUpdate("INSERT INTO CP_COMP_TS_PARM VALUES (10, 'dailyEvap', 456)");
            statement.executeUpdate("INSERT INTO CP_COMP_PROPERTY VALUES (10, 'hourlyEvap_MISSING', 'IGNORE')");
        }

        AlgorithmUpdater.synchronize(source, true);
        assertEquals("hourlyEvap", scalar(source, "SELECT ALGO_ROLE_NAME FROM CP_COMP_TS_PARM WHERE SITE_DATATYPE_ID = 123"));

        var first = AlgorithmUpdater.synchronize(source, false);
        assertEquals(2, first.get(0).renamedParameters());
        assertEquals("hourlyEvapRate", scalar(source, "SELECT ALGO_ROLE_NAME FROM CP_COMP_TS_PARM WHERE SITE_DATATYPE_ID = 123"));
        assertEquals("dailyEvapDepth", scalar(source, "SELECT ALGO_ROLE_NAME FROM CP_COMP_TS_PARM WHERE SITE_DATATYPE_ID = 456"));
        assertEquals("IGNORE", scalar(source, "SELECT PROP_VALUE FROM CP_COMP_PROPERTY WHERE PROP_NAME = 'hourlyEvapRate_MISSING'"));
        assertEquals("Keep this comment", scalar(source, "SELECT CMMNT FROM CP_ALGORITHM WHERE ALGORITHM_ID = 1"));
        assertTrue(exists(source, "SELECT 1 FROM CP_ALGO_TS_PARM WHERE ALGO_ROLE_NAME = 'hourlyEvapDepth'"));
        assertFalse(exists(source, "SELECT 1 FROM CP_ALGO_TS_PARM WHERE ALGO_ROLE_NAME = 'hourlyEvap'"));

        var second = AlgorithmUpdater.synchronize(source, false);
        assertEquals(0, second.get(0).renamedParameters());

        // An algorithm record may have been reimported before the computation was repaired.
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement())
        {
            statement.executeUpdate("UPDATE CP_COMP_TS_PARM SET ALGO_ROLE_NAME = 'hourlyEvap' WHERE SITE_DATATYPE_ID = 123");
        }
        var reimported = AlgorithmUpdater.synchronize(source, false);
        assertEquals(1, reimported.get(0).renamedParameters());
        assertEquals("hourlyEvapRate", scalar(source, "SELECT ALGO_ROLE_NAME FROM CP_COMP_TS_PARM WHERE SITE_DATATYPE_ID = 123"));

        // A computation with both names needs a user's decision; keep both mappings.
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement())
        {
            statement.executeUpdate("INSERT INTO CP_COMP_TS_PARM VALUES (10, 'hourlyEvap', 789)");
        }
        var ambiguous = AlgorithmUpdater.synchronize(source, false);
        assertEquals(0, ambiguous.get(0).renamedParameters());
        assertEquals(1, ambiguous.get(0).needsReview());
        assertEquals("hourlyEvap", scalar(source, "SELECT ALGO_ROLE_NAME FROM CP_COMP_TS_PARM WHERE SITE_DATATYPE_ID = 789"));

        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement())
        {
            statement.executeUpdate("INSERT INTO CP_ALGO_TS_PARM VALUES (1, 'hourlyEvap', 'o')");
        }
        var duplicateAlgorithmRole = AlgorithmUpdater.synchronize(source, false);
        assertEquals(1, duplicateAlgorithmRole.get(0).removedAlgorithmParameters());
        assertFalse(exists(source, "SELECT 1 FROM CP_ALGO_TS_PARM WHERE ALGO_ROLE_NAME = 'hourlyEvap'"));
    }

    private static String scalar(SimpleDataSource source, String sql) throws Exception
    {
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql))
        {
            result.next();
            return result.getString(1);
        }
    }

    private static boolean exists(SimpleDataSource source, String sql) throws Exception
    {
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql))
        {
            return result.next();
        }
    }
}
