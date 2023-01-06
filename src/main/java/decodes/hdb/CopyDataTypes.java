package decodes.hdb;

import java.sql.*;

import decodes.tsdb.TsdbAppTemplate;
import opendcs.dai.DataTypeDAI;
import decodes.util.CmdLineArgs;
import decodes.tsdb.xml.CompXioTags;

/**
This is a stand-alone program to copy all HDB_DATATYPE entries into the
DECODES DataType table for use by DECODES and by the Computation Processor.
*/
public class CopyDataTypes extends TsdbAppTemplate
{
	public CopyDataTypes()
	{
		super("util.log");
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		//No-op
	}

	public static void main(String args[])
		throws Exception
	{
		CopyDataTypes tp = new CopyDataTypes();
		tp.execute(args);
	}

	protected void runApp() throws Exception
	{
		String q = "SELECT DATATYPE_ID from HDB_DATATYPE";
		
		DataTypeDAI dtDao = theDb.makeDataTypeDAO();
		try
		{
			ResultSet rs = dtDao.doQuery(q);
			while(rs.next())
			{
				int id = rs.getInt(1);
				String iq = "insert into datatype values(" + id + ", "
					+ theDb.sqlString(CompXioTags.hdb) + ", "
					+ theDb.sqlString("" + id) + ")";
				dtDao.doModify(iq);
			}
		}
		finally
		{
			dtDao.close();
		}
	}
}
