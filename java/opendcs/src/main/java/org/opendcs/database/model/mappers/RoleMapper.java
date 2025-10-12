package org.opendcs.database.model.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.Role;

import decodes.sql.DbKey;

public class RoleMapper implements RowMapper<Role>
{

    @Override
    public Role map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        DbKey key = DbKey.createDbKey(rs, "id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        ColumnMapper<ZonedDateTime> columnMapperForZDT = ctx.findColumnMapperFor(ZonedDateTime.class)
                                                            .get();
        ZonedDateTime updatedAt = columnMapperForZDT.map(rs, "updated_at", ctx);
        return new Role(key, name, description, updatedAt);
    }
    
}