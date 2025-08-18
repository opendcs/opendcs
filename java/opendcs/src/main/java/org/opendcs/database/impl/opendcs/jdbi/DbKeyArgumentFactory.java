package org.opendcs.database.impl.opendcs.jdbi;

import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import decodes.sql.DbKey;

public class DbKeyArgumentFactory extends AbstractArgumentFactory<DbKey>
{

    public DbKeyArgumentFactory()
    {
        super(Types.BIGINT);
    }

    @Override
    protected Argument build(DbKey value, ConfigRegistry config)
    {
        return (position, statement, ctx) -> statement.setLong(position, value.getValue());
    }    
}
