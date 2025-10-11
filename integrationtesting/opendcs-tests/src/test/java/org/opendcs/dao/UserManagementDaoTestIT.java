package org.opendcs.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.Role;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
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
        DbKey id = DbKey.NullKey;
        try (DataTransaction tx = db.newTransaction())
        {
            Role role = dao.addRole(tx, new Role(DbKey.NullKey, "user", "a test", null));
            assertEquals("user", role.name);
            assertNotEquals(DbKey.NullKey, role.id);
            id = role.id;
        }

        try (DataTransaction tx = db.newTransaction())
        {
            Role role = dao.getRole(tx, id).orElseGet(() -> fail("could not retrieve role"));
            assertEquals("user", role.name);

            Role updated = new Role(null, "test", "a test2", null);
            dao.updateRole(tx, id, updated);

            dao.deleteRole(tx, id);
            dao.getRole(tx, id).ifPresent(r -> fail("role was not deleted"));
        }
    }
}
