/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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

import static org.opendcs.utils.sql.SqlQueries.LEFT_OUTER;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListEntryMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing.RoutingSpecMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing.RoutingSpecMappers;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing.RoutingSpecNetworkListMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing.RoutingSpecReducer;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import decodes.db.RoutingSpec;
import decodes.sql.DbKey;

@ServiceProviders({
    @ServiceProvider(service = RoutingSpecDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = RoutingSpecDao.class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = RoutingSpecDao.class, path = "dao/OPENTSDB"),
    @ServiceProvider(service = RoutingSpecDao.class)
})

public class RoutingSpecDaoImpl implements RoutingSpecDao
{
    private static final String SELECT = "select";

    private final STGroup queries;
    private final RoutingSpecMappers allData;
    private final RoutingSpecMappers specOnlyData;
    

    public RoutingSpecDaoImpl()
    {
        STGroup.verbose = true;
        queries = StringTemplateSqlLocator.findStringTemplateGroup(RoutingSpecDaoImpl.class);
        
        allData = new RoutingSpecMappers(
            RoutingSpecMapper.withPrefix("rs"),
            RoutingSpecNetworkListMapper.withPrefix("rnl"),
            PropertiesMapper.withPrefix("rp", true),
            NetworkListMapper.withPrefix("nl"),
            NetworkListEntryMapper.withPrefix("nle")
        );

        specOnlyData = new RoutingSpecMappers(allData.specMapper(), null, null, null, null);
    }

    @Override
    public Optional<RoutingSpec> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }

        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var selectTemplate = queries.getInstanceOf(SELECT);
        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }
        selectTemplate.add("where", " where id = :id ");
        var selectSql = setDefines(selectTemplate, allData);
        try (var select = handle.createQuery(selectSql))
        {
            registerMappers(select, allData);
            return select.bind(RoutingSpecMapper.Columns.ID.column(), id)
                         .reduceRows(new RoutingSpecReducer(allData))
                         .map(rs -> rs)
                         .findFirst();
        }
    }

    @Override
    public Optional<RoutingSpec> getByName(DataTransaction tx, String specName) throws OpenDcsDataException
    {
        if (specName == null || specName.isBlank())
        {
            return Optional.empty();
        }

        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var selectTemplate = queries.getInstanceOf(SELECT);
        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }
        selectTemplate.add("where", " where name = :name ");
        var selectSql = setDefines(selectTemplate, allData);
        try (var select = handle.createQuery(selectSql))
        {
            registerMappers(select, allData);
            return select.bind(RoutingSpecMapper.Columns.NAME.column(), specName)
                         .reduceRows(new RoutingSpecReducer(allData))
                         .map(rs ->
                         {
                            rs.outputTimeZone = TimeZone.getTimeZone(rs.outputTimeZoneAbbr);
                            rs.setProperty("RoutingSpecName", rs.getName());
                            rs.forceSetPrepared();
                            return rs;
                         })
                         .findFirst();
        }
    }

    private static Query registerMappers(Query select, RoutingSpecMappers mappers)
    {
        select.registerRowMapper(mappers.specMapper());

        if (mappers.specListMapper() != null)
        {
            select.registerRowMapper(mappers.specListMapper());
        }

        if (mappers.specPropertiesMapper() != null)
        {
            select.registerRowMapper(RoutingSpecReducer.ROUTING_SPEC_PROPERTIES, mappers.specPropertiesMapper());
        }
        if (mappers.listMapper() != null)
        {
            select.registerRowMapper(mappers.listMapper());
            select.registerRowMapper(mappers.listEntryMapper());
        }
        return select;
    }

    private static String setDefines(ST select, RoutingSpecMappers mappers)
    {
        select.add("spec_columns", mappers.specMapper().columnsForSelect());

        if (mappers.specListMapper() != null)
        {
            select.add("spec_list_columns", mappers.specListMapper().columnsForSelect());
            select.add("spec_list_join",
                       mappers.specListMapper()
                              .joinStatement(LEFT_OUTER, RoutingSpecNetworkListMapper.Columns.ROUTING_SPEC_ID, "rs", "id")
            );
        }

        if (mappers.specPropertiesMapper() != null)
        {
            select.add("prop_columns", mappers.specPropertiesMapper().columnsForSelect());
            select.add("prop_join", "left outer join routingspecproperty rp on rp.routingspecid = rs.id");
        }


        if (mappers.listMapper() != null)
        {
            select.add("list_columns", mappers.listMapper().columnsForSelect())
                  .add("list_entry_columns", mappers.listEntryMapper().columnsForSelect());
            
            select.add("list_join",
                       mappers.listMapper()
                              .joinStatement(LEFT_OUTER, NetworkListMapper.Columns.ID,
                                 "rs", "id")
                        );
            select.add("list_entry_join",
                        mappers.listEntryMapper().joinStatement(LEFT_OUTER,
                            NetworkListEntryMapper.Columns.NETWORKLIST_ID, "nl", "id")
            );
        }

        return select.render();
    }


    @Override
    public RoutingSpec save(DataTransaction arg0, RoutingSpec arg1) throws OpenDcsDataException
    {
        return null;
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        /* empty */
    }

    @Override
    public List<RoutingSpec> getAll(DataTransaction tx, int limit, int offset, boolean includeAll, String forSchedule)
            throws OpenDcsDataException
    {
        return List.of();
    }    
}
