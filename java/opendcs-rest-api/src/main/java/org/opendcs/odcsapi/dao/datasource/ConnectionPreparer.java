package org.opendcs.odcsapi.dao.datasource;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPreparer
{
	Connection prepare(Connection connection) throws SQLException;
}
