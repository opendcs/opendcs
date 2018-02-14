package decodes.hdb;

import opendcs.dai.IntervalDAI;
import opendcs.dai.SiteDAI;
import decodes.db.DatabaseException;
import decodes.sql.SqlDatabaseIO;

public class HdbSqlDatabaseIO extends SqlDatabaseIO
{
	public HdbSqlDatabaseIO()
		throws DatabaseException
	{
		this(null);
	}
	
	public HdbSqlDatabaseIO(String sqlDbLocation)
		throws DatabaseException
	{
		// No-args base class ctor doesn't connect to DB.
		super(sqlDbLocation);
		keyGenerator = new OracleSequenceHDBGenerator();
	}
	
	@Override
	public boolean isHdb() { return true; }
	
	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new HdbIntervalDAO(this);
	}
	
	@Override
	public SiteDAI makeSiteDAO()
	{
		return new HdbSiteDAO(this);
	}

}
