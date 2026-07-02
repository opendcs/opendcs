package org.opendcs.database.impl.opendcs.jdbi.column.chrono;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;

public class OpenDcsTimeColumnArgumentFactory implements ArgumentFactory.Preparable
{

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config)
    {
        if (type == Date.class)
        {
            return Optional.of(
                (position, statement, ctx) ->
                {
                    final var tmp = (Boolean)ctx.getAttribute("numeric_date");
                    final var datesAreInt =  tmp != null && tmp;
                    setDate(statement, position, datesAreInt, (Date)value);
                });
        }
        else
        {
            return Optional.empty();
        }
    }

    private static void setDate(PreparedStatement statement, int position, boolean datesAreInt, Date value)
        throws SQLException
    {
        if (datesAreInt)
        {
            if (value == null)
            {
                statement.setNull(position, Types.BIGINT);
            }
            else
            {
                statement.setLong(position, value.getTime());
            }
        }
        else
        {
            statement.setDate(position, value == null ? null : new java.sql.Date(value.getTime()));
        }
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config)
    {
        if (type == Date.class)
        {
            return Optional.of(value -> build(type, value, config).get());
        }
        return Optional.empty();
    }
}
