package opendcs.util.functional;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetFunction<R> extends ThrowingFunction<ResultSet,R,SQLException> {
    public R accept(ResultSet rs) throws SQLException;
}
