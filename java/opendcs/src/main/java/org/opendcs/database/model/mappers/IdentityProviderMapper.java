package org.opendcs.database.model.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.BuiltInIdentityProvider;
import org.opendcs.database.model.IdentityProvider;

import decodes.sql.DbKey;

public class IdentityProviderMapper implements RowMapper<IdentityProvider>
{

    @Override
    public IdentityProvider map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        DbKey id = DbKey.createDbKey(rs, "id");
        String name = rs.getString("name");
        String type = rs.getString("type");
        ColumnMapper<ZonedDateTime> zdtMapper = ctx.findColumnMapperFor(ZonedDateTime.class).orElseThrow(() -> new SQLException("No mapper registered for ZonedDateTime."));
        ZonedDateTime updatedAt = zdtMapper.map(rs, "updated_at", ctx);
        // TODO: config map
        // TODO: other providers
        return new BuiltInIdentityProvider(id, name, updatedAt, null); 
    }
    
}
