/*
*  $Id$
*/
package decodes.tsdb.test;

import opendcs.dai.TimeSeriesDAI;
import ilex.cmdline.*;
import decodes.util.CmdLineArgs;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.tsdb.*;

/**
Writes some test data to a time series in the database.
*/
public class CreateTS extends TestProg
{
	private StringToken tsidArg = new StringToken("", "Unique path string", "",
		TokenOptions.optArgument | TokenOptions.optRequired, "");

	public CreateTS()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(tsidArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TestProg tp = new CreateTS();
		tp.execute(args);
	}

	protected void runTest()
		throws Exception
	{
		String path = tsidArg.getValue();
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		
		try
		{
			TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(path);
			System.out.println("ts_id '" + path + "' already exists with ts_code="
				+ tsid.getKey());
			System.exit(0);
		}
		catch(NoSuchObjectException ex)
		{
			System.out.println("ts_id '" + path + "' does not exist. Will create.");
		}
		
		CwmsTimeSeriesDb cwmsTsDb = (CwmsTimeSeriesDb)theDb;
		CwmsTsId tsid = new CwmsTsId();
		tsid = new CwmsTsId();
		tsid.setUniqueString(path);

		cwmsTsDb.makeTimeSeriesDAO().createTimeSeries(tsid);
		System.out.println("Create successful, ts_code=" + tsid.getKey());
		long now = System.currentTimeMillis();
		while(System.currentTimeMillis() - now < 600000L)
		{
			System.out.println("Attempting to read view ...");
			try
			{
				TimeSeriesIdentifier readTsid = timeSeriesDAO.getTimeSeriesIdentifier(path);
				System.out.println("ts_id '" + path + "' exists with ts_code="
					+ readTsid.getKey());
				System.exit(0);
			}
			catch(NoSuchObjectException ex)
			{
				System.out.println("ts_id '" + path + "' not in view.");
				try { Thread.sleep(5000L); }
				catch(InterruptedException ex2) {}
			}
		}
		System.out.println("Failed to read '" + path + "' from view after 10 minutes.");
		timeSeriesDAO.close();
	}
}
