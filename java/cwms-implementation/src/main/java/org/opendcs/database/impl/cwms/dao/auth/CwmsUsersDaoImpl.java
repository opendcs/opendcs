package org.opendcs.database.impl.cwms.dao.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.cwms.data.CwmsOffice;
import org.opendcs.data.Organization;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.RolesDao;
import org.opendcs.database.dai.UsersDao;
import org.opendcs.database.impl.cwms.jdbi.mapper.CwmsOfficeMapper;
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
import org.stringtemplate.v4.STGroup;

import decodes.sql.DbKey;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = UsersDao.class, path ="dao/CWMS-Oracle")
public class CwmsUsersDaoImpl implements UsersDao
{
    private static final CwmsOfficeMapper officeBuilderMapper = CwmsOfficeMapper.withPrefix("ofc");
    /**
     * At this time we don't care about the reports to office in User roles, so we
     * wrap the builderMapper to immediately build the office instead of introducing
     * reducer logic.
     */
    private static final RowMapper<CwmsOffice> officeMapper = new RowMapper<>()
    {

        @Override
        public CwmsOffice map(ResultSet rs, StatementContext ctx) throws SQLException
        {
            return officeBuilderMapper.map(rs, ctx).build();
        }
    };

    private static final String SELECT = "select";

    @InjectDao
    RolesDao rolesDao;


    private final STGroup queries;

    public CwmsUsersDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(CwmsUsersDaoImpl.class);
    }

    @Override
    public List<User> getUsers(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        var selectTemplate = queries.getInstanceOf(SELECT);
        selectTemplate.add("limit", addLimitOffset(limit, offset))
                      .add("office_columns", officeBuilderMapper.columnsForSelect());

        try (var q = handle.createQuery(selectTemplate.render()))
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
                .registerRowMapper(Organization.class, officeMapper)
                .reduceRows(UserBuilderReducer.USER_BUILDER_REDUCER)
                .map(UserBuilder::build)
                .toList();
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
            id = addUser.bind(GenericColumns.EMAIL.column(), user.email)
                        .bindByType(GenericColumns.PREFERENCES.column(), user.preferences, ConfigColumnMapper.CONFIG_TYPE)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(DbKey.class)
                        .one();
        }

        try (PreparedBatch roleBatch = handle.prepareBatch(
                    "insert into user_roles(office_code, user_id, role_id) values (:office_code, :user_id, :role_id)"))
        {
            for (var entry: user.roles.entrySet())
            {
                var org = entry.getKey();
                var roles = entry.getValue();
                for (var role: roles)
                {
                    var roleId = role.id();
                    if (DbKey.isNull(role.id()))
                    {
                        roleId = rolesDao.getRoleByName(tx, role.name())
                                        .orElseThrow(() -> new OpenDcsDataException("Request to map role '" + role.name() +
                                                                                    "' that doesn't exist."))
                                        .id();
                    }
                    roleBatch.bindByType("office_code", org.getId(), DbKey.class)
                            .bind(UserBuilderMapper.USER_ID, id)
                            .bind(RoleMapper.ROLE_ID, roleId)
                            .add();
                }
            };
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
                        .bind(GenericColumns.SUBJECT.column(), idpM.subject)
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
        var selectTemplate = queries.getInstanceOf(SELECT);
        selectTemplate.add("where", " where id = :id")
                      .add("office_columns", officeBuilderMapper.columnsForSelect());
        

        try (var user = handle.createQuery(selectTemplate.render()))
        {
             return user.bind(GenericColumns.ID.column(), id)
              .registerRowMapper(UserBuilder.class, UserBuilderMapper.withPrefix("u"))
              .registerRowMapper(Role.class, RoleMapper.withPrefix("r"))
              .registerRowMapper(IdentityProviderMapping.class, IdentityProviderMappingMapper.withPrefix("i"))
              .registerRowMapper(Organization.class, officeMapper)
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
            userUpdate.bind(GenericColumns.ID.column(), id)
                        .bind(GenericColumns.PREFERENCES.column(), user.preferences)
                        .bind(GenericColumns.EMAIL.column(), user.email) // wait should we allow changing the email?
                        .execute();
        }
        try (var deleteRoles = handle.createUpdate("delete from user_roles where user_id = :id"))
        {
            deleteRoles.bind(GenericColumns.ID.column(), id).execute();
        }
        try (PreparedBatch roleBatch = handle.prepareBatch(
            "insert into user_roles(office_code, user_id, role_id) values (:office_code, :user_id, :role_id)"))
        {
            for (var entry: user.roles.entrySet())
            {
                var org = entry.getKey();
                var roles = entry.getValue();
                for (var role: roles)
                {
                    roleBatch.bind("office_code", org.getId())
                            .bind(UserBuilderMapper.USER_ID, id)
                            .bind(RoleMapper.ROLE_ID, role.id())
                            .add();
                }
            }
            roleBatch.execute();
        }

        try (var deleteProviders = handle.createUpdate(
            "delete from user_identity_provider where user_id=:id"))
        {
            deleteProviders.bind(GenericColumns.ID.column(), id).execute();
        }

        try (PreparedBatch idpBatch =
                handle.prepareBatch("insert into user_identity_provider (user_id, identity_provider_id, subject) " +
                                                                "values (:user_id, :identity_provider_id, :subject)"))
        {
            for (IdentityProviderMapping idpM: user.identityProviders)
            {
                idpBatch.bind(UserBuilderMapper.USER_ID, id)
                        .bind(IdentityProviderMapper.IDENTITY_PROVIDER_ID, idpM.provider.getId())
                        .bind(GenericColumns.SUBJECT.column(), idpM.subject)
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
            deletePassword.bind(GenericColumns.ID.column(), id).execute();
            deleteRoles.bind(GenericColumns.ID.column(), id).execute();
            deleteIdps.bind(GenericColumns.ID.column(), id).execute();
            deleteUser.bind(GenericColumns.ID.column(), id).execute();
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
