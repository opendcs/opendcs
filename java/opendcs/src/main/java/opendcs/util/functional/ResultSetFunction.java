package opendcs.util.functional;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.opendcs.util.functional.ThrowingFunction;

public interface ResultSetFunction<R> extends ThrowingFunction<ResultSet,R,SQLException> {
    public R apply(ResultSet rs) throws SQLException;
}
