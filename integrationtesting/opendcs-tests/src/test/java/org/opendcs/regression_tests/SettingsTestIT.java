package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.SimpleOpenDcsDatabaseWrapper;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfSql;

import decodes.util.DecodesSettings;

class SettingsTestIT extends AppTestBase
{
    @ConfiguredField
    private OpenDcsDatabase db;

    @EnableIfSql
    @Test
    void test_load_settings() throws Exception
    {
        // that's it, we're just making sure these don't throw
        // may need more in the future to deal with oracle asinine "empty strings are null though"
        assertDoesNotThrow(()->
        {
            var props = new Properties();
            var settings = db.getSettings(DecodesSettings.class).orElseThrow();
            settings.saveToProps(props);
            try (var tx = db.newTransaction();
                var conn = tx.connection(Connection.class).orElseThrow();
                PreparedStatement stmt = conn.prepareStatement("insert into tsdb_property(prop_name,prop_value) values(?,?)"))
            {
                for (var key: props.keySet())
                {
                    stmt.setString(1, key.toString());
                    stmt.setString(2, props.getProperty(key.toString()));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        });


        var ds = ((SimpleOpenDcsDatabaseWrapper)db).getDataSource();
        assertDoesNotThrow(() -> DatabaseService.getDatabaseFor(ds));
    }
}
