package org.opendcs.database.impl.opendcs.jdbi.column.chrono;

import java.lang.reflect.Type;
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
                    if (datesAreInt)
                    {
                        statement.setLong(position, ((Date)value).getTime());
                    }
                    else
                    {
                        statement.setDate(position, new java.sql.Date(((Date)value).getTime()));
                    }
                });
        }
        else
        {
            return Optional.empty();
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
