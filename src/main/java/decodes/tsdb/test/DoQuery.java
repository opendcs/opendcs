package decodes.tsdb.test;

import java.sql.*;
import ilex.cmdline.*;
import ilex.util.TTYEcho;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;

public class DoQuery extends TestProg
{
	private StringToken queryArg;

	public DoQuery()
	{
		super(null);
	}

	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		queryArg = new StringToken("q","query","",TokenOptions.optSwitch, ""); 
		cmdLineArgs.addToken(queryArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TestProg tp = new DoQuery();
		tp.execute(args);
	}

	protected void runTest()
		throws Exception
	{
		String q;
		while((q = TTYEcho.readLn()) != null && q.length() > 0)
		{
			ResultSet rs = theDb.doQuery(q);
			ResultSetMetaData rsmd = rs.getMetaData();
			int ncol = rsmd.getColumnCount();
			System.out.println("Meta Data:");
			for(int i=1; i<=ncol; i++)
				System.out.println("\tColumn "+i+": "+rsmd.getColumnName(i));
			System.out.println("Query Results:");
			while(rs.next())
			{
				for(int i=1; i<=ncol; i++)
					System.out.print(rs.getString(i) + " : ");
				System.out.println("");
			}
		}
	}
}
