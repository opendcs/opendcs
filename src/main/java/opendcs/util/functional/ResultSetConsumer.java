package opendcs.util.functional;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User provided function that operates on a single valid result set.
 * 
 */
@FunctionalInterface
public interface ResultSetConsumer extends ThrowingConsumer<ResultSet,SQLException>
{
    /**
     * @param rs a valid resultset, user function should *NOT* call next
     */
    public void accept(ResultSet rs) throws SQLException;
}
