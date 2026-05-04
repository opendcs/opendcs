package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.EngineeringUnitDao;
import org.opendcs.database.model.mappers.engineeringunit.EngineeringUnitMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.EngineeringUnit;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = EngineeringUnitDao.class)
public class EngineeringUnitDaoImp implements EngineeringUnitDao
{
    @Override
    public EngineeringUnit save(DataTransaction tx, EngineeringUnit unit) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        
        final String insertSql = """
                merge into engineeringunit eu
                using (select :unitabbr unitabbr, :name name, :family family, :measures measures <dual>) input
                on (upper(eu.unitabbr) = upper(input.unitabbr))
                when matched then
                    update set name = input.name, family = input.family, measures = input.measures
                when not matched then
                    insert(unitabbr, name, family, measures)
                    values(input.unitabbr, input.name, input.family, input.measures)
                """;
        try (var query = handle.createUpdate(insertSql)
                               .define("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : ""))
        {
            query.bind("unitabbr", unit.getAbbr())
                 .bind(GenericColumns.NAME, unit.getName())
                 .bind("family", unit.getFamily())
                 .bind("measures", unit.getMeasures())
                 .execute();
            return getByName(tx, unit.abbr).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Engineering Unit we just saved."));
        }
    }

    @Override
    public void delete(DataTransaction tx, String unitAbbreviation) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final var deleteDataTypeSql = "delete from engineeringunit where unitabbr = :unitabbr";
        try (var deleteDataType = handle.createUpdate(deleteDataTypeSql))
        {
            deleteDataType.bind("unitabbr", unitAbbreviation).execute();
        }
    }

    @Override
    public Optional<EngineeringUnit> getByName(DataTransaction tx, String unit) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String querySql = """
                    select unitabbr, name, family, measures 
                      from engineeringunit 
                     where upper(unitabbr) = upper(:unit)
                        or name = :unit
                """;
        try (var query = handle.createQuery(querySql))
        {
            return query.bind("unit", unit)
                        .registerRowMapper(EngineeringUnit.class, EngineeringUnitMapper.withPrefix(""))
                        .mapTo(EngineeringUnit.class)
                        .findOne();
        }
    }

    @Override
    public List<EngineeringUnit> getEngineeringUnits(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String querySql = """
                    select unitabbr, name, family, measures 
                      from engineeringunit
                      order by unitabbr <collate> asc
                      <limit>

                """;
        var dbType = tx.getContext().getDatabase();
        try (var query = handle.createQuery(querySql))
        {
            if (limit != -1)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }

            if (offset != -1)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }
            return query.define("limit", addLimitOffset(limit, offset))
                        .define("collate", dbType == DatabaseEngine.POSTGRES ? "COLLATE \"C\"" : "COLLATE BINARY")
                        .registerRowMapper(EngineeringUnit.class, EngineeringUnitMapper.withPrefix(""))
                        .mapTo(EngineeringUnit.class)
                        .list();
        }
    }
    
}
