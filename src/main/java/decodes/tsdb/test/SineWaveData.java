package decodes.tsdb.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import java.text.NumberFormat;

/**
 * Generates test data for regression tests as a sine wave with a period of 1 day.
 * Arguments:
 *    args[0]: Start Date in format YYYY/MM/DD
 *    args[1]: # minutes for interval
 *    args[2]: Time Zone
 *    args[3]: # days (periods
 *    args[4]: min
 *    args[5]: max
 */
public class SineWaveData
{
	
	public static void main(String[] args)
		throws Exception
	{
		if (args.length != 6)
		{
			System.err.println("Usage: java decodes.tsdb.test.SineWaveData YYYY/MM/DD #Minutes TZ #days min max");
			System.exit(1);
		}
		
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(args[2]));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		sdf.setTimeZone(TimeZone.getTimeZone(args[2]));
		Date start = sdf.parse(args[0]);
		cal.setTime(start);
		int intervalMinutes = Integer.parseInt(args[1]);
		int numDays = Integer.parseInt(args[3]);
		double min = Double.parseDouble(args[4]);
		double max = Double.parseDouble(args[5]);
		
		sdf.applyPattern("yyyy/MM/dd-HH:mm:ss");

		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(3);
		nf.setGroupingUsed(false);
		int intervalsPerDay = 24 * 60 / intervalMinutes;
		double angleIncrement = 2.0 * Math.PI / intervalsPerDay;
		double angle = 0.0;
		for(int idx = 0; idx < numDays * intervalsPerDay; idx++)
		{
			System.out.println(sdf.format(cal.getTime()) + "," 
				+ nf.format(min + (max-min)/2*(Math.sin(angle)+1)) 
				+ ",0");
		
			angle += angleIncrement;
			cal.add(Calendar.MINUTE, intervalMinutes);
		}

	}

}
