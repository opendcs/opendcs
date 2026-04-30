package org.opendcs.database.impl.opendcs.jdbi.column.json;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigArgumentFactory extends AbstractArgumentFactory<Map<String, Object>>
{
    private ObjectMapper om = new ObjectMapper();
    public ConfigArgumentFactory()
    {
        super(Types.VARCHAR);
    }

    @Override
    protected Argument build(Map<String, Object> value, ConfigRegistry config)
    {
        return (position, statement, ctx) ->
        {
            try
            {
                final String val = om.writeValueAsString(value);
                statement.setString(position, val);
            }
            catch (JsonProcessingException ex)
            {
                throw new SQLException("Unable to create json string from map", ex);
            }
        };
    }
}
