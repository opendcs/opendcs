package org.opendcs.database.impl.opendcs.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.json.Json;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.User;
import org.opendcs.database.model.mappers.IdentityProviderMapper;
import org.opendcs.database.model.mappers.RoleMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.sql.DbKey;

public class UserManagementImpl implements UserManagementDao
{

    private static final RoleMapper ROLE_MAPPER = new RoleMapper();
    private static final IdentityProviderMapper PROVIDER_MAPPER = new IdentityProviderMapper();

    @Override
    public List<User> getUsers(DataTransaction tx, int limit, int offset)
    {
        return new ArrayList<>();
    }

    @Override
    public User addUser(DataTransaction tx, User user) throws OpenDcsDataException
    {
        return null;
    }

    @Override
    public Optional<User> getUser(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return Optional.empty();
    }

    @Override
    public User updateUser(DataTransaction tx, DbKey id, User user) throws OpenDcsDataException
    {
        return null;
    }

    @Override
    public void deleteUser(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {

    }

    @Override
    public List<IdentityProvider> getIdentityProviders(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException
    {
        return new ArrayList<>();
    }

    @Override
    public IdentityProvider addIdentityProvider(DataTransaction tx, IdentityProvider provider)
            throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
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
        Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
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
         Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
        handle.getJdbi().installPlugin(new Jackson2Plugin());
        ObjectMapper om = new ObjectMapper();
        try
        {
            String config = om.writeValueAsString(provider.configToMap());
            return
                handle.createQuery("update identity_provider set name = :name, type = :type, updated_at = now(), " +
                                   "config = :config::jsonb returning id, name, type, updated_at, config::text")
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
        Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
        handle.createUpdate("delete from identity_provider where id = :id")
              .bind("id", id.getValue())
              .execute();
    }

    @Override
    public List<Role> getRoles(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
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
        Connection conn = tx.connection(Connection.class).get();
        Handle handle = Jdbi.open(conn);
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



}
