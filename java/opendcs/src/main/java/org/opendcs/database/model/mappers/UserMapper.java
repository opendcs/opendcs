package org.opendcs.database.model.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.User;

public class UserMapper implements RowMapper<User>
{

    @Override
    public User map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        

        return User;
    }
    
}
