package org.opendcs.database.impl.cwms.dao.auth;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.RolesDao;
import org.opendcs.database.dai.UsersDao;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigColumnMapper;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.User;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.database.model.mappers.IdentityProviderMapper;
import org.opendcs.database.model.mappers.RoleMapper;
import org.opendcs.database.model.mappers.user.IdentityProviderMappingMapper;
import org.opendcs.database.model.mappers.user.UserBuilderMapper;
import org.opendcs.database.model.mappers.user.UserBuilderReducer;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;

import decodes.sql.DbKey;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = UsersDao.class, path ="dao/CWMS-Oracle")
public class CwmsUsersDaoImpl implements UsersDao
{
    @InjectDao
    RolesDao rolesDao;
    
    @Override
    public List<User> getUsers(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        final String userSelect = """
            with user_cte (id, preferences, email, created_at, updated_at) as
                (select id, preferences, email, created_at, updated_at
                 from opendcs_user order by email asc
            """ +
            addLimitOffset(limit, offset) +
            """
                )
            select u.id u_id, u.preferences u_preferences, u.email u_email,
                u.created_at u_created_at, u.updated_at u_updated_at,
                r.id r_id, r.name r_name, r.description r_description, r.updated_at r_updated_at,
                uip.identity_provider_id i_id, uip.subject i_subject,
                idp.name i_name, idp.type i_type, idp.updated_at i_updated_at, idp.config i_config
            from user_cte u
            left join user_roles ur on ur.user_id = u.id
            left join opendcs_role r on r.id = ur.role_id
            left join user_identity_provider uip on uip.user_id = u.id
            left join identity_provider idp on idp.id = uip.identity_provider_id
            order by u.email asc
            """;

        try (var q = handle.createQuery(userSelect))
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

        DbKey id = DbKey.NullKey;

        try (var addUser = handle.createUpdate(
                    """
                        insert into opendcs_user(email, preferences)
                        values (:email, :preferences)               
                    """
            ))
        {
            id = addUser.bind(GenericColumns.EMAIL, user.email)
                        .bindByType(GenericColumns.PREFERENCES, user.preferences, ConfigColumnMapper.CONFIG_TYPE)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(DbKey.class)
                        .one();
        }

        try (PreparedBatch roleBatch = handle.prepareBatch(
                    "insert into user_roles(user_id, role_id) values (:user_id, :role_id)"))
        {
            for (Role role: user.roles)
            {
                var roleId = role.id;
                if (DbKey.isNull(role.id))
                {
                    roleId = rolesDao.getRoleByName(tx, role.name)
                                     .orElseThrow(() -> new OpenDcsDataException("Request to map role '" + role.name +
                                                                                 "' that doesn't exist."))
                                     .id;
                }
                roleBatch.bind(UserBuilderMapper.USER_ID, id)
                        .bind(RoleMapper.ROLE_ID, roleId)
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

        try (var user = handle.createQuery(
            """
              select u.id u_id, u.preferences u_preferences, u.email u_email,
                   u.created_at u_created_at, u.updated_at u_updated_at,
                   r.id r_id, r.name r_name, r.description r_description, r.updated_at r_updated_at,
                   uip.identity_provider_id i_id, uip.subject i_subject,
                   idp.name i_name, idp.type i_type, idp.updated_at i_updated_at, idp.config i_config
              from opendcs_user u
              left join user_roles ur on ur.user_id = u.id
              left join opendcs_role r on r.id = ur.role_id
              left join user_identity_provider uip on uip.user_id = u.id
              left join identity_provider idp on idp.id = uip.identity_provider_id
              where u.id = :id
            """
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
        
        try (var userUpdate =handle.createUpdate(
            "update opendcs_user set email = :email, preferences = :preferences " +
            "where id = :id"))
        {
            userUpdate.bind(GenericColumns.ID, id)
                        .bind(GenericColumns.PREFERENCES, user.preferences)
                        .bind(GenericColumns.EMAIL, user.email) // wait should we allow changing the email?
                        .execute();
        }
        try (var deleteRoles = handle.createUpdate("delete from user_roles where user_id = :id"))
        {
            deleteRoles.bind(GenericColumns.ID, id).execute();
        }
        try (PreparedBatch roleBatch = handle.prepareBatch(
            "insert into user_roles(user_id, role_id) values (:user_id, :role_id)"))
        {
            for (var role: user.roles)
            {
                roleBatch.bind(UserBuilderMapper.USER_ID, id)
                        .bind(RoleMapper.ROLE_ID, role.id)
                        .add();
            }
            roleBatch.execute();
        }

        try (var deleteProviders = handle.createUpdate(
            "delete from user_identity_provider where user_id=:id"))
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
        try (var deleteRoles = handle.createUpdate("delete from user_roles where user_id = :id");
             var deleteIdps = handle.createUpdate("delete from user_identity_provider where user_id = :id");
             var deletePassword = handle.createUpdate("delete from opendcs_user_password where user_id = :id");
             var deleteUser = handle.createUpdate("delete from opendcs_user where id = :id"))
        {
            deletePassword.bind(GenericColumns.ID, id).execute();
            deleteRoles.bind(GenericColumns.ID, id).execute();
            deleteIdps.bind(GenericColumns.ID, id).execute();
            deleteUser.bind(GenericColumns.ID, id).execute();
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
        var h = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Connection from transaction."));
        h.getJdbi()
         .installPlugin(new Jackson2Plugin());
        return h.registerArgument(new ConfigArgumentFactory())
                .registerColumnMapper(new ConfigColumnMapper());
    }
}
