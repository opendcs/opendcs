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

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.User;
import org.opendcs.database.model.mappers.IdentityProviderMapper;
import org.opendcs.database.model.mappers.RoleMapper;
import org.opendcs.database.model.mappers.user.IdentityProviderMappingMapper;
import org.opendcs.database.model.mappers.user.UserBuilderMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.sql.DbKey;

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
            (limit != -1 ? " limit :limit ": "") +
            (offset != -1 ? " offset :offset " : "");

        Query q = handle.createQuery(userSelect);
        if (limit != -1)
        {
            q.bind("limit", limit);
        }

        if (offset != -1)
        {
            q.bind("offset", offset);
        }

        return q.registerRowMapper(User.Builder.class, UserBuilderMapper.withPrefix("u"))
              .registerRowMapper(Role.class, RoleMapper.withPrefix("r"))
              .registerRowMapper(IdentityProviderMapping.class, IdentityProviderMappingMapper.withPrefix("i"))
              .reduceRows(UserBuilderMapper.UserBuilderReducer.USER_BUILDER_REDUCER)
              .map(ub -> ub.build())
              .collect(Collectors.toList());
    }

    @Override
    public User addUser(DataTransaction tx, User user) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());
        ObjectMapper om = new ObjectMapper();
        try
        {
            String preferences = om.writeValueAsString(user.preferences);

            DbKey id = DbKey.createDbKey(handle.createQuery(
                "insert into opendcs_user(email, updated_at, preferences) " +
                "values (:email, now(), :preferences::jsonb) returning id")
            .bind("email", user.email)
            .bind("preferences", preferences)
            .mapTo(Long.class)
            .one());

            PreparedBatch roleBatch = handle.prepareBatch("insert into user_roles(user_id, role_id) values (:user_id, :role_id)");
            for (Role role: user.roles)
            {
                roleBatch.bind("user_id", id.getValue())
                         .bind("role_id", role.id.getValue())
                         .add();
            }
            roleBatch.execute();

            PreparedBatch idpBatch = handle.prepareBatch("insert into user_identity_provider (user_id, identity_provider_id, subject) values (:user_id, :identity_provider_id, :subject)");
            for (IdentityProviderMapping idpM: user.identityProviders)
            {
                idpBatch.bind("user_id", id.getValue())
                        .bind("identity_provider_id", idpM.provider.getId())
                        .bind("subject", idpM.subject)
                        .add();
            }
            idpBatch.execute();
            // TODO password, need to handle actual hashing

            return getUser(tx, id).orElseThrow(() -> new OpenDcsDataException("Created User could not be retrieved."));
        }
        catch (JsonProcessingException ex)
        {
            throw new OpenDcsDataException("unable to covert config to json", ex);
        }
    }

    @Override
    public Optional<User> getUser(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());

        return
        handle.createQuery(
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
            )
              .bind("id", id.getValue())
              .registerRowMapper(User.Builder.class, UserBuilderMapper.withPrefix("u"))
              .registerRowMapper(Role.class, RoleMapper.withPrefix("r"))
              .registerRowMapper(IdentityProviderMapping.class, IdentityProviderMappingMapper.withPrefix("i"))
              .reduceRows(UserBuilderMapper.UserBuilderReducer.USER_BUILDER_REDUCER)
              .map(ub -> ub.build())
              .findFirst()
              ;
    }



    @Override
    public User updateUser(DataTransaction tx, DbKey id, User user) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        ObjectMapper om = new ObjectMapper();
        try
        {
            String preferences = om.writeValueAsString(user.preferences);

            handle.createUpdate(
                "update opendcs_user set email = :email, updated_at = now(), preferences = :preferences::jsonb " +
                "where id = :id")
                .bind("id", id.getValue())
                .bind("preferences", preferences)
                .bind("email", user.email) // wait should we allow changing the email?
                .execute();
            handle.createUpdate("delete from user_roles where user_id = :id").bind("id", id.getValue()).execute();
            PreparedBatch roleBatch = handle.prepareBatch("insert into user_roles(user_id, role_id) values (:user_id, :role_id)");
            for (Role role: user.roles)
            {
                roleBatch.bind("user_id", id.getValue())
                         .bind("role_id", role.id.getValue())
                         .add();
            }
            roleBatch.execute();

            handle.createUpdate("delete from user_identity_provider where user_id=:id").bind("id", id.getValue()).execute();
            PreparedBatch idpBatch = handle.prepareBatch("insert into user_identity_provider (user_id, identity_provider_id, subject) values (:user_id, :identity_provider_id, :subject)");
            for (IdentityProviderMapping idpM: user.identityProviders)
            {
                idpBatch.bind("user_id", id.getValue())
                        .bind("identity_provider_id", idpM.provider.getId())
                        .bind("subject", idpM.subject)
                        .add();
            }
            idpBatch.execute();
            // TODO password, need to handle actual hashing

            return getUser(tx, id).orElseThrow(() -> new OpenDcsDataException("Updated User could not be retrieved."));
        }
        catch (JsonProcessingException ex)
        {
            throw new OpenDcsDataException("unable to covert config to json", ex);
        }
    }

    @Override
    public void deleteUser(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.createUpdate("delete from user_roles where user_id = :id").bind("id", id.getValue()).execute();
        handle.createUpdate("delete from user_identity_provider where user_id=:id").bind("id", id.getValue()).execute();
        // password?
        handle.createUpdate("delete from opendcs_user where id = :id").bind("id", id.getValue()).execute();

    }

    @Override
    public List<IdentityProvider> getIdentityProviders(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        if (limit == -1 || offset == -1)
        {
            return handle.createQuery("select id, name, type, updated_at, config::text from identity_provider")
                         .map(PROVIDER_MAPPER).list();
        }
        else
        {
            return handle.createQuery("select id, name, type, updated_at, config::text from identity_provider limit :limit offset :offset")
                         .bind("limit", limit)
                         .bind("offset", offset)
                         .map(PROVIDER_MAPPER).list();
        }
    }

    @Override
    public IdentityProvider addIdentityProvider(DataTransaction tx, IdentityProvider provider)
            throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());
        ObjectMapper om = new ObjectMapper();
        try
        {
            String config = om.writeValueAsString(provider.configToMap());
            return
                handle.createQuery("insert into identity_provider(name, type, updated_at, config) values (:name, :type, now(), :config::jsonb) returning id, name, type, updated_at, config::text")
                .bind("name", provider.getName())
                .bind("type", provider.getType())
                .bind("config", config)
                .map(PROVIDER_MAPPER).one();
        }
        catch (JsonProcessingException ex)
        {
            throw new OpenDcsDataException("unable to covert config to json", ex);
        }
    }

    @Override
    public Optional<IdentityProvider> getIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());

        return
            handle.createQuery("select id, name, type, updated_at, config::text from identity_provider where id = :id")
              .bind("id", id.getValue())
              .map(PROVIDER_MAPPER).findOne();
    }

    @Override
    public IdentityProvider updateIdentityProvider(DataTransaction tx, DbKey id, IdentityProvider provider)
            throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.getJdbi().installPlugin(new Jackson2Plugin());
        ObjectMapper om = new ObjectMapper();
        try
        {
            String config = om.writeValueAsString(provider.configToMap());
            return
                handle.createQuery("update identity_provider set name = :name, type = :type, updated_at = now(), " +
                                   "config = :config::jsonb where id = :id returning id, name, type, updated_at, config::text")
                    .bind("id", id.getValue())
                    .bind("name", provider.getName())
                    .bind("type", provider.getType())
                    .bind("config", config)
                    .map(PROVIDER_MAPPER).one();
        }
        catch (JsonProcessingException ex)
        {
            throw new OpenDcsDataException("unable to covert config to json", ex);
        }
    }

    @Override
    public void deleteIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        handle.createUpdate("delete from identity_provider where id = :id")
              .bind("id", id.getValue())
              .execute();
    }

    @Override
    public List<Role> getRoles(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        if (limit == -1 || offset == -1)
        {
            return handle.createQuery("select id, name, description, updated_at from opendcs_role")
                         .map(ROLE_MAPPER).list();
        }
        else
        {
            return handle.createQuery("select id, name, description, updated_at from opendcs_role limit :limit offset :offset")
                         .bind("limit", limit)
                         .bind("offset", offset)
                         .map(ROLE_MAPPER).list();
        }

    }

    @Override
    public Role addRole(DataTransaction tx, Role role) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        return
            handle.createQuery("insert into opendcs_role(name, description, updated_at) values (:name, :description, now()) returning id, name, description,updated_at")
              .bind("name", role.name)
              .bind("description", role.description)
              .map(ROLE_MAPPER).one();
    }

    @Override
    public Optional<Role> getRole(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
        return handle.createQuery("select id, name, description, updated_at from opendcs_role where id = :id")
              .bind("id", id.getValue()) // todo: create create arg handler
              .map(ROLE_MAPPER).findOne();
    }

    @Override
    public Role updateRole(DataTransaction tx, DbKey id, Role role) throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
        return
            handle.createQuery("update opendcs_role set name =:name, description = :description where id=:id returning id, name, description,updated_at")
              .bind("name", role.name)
              .bind("description", role.description)
              .bind("id", id.getValue())
              .map(ROLE_MAPPER).one();
    }

    @Override
    public void deleteRole(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
        handle.createUpdate("delete from opendcs_role where id = :id")
              .bind("id", id.getValue())
              .execute();
    }


    /**
     * Helper function. Will be able to just call tx.getConnection(Handle.class)
     * in the future
     * @param tx
     * @return
     */
    private Handle getHandle(DataTransaction tx) throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class).get();
        return Jdbi.open(conn);
    }

}
