package org.opendcs.database.model.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.BuiltInIdentityProvider;
import org.opendcs.database.model.IdentityProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.sql.DbKey;

public class IdentityProviderMapper implements RowMapper<IdentityProvider>
{
    private final ObjectMapper om = new ObjectMapper();

    private final String prefix;

    public IdentityProviderMapper(String prefix)
    {
        this.prefix = prefix == null ? ""
                    : (prefix.endsWith("_") ? prefix : prefix+"_");
    }


    @Override
    public IdentityProvider map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        DbKey id = DbKey.createDbKey(rs, prefix+"id");
        String name = rs.getString(prefix+"name");
        String type = rs.getString(prefix+"type");
        ColumnMapper<ZonedDateTime> zdtMapper = ctx.findColumnMapperFor(ZonedDateTime.class).orElseThrow(() -> new SQLException("No mapper registered for ZonedDateTime."));
        ZonedDateTime updatedAt = zdtMapper.map(rs, prefix+"updated_at", ctx);
        try
        {
            String jsonData = rs.getString(prefix+"config");
            Map<String, Object> config = om.readValue(jsonData, new TypeReference<Map<String, Object>>() {});
            // TODO: other providers
            return new BuiltInIdentityProvider(id, name, updatedAt, config);
        }
        catch (JsonProcessingException | SQLException ex)
        {
            throw new SQLException("Unable to parse provided json", ex);
        }

    }

}
