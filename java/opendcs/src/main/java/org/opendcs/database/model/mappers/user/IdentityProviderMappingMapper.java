package org.opendcs.database.model.mappers.user;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.mappers.IdentityProviderMapper;

public class IdentityProviderMappingMapper implements RowMapper<IdentityProviderMapping>
{
    private final String prefix;
    private final IdentityProviderMapper idpMapper;

    private IdentityProviderMappingMapper(String prefix)
    {
        this.prefix = prefix == null ? ""
                    : (prefix.endsWith("_") ? prefix : prefix+"_");
        idpMapper = new IdentityProviderMapper(prefix);
    }

    @Override
    public IdentityProviderMapping map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        
        IdentityProvider provider = idpMapper.map(rs, ctx);
        String subject = rs.getString(prefix+"subject");

        return new IdentityProviderMapping(provider, subject);
    }
    
    public static IdentityProviderMappingMapper withPrefix(String prefix)
    {
        return new IdentityProviderMappingMapper(prefix);
    }
}
