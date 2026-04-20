package org.opendcs.database.impl.cwms.jdbi;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;

public class CwmsBooleanArgumentFactory implements ArgumentFactory.Preparable
{

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config)
    {
        if (type == boolean.class || type == Boolean.class) {
            final var val = (boolean) value;
            return Optional.of((position, statement, ctx) -> {
                if (value == null) {
                    statement.setNull(position, Types.VARCHAR);
                } else {
                    statement.setString(position, val ? "T" : "F");
                }
            });
        }
        return Optional.empty();
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        if (type == double.class || type == Double.class) {
            return Optional.of(value -> build(type, value, config).get());
        }
        return Optional.empty();
    }
}
