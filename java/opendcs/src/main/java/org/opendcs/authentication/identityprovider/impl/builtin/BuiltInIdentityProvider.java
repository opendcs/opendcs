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
*/
package org.opendcs.authentication.identityprovider.impl.builtin;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.authentication.IdentityProviderCredentials;
import org.opendcs.authentication.InvalidCredentialsType;
import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.User;
import org.opendcs.spi.authentication.IdentityProviderProvider;

import com.google.auto.service.AutoService;

import com.password4j.Password;

import decodes.sql.DbKey;

/**
 * Identity Provider that marks an internal user.
 * Always active, at least used for initial setup/breakfix admin
 * account.
 */
public final class BuiltInIdentityProvider implements IdentityProvider
{
    public static final String TYPE = "BuiltIn";
    private final DbKey id;
    private final String name;
    private final ZonedDateTime updatedAt;
    private final Map<String, Object> config;

    public BuiltInIdentityProvider(DbKey id, String name, ZonedDateTime updateAt, Map<String, Object> configMap)
    {
        this.id = id;
        this.name = name;
        this.updatedAt = updateAt;
        this.config = configMap;
    }


    @Override
    public DbKey getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public ZonedDateTime getUpdatedAt()
    {
        return updatedAt;
    }

    @Override
    public Map<String, Object> configToMap()
    {
        return config;
    }

    @Override
    public Optional<User> login(OpenDcsDatabase db, DataTransaction tx, IdentityProviderCredentials credentials)
            throws OpenDcsAuthException
    {
        try
        {
            if (credentials instanceof BuiltInProviderCredentials creds)
            {
                var handle = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsAuthException("Unable to get database connection object"));

                var userId = handle.createQuery(
                                    """
                                        select user_id
                                        from user_identity_provider
                                        where identity_provider_id = :id
                                        and subject = :username
                                    """)
                                    .bind("id", this.id)
                                    .bind("username", creds.username)
                                    .mapTo(DbKey.class)
                                    .findOne()
                                    .orElseThrow(() -> new OpenDcsAuthException("User does not exist."));

                var hashedPassword  = handle.createQuery("select password from opendcs_user_password where user_id = :id")
                                      .bind("id", userId)
                                      .mapTo(String.class)
                                      .findOne()

                                      .orElseThrow(() -> new OpenDcsAuthException("Unable to authenticate user"));
                var pw = Password.check(creds.password, hashedPassword);
                if (pw.withArgon2())
                {
                    var userDao = db.getDao(UserManagementDao.class)
                                .orElseThrow(() -> new OpenDcsAuthException("No User Management DAO is available."));
                    return userDao.getUser(tx, userId);
                }
                return Optional.empty();
            }
            else
            {
                throw new InvalidCredentialsType("This provider cannot handle credentials of type " +
                                                 credentials.getClass().getName());
            }
        }
        catch (OpenDcsDataException ex)
        {
            throw new OpenDcsAuthException("Unable to perform database operations.", ex);
        }
    }


    @Override
    public void updateUserCredentials(OpenDcsDatabase db, DataTransaction tx, User user, IdentityProviderCredentials credentials)
            throws OpenDcsAuthException
    {
        if (credentials instanceof BuiltInProviderCredentials creds)
        {
            try
            {
                var hashed = Password.hash(creds.password.getBytes()).addPepper().addRandomSalt().withArgon2();
                var handle = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsAuthException("Unable to retrieve database connection object."));
                handle.createUpdate(
                       """
                        insert into opendcs_user_password(user_id, password)
                        values (:id, :password)
                        on conflict (user_id) do update set password = :password
                       """)
                      .bind("id", user.id)
                      .bind("password", hashed.getResult())
                      .execute();
            }
            catch (OpenDcsDataException ex)
            {
                throw new OpenDcsAuthException("Database operations failed.", ex);
            }
        }
        else
        {
            throw new InvalidCredentialsType("This provider cannot handle credentials of type " +
                                                credentials.getClass().getName());
        }
    }

    @Override
    public boolean canUpdateCredentials()
    {
        return true;
    }

    @AutoService(IdentityProviderProvider.class)
    public static class BuiltInIdentityProviderProvider implements IdentityProviderProvider
    {

        @Override
        public IdentityProvider create(DbKey id, String name, ZonedDateTime updatedAt, Map<String, Object> config)
        {
            return new BuiltInIdentityProvider(id, name, updatedAt, config);
        }

        @Override
        public String getName()
        {
            return TYPE;
        }
    }

}
