package opendcs.util.functional;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.opendcs.util.functional.ThrowingFunction;

@FunctionalInterface
public interface ResultSetFunction<R> extends ThrowingFunction<ResultSet,R,SQLException>
{
    R apply(ResultSet rs) throws SQLException;
}
