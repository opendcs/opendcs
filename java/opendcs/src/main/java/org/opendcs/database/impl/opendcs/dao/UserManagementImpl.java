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
package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigColumnMapper;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.User;
import org.opendcs.database.model.mappers.IdentityProviderMapper;
import org.opendcs.database.model.mappers.RoleMapper;
import org.opendcs.database.model.mappers.user.IdentityProviderMappingMapper;
import org.opendcs.database.model.mappers.user.UserBuilderMapper;
import org.opendcs.database.model.mappers.user.UserBuilderReducer;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;
import org.opendcs.utils.sql.GenericColumns;

import decodes.sql.DbKey;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = UserManagementDao.class)
public class UserManagementImpl implements UserManagementDao
{

    private static final RoleMapper ROLE_MAPPER = RoleMapper.withPrefix(null);
    private static final IdentityProviderMapper PROVIDER_MAPPER = IdentityProviderMapper.withPrefix(null);

    @Override
    public List<User> getUsers(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());
        final String userSelect = "select u.id u_id, u.preferences::text u_preferences, u.email u_email," +
            "       u.created_at u_created_at, u.updated_at u_updated_at, " +
            "       r.id r_id, r.name r_name, r.description r_description, r.updated_at r_updated_at," +
            "       uip.identity_provider_id i_id, uip.subject i_subject,  " +
            "       idp.name i_name, idp.type i_type, idp.updated_at i_updated_at, idp.config::text i_config" +
            "  from opendcs_user u" +
            "  left join user_roles ur on ur.user_id = u.id" +
            "  left join opendcs_role r on r.id = ur.role_id" +
            "  left join user_identity_provider uip on uip.user_id = u.id" +
            "  left join identity_provider idp on idp.id = uip.identity_provider_id" +
            addLimitOffset(limit, offset);

        try (Query q = handle.createQuery(userSelect))
        {
            if (limit != -1)
            {
                q.bind(SqlKeywords.LIMIT, limit);
            }

            if (offset != -1)
            {
                q.bind(SqlKeywords.OFFSET, offset);
            }

            return q.registerRowMapper(UserBuilder.class, UserBuilderMapper.withPrefix("u"))
                .registerRowMapper(Role.class, RoleMapper.withPrefix("r"))
                .registerRowMapper(IdentityProviderMapping.class, IdentityProviderMappingMapper.withPrefix("i"))
                .reduceRows(UserBuilderReducer.USER_BUILDER_REDUCER)
                .map(UserBuilder::build)
                .collect(Collectors.toList());
        }
    }

    @Override
    public User addUser(DataTransaction tx, User user) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());

        DbKey id = DbKey.NullKey;

        try (Query addUser = handle.createQuery("insert into opendcs_user(email, updated_at, preferences) " +
                                        "values (:email, now(), :preferences::jsonb) returning id"))
        {
            id = DbKey.createDbKey(
                addUser.bind(GenericColumns.EMAIL, user.email)
                    .bind(GenericColumns.PREFERENCES, user.preferences)
                    .mapTo(Long.class)
                    .one()
                );
        }

        try (PreparedBatch roleBatch = handle.prepareBatch("insert into user_roles(user_id, role_id) values (:user_id, :role_id)"))
        {
            for (Role role: user.roles)
            {
                roleBatch.bind(UserBuilderMapper.USER_ID, id)
                        .bind(RoleMapper.ROLE_ID, role.id)
                        .add();
            }
            roleBatch.execute();
        }

        try (PreparedBatch idpBatch = 
                handle.prepareBatch(
                    "insert into user_identity_provider (user_id, identity_provider_id, subject) " +
                                                "values (:user_id, :identity_provider_id, :subject)"))
        {
            for (IdentityProviderMapping idpM: user.identityProviders)
            {
                idpBatch.bind(UserBuilderMapper.USER_ID, id)
                        .bind(IdentityProviderMapper.IDENTITY_PROVIDER_ID, idpM.provider.getId())
                        .bind(GenericColumns.SUBJECT, idpM.subject)
                        .add();
            }
            idpBatch.execute();
        }

        return getUser(tx, id).orElseThrow(() -> new OpenDcsDataException("Created User could not be retrieved."));
    
    }

    @Override
    public Optional<User> getUser(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());

        try (Query user = handle.createQuery(
            "select u.id u_id, u.preferences::text u_preferences, u.email u_email," +
            "       u.created_at u_created_at, u.updated_at u_updated_at, " +
            "       r.id r_id, r.name r_name, r.description r_description, r.updated_at r_updated_at," +
            "       uip.identity_provider_id i_id, uip.subject i_subject,  " +
            "       idp.name i_name, idp.type i_type, idp.updated_at i_updated_at, idp.config::text i_config" +
            "  from opendcs_user u" +
            "  left join user_roles ur on ur.user_id = u.id" +
            "  left join opendcs_role r on r.id = ur.role_id" +
            "  left join user_identity_provider uip on uip.user_id = u.id" +
            "  left join identity_provider idp on idp.id = uip.identity_provider_id" +
            "  where u.id = :id"
            ))
        {
             return user.bind(GenericColumns.ID, id)
              .registerRowMapper(UserBuilder.class, UserBuilderMapper.withPrefix("u"))
              .registerRowMapper(Role.class, RoleMapper.withPrefix("r"))
              .registerRowMapper(IdentityProviderMapping.class, IdentityProviderMappingMapper.withPrefix("i"))
              .reduceRows(UserBuilderReducer.USER_BUILDER_REDUCER)
              .map(UserBuilder::build)
              .findFirst()
              ;
        }
    }



    @Override
    public User updateUser(DataTransaction tx, DbKey id, User user) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        
        try (Update userUpdate =handle.createUpdate(
            "update opendcs_user set email = :email, preferences = :preferences::jsonb " +
            "where id = :id"))
        {
            userUpdate.bind(GenericColumns.ID, id)
                        .bind(GenericColumns.PREFERENCES, user.preferences)
                        .bind(GenericColumns.EMAIL, user.email) // wait should we allow changing the email?
                        .execute();
        }
        try (Update deleteRoles = handle.createUpdate("delete from user_roles where user_id = :id"))
        {
            deleteRoles.bind(GenericColumns.ID, id).execute();
        }
        try (PreparedBatch roleBatch = handle.prepareBatch("insert into user_roles(user_id, role_id) values (:user_id, :role_id)"))
        {
            for (Role role: user.roles)
            {
                roleBatch.bind(UserBuilderMapper.USER_ID, id)
                        .bind(RoleMapper.ROLE_ID, role.id)
                        .add();
            }
            roleBatch.execute();
        }

        try (Update deleteProviders = handle.createUpdate("delete from user_identity_provider where user_id=:id"))
        {
            deleteProviders.bind(GenericColumns.ID, id).execute();
        }

        try (PreparedBatch idpBatch = 
                handle.prepareBatch("insert into user_identity_provider (user_id, identity_provider_id, subject) " +
                                                                "values (:user_id, :identity_provider_id, :subject)"))
        {
            for (IdentityProviderMapping idpM: user.identityProviders)
            {
                idpBatch.bind(UserBuilderMapper.USER_ID, id)
                        .bind(IdentityProviderMapper.IDENTITY_PROVIDER_ID, idpM.provider.getId())
                        .bind(GenericColumns.SUBJECT, idpM.subject)
                        .add();
            }
            idpBatch.execute();
        }

        return getUser(tx, id).orElseThrow(() -> new OpenDcsDataException("Updated User could not be retrieved."));
    }

    @Override
    public void deleteUser(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Update deleteRoles = handle.createUpdate("delete from user_roles where user_id = :id");
             Update deleteIdps = handle.createUpdate("delete from user_identity_provider where user_id = :id");
             Update deletePassword = handle.createUpdate("delete from opendcs_user_password where user_id = :id");
             Update deleteUser = handle.createUpdate("delete from opendcs_user where id = :id"))
        {
            deletePassword.bind(GenericColumns.ID, id).execute();
            deleteRoles.bind(GenericColumns.ID, id).execute();
            deleteIdps.bind(GenericColumns.ID, id).execute();
            deleteUser.bind(GenericColumns.ID, id).execute();
        }
    }

    @Override
    public List<IdentityProvider> getIdentityProviders(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);

        try (Query select = handle.createQuery(
            "select id, name, type, updated_at, config::text from identity_provider" +
            addLimitOffset(limit, offset)))
        {
            if (limit != -1)
            {
                select.bind(SqlKeywords.LIMIT, limit);
            }

            if (offset != -1)
            {
                select.bind(SqlKeywords.OFFSET, offset);
            }

            return select.map(PROVIDER_MAPPER).list();
        }
    }

    @Override
    public IdentityProvider addIdentityProvider(DataTransaction tx, IdentityProvider provider)
            throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());

        try (Query addIdp = 
                handle.createQuery("insert into identity_provider (name, type, updated_at, config) " +
                                                            "values (:name, :type, now(), :config::jsonb) returning id, name, type, updated_at, config::text"))
        {
            return addIdp.bind(GenericColumns.NAME, provider.getName())
                            .bind(IdentityProviderMapper.TYPE, provider.getType())
                            .bind(GenericColumns.CONFIG, provider.configToMap())
                            .map(PROVIDER_MAPPER).one();
        }
    }

    @Override
    public Optional<IdentityProvider> getIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());

        try (Query getIdp = handle.createQuery("select id, name, type, updated_at, config::text from identity_provider where id = :id"))
        {
            return getIdp.bind(GenericColumns.ID, id).map(PROVIDER_MAPPER).findOne();
        }
    }

    @Override
    public IdentityProvider updateIdentityProvider(DataTransaction tx, DbKey id, IdentityProvider provider)
            throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());
        try (Query updateIdp =
                handle.createQuery("update identity_provider set name = :name, type = :type, " +
                                    "config = :config::jsonb where id = :id returning id, name, type, updated_at, config::text"))
        {
            return updateIdp.bind(GenericColumns.ID, id)
                            .bind(GenericColumns.NAME, provider.getName())
                            .bind(IdentityProviderMapper.TYPE, provider.getType())
                            .bind(GenericColumns.CONFIG, provider.configToMap())
                            .map(PROVIDER_MAPPER).one();
        }
    }

    @Override
    public void deleteIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Update deleteIdp = handle.createUpdate("delete from identity_provider where id = :id"))
        {
            deleteIdp.bind(GenericColumns.ID, id).execute();
        }
    }

    @Override
    public List<Role> getRoles(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Query select = handle.createQuery("select id, name, description, updated_at from opendcs_role" +
                                              addLimitOffset(limit, offset)))
        {
            if (limit != -1)
            {
                select.bind(SqlKeywords.LIMIT, limit);
            }

            if (offset != -1)
            {
                select.bind(SqlKeywords.OFFSET, offset);
            }
            return select.map(ROLE_MAPPER).list();
        }
    }

    @Override
    public Role addRole(DataTransaction tx, Role role) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Query addRole = 
                handle.createQuery("insert into opendcs_role (name, description, updated_at) " +
                                                     "values (:name, :description, now()) returning id, name, description,updated_at"))
        {
            return addRole.bind(GenericColumns.NAME, role.name)
                          .bind(GenericColumns.DESCRIPTION, role.description)
                          .map(ROLE_MAPPER).one();
        }
    }

    @Override
    public Optional<Role> getRole(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Query select = handle.createQuery("select id, name, description, updated_at from opendcs_role where id = :id"))
        {
            return select.bind(GenericColumns.ID, id)
                         .map(ROLE_MAPPER).findOne();
        }
    }

    @Override
    public Role updateRole(DataTransaction tx, DbKey id, Role role) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Query update = handle.createQuery("update opendcs_role set name =:name, description = :description where id=:id returning id, name, description,updated_at"))
        {
            return update.bind(GenericColumns.NAME, role.name)
                         .bind(GenericColumns.DESCRIPTION, role.description)
                         .bind(GenericColumns.ID, id)
                         .map(ROLE_MAPPER).one();
        }
    }

    @Override
    public void deleteRole(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Update delete = handle.createUpdate("delete from opendcs_role where id = :id"))
        {
             delete.bind(GenericColumns.ID, id)
                   .execute();
        }
    }


    /**
     * Helper function. Will be able to just call tx.getConnection(Handle.class)
     * in the future
     * @param tx
     * @return
     */
    // Use of this suppress is temporary. Handle should be part of DataTransaction
    // which would handle the close operation.
    @SuppressWarnings("resource")
    private Handle getHandle(DataTransaction tx) throws OpenDcsDataException
    {
        Handle h = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Connection from transaction."));
        return h.registerArgument(new ConfigArgumentFactory())
                .registerColumnMapper(new ConfigColumnMapper());
    }
}
