package opendcs.util.functional;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.opendcs.util.functional.ThrowingConsumer;

@FunctionalInterface
public interface StatementConsumer extends ThrowingConsumer<PreparedStatement,SQLException>
{
    /**
     * User provided function that operates on a valid PreparedStatement
     * 
     */
    public void accept(PreparedStatement stmt) throws SQLException;
}
