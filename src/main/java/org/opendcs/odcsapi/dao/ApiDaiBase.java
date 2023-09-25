package org.opendcs.odcsapi.dao;

import java.sql.ResultSet;

public interface ApiDaiBase
	extends AutoCloseable
{
	public ResultSet doQuery(String q)
		throws DbException;
	
	public ResultSet doQuery2(String q) 
		throws DbException;

	public int doModify(String q)
		throws DbException;

	public void close();
	
	public Long getKey(String tableName)
		throws DbException;
}
