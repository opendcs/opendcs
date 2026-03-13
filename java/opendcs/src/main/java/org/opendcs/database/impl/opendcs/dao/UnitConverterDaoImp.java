package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UnitConverterDao;
import org.opendcs.database.model.mappers.unitconverter.UnitConverterMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.EngineeringUnit;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;

@ServiceProvider(service = UnitConverterDao.class)
public class UnitConverterDaoImp implements UnitConverterDao
{

    @Override
    public Optional<UnitConverterDb> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String querySql = """
                    select uc.id, uc.fromunitsabbr, uc.tounitsabbr, uc.algorithm, uc.a,uc.b,uc.c,uc.d,uc.e,uc.f,
                        from_eu.unitabbr from_unitabbr, from_eu.name from_name, from_eu.family from_family, from_eu.measures from_measures,
                        to_eu.unitabbr to_unitabbr, to_eu.name to_name, to_eu.family to_family, to_eu.measures to_measures
                      from unitconverter uc 
                       join engineeringunit from_eu on from_eu.unitabbr = uc.fromunitsabbr
                       join engineeringunit to_eu on to_eu.unitabbr = uc.tounitsabbr
                      where id =:id
                """;
        try (var query = handle.createQuery(querySql))
        {
            return query.bind(GenericColumns.ID, id)
                        .registerRowMapper(UnitConverterDb.class, UnitConverterMapper.withPrefix(""))
                        .mapTo(UnitConverterDb.class)
                        .findOne();
        }
    }

    @Override
    public UnitConverterDb save(DataTransaction tx, UnitConverterDb unitConverter) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public Optional<UnitConverterDb> lookup(DataTransaction tx, String fromAbbr, String toAbbr)
            throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'lookup'");
    }

    @Override
    public Optional<UnitConverterDb> lookup(DataTransaction tx, EngineeringUnit from, EngineeringUnit to)
            throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'lookup'");
    }

    @Override
    public List<UnitConverterDb> getUnitConverterDbs(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUnitConverterDbs'");
    }
    
}
