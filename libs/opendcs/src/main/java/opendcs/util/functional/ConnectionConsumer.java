package opendcs.util.functional;

import java.sql.Connection;
import java.sql.SQLException;

import decodes.tsdb.DbIoException;

@FunctionalInterface
public interface ConnectionConsumer extends ThrowingConsumer<Connection,SQLException>
{
    @Override
    public abstract void accept(Connection conn) throws SQLException;
    
}
