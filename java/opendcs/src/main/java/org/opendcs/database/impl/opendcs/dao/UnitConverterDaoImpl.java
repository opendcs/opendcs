package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.EngineeringUnitDao;
import org.opendcs.database.dai.UnitConverterDao;
import org.opendcs.database.model.mappers.unitconverter.UnitConverterMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = UnitConverterDao.class)
public class UnitConverterDaoImpl implements UnitConverterDao
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
        double[] coefficients = new double[]{Constants.undefinedDouble, Constants.undefinedDouble, Constants.undefinedDouble,
                                       Constants.undefinedDouble, Constants.undefinedDouble, Constants.undefinedDouble};
        if (unitConverter.coefficients != null)
        {
            for (int i = 0; i < unitConverter.coefficients.length; i++)
            {
                coefficients[i] = unitConverter.coefficients[i]; // NOSONAR
            }
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));

        final String insertSql = """
                merge into unitconverter uc
                using (select :id id, :fromunitsabbr fromunitsabbr, :tounitsabbr tounitsabbr, :algorithm algorithm,
                              :a a, :b b, :c c, :d d, :e e, :f f <dual>) input
                on (uc.id = input.id)
                when matched then
                    update set fromunitsabbr = input.fromunitsabbr, tounitsabbr = input.tounitsabbr, algorithm = input.algorithm,
                                a = input.a, b = input.b, c = input.c, d = input.d, e = input.e, f = input.f
                when not matched then
                    insert(id, fromunitsabbr, tounitsabbr, algorithm, a, b, c, d, e, f)
                    values(input.id, input.fromunitsabbr, input.tounitsabbr, input.algorithm, input.a, input.b, input.c, input.d, input.e, input.f)
                """;
        try (var query = handle.createUpdate(insertSql)
                               .define("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : ""))
        {
            final DbKey id = unitConverter.idIsSet() ? unitConverter.getId() : keyGen.getKey("unitconverter", handle.getConnection());
            query.bind(GenericColumns.ID, id)
                 .bind("fromunitsabbr", unitConverter.fromAbbr)
                 .bind("tounitsabbr", unitConverter.toAbbr)
                 .bind("algorithm", unitConverter.algorithm)
                 .bind("a", coefficients[0] != Constants.undefinedDouble ? coefficients[0] : null)
                 .bind("b", coefficients[1] != Constants.undefinedDouble ? coefficients[1] : null)
                 .bind("c", coefficients[2] != Constants.undefinedDouble ? coefficients[2] : null)
                 .bind("d", coefficients[3] != Constants.undefinedDouble ? coefficients[3] : null)
                 .bind("e", coefficients[4] != Constants.undefinedDouble ? coefficients[4] : null)
                 .bind("f", coefficients[5] != Constants.undefinedDouble ? coefficients[5] : null)
                 .execute();
            return getById(tx, id).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Unit Converter we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key for new unit converter", ex);
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final var deleteDataTypeSql = "delete from unitconverter where id = :id";
        try (var deleteDataType = handle.createUpdate(deleteDataTypeSql))
        {
            deleteDataType.bind(GenericColumns.ID, id).execute();
        }
    }

    @Override
    public Optional<UnitConverterDb> lookup(DataTransaction tx, String fromAbbr, String toAbbr)
            throws OpenDcsDataException
    {
        var euDao = tx.getDao(EngineeringUnitDao.class).orElseThrow();
        var from = euDao.lookup(tx, fromAbbr).orElseThrow();
        var to = euDao.lookup(tx, toAbbr).orElseThrow(0);
        return lookup(tx, from, to);
    }

    @Override
    public Optional<UnitConverterDb> lookup(DataTransaction tx, EngineeringUnit from, EngineeringUnit to)
            throws OpenDcsDataException
    {
        Optional<UnitConverterDb> ret = Optional.empty();
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String querySql = """
                    select uc.id, uc.fromunitsabbr, uc.tounitsabbr, uc.algorithm, uc.a,uc.b,uc.c,uc.d,uc.e,uc.f,
                        from_eu.unitabbr from_unitabbr, from_eu.name from_name, from_eu.family from_family, from_eu.measures from_measures,
                        to_eu.unitabbr to_unitabbr, to_eu.name to_name, to_eu.family to_family, to_eu.measures to_measures
                      from unitconverter uc 
                       left join engineeringunit from_eu on from_eu.unitabbr = uc.fromunitsabbr
                       left join engineeringunit to_eu on to_eu.unitabbr = uc.tounitsabbr
                      where uc.fromunitsabbr = :from
                        and uc.tounitsabbr = :to
                """;
        try (var query = handle.createQuery(querySql))
        {
            ret = query.bind("from", from.abbr)
                        .bind("to", to.abbr)
                        .registerRowMapper(UnitConverterDb.class, UnitConverterMapper.withPrefix(""))
                        .mapTo(UnitConverterDb.class)
                        .findFirst(); // there shouldn't be extras, but just in case, don't fail about it.
        }

        if (ret.isEmpty())
        {
            ret = buildComposite(tx, from, to);
        }

        return ret;
        
    }

    @Override
    public List<UnitConverterDb> getUnitConverterDbs(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException
    {
        return getUnitConverterDbs(tx, null, limit, offset);
    }

    @Override
    public List<UnitConverterDb> getUnitConverterDbs(DataTransaction tx, String family, int limit, int offset)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var dbType = tx.getContext().getDatabase();
        final String querySql = """
                select uc.id, uc.fromunitsabbr, uc.tounitsabbr, uc.algorithm, uc.a,uc.b,uc.c,uc.d,uc.e,uc.f,
                    from_eu.unitabbr from_unitabbr, from_eu.name from_name, from_eu.family from_family, from_eu.measures from_measures,
                    to_eu.unitabbr to_unitabbr, to_eu.name to_name, to_eu.family to_family, to_eu.measures to_measures
                    from unitconverter uc 
                    left join engineeringunit from_eu on from_eu.unitabbr = uc.fromunitsabbr
                    left join engineeringunit to_eu on to_eu.unitabbr = uc.tounitsabbr
                    <if(family)> where upper(from_eu.family) = :familyInput or upper(to_eu.family) = :familyInput <endif>
                    order by uc.fromunitsabbr <collate> asc, uc.toounitsabbr <collate> asc
                    <limit>
            """;
        try (var query = handle.createQuery(querySql).setTemplateEngine(new StringTemplateEngine()))
        {
            return query.define("limit", addLimitOffset(limit, offset))
                        .define("family", family)
                        .bind("familyInput", family)
                        .define("collate", dbType == DatabaseEngine.POSTGRES ? "COLLATE \"C\"" : "COLLATE BINARY" )
                        .registerRowMapper(UnitConverterDb.class, UnitConverterMapper.withPrefix(""))
                        .mapTo(UnitConverterDb.class)
                        .list();
        }
    }
    
 
    /**
     * No direct conversion was available so now it's grab them all (for a family) and and
     * do some graph theory work.
     * @param tx
     * @param fromAbbr
     * @param toAbbr
     * @return
     */
    private Optional<UnitConverterDb> buildComposite(DataTransaction tx, EngineeringUnit from, EngineeringUnit to)
        throws OpenDcsDataException
    {
        var converters = getUnitConverterDbs(tx, to.getFamily(), -1, -1);

        return Optional.empty();
    }
}
