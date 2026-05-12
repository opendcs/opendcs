package org.opendcs.database.impl.opendcs.jdbi.column.numeric;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * For those cases where we use {@see decodes.db.Constants.undefinedDouble}, or some other number,
 * to indicate an invalide value but the database expects to store it as a NULL but we don't
 * want to write out all that logic every time.
 *
 * Due to the use of primitives in most cases we have to use the ArgumentFactory directly,
 * specifically ArgumentFactory.Preparable to how the precidents of the arguments binding functions.
 *
 * This should not be applied to a jdbi instance directly but to individual handles when required.
 *
 */
public class NullableDoubleArgumentFactory implements ArgumentFactory.Preparable
{
    private final double undefinedValue;

    public NullableDoubleArgumentFactory()
    {
        this(NullableDouble.DEFAULT_NULL_REPLACEMENT);
    }

    public NullableDoubleArgumentFactory(double undefinedValue)
    {
        this.undefinedValue = undefinedValue;
    }

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config)
    {
        if (type == double.class || type == Double.class)
        {
            return Optional.of((position, statement, ctx) ->
            {
                if (value == null || (double)value == undefinedValue)
                {
                    statement.setNull(position, Types.DOUBLE);
                }
                else
                {
                    statement.setDouble(position, (double)value);
                }
            });
        }
        return Optional.empty();
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config)
    {
        if (type == double.class || type == Double.class)
        {
            return Optional.of(value -> build(type, value, config).get());
        }
        return Optional.empty();
    }
}
