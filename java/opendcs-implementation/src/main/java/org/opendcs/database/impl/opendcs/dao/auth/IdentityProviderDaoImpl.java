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
package org.opendcs.database.impl.opendcs.dao.auth;

import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.IdentityProviderDao;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigColumnMapper;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.mappers.IdentityProviderMapper;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.opendcs.utils.sql.GenericColumns;

import decodes.sql.DbKey;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

// This uses Postgres specific features and is not compatible with Oracle
@ServiceProviders({
    @ServiceProvider(service = IdentityProviderDao.class, path = "dao/OpenDCS-Postgres"),    
    // deprecated and also implies supported by the Oracle impl which is false.
    // Unfortunately correct behavior requires eliminating the use of the editDatabaseCode so the names are used.
    // This will be done in a follow up PR.
    @ServiceProvider(service = IdentityProviderDao.class, path = "dao/OPENTSDB")
})
public class IdentityProviderDaoImpl implements IdentityProviderDao
{
    private static final IdentityProviderMapper PROVIDER_MAPPER = IdentityProviderMapper.withPrefix(null);



    

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
            return addIdp.bind(GenericColumns.NAME.column(), provider.getName())
                            .bind(IdentityProviderMapper.Columns.TYPE.column(), provider.getType())
                            .bind(IdentityProviderMapper.Columns.CONFIG.column(), provider.configToMap())
                            .map(PROVIDER_MAPPER).one();
        }
    }

    @Override
    public Optional<IdentityProvider> getIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Query getIdp = handle.createQuery("select id, name, type, updated_at, config::text from identity_provider where id = :id"))
        {
            return getIdp.bind(GenericColumns.ID.column(), id).map(PROVIDER_MAPPER).findOne();
        }
    }

    @Override
    public Optional<IdentityProvider> getIdentityProvider(DataTransaction tx, String name) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Query getIdp = handle.createQuery("select id, name, type, updated_at, config::text from identity_provider where name = :name"))
        {
            return getIdp.bind(GenericColumns.NAME.column(), name).map(PROVIDER_MAPPER).findOne();
        }
    }

    @Override
    public IdentityProvider updateIdentityProvider(DataTransaction tx, DbKey id, IdentityProvider provider)
            throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Query updateIdp =
                handle.createQuery("update identity_provider set name = :name, type = :type, " +
                                    "config = :config::jsonb where id = :id returning id, name, type, updated_at, config::text"))
        {
            return updateIdp.bind(GenericColumns.ID.column(), id)
                            .bind(GenericColumns.NAME.column(), provider.getName())
                            .bind(IdentityProviderMapper.Columns.TYPE.column(), provider.getType())
                            .bind(GenericColumns.CONFIG.column(), provider.configToMap())
                            .map(PROVIDER_MAPPER).one();
        }
    }

    @Override
    public void deleteIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        Handle handle = getHandle(tx);
        try (Update deleteIdp = handle.createUpdate("delete from identity_provider where id = :id"))
        {
            deleteIdp.bind(GenericColumns.ID.column(), id).execute();
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
        h.getJdbi()
         .installPlugin(new Jackson2Plugin());
        return h.registerArgument(new ConfigArgumentFactory())
                .registerColumnMapper(new ConfigColumnMapper());
    }

    @Override
    public List<IdentityProvider> getIdentityProvidersForSubject(DataTransaction tx, String subject)
            throws OpenDcsDataException
    {
        var handle = getHandle(tx);
        final String idpsSql = """
                    select idp.id idp_id, idp.name idp_name, idp.type idp_type, idp.updated_at idp_updated_at, idp.config::text idp_config
                      from user_identity_provider mapping
                      join identity_provider idp on idp.id = mapping.identity_provider_id
                      where mapping.subject = :subject
                """;
        try (var getIdps = handle.createQuery(idpsSql))
        {
            return getIdps.bind(GenericColumns.SUBJECT.column(), subject)
                          .map(IdentityProviderMapper.withPrefix("idp"))
                          .collectIntoList();
        }
    }
}
