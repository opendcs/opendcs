package decodes.hdb;

import java.sql.*;
import ilex.cmdline.*;
import ilex.util.TTYEcho;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;
import decodes.tsdb.test.TestProg;
import decodes.tsdb.xml.CompXioTags;

/**
This is a stand-alone program to copy all HDB_DATATYPE entries into the
DECODES DataType table for use by DECODES and by the Computation Processor.
*/
public class CopyDataTypes extends TestProg
{
	public CopyDataTypes()
	{
		super("util.log");
	}

	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
	}

	public static void main(String args[])
		throws Exception
	{
		TestProg tp = new CopyDataTypes();
		tp.execute(args);
	}

	protected void runTest()
		throws Exception
	{
		String q = "SELECT DATATYPE_ID from HDB_DATATYPE";
		
		ResultSet rs = theDb.doQuery(q);
		while(rs.next())
		{
			int id = rs.getInt(1);
			String iq = "insert into datatype values(" + id + ", "
				+ theDb.sqlString(CompXioTags.hdb) + ", "
				+ theDb.sqlString("" + id) + ")";
			theDb.doModify(iq);
		}
	}
}
