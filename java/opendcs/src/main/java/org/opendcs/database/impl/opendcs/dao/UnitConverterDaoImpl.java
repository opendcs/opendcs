package org.opendcs.database.impl.opendcs.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.Vector;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.EngineeringUnitDao;
import org.opendcs.database.dai.UnitConverterDao;
import org.opendcs.database.impl.opendcs.jdbi.column.numeric.NullableDoubleArgumentFactory;
import org.opendcs.database.model.mappers.unitconverter.UnitConverterMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.CompositeConverter;
import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = UnitConverterDao.class)
public class UnitConverterDaoImpl implements UnitConverterDao
{
    @InjectDao
    EngineeringUnitDao euDao;

    @Override
    public Optional<UnitConverterDb> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String querySql = """
                    select uc.id, uc.fromunitsabbr, uc.tounitsabbr, uc.algorithm, uc.a, uc.b, uc.c, uc.d, uc.e, uc.f,
                        from_eu.unitabbr from_unitabbr, from_eu.name from_name, from_eu.family from_family, from_eu.measures from_measures,
                        to_eu.unitabbr to_unitabbr, to_eu.name to_name, to_eu.family to_family, to_eu.measures to_measures
                      from unitconverter uc
                      left join engineeringunit from_eu on from_eu.unitabbr = uc.fromunitsabbr
                      left join engineeringunit to_eu on to_eu.unitabbr = uc.tounitsabbr
                     where uc.id = :id
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
                 .registerArgument(new NullableDoubleArgumentFactory())
                 .bind("a", coefficients[0])
                 .bind("b", coefficients[1])
                 .bind("c", coefficients[2])
                 .bind("d", coefficients[3])
                 .bind("e", coefficients[4])
                 .bind("f", coefficients[5])
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
    public Optional<UnitConverterDb> findUnitConverterFor(DataTransaction tx, String fromAbbr, String toAbbr)
            throws OpenDcsDataException
    {
        var from = euDao.getByName(tx, fromAbbr).orElseThrow(() -> new NoSuchUnitException(fromAbbr));
        var to = euDao.getByName(tx, toAbbr).orElseThrow(() -> new NoSuchUnitException(toAbbr));
        return findUnitConverterFor(tx, from, to);
    }

    @Override
    public Optional<UnitConverterDb> findUnitConverterFor(DataTransaction tx, EngineeringUnit from, EngineeringUnit to)
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
                      where upper(uc.fromunitsabbr) = upper(:from)
                        and upper(uc.tounitsabbr) = upper(:to)
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
                    where uc.fromunitsabbr != 'raw'
                    <if(family)> and upper(from_eu.family) = :familyValue or upper(to_eu.family) = :familyValue <endif>
                    order by uc.fromunitsabbr <collate> asc, uc.tounitsabbr <collate> asc
                    <limit>
            """;
        try (var query = handle.createQuery(querySql).setTemplateEngine(new StringTemplateEngine()))
        {
            query.define("limit", addLimitOffset(limit, offset))
                        .define("family", family)
                        .define("collate", dbType == DatabaseEngine.POSTGRES ? "COLLATE \"C\"" : "COLLATE BINARY" );
            if (family != null)
            {
                query.bind("familyValue", family);
            }
            if (limit != -1)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }

            if (offset != -1)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }
            return query.registerRowMapper(UnitConverterDb.class, UnitConverterMapper.withPrefix(""))
                        .mapTo(UnitConverterDb.class)
                        .list();
        }
    }


    /**
     * No direct conversion was available so now it's grab them all (for a family) and and
     * do some graph theory work.
     *
     * NOTE: this code is intentionally copied from the CompositeConverter class. There
     * would be too many changes required to clean that up and not depend on the static Database instance.
     *
     * Also, while we could cache this, the required list if limited to the specific family of conversions and
     * is thus rather small. If a performance impact is disocvered either improvements can be made or
     * points of usage can cache as required.
     *
     * @param tx DataTransaction required to get current list of unit converters
     * @param fromAbbr source unit
     * @param toAbbr target unit
     * @return
     */
    private Optional<UnitConverterDb> buildComposite(DataTransaction tx, EngineeringUnit from, EngineeringUnit to)
        throws OpenDcsDataException
    {
        var converters = getUnitConverterDbs(tx, to.getFamily(), -1, -1);
        List<UnitConverter> solutions = new ArrayList<>();
        @SuppressWarnings("java:S1149") // copy of existing code
        Stack<UnitConverter> callStack = new Stack<>();
        HashSet<String> unitsSearched = new HashSet<>();

        search(solutions, converters.stream().map(uc -> uc.execConverter).toList(), from, to, callStack, unitsSearched);
        if (solutions.isEmpty())
        {
            return Optional.empty(); // no conversions found.
        }
        UnitConverter best = null;
        double bestWeight = Double.MAX_VALUE;
        for (var uc: solutions)
        {
            double w = uc.getWeight();
            if (w < bestWeight)
            {
                best = uc;
                bestWeight = w;
            }
        }
        var ucDb = new UnitConverterDb(from.abbr, to.abbr);
        ucDb.execConverter = best;

        return Optional.of(ucDb);
    }

    /**
     *
     * Recursively search through the known unit converters attempting to build a converter that is a sequence of conversions from
     * the source to target units.
     *
     * same note as above, mostly copied from CompositeConverter to avoid dealing the the required changes to allow the new DAOs and old access method.
     *
     * @param solutiosn
     * @param converters
     * @param from current source unit
     * @param to current target unit
     * @param callStack current composite as we search through and keep building
     * @param unitsSearch set of units we have already searched.
     */
    @SuppressWarnings({"java:S1149","java:S3047"}) // S1149copy of existing code, S3047 these loops operate in fundementally different ways.
    private static void search(List<UnitConverter> solutions, List<UnitConverter> converters, EngineeringUnit from, EngineeringUnit to,
                               Stack<UnitConverter> callStack, HashSet<String> unitsSearched)
    {
        unitsSearched.add(from.getAbbr());

        // check for direct
        for (var uc: converters)
        {
			if (!uc.getFrom().getAbbr().equalsIgnoreCase(from.getAbbr()))
				continue;
			if (uc.getTo().getAbbr().equalsIgnoreCase(to.getAbbr()))
			{
				callStack.push(uc);
				CompositeConverter cc = new CompositeConverter(
					(callStack.elementAt(0)).getFrom(), to, new Vector<>(callStack));
				solutions.add(cc);
				callStack.pop();
				return;
			}
        }

        for (var uc: converters)
        {
            // Skip if 'from' doesn't match or if I've already searched 'To'.
			if (!uc.getFrom().getAbbr().equalsIgnoreCase(from.getAbbr()) || unitsSearched.contains(to.getAbbr()))
				continue;

			callStack.push(uc);
			search(solutions, converters, uc.getTo(), to, callStack, unitsSearched);
			callStack.pop();
        }
    }
}
