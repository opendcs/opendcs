package org.opendcs.odcsapi.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An implementation of this interface must be passed to ApiDaoBase.doQueryV. It will
 * be passed the ResultSet after inserting params and executing the PreparedStatement.
 * The implementation is typically an anonymous class or a lambda.
 */
@FunctionalInterface
public interface ResultSetConsumer
{
	public abstract void accept(ResultSet rs) throws SQLException;
}
