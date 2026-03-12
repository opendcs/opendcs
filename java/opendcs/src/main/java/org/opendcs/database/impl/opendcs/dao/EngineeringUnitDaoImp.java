package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.EngineeringUnitDao;
import org.opendcs.database.model.mappers.engineeringunit.EngineeringUnitMapper;
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public void delete(DataTransaction tx, String unitAbbreviation) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public Optional<EngineeringUnit> lookup(DataTransaction tx, String unit) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String querySql = """
                    select unitabbr, name, family, measures 
                      from engineeringunit 
                     where unitabbr = :unit 
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
                      <limit>
                """;
            
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
                        .registerRowMapper(EngineeringUnit.class, EngineeringUnitMapper.withPrefix(""))
                        .mapTo(EngineeringUnit.class)
                        .list();
        }
    }
    
}
