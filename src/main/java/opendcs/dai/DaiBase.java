package opendcs.dai;

import java.sql.Connection;
import java.sql.ResultSet;

import decodes.tsdb.DbIoException;

public interface DaiBase
{
	public ResultSet doQuery(String q)
		throws DbIoException;
	
	public ResultSet doQuery2(String q) 
		throws DbIoException;

	public int doModify(String q)
		throws DbIoException;

	public void close();
	
	public void setManualConnection(Connection conn);

}
