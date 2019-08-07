/*
 * $Id$
 * 
 * $Log$
 * Revision 1.1  2017/08/22 19:49:55  mmaloney
 * Refactor
 *
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
*/
package decodes.tsdb.alarm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import opendcs.dai.AlarmDAI;
import opendcs.dai.TimeSeriesDAI;
import ilex.cmdline.*;
import ilex.util.Logger;
import lrgs.gui.DecodesInterface;
import decodes.db.Database;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;

public class ShowAlarms
	extends TsdbAppTemplate
{
	private StringToken timezoneArg = new StringToken("Z", "Time Zone", "", TokenOptions.optSwitch, "UTC");
	private StringToken outArg = new StringToken("", "time-series-IDs", "", 
		TokenOptions.optArgument |TokenOptions.optMultiple, "");
	private static TimeZone tz = null;
	private static SimpleDateFormat sdf = null;

	public ShowAlarms()
	{
		super("util.log");
		setSilent(true);
	}

	public static void main(String args[])
		throws Exception
	{
		new ShowAlarms().execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(timezoneArg);
		cmdLineArgs.addToken(outArg);
	}

	@Override
	protected void runApp() 
		throws DbIoException, BadTimeSeriesException, NoSuchObjectException, IOException 
	{
		tz = TimeZone.getTimeZone(timezoneArg.getValue());
		sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		sdf.setTimeZone(tz);
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(4);
		nf.setGroupingUsed(false);

		ArrayList<TimeSeriesIdentifier> tsids = new ArrayList<TimeSeriesIdentifier>();
		for(int n = outArg.NumberOfValues(), i=0; i<n; i++)
		{
			String outTS = outArg.getValue(i);
			if (outTS == null || outTS.trim().length() == 0)
				continue;
			
			TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
			
			try
			{
				TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(outTS);
				tsids.add(tsid);
			}
			catch(NoSuchObjectException ex)
			{
				Logger.instance().warning("No time series for '" + outTS + "': " + ex + " -- skipped.");
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}

		AlarmDAI alarmDAO = theDb.makeAlarmDAO();
		try
		{
			HashMap<DbKey, Alarm> alarmMap = new HashMap<DbKey, Alarm>();
			alarmDAO.refreshCurrentAlarms(alarmMap);
			ArrayList<Alarm> alarmHistory = alarmDAO.readAlarmHistory(tsids);
			
			System.out.println("All times in " + tz.getID());
			System.out.println();
			System.out.println("Current Alarms (" + alarmMap.values().size() + "):");
			System.out.println("tsid,screening,season,assertion,value,data_time,flags,msg,last_notify");
			for(Alarm al : alarmMap.values())
			{
				if (tsids.size() > 0)
				{
					boolean found = false;
					for(TimeSeriesIdentifier tsid : tsids)
						if (al.getTsidKey().equals(tsid.getKey()))
						{
							found = true;
							break;
						}
					if (!found)
						continue;
					AlarmScreening scrn =
						al.getLimitSet() == null ? null : alarmDAO.getScreening(al.getLimitSet().getScreeningId());
					System.out.println(al.getTsid().getUniqueString() + ", "
						+ (scrn == null ? "null" : scrn.getScreeningName()) + ", "
						+ (al.getLimitSet() == null ? "null" : 
							al.getLimitSet().getSeasonName() == null ? "default" :
							al.getLimitSet().getSeasonName()) + ", "
						+ sdf.format(al.getAssertTime()) + ", "
						+ nf.format(al.getDataValue()) + ", "
						+ sdf.format(al.getDataTime()) + ", "
						+ "0x" + Integer.toHexString(al.getAlarmFlags()) + ", "
						+ al.getMessage() + ", "
						+ (al.getLastNotificationTime() == null ? "never" : sdf.format(al.getLastNotificationTime())));
				}
			}
			
			if (alarmHistory.size() > 0)
			{
				System.out.println("Historical Alarms (" + alarmHistory.size() + "):");
				System.out.println("tsid,screening,season,assertion,value,data_time,flags,msg,end_time,cancelled_by");
				for(Alarm al : alarmHistory)
				{
					if (tsids.size() > 0)
					{
						boolean found = false;
						for(TimeSeriesIdentifier tsid : tsids)
							if (al.getTsidKey().equals(tsid.getKey()))
							{
								found = true;
								break;
							}
						if (!found)
						{
							warning("Alarm for tsid key=" + al.getTsidKey() + ", but not TSID in database -- skipped.");
							continue;
						}
					}
					AlarmScreening scrn =
						al.getLimitSet() == null ? null : alarmDAO.getScreening(al.getLimitSet().getScreeningId());
					System.out.println(al.getTsid().getUniqueString() + ", "
						+ (scrn == null ? "null" : scrn.getScreeningName()) + ", "
						+ (al.getLimitSet() == null ? "null" : 
							al.getLimitSet().getSeasonName() == null ? "default" :
							al.getLimitSet().getSeasonName()) + ", "
						+ sdf.format(al.getAssertTime()) + ", "
						+ nf.format(al.getDataValue()) + ", "
						+ sdf.format(al.getDataTime()) + ", "
						+ "0x" + Integer.toHexString(al.getAlarmFlags()) + ", "
						+ al.getMessage() + ", "
						+ sdf.format(al.getEndTime()) + ", "
						+ (al.getCancelledBy() == null ? "" : al.getCancelledBy()));
				}
			}
		}
		finally
		{
			alarmDAO.close();
		}
	}
	
	@Override
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		Database db = Database.getDb();
		db.enumList.read();
		db.dataTypeSet.read();
		Site.explicitList = true;
		db.siteList.read();
	}


}
