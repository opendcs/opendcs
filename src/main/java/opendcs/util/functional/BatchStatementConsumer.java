package opendcs.util.functional;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface BatchStatementConsumer<R> extends BiThrowingConsumer<PreparedStatement,SQLException,R>
{
    /**
     * User provided function that operates on a valid PreparedStatement
     * 
     */
    public void accept(PreparedStatement stmt, R item) throws SQLException;
}
