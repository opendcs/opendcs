/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInIdentityProvider;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.User;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;


@EnableIfTsDb({"OpenDCS-Postgres"})
class UserManagementDaoTestIT extends AppTestBase
{

    @ConfiguredField
    private OpenDcsDatabase db;

    @Test
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
    void test_role_pagination() throws Exception
    {
        UserManagementDao dao = db.getDao(UserManagementDao.class)
                                  .orElseThrow(() -> new UnsupportedOperationException("user dao not supported."));
        try (DataTransaction tx = db.newTransaction())
        {
            for (int i = 0; i < 100; i++)
            {
                // silly name so it should sort first and make the checks easier.
                dao.addRole(tx, new Role(DbKey.NullKey, "arole" + String.format("%03d",i), "a test role " + i, null));
            }

            List<Role> roles = dao.getRoles(tx, -1, -1);
            assertTrue(roles.size() >= 100);

            List<Role> rolesLimit = dao.getRoles(tx, 10, 0);
            assertEquals(10, rolesLimit.size());
            assertEquals("arole009", rolesLimit.get(rolesLimit.size()-1).name);

            List<Role> rolesLimitOffset = dao.getRoles(tx, 10, 10);
            assertEquals(10, rolesLimitOffset.size());
            assertEquals("arole019", rolesLimitOffset.get(rolesLimitOffset.size()-1).name);

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
            IdentityProvider updater = new BuiltInIdentityProvider(id, "odcs-idpA", null, config);
            IdentityProvider updated = dao.updateIdentityProvider(tx, id, updater);
            assertEquals(updater.getName(), updated.getName());
            assertTrue(updated.configToMap().size() > 0);


            dao.deleteIdentityProvider(tx, id);
            dao.getIdentityProvider(tx, id).ifPresent(idp -> fail("Provider was not deleted."));
        }
    }

    @Test
    void test_idp_pagination() throws Exception
    {
        UserManagementDao dao = db.getDao(UserManagementDao.class)
                                  .orElseThrow(() -> new UnsupportedOperationException("user dao not supported."));
        try (DataTransaction tx = db.newTransaction())
        {
            for (int i = 0; i < 100; i++)
            {
                HashMap<String, Object> config = new HashMap<>();
                config.put("i", i);
                dao.addIdentityProvider(tx, new BuiltInIdentityProvider(DbKey.NullKey, "idp" + String.format("%03d", i), null, config));
            }

            List<IdentityProvider> providers = dao.getIdentityProviders(tx, -1, -1);
            assertTrue(providers.size() >= 100);

            List<IdentityProvider> providerLimit = dao.getIdentityProviders(tx, 10, 0);
            assertEquals(10, providerLimit.size());
            // The default builtin is provided by the schema migration as part of initial setup.
            assertEquals("idp008", providerLimit.get(providerLimit.size()-1).getName());

            List<IdentityProvider> providerLimitOffset = dao.getIdentityProviders(tx, 10, 10);
            assertEquals(10, providerLimitOffset.size());
            assertEquals("idp018", providerLimitOffset.get(providerLimitOffset.size()-1).getName());

        }
    }


    @Test
    void test_user_operations() throws Exception
    {
        UserManagementDao dao = db.getDao(UserManagementDao.class)
                                  .orElseGet(() -> fail("user dao not supported."));
        DbKey id = DbKey.NullKey;
        try (DataTransaction tx = db.newTransaction())
        {
            User userIn = new UserBuilder()
                                  .withEmail("test@test.com")
                                  .build();
            User userOut = dao.addUser(tx, userIn);
            assertNotEquals(DbKey.NullKey, userOut.id);
            assertEquals(userIn.email, userOut.email);
            id = userOut.id;

            User out2 = dao.getUser(tx, id).orElseGet(() -> fail("could not retrieve identity provided"));
            assertNotEquals(DbKey.NullKey, out2.id);
            assertEquals(userIn.email, out2.email);
            HashMap<String, Object> preferences = new HashMap<>();
            preferences.put("a test", "value");
            User updater = new UserBuilder()
                                    .withPreferences(preferences)
                                    .withEmail("test@test.com")
                                    .build();
            User updated = dao.updateUser(tx, id, updater);
            assertEquals(updater.email, updated.email);
            assertTrue(updated.preferences.size() > 0);


            dao.deleteUser(tx, id);
            dao.getUser(tx, id).ifPresent(idp -> fail("User was not deleted."));
        }
    }

    @Test
    void test_user_pagination() throws Exception
    {
        UserManagementDao dao = db.getDao(UserManagementDao.class)
                                  .orElseThrow(() -> new UnsupportedOperationException("user dao not supported."));
        try (DataTransaction tx = db.newTransaction())
        {
            for (int i = 0; i < 100; i++)
            {
                HashMap<String, Object> preferences = new HashMap<>();
                preferences.put("i", i);
                dao.addUser(tx, new UserBuilder()
                                        .withPreferences(preferences)
                                        .withEmail("user"+i)
                                        .build());
            }

            List<User> users = dao.getUsers(tx, -1, -1);
            assertTrue(users.size() >= 100);

            List<User> usersLimit = dao.getUsers(tx, 10, 0);
            assertEquals(10, usersLimit.size());
            assertEquals("user9", usersLimit.get(usersLimit.size()-1).email);

            List<User> usersLimitOffset = dao.getUsers(tx, 10, 10);
            assertEquals(10, usersLimitOffset.size());
            assertEquals("user19", usersLimitOffset.get(usersLimitOffset.size()-1).email);

        }
    }
}
