package org.opendcs.database.impl.opendcs.jdbi.column.json;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigColumnMapper implements ColumnMapper<Map<String, Object>>
{
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public Map<String, Object> map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException
    {
        final String configStr = rs.getString(columnNumber);
        try {
            return om.readValue(configStr, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex)
        {
            throw new SQLException("Unable to parse provided json preferences", ex);
        }
    }
    
}
