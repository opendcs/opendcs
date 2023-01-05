package decodes.tsdb;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.var.TimedVariable;

import java.util.Date;

import lrgs.gui.DecodesInterface;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import decodes.db.Site;
import decodes.util.CmdLineArgs;

public class Gen5Min extends TsdbAppTemplate
{
	public static final String module = "Gen5Min";
	/** One or more input files specified on end of command line */
	private StringToken tsidArg = new StringToken("", "Time Series Identifier", "", 
		TokenOptions.optArgument|TokenOptions.optRequired, "");
	private StringToken unitsArg = new StringToken("u", "Units (default = ft)", "", 
		TokenOptions.optSwitch, "ft");

	private CTimeSeries currentTS = null;
	private SiteDAI siteDAO = null;
	TimeSeriesDAI timeSeriesDAO = null;

	public Gen5Min()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}

	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(unitsArg);
		cmdLineArgs.addToken(tsidArg);
		appNameArg.setDefaultValue("utility");
	}

	@Override
	protected void runApp()
		throws Exception
	{
		siteDAO = theDb.makeSiteDAO();
		timeSeriesDAO = theDb.makeTimeSeriesDAO();
		
		String tsidStr = tsidArg.getValue();
		String units = unitsArg.getValue();
		
		System.out.println("Will write to TSID '" + tsidStr + "' with units '" + units + "' at 5 min interval.");
		System.out.print("OK to proceed? (y/n)");
		String s = System.console().readLine();
		if (s.charAt(0) != 'y')
			System.exit(0);
		System.out.println("Working. Hit CTRL-C to stop.");
		
		try
		{
			currentTS = theDb.makeTimeSeries(tsidStr);
		}
		catch (NoSuchObjectException ex)
		{
			warning("No existing time series '" + tsidStr + "'. Will attempt to create.");
			TimeSeriesIdentifier tsid = theDb.makeEmptyTsId();
			try
			{
				tsid.setUniqueString(tsidStr);
				Site site = theDb.getSiteById(siteDAO.lookupSiteID(tsid.getSiteName()));
				tsid.setSite(site);
				System.out.println("Calling createTimeSeries with tsid '" + tsid.getUniqueString() + "'");
				timeSeriesDAO.createTimeSeries(tsid);
				System.out.println("After createTimeSeries, ts key = " + tsid.getKey());
			}
			catch(Exception ex2)
			{
				System.err.println("No such time series and cannot create for '" + tsidStr + "': " + ex2);
				ex2.printStackTrace();
				System.exit(1);
			}
		}
		currentTS.setUnitsAbbr(units);
		
		long writeIntervalMsec = 5 * 60 * 1000L;
		long nextWriteAt = ((System.currentTimeMillis() / writeIntervalMsec) + 1) * writeIntervalMsec;
		
		double value = 10.0;
		System.out.println("Next write at " + new Date(nextWriteAt));
		while(true)
		{
			if (System.currentTimeMillis() > nextWriteAt)
			{
				// Add random hundreth of a unit
				value = value + (double)((long)((Math.random()-.5) * 100)) / 100.0;
				currentTS.deleteAll();
				TimedVariable tv = new TimedVariable(new Date(nextWriteAt), value, 0);
				VarFlags.setToWrite(tv);
				currentTS.addSample(tv);
				System.out.println("Saving value " + value + " at time " + new Date(nextWriteAt));
				timeSeriesDAO.saveTimeSeries(currentTS);

				nextWriteAt += writeIntervalMsec;
			}
			Thread.sleep(1000L);
		}
		
		
//		timeSeriesDAO.close();
//		siteDAO.close();
	}

	public static void main(String args[])
		throws Exception
	{
		Gen5Min app = new Gen5Min();
		app.execute(args);
	}
}
