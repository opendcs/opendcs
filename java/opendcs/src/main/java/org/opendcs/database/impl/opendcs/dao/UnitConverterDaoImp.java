package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UnitConversionDao;
import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.database.model.mappers.unitconverter.UnitConverterMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.EngineeringUnit;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;

public class UnitConverterDaoImp implements UnitConversionDao
{

    @Override
    public Optional<UnitConverterDb> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String querySql = """
                    select id, fromunitsabbr, tounitsabbr, algorithms, a,b,c,d,e,f from unitconverter where id =:id
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
    public UnitConverterDb save(DataTransaction tx, UnitConverterDb unitConverter) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException {
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
