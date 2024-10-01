/*
*  $Id$
*/
package decodes.tsdb.test;

import java.util.Date;
import java.util.StringTokenizer;

import opendcs.dai.TimeSeriesDAI;

import ilex.cmdline.*;

import decodes.util.CmdLineArgs;
import decodes.sql.DbKey;
import decodes.tsdb.*;

/**
Deletes some test data from a time series in the database.
*/
public class DeleteTimeSeries extends TestProg
{
	private StringToken outArg;

	public DeleteTimeSeries()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		outArg = new StringToken("", "sdi:intv:tabsel:modid", "", 
			TokenOptions.optArgument|TokenOptions.optRequired, "");
		cmdLineArgs.addToken(outArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TestProg tp = new DeleteTimeSeries();
		tp.execute(args);
	}

	protected void runTest()
		throws Exception
	{
		String outTS = outArg.getValue();

		// Build an empty time series to use for deleting.
		CTimeSeries ts = makeTimeSeries(outTS);
		ts.setComputationId(DbKey.createDbKey(1));
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		timeSeriesDAO.deleteTimeSeriesRange(ts, 
			new Date(System.currentTimeMillis() - (3600000L * 24)),
			new Date());
		timeSeriesDAO.close();
	}

	private static CTimeSeries makeTimeSeries(String x)
		throws Exception
	{
		StringTokenizer st = new StringTokenizer(x, ":");
		DbKey sdi = DbKey.createDbKey(Long.parseLong(st.nextToken()));
		String intv = st.nextToken();
		String tabsel = st.nextToken();
		CTimeSeries ret = new CTimeSeries(sdi, intv, tabsel);
		if (st.hasMoreTokens())
		{
			int modid = Integer.parseInt(st.nextToken());
			ret.setModelRunId(modid);
		}
		return ret;
	}
}
