/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/31 16:31:21  mjmaloney
*  javadoc
*
*  Revision 1.2  2003/12/10 20:35:02  mjmaloney
*  Modified time-stamping to support usgs-style time zones.
*
*  Revision 1.1  2003/11/29 17:40:39  mjmaloney
*  Several modifications to the way time-stamping is done. These were necessary
*  to support EDL files. Time stamping is now done all in one pass. All time/date
*  operators are now supported.
*
*/
package decodes.decoder;

import java.util.*;
import ilex.util.*;

/**
Stand-alone test program for RecordedTimeStamp.
*/
public class RecordedTimeStampTest
{
	public RecordedTimeStamp rts;

	public RecordedTimeStampTest()
	{
		rts = new RecordedTimeStamp();
	}

	public void run()
		throws Exception
	{
		CmdLineProcessor clp = new CmdLineProcessor(System.in);
		clp.addHelpAndQuitCommands();
		final RecordedTimeStampTest parent = this;
		clp.addCmd(
			new CmdLine("y", "set year")
			{
				public void execute(String[] tok)
				{
					int y = Integer.parseInt(tok[1]);
					System.out.println("Setting year to " + y);
					parent.rts.setYear(y);
					parent.show();
				}
			});

		clp.addCmd(
			new CmdLine("tz", "set time zone")
			{
				public void execute(String[] tok)
				{
					String tz = tok[1];
					if (tok.length > 2)
						tz = tz + " " + tok[2];
					System.out.println("Setting time zone to " 
						+ tz + ", and resetting timer.");
					parent.rts.setTimeZoneName(tz);
					parent.rts.reset();
					parent.show();
				}
			});

		clp.addCmd(
			new CmdLine("h", "set hour")
			{
				public void execute(String[] tok)
				{
					int i = Integer.parseInt(tok[1]);
					System.out.println("Setting hour to " + i);
					parent.rts.setHour(i);
					parent.show();
				}
			});


		clp.addCmd(
			new CmdLine("m", "set minute")
			{
				public void execute(String[] tok)
				{
					int i = Integer.parseInt(tok[1]);
					System.out.println("Setting mintue to " + i);
					parent.rts.setMinute(i);
					parent.show();
				}
			});

		clp.addCmd(
			new CmdLine("d", "set day-of-year")
			{
				public void execute(String[] tok)
				{
					int i = Integer.parseInt(tok[1]);
					System.out.println("Setting day-of-year to " + i);
					parent.rts.setDayOfYear(i);
					parent.show();
				}
			});

		clp.addCmd(
			new CmdLine("s", "set second")
			{
				public void execute(String[] tok)
				{
					int i = Integer.parseInt(tok[1]);
					System.out.println("Setting second to " + i);
					parent.rts.setSecond(i);
					parent.show();
				}
			});

		clp.addCmd(
			new CmdLine("mo", "set Month")
			{
				public void execute(String[] tok)
				{
					int i = Integer.parseInt(tok[1]);
					System.out.println("Setting Month to " + i);
					parent.rts.setMonth(i);
					parent.show();
				}
			});

		clp.addCmd(
			new CmdLine("dm", "set day-of-month")
			{
				public void execute(String[] tok)
				{
					int i = Integer.parseInt(tok[1]);
					System.out.println("Setting day-of-month to " + i);
					parent.rts.setDayOfMonth(i);
					parent.show();
				}
			});

		clp.addCmd(
			new CmdLine("a", "set AM/PM")
			{
				public void execute(String[] tok)
				{
					boolean tf = tok[1].charAt(0) == 'P' || tok[1].charAt(0) == 'p';
					System.out.println("Setting PM flag to to " + tf);
					parent.rts.setPM(tf);
					parent.show();
				}
			});


		clp.processInput();
	}

	public void show()
	{
		System.out.println("TZ: " + rts.getTimeZoneName()
			+ "    Status=" + rts.getStatus());
		System.out.println("Seconds since epoch: " + (rts.getMsec() / 1000L));
		System.out.println("Seconds to start of this year: " + (rts.getYearMsecOffset() / 1000L));
		System.out.println("Seconds to start of this day: " + (rts.getDayMsecOffset() / 1000L));
		long msec = rts.getMsec();
		System.out.println("Value: " + new Date(msec) + ", msec=" + msec);
		System.out.println();
	}

	public static void main(String args[])
		throws Exception
	{
		RecordedTimeStampTest rtst = new RecordedTimeStampTest();
		rtst.run();
	}
}
