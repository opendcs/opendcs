package decodes.dcpmon;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import opendcs.dai.XmitRecordDAI;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

/**
 * Tabulate and print statistics for a single day.
 * @author mmaloney Mike Maloney, Cove Software, LLC.
 */
public class PrintStats extends TsdbAppTemplate
{
	StringToken dateToken = new StringToken("date",
		"Date for which to generate stats (mm/dd/yyyy)", "", TokenOptions.optSwitch, null);
	public static final long MSEC_PER_DAY = (24L * 60L * 60L * 1000L);
	public static final int SEC_PER_DAY = 24 * 60 * 60;

	class ChanHour
	{
		int chan;
		int numMessages[];
		int total = 0;
		ChanHour(int chan)
		{
			this.chan = chan;
			numMessages = new int[24];
			for(int i=0; i<24; i++)
				numMessages[i] = 0;
		}
		void bump(int hour)
		{
			numMessages[hour]++;
			total++;
		}
	}
	
	public PrintStats()
	{
		super("util.log");
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(dateToken);
	}

	@Override
	protected void runApp() throws Exception
	{
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String x = dateToken.getValue();
		Date day = new Date();
		if (x != null)
		{
			day = sdf.parse(x);
		}
		int dayNum = (int)(day.getTime() / MSEC_PER_DAY);
		if (x == null)
		{
			dayNum--; // default to yesterday if date not specified.
			day.setTime(dayNum * MSEC_PER_DAY);
		}
		
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		String suffix = xmitRecordDao.getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
		{
			System.out.println("Cannot get suffix for dayNum=" + dayNum + ", date=" + sdf.format(day));
			return;
		}
		String tab = "my_dcp_trans_" + suffix;
		String q = "SELECT goes_channel, carrier_start, carrier_end, dcp_address "
			+ " from " + tab
			+ " order by record_id";
		
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		ChanHour chanHour[] = new ChanHour[500];
		for(int i=0; i<500; i++)
			chanHour[i] = new ChanHour(i);
		ResultSet rs = xmitRecordDao.doQuery(q);
		
		int utilEast[] = new int[SEC_PER_DAY];
		int utilWest[] = new int[SEC_PER_DAY];
		for(int i=0; i<SEC_PER_DAY; i++)
			utilEast[i] = utilWest[i] = 0;
		HashSet<String> uniqueDcpAddrs = new HashSet<String>();
		
		int totalEast = 0;
		int totalWest = 0;
		while(rs.next())
		{
			int chan = rs.getInt(1);
			Date cstart = theDb.getFullDate(rs, 2);
			Date cstop = theDb.getFullDate(rs, 3);
			String dcpAddr = rs.getString(4);
			
			cal.setTime(cstart);
			chanHour[chan].bump(cal.get(Calendar.HOUR_OF_DAY));
			
			int sodStart = cal.get(Calendar.HOUR_OF_DAY) * 3600
				+ cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
			
			cal.setTime(cstop);
			int sodStop = cal.get(Calendar.HOUR_OF_DAY) * 3600
			+ cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
			
			if (chan % 2 == 1) // Odd channels are GOES East
			{
				totalEast++;
				for(int i = sodStart; i<SEC_PER_DAY && i<=sodStop; i++)
					utilEast[i]++;
			}
			else // Even chan is GOES West
			{
				totalWest++;
				for(int i = sodStart; i<SEC_PER_DAY && i<=sodStop; i++)
					utilWest[i]++;
			}
			
			uniqueDcpAddrs.add(dcpAddr);
		}

		System.out.println("GOES Transmission Statistics for " + sdf.format(day));
		System.out.println("Total Messages -- GOES East: " + totalEast + ", West: " + totalWest);
		System.out.println();
		
		// Print table with channel stats
		System.out.println("GOES EAST Number of messages per hour per channel");
		System.out.println("Channel,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,total");
		for(ChanHour ch : chanHour)
		{
			if (ch.chan % 2 == 0)
				continue;
			if (ch.total == 0)
				continue;
			System.out.print("" + ch.chan + ",");
			for(int i=0; i<24; i++)
				System.out.print("" + ch.numMessages[i] + ",");
			System.out.println("" + ch.total);
		}
		
		System.out.println();
		System.out.println("GOES WEST Number of messages per hour per channel");
		System.out.println("Channel,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,total");
		for(ChanHour ch : chanHour)
		{
			if (ch.chan % 2 == 1)
				continue;
			if (ch.total == 0)
				continue;
			System.out.print("" + ch.chan + ",");
			for(int i=0; i<24; i++)
				System.out.print("" + ch.numMessages[i] + ",");
			System.out.println("" + ch.total);
		}
		
		System.out.println();
		System.out.println("Utilization - Number of Carriers in each Second");
		System.out.println("Time,EAST,WEST");
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<SEC_PER_DAY; i++)
		{
			int h = i/3600;
			int m = (i - h*3600) / 60;
			int s = i % 60;
			sb.setLength(0);
			if (h < 10)
				sb.append('0');
			sb.append(h);
			sb.append(':');
			if (m < 10)
				sb.append('0');
			sb.append(m);
			sb.append(':');
			if (s < 10)
				sb.append('0');
			sb.append(s);
			sb.append(',');
			sb.append(utilEast[i]);
			sb.append(',');
			sb.append(utilWest[i]);
			System.out.println(sb.toString());
		}
		System.out.println();

		
		System.out.println("Utilization - Max number of Carriers in each 10-second window");
		System.out.println("Time,EAST,WEST");
		for(int i=0; i<SEC_PER_DAY; i += 10)
		{
			int eastMax=0;
			int westMax=0;
			for(int j=0; j<10; j++)
			{
				if (utilEast[i+j] > eastMax)
					eastMax = utilEast[i+j];
				if (utilWest[i+j] > westMax)
					westMax = utilWest[i+j];
			}

			int h = i/3600;
			int m = (i - h*3600) / 60;
			int s = i % 60;
			
			sb.setLength(0);
			if (h < 10)
				sb.append('0');
			sb.append(h);
			sb.append(':');
			if (m < 10)
				sb.append('0');
			sb.append(m);
			sb.append(':');
			if (s < 10)
				sb.append('0');
			sb.append(s);
			sb.append(',');
			sb.append(eastMax);
			sb.append(',');
			sb.append(westMax);
			System.out.println(sb.toString());
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		PrintStats app = new PrintStats();
		app.execute(args);
	}

}


