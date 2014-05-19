package decodes.tsdb.test;

import decodes.util.CmdLineArgs;
import decodes.cwms.CwmsTimeSeriesDb;

public class TsCat extends TestProg
{
	public TsCat()
	{
		super(null);
	}

	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
	}

	public static void main(String args[])
		throws Exception
	{
		TsCat tp = new TsCat();
		tp.execute(args);
	}

	protected void runTest()
		throws Exception
	{
		((CwmsTimeSeriesDb)theDb).printCat();
	}
}
