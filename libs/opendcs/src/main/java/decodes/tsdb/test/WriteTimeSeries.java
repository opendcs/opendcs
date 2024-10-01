/*
*  $Id$
*/
package decodes.tsdb.test;

import java.util.Date;
import java.util.StringTokenizer;

import opendcs.dai.TimeSeriesDAI;

import ilex.cmdline.*;
import ilex.var.TimedVariable;

import decodes.util.CmdLineArgs;
import decodes.sql.DbKey;
import decodes.tsdb.*;

/**
Writes some test data to a time series in the database.
*/
public class WriteTimeSeries extends TestProg
{
	private StringToken outArg;
	private IntegerToken periodArg;
	private IntegerToken numSamplesArg;
	private BooleanToken lineArg;

	public WriteTimeSeries()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		lineArg = new BooleanToken("L", "Ascending Line", "", 
			TokenOptions.optSwitch, false);
		periodArg = new IntegerToken("S", "sample period in seconds", "", 
			TokenOptions.optSwitch, 3600);
		numSamplesArg = new IntegerToken("N", "Number of Samples", "", 
			TokenOptions.optSwitch, 24);
		outArg = new StringToken("", "sdi:intv:tabsel:modid", "", 
			TokenOptions.optArgument|TokenOptions.optRequired, "");
		cmdLineArgs.addToken(lineArg);
		cmdLineArgs.addToken(periodArg);
		cmdLineArgs.addToken(numSamplesArg);
		cmdLineArgs.addToken(outArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TestProg tp = new WriteTimeSeries();
		tp.execute(args);
	}

	protected void runTest()
		throws Exception
	{
		String outTS = outArg.getValue();

		CTimeSeries ts = makeTimeSeries(outTS);
		ts.setComputationId(DbKey.createDbKey(1));

		long periodMsec = periodArg.getValue() * 1000L;
		int num = numSamplesArg.getValue();

		// Build a time series with 1 day's worth of hourly stage values.
		long t = System.currentTimeMillis();
		// Truncate to even period
		t = (t / periodMsec) * periodMsec;
		t -= (periodMsec * (num-1));
		for(int i=0; i<num; i++)
		{
			TimedVariable tv = new TimedVariable(
				50. + 50. * Math.sin((double)i * Math.PI / 12));
			tv.setTime(new Date(t + (i * periodMsec)));
			tv.setFlags(VarFlags.TO_WRITE);
			ts.addSample(tv);
			System.out.println("Added timed variable: " + tv);
		}
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		timeSeriesDAO.saveTimeSeries(ts);
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
