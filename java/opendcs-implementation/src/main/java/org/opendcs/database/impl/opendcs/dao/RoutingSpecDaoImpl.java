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
import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.OFFSET_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListEntryMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing.RoutingSpecMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing.RoutingSpecMappers;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing.RoutingSpecNetworkListMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing.RoutingSpecReducer;
import org.opendcs.database.model.mappers.datasource.DataSourceAccumulator;
import org.opendcs.database.model.mappers.datasource.DataSourceMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import decodes.db.DataSource;
import decodes.db.DatabaseException;
import decodes.db.RoutingSpec;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

@ServiceProviders({
    @ServiceProvider(service = RoutingSpecDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = RoutingSpecDao.class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = RoutingSpecDao.class, path = "dao/OPENTSDB"),
    @ServiceProvider(service = RoutingSpecDao.class)
})
@SuppressWarnings("java:S2143") // to be fixed at a later date
public class RoutingSpecDaoImpl implements RoutingSpecDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final String SELECT = "select";
    private static final String MERGE = "mergeSpec";
    private static final String DELETE_SPEC = "deleteSpec";
    private static final String DELETE_SPEC_LISTS = "deleteSpecLists";
    private static final String DELETE_SPEC_PROPS = "deleteSpecProps";
    private static final String INSERT_SPEC_LIST = "insertSpecList";
    private static final String INSERT_SPEC_PROPS = "insertSpecProps";

    private final STGroup queries;
    private final RoutingSpecMappers allData;
    private final RoutingSpecMappers specOnlyData;


    public RoutingSpecDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(RoutingSpecDaoImpl.class);

        allData = new RoutingSpecMappers(
            RoutingSpecMapper.withPrefix("rs"),
            RoutingSpecNetworkListMapper.withPrefix("rnl"),
            PropertiesMapper.withPrefix("rp", true),
            NetworkListMapper.withPrefix("nl"),
            NetworkListEntryMapper.withPrefix("nle"),
            new DataSourceAccumulator(DataSourceMapper.withPrefix("ds"),
                                      DataSourceMapper.withPrefix("dsm"))
        );

        specOnlyData = new RoutingSpecMappers(
            allData.specMapper(), null, null,
            null, null, null);
    }

    @Override
    public Optional<RoutingSpec> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }
        return get(tx, " where id = :id", "id", id);
    }

    @Override
    public Optional<RoutingSpec> getByName(DataTransaction tx, String specName) throws OpenDcsDataException
    {
        if (specName == null || specName.isBlank())
        {
            return Optional.empty();
        }
        return get(tx, " where name = :name ", "name", specName);
    }

    private Optional<RoutingSpec> get(DataTransaction tx, String whereClause, String whereKey, Object whereBind) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var selectTemplate = queries.getInstanceOf(SELECT);
        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }
        selectTemplate.add("where", whereClause);
        var selectSql = setDefines(selectTemplate, allData);
        try (var select = handle.createQuery(selectSql))
        {
            registerMappers(select, allData);
            return select.bind(whereKey, whereBind)
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
        if (mappers.dataSourceAccumulator() != null)
        {
            select.registerRowMapper(DataSource.class, mappers.dataSourceAccumulator().primaryMapper);
            select.registerRowMapper(DataSourceAccumulator.MEMBER_SOURCE, mappers.dataSourceAccumulator().memberMapper);
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
                              .joinStatement(LEFT_OUTER, NetworkListMapper.Columns.NAME,
                                 "rnl", "networklistname")
                        );
            select.add("list_entry_join",
                        mappers.listEntryMapper().joinStatement(LEFT_OUTER,
                            NetworkListEntryMapper.Columns.NETWORKLIST_ID, "nl", "id")
            );
        }

        if (mappers.dataSourceAccumulator() != null)
        {
            var sourceMapper = mappers.dataSourceAccumulator().primaryMapper;
            var memberMapper = mappers.dataSourceAccumulator().memberMapper;
            select.add("datasource_columns",
                       sourceMapper.columnsForSelect(DataSourceMapper.Columns.SEQUENCE_NUMBER))
                  .add("datasource_member_columns",
                       memberMapper.columnsForSelect(DataSourceMapper.Columns.SEQUENCE_NUMBER));

            select.add("datasource_join",
                sourceMapper.joinStatement(LEFT_OUTER, DataSourceMapper.Columns.ID, "rs", "datasourceid")
            );

            select.add("datasource_member_join", """
                    left outer join datasourcegroupmember dsgm on ds.id = dsgm.groupid
                    left outer join datasource dsm on dsm.id = dsgm.memberid
                    """);
        }

        return select.render();
    }


    @Override
    public RoutingSpec save(DataTransaction tx, RoutingSpec spec) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        var mergeTemplate = queries.getInstanceOf(MERGE)
                                   .add("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
        var insertPropsTemplate = queries.getInstanceOf(INSERT_SPEC_PROPS);
        var insertSpecListTemplate = queries.getInstanceOf(INSERT_SPEC_LIST);
        try (var merge = handle.createUpdate(mergeTemplate.render());
             var insertProps = handle.prepareBatch(insertPropsTemplate.render());
             var insertSpecList = handle.prepareBatch(insertSpecListTemplate.render()))
        {
            DbKey id = spec.getId();
            var existing = getByName(tx, spec.getName());
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getId();
                log.trace("""
                    Using ID from existing Routing, id={}, that was found. Provided ID was {}.
                    """,
                    id, spec.getId());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("routingspec", handle.getConnection());
            merge.bind(RoutingSpecMapper.Columns.ID.column(), bindKey)
                 .bind(RoutingSpecMapper.Columns.NAME.column(), spec.getName())
                 .bind(RoutingSpecMapper.Columns.DATA_SOURCE_ID.column(), spec.dataSource.getId())
                 .bind(RoutingSpecMapper.Columns.ENABLE_EQUATIONS.column(), spec.enableEquations)
                 .bind(RoutingSpecMapper.Columns.USE_PERFORMANCE_MEASUREMENTS.column(), spec.usePerformanceMeasurements)
                 .bind(RoutingSpecMapper.Columns.OUTPUT_FORMAT.column(), spec.outputFormat)
                 .bind(RoutingSpecMapper.Columns.OUTPUT_TIME_ZONE.column(), spec.outputTimeZoneAbbr)
                 .bind(RoutingSpecMapper.Columns.PRESENTATION_GROUP_NAME.column(), spec.presentationGroupName)
                 .bind(RoutingSpecMapper.Columns.SINCE_TIME.column(), spec.sinceTime)
                 .bind(RoutingSpecMapper.Columns.UNTIL_TIME.column(), spec.untilTime)
                 .bind(RoutingSpecMapper.Columns.CONSUMER_TYPE.column(), spec.consumerType)
                 .bind(RoutingSpecMapper.Columns.CONSUMER_ARGS.column(), spec.consumerArg)
                 .bindByType(RoutingSpecMapper.Columns.LAST_MODIFY_TIME.column(),
                       ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli(), Date.class)
                 .bind(RoutingSpecMapper.Columns.IS_PRODUCTION.column(), spec.isProduction)
                 .execute();

            deleteProps(handle, bindKey);
            deleteSpecLists(handle, id);

            if (!spec.networkLists.isEmpty())
            {
                for (var nl: spec.networkLists)
                {
                    insertSpecList.bind(RoutingSpecNetworkListMapper.Columns.ROUTING_SPEC_ID.column(), bindKey)
                                  .bind(RoutingSpecNetworkListMapper.Columns.NETWORK_LIST_NAME.column(), nl.name)
                                  .add();
                }
                insertSpecList.execute();
            }

            final var props = spec.getProperties();
            if (!props.isEmpty())
            {
                props.forEach((k,v) ->
                    insertProps.bind("routingspecid", bindKey)
                               .bind("prop_name", k)
                               .bind("prop_value", v)
                               .add()
                );
                insertProps.execute();
            }


            return getById(tx, bindKey)
                    .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve RoutingSpec we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key to save routing spec.", ex);
        }
    }

    private void deleteProps(Handle handle, DbKey specId)
    {
        final var deletePropTemplate = queries.getInstanceOf(DELETE_SPEC_PROPS);
        try (var deleteProps = handle.createUpdate(deletePropTemplate.render()))
        {
            deleteProps.bind(RoutingSpecMapper.Columns.ID.column(), specId).execute();
        }
    }

    private void deleteSpecLists(Handle handle, DbKey specId)
    {
        final var deleteSpecListTemplate = queries.getInstanceOf(DELETE_SPEC_LISTS);
        try (var deleteSpecList = handle.createUpdate(deleteSpecListTemplate.render()))
        {
            deleteSpecList.bind(RoutingSpecMapper.Columns.ID.column(), specId).execute();
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var deleteSpecTemplate = queries.getInstanceOf(DELETE_SPEC);
        deleteProps(handle, id);
        deleteSpecLists(handle, id);
        try (var deleteSpec = handle.createUpdate(deleteSpecTemplate.render()))
        {
            deleteSpec.bind(RoutingSpecMapper.Columns.ID.column(), id).execute();
        }
    }

    @Override
    public List<RoutingSpec> getAll(DataTransaction tx, int limit, int offset, boolean includeAll, String forSchedule)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var selectTemplate = queries.getInstanceOf(SELECT);
        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }

        var mappers = includeAll ? allData : specOnlyData;
        selectTemplate.add(LIMIT_CLAUSE, addLimitOffset(limit, offset));
        if (forSchedule != null)
        {
            selectTemplate.add("schedule",
                        "id in (select routingspec_id from schedule_entry where upper(name) = upper(:scheduleName))");
        }
        var selectSql = setDefines(selectTemplate, mappers);
        try (var select = handle.createQuery(selectSql))
        {
            registerMappers(select, mappers);

            if (limit > -1)
            {
                select.bind(LIMIT_CLAUSE, limit);
            }

            if (offset > -1)
            {
                select.bind(OFFSET_CLAUSE, offset);
            }

            if (forSchedule != null)
            {
                select.bind("scheduleName", forSchedule);
            }

            return select.reduceRows(new RoutingSpecReducer(mappers))
                         .map(rs ->
                         {
                            rs.outputTimeZone = TimeZone.getTimeZone(rs.outputTimeZoneAbbr);
                            rs.setProperty("RoutingSpecName", rs.getName());
                            rs.forceSetPrepared();
                            return rs;
                         })
                         .toList();
        }
    }
}
