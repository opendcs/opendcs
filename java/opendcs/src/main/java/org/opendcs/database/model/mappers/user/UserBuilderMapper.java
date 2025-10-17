package org.opendcs.database.model.mappers.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.sql.DbKey;

public class UserBuilderMapper implements RowMapper<User.Builder>
{
    private final String prefix;
    private final ObjectMapper om = new ObjectMapper();

    private UserBuilderMapper(String prefix)
    {
        this.prefix = prefix == null ? ""
                    : (prefix.endsWith("_") ? prefix : prefix+"_");
    }


    public static UserBuilderMapper withPrefix(String prefix)
    {
        return new UserBuilderMapper(prefix);
    }

    @Override
    public User.Builder map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        User.Builder builder = new User.Builder();
        ColumnMapper<ZonedDateTime> columnMapperForZDT 
                = ctx.findColumnMapperFor(ZonedDateTime.class)
                     .get();
        
        builder.withId(DbKey.createDbKey(rs, prefix+"id"))
               .withEmail(rs.getString(prefix+"email"))
               .withUpdatedAt(columnMapperForZDT.map(rs, prefix+"updated_at", ctx))
               .withCreatedAt(columnMapperForZDT.map(rs, prefix+"created_at", ctx))
                ;
        String jsonData = rs.getString(prefix+"preferences");
        Map<String, Object> preferences;
        try
        {
            preferences = om.readValue(jsonData, new TypeReference<Map<String, Object>>() {});
            return builder.withPreferences(preferences);
        }
        catch (JsonProcessingException ex)
        {
            throw new SQLException("Unable to parse provided json preferences", ex);
        }
    }
}