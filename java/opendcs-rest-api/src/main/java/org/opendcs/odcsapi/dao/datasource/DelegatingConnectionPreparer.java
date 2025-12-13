package org.opendcs.odcsapi.dao.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegatingConnectionPreparer implements ConnectionPreparer
{

	private static final Logger logger = LoggerFactory.getLogger(DelegatingConnectionPreparer.class);
	private final List<ConnectionPreparer> delegates = new ArrayList<>();

	public DelegatingConnectionPreparer(List<ConnectionPreparer> preparers)
	{
		if(preparers != null)
		{
			delegates.addAll(preparers);
		}
	}

	@Override
	public Connection prepare(Connection connection) throws SQLException
	{
		Connection retval = connection;
		for(ConnectionPreparer delegate : delegates)
		{
			logger.atTrace().log(delegate.getClass().getName());
			retval = delegate.prepare(retval);
		}
		return retval;
	}

}
