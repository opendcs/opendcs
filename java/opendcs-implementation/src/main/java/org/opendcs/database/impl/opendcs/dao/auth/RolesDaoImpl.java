package org.opendcs.database.impl.opendcs.dao.auth;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.RolesDao;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigColumnMapper;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.mappers.RoleMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlKeywords;

import decodes.sql.DbKey;


import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProviders({
    @ServiceProvider(service = RolesDao.class, path = "dao/OpenDCS-Postgres"),
    // deprecated and also implies supported by the Oracle impl which is false.
    // Unfortunately correct behavior requires eliminating the use of the editDatabaseCode so the names are used.
    // This will be done in a follow up PR.
    @ServiceProvider(service = RolesDao.class, path = "dao/OPENTSDB"),
})
public class RolesDaoImpl implements RolesDao
{
    private static final RoleMapper ROLE_MAPPER = RoleMapper.withPrefix(null);

    @Override
    public Optional<Role> getRoleByName(DataTransaction tx, String role) throws OpenDcsDataException
    {
        var handle = getHandle(tx);
        try (var select = handle.createQuery("select id, name, description, updated_at from opendcs_role where name = :name"))
        {
            return select.bind(GenericColumns.NAME, role)
                         .map(ROLE_MAPPER)
                         .findOne();
        }
    }

    @Override
    public List<Role> getRoles(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (var select = handle.createQuery("""
                        select id, name, description, updated_at from opendcs_role order by name COLLATE "C" ASC
                    """ +
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
        try (var addRole = 
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
        try (var select = handle.createQuery("select id, name, description, updated_at from opendcs_role where id = :id"))
        {
            return select.bind(GenericColumns.ID, id)
                         .map(ROLE_MAPPER).findOne();
        }
    }

    @Override
    public Role updateRole(DataTransaction tx, DbKey id, Role role) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (var update = handle.createQuery("update opendcs_role set name =:name, description = :description where id=:id returning id, name, description,updated_at"))
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
        try (var delete = handle.createUpdate("delete from opendcs_role where id = :id"))
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
        var h = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Connection from transaction."));
        h.getJdbi()
         .installPlugin(new Jackson2Plugin());
        return h.registerArgument(new ConfigArgumentFactory())
                .registerColumnMapper(new ConfigColumnMapper());
    }
}
