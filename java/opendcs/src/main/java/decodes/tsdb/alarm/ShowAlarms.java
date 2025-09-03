/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb.alarm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.AlarmDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import ilex.cmdline.*;
import ilex.util.TextUtil;
import lrgs.gui.DecodesInterface;
import decodes.db.Database;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;

public class ShowAlarms extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.atWarn().setCause(ex).log("No time series for '{}' -- skipped.", outTS);
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}

		class AlarmComparator
			implements Comparator<Alarm>
		{
			/**
			 * For sorting by TSID alphabetically and then by data time (descending).
			 */
			public int compare(Alarm a1, Alarm a2)
			{
				int r = TextUtil.strCompareIgnoreCase(a1.getTsid().getUniqueString(), a2.getTsid().getUniqueString());
				if (r != 0)
					return r;
				long rl = a1.getDataTime().getTime() - a2.getDataTime().getTime();
				return rl < 0 ? 1 : rl > 0 ? -1 : 0; // by date descending
			}
		};
		AlarmComparator alarmComparator = new AlarmComparator();

		AlarmDAI alarmDAO = theDb.makeAlarmDAO();
		LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
		try
		{
			ArrayList<Alarm> curAlarms = alarmDAO.getAllCurrentAlarms();
			ArrayList<Alarm> alarmHistory = alarmDAO.readAlarmHistory(tsids);

			System.out.println("All times in " + tz.getID());
			System.out.println();
			System.out.println("Current Alarms (" + curAlarms.size() + "):");

			boolean incAppId = theDb.tsdbVersion >= TsdbDatabaseVersion.VERSION_68;
			HashMap<DbKey, CompAppInfo> apps = new HashMap<DbKey, CompAppInfo>();
			for(CompAppInfo app : loadingAppDAO.listComputationApps(false))
				apps.put(app.getAppId(), app);

			System.out.println("tsid,screening,season,assertion,value,data_time,flags,msg,last_notify"
				+ (incAppId ? ",loading_app" : ""));

			Collections.sort(curAlarms, alarmComparator);
			for(Alarm al : curAlarms)
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
				}
				AlarmScreening scrn =
					al.getLimitSet() == null ? null : alarmDAO.getScreening(al.getLimitSet().getScreeningId());

				CompAppInfo appInfo = apps.get(scrn.getAppId());

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
					+ (al.getLastNotificationTime() == null ? "never" : sdf.format(al.getLastNotificationTime()))
					+ (appInfo != null ? (", " + appInfo.getAppName()) : "")
					);
			}

			System.out.println();
			Collections.sort(alarmHistory, alarmComparator);
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
							log.warn("Alarm for tsid key={}, but not TSID in database -- skipped.", al.getTsidKey());
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
			loadingAppDAO.close();
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
