package org.opendcs.database.impl.opendcs.jdbi.column.numeric;

import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

public class NullableDoubleArgumentFactory extends AbstractArgumentFactory<Double>
{

    private final Double undefinedValue;

    public NullableDoubleArgumentFactory()
    {
        this(NullableDouble.DEFAULT_NULL_REPLACEMENT);
    }

    public NullableDoubleArgumentFactory(double undefinedValue)
    {
        super(Types.DOUBLE);
        this.undefinedValue = undefinedValue;
    }

    @Override
    protected Argument build(Double value, ConfigRegistry config)
    {
        return (position, statement, ctx) -> statement.setDouble(position, value == undefinedValue ? value : null);
        
    }
}
