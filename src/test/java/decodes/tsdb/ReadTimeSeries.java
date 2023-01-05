/*
*  $Id$
*/
package decodes.tsdb;

import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import opendcs.dai.TimeSeriesDAI;

import lrgs.gui.DecodesInterface;

import ilex.cmdline.*;
import ilex.util.IDateFormat;
import ilex.var.TimedVariable;

import decodes.db.SiteName;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.sql.DbKey;

/**
Reads some test data from a time series in the database.
*/
public class ReadTimeSeries extends TsdbAppTemplate
{
	private StringToken outArg;
	private StringToken sinceArg;
	private StringToken untilArg;
	
	public ReadTimeSeries()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		sinceArg = new StringToken("S", "Since Time", "", TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(sinceArg);
		untilArg = new StringToken("U", "Until Time", "", TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(untilArg);

		outArg = new StringToken("", "sdi:intv:tabsel:modid", "", 
			TokenOptions.optArgument|TokenOptions.optRequired
			|TokenOptions.optMultiple, "");
		cmdLineArgs.addToken(outArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new ReadTimeSeries();
		tp.execute(args);
	}

	protected void runApp()
		throws Exception
	{
		String outTS = outArg.getValue();

		CTimeSeries ts = makeTimeSeries(outTS);
		ts.setComputationId(DbKey.createDbKey(1));

		Date since;
		String s = sinceArg.getValue().trim();
		if (s.length() > 0)
			since = IDateFormat.parse(s);
		else
			since = new Date(System.currentTimeMillis() - (3600000L * 48));

		Date until;
		s = untilArg.getValue().trim();
		if (s.length() > 0)
			until = IDateFormat.parse(s);
		else
			until = new Date();

		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		int n = timeSeriesDAO.fillTimeSeries(ts, since, until);
		timeSeriesDAO.close();
		
		TimeSeriesIdentifier tsid = ts.getTimeSeriesIdentifier();
		if (tsid.getSite() != null)
			for(Iterator<SiteName> snit = tsid.getSite().getNames(); snit.hasNext(); )
			{
				SiteName sn = snit.next();
				System.out.println("Site Name: " + sn);
			}

		System.out.println("Data Type: " + tsid.getDataType());
		System.out.println("Number of values: " + n);

		SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy/MM/dd-HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		for(int i=0; i<ts.size(); i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			System.out.println( 
				sdf.format(tv.getTime()) + ","
				+ tv.getDoubleValue() + ",0x" 
				+ Integer.toHexString(tv.getFlags()));
		}
	}
	
	public void initDecodes()
	throws DecodesException
	{
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(
			cmdLineArgs.getPropertiesFile());
	}

	private static CTimeSeries makeTimeSeries(String x)
		throws Exception
	{
		StringTokenizer st = new StringTokenizer(x, ":");
		DbKey sdi = DbKey.createDbKey(Long.parseLong(st.nextToken()));
		String intv = st.hasMoreTokens() ? st.nextToken() : null;
		String tabsel = st.hasMoreTokens() ? st.nextToken() : null;
		CTimeSeries ret = new CTimeSeries(sdi, intv, tabsel);
		if (st.hasMoreTokens())
		{
			int modid = Integer.parseInt(st.nextToken());
			ret.setModelRunId(modid);
		}
		return ret;
	}
}
