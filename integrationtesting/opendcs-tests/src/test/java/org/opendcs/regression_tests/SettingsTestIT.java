package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.SimpleOpenDcsDatabaseWrapper;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfSql;

import decodes.util.DecodesSettings;

class SettingsTestIT extends AppTestBase
{
    @ConfiguredField
    private OpenDcsDatabase db;

    private static final String MERGE_QUERY = """
                    merge into tsdb_property p
                    using (select :name prop_name, :value prop_value <dual>) input
                    on (p.prop_name = input.prop_name)
                    when matched then
                    update set prop_value = input.prop_value
                    when not matched then
                    insert (prop_name, prop_value)
                    values(input.prop_name, input.prop_value)
                    """;

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
            try (var tx = db.newTransaction())
		    {
                // looks odd, need ro make surer the transaction closes it.
                var conn = tx.connection(Handle.class).orElseThrow();
                try (var upsertProp = conn.prepareBatch(MERGE_QUERY)
                                          .define("dual", tx.getContext().getDatabase() == DatabaseEngine.ORACLE ? "from dual" : ""))
                {
                    settings.saveToProps(props);
                    for(var k: props.keySet())
                    {
                        final String value = props.getProperty(k.toString(), null);
                        upsertProp.bind("name", k.toString())
                                .bind("value", value)
                                .add();
                    }
                    
                    upsertProp.execute();
                
                }
            }
        });


        var ds = ((SimpleOpenDcsDatabaseWrapper)db).getDataSource();
        assertDoesNotThrow(() -> DatabaseService.getDatabaseFor(ds));
    }
}
