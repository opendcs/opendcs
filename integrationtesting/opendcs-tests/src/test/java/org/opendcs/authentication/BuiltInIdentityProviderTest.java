package org.opendcs.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInIdentityProvider;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInProviderCredentials;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;

import decodes.sql.DbKey;

@TestInstance(Lifecycle.PER_CLASS)
class BuiltInIdentityProviderTest extends AppTestBase
{
    @ConfiguredField
    private OpenDcsDatabase db;

    private IdentityProvider provider;


    @BeforeAll
    void setup_provider() throws Exception
    {
        var userDao = db.getDao(UserManagementDao.class).get();
        provider = new BuiltInIdentityProvider(DbKey.NullKey, "builtin", null, Map.of());
        try (var tx = db.newTransaction())
        {
            provider = userDao.addIdentityProvider(tx, provider);

            var userIn = new UserBuilder().withEmail("test@example.com")
                                        .withIdentityMapping(new IdentityProviderMapping(provider, "test"))
                                        .build();
            var userOut = userDao.addUser(tx, userIn);

            provider.updateUserCredentials(db, tx, userOut, new BuiltInProviderCredentials("test", "testpassword"));
        }
    }


    @Test
    void test_login() throws Exception
    {
        try (var tx = db.newTransaction())
        {
            assertDoesNotThrow(() -> provider.login(db, tx, new BuiltInProviderCredentials("test", "testpassword")));
        }
    }

}
