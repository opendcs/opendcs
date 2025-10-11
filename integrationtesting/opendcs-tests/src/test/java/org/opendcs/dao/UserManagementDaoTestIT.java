package org.opendcs.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.Role;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfSql;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;

public class UserManagementDaoTestIT extends AppTestBase
{

    @ConfiguredField
    private OpenDcsDatabase db;

    @Test
    @EnableIfTsDb({"OpenDCS-Postgres"})
    void test_create_role() throws Exception
    {
        UserManagementDao dao = db.getDao(UserManagementDao.class)
                                  .orElseThrow(() -> new UnsupportedOperationException("user dao not supported."));
        try (DataTransaction tx = db.newTransaction())
        {
            Role role = dao.addRole(tx, new Role(DbKey.NullKey, "user", "a test", null));
            assertEquals("test", role.name);
            assertNotEquals(DbKey.NullKey, role.id);
        }
    }
}
