package org.opendcs.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.impl.opendcs.BuiltInIdentityProvider;
import org.opendcs.database.model.IdentityProvider;
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


    @Test
    @EnableIfTsDb({"OpenDCS-Postgres"})
    void test_role_pagination() throws Exception
    {
        UserManagementDao dao = db.getDao(UserManagementDao.class)
                                  .orElseThrow(() -> new UnsupportedOperationException("user dao not supported."));
        try (DataTransaction tx = db.newTransaction())
        {
            for (int i = 0; i < 100; i++)
            {
                dao.addRole(tx, new Role(DbKey.NullKey, "user" + i, "a test " + i, null));
            }

            List<Role> roles = dao.getRoles(tx, -1, -1);
            assertEquals(100, roles.size());

            List<Role> rolesLimit = dao.getRoles(tx, 10, 0);
            assertEquals(10, rolesLimit.size());
            assertEquals("user9", rolesLimit.get(rolesLimit.size()-1).name);

            List<Role> rolesLimitOffset = dao.getRoles(tx, 10, 10);
            assertEquals(10, rolesLimitOffset.size());
            assertEquals("user19", rolesLimitOffset.get(rolesLimit.size()-1).name);

        }
    }


    @Test
    void test_identity_provider_operations() throws Exception
    {
        UserManagementDao dao = db.getDao(UserManagementDao.class)
                                  .orElseGet(() -> fail("user dao not supported."));
        DbKey id = DbKey.NullKey;
        try (DataTransaction tx = db.newTransaction())
        {
            IdentityProvider idpIn = new BuiltInIdentityProvider(DbKey.NullKey, "odcs-idp", null, null);
            IdentityProvider idpOut = dao.addIdentityProvider(tx, idpIn);
            assertNotEquals(DbKey.NullKey, idpOut.getId());
            assertEquals("odcs-idp", idpOut.getName());
            id = idpOut.getId();

            IdentityProvider out2 = dao.getIdentityProvider(tx, id).orElseGet(() -> fail("could not retrieve identity provided"));
            assertNotEquals(DbKey.NullKey, out2.getId());
            assertEquals(idpIn.getName(), out2.getName());
            HashMap<String, Object> config = new HashMap<>();
            config.put("a test", "value");
            IdentityProvider updater = new BuiltInIdentityProvider(id, "odcs-idp2", null, config);
            IdentityProvider updated = dao.updateIdentityProvider(tx, id, updater);
            assertEquals(updater.getName(), updated.getName());
            assertTrue(updated.configToMap().size() > 0);


            dao.deleteIdentityProvider(tx, id);
            dao.getIdentityProvider(tx, id).ifPresent(idp -> fail("Provider was not deleted."));
        }
    }
}
