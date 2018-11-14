package decodes.tsdb;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TestTimedCompParse
{

	public TestTimedCompParse()
	{
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			System.err.println("Usage: java decodes.tsdb.TestTimedCompParse timezone interval [offset]");
			System.exit(1);
		}
		TimeZone dbtz = TimeZone.getTimeZone(args[0]);
		System.out.println("Timezone = " + dbtz.getID());
		String timedCompInterval = args[1];
		String timedCompOffset = args.length > 2 ? args[2] : null;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		sdf.setTimeZone(dbtz);
		Date now = new Date();
		System.out.println("Current time: " + sdf.format(now));
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(dbtz);
		Date nextExec = ComputationApp.computeNextRunTime(timedCompInterval, timedCompOffset, cal, now);
		System.out.println("Next exec will be " + sdf.format(nextExec));

	}

}
