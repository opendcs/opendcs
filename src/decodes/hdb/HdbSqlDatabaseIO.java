package decodes.hdb;

import opendcs.dai.IntervalDAI;
import decodes.sql.SqlDatabaseIO;

public class HdbSqlDatabaseIO extends SqlDatabaseIO
{
	@Override
	public boolean isHdb() { return true; }
	
	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new HdbIntervalDAO(this);
	}

}
