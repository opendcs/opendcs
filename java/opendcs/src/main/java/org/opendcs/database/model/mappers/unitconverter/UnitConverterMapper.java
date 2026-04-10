package org.opendcs.database.model.mappers.unitconverter;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.jdbi.column.numeric.NullableDouble;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.model.mappers.engineeringunit.EngineeringUnitMapper;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.LinearConverter;
import decodes.db.NullConverter;
import decodes.db.Poly5Converter;
import decodes.db.UnitConverterDb;
import decodes.db.UsgsStdConverter;
import decodes.sql.DbKey;

public final class UnitConverterMapper extends PrefixRowMapper<UnitConverterDb>
{

    private UnitConverterMapper(String prefix)
    {
        super(prefix);
    }

    public static UnitConverterMapper withPrefix(String prefix)
    {
        return new UnitConverterMapper(prefix);
    }

    @Override
    public UnitConverterDb map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class)
                                             .orElseThrow(() -> new SQLException("No mapper registered for DbKey class."));
        ColumnMapper<Double> doubleMapper = new NullableDouble();
        var fromMapper = EngineeringUnitMapper.withPrefix("from");
        var toMapper = EngineeringUnitMapper.withPrefix("to");
        final DbKey id = dbKeyMapper.map(rs, prefix + GenericColumns.ID, ctx);
        String fromUnits = rs.getString(prefix + "fromunitsabbr");
        String toUnits = rs.getString(prefix + "tounitsabbr");
        String algorithm = rs.getString(prefix + "algorithm");

        double[] coefficients = new double[6];
        coefficients[0] = doubleMapper.map(rs, prefix + "a", ctx);
        coefficients[1] = doubleMapper.map(rs, prefix + "b", ctx);
        coefficients[2] = doubleMapper.map(rs, prefix + "c", ctx);
        coefficients[3] = doubleMapper.map(rs, prefix + "d", ctx);
        coefficients[4] = doubleMapper.map(rs, prefix + "e", ctx);
        coefficients[5] = doubleMapper.map(rs, prefix + "f", ctx);

        var converter = new UnitConverterDb(fromUnits, toUnits);
        try
        {
            converter.setId(id);
            converter.algorithm = algorithm;
            converter.coefficients = coefficients;
            // implement the logic of UnitConverterDb.prepareForExec so that we simply don't need to.            
            var from = fromMapper.map(rs, ctx);
            var to = toMapper.map(rs, ctx);

            if (algorithm.equalsIgnoreCase(Constants.eucvt_none))
            {
    			converter.execConverter = new NullConverter(from, to);
            }
            else if (algorithm.equalsIgnoreCase(Constants.eucvt_linear))
            {
                converter.execConverter = new LinearConverter(from, to);
            }
            else if (algorithm.equalsIgnoreCase(Constants.eucvt_usgsstd))
            {
                converter.execConverter = new UsgsStdConverter(from, to);
            }
            else if (algorithm.equalsIgnoreCase(Constants.eucvt_poly5))
            {
                converter.execConverter = new Poly5Converter(from, to);
            }
            else
            {
                // Possibly some user-custom converter. Try to use an enum
                // to construct the class dynamically.
                // TBD
            }
		    converter.execConverter.setCoefficients(converter.coefficients);
        }
        catch (DatabaseException ex)
        {
            throw new SQLException("Unable create unit converter", ex);
        }
        return converter;
    }
    
}
