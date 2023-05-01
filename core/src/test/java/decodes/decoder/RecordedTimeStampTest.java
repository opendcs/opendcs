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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import ilex.util.CmdLine;
import ilex.util.CmdLineProcessor;
import org.jfree.data.time.Month;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Stand-alone test program for RecordedTimeStamp.
 */
class RecordedTimeStampTest
{

	private static RecordedTimeStamp setup(CmdLineProcessor clp)
	{
		clp.addHelpAndQuitCommands();
		RecordedTimeStamp rts = new RecordedTimeStamp();
		clp.addCmd(new CmdLine("y", "set year")
		{
			public void execute(String[] tok)
			{
				int y = Integer.parseInt(tok[1]);
				rts.setYear(y);
			}
		});
		clp.addCmd(new CmdLine("tz", "set time zone")
		{
			public void execute(String[] tok)
			{
				String tz = tok[1];
				if(tok.length > 2)
				{
					tz = tz + " " + tok[2];
				}
				rts.setTimeZoneName(tz);
			}
		});

		clp.addCmd(new CmdLine("h", "set hour")
		{
			public void execute(String[] tok)
			{
				int i = Integer.parseInt(tok[1]);
				rts.setHour(i);
			}
		});


		clp.addCmd(new CmdLine("m", "set minute")
		{
			public void execute(String[] tok)
			{
				int i = Integer.parseInt(tok[1]);
				rts.setMinute(i);
			}
		});

		clp.addCmd(new CmdLine("d", "set day-of-year")
		{
			public void execute(String[] tok)
			{
				int i = Integer.parseInt(tok[1]);
				rts.setDayOfYear(i);
			}
		});

		clp.addCmd(new CmdLine("s", "set second")
		{
			public void execute(String[] tok)
			{
				int i = Integer.parseInt(tok[1]);
				rts.setSecond(i);
			}
		});

		clp.addCmd(new CmdLine("mo", "set Month")
		{
			public void execute(String[] tok)
			{
				int i = Integer.parseInt(tok[1]);
				rts.setMonth(i);
			}
		});

		clp.addCmd(new CmdLine("dm", "set day-of-month")
		{
			public void execute(String[] tok)
			{
				int i = Integer.parseInt(tok[1]);
				rts.setDayOfMonth(i);
			}
		});

		clp.addCmd(new CmdLine("a", "set AM/PM")
		{
			public void execute(String[] tok)
			{
				boolean tf = tok[1].charAt(0) == 'P' || tok[1].charAt(0) == 'p';
				rts.setPM(tf);
			}
		});
		return rts;
	}

	@Test
	void testSetDayOfYear() throws Exception
	{
		String initialString = new StringBuilder("y 1991")
				.append("\n")
				.append("d 36")
				.append("\n")
				.append("tz America/Los_Angeles")
				.append("\n")
				.append("h 11")
				.append("\n")
				.append("m 59")
				.append("\n")
				.append("s 31")
				.append("\n")
				.append("a AM")
				.toString();
		InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
		CmdLineProcessor clp = new CmdLineProcessor(targetStream);
		RecordedTimeStamp rts = setup(clp);
		clp.processInput();
		ZonedDateTime zonedDateTime = rts.getCalendar().toZonedDateTime();
		ZonedDateTime expected = ZonedDateTime.of(1991, Month.FEBRUARY, 5, 11, 59, 31, 0, ZoneId.of("America/Los_Angeles"));
		assertEquals(expected, zonedDateTime);
	}

	@Test
	void testSetDayOfMonth() throws Exception
	{
		String initialString = new StringBuilder("y 1991")
				.append("\n")
				.append("tz America/Los_Angeles")
				.append("\n")
				.append("h 11")
				.append("\n")
				.append("m 59")
				.append("\n")
				.append("s 31")
				.append("\n")
				.append("mo 5")
				.append("\n")
				.append("dm 5")
				.append("\n")
				.append("a AM")
				.toString();
		InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
		CmdLineProcessor clp = new CmdLineProcessor(targetStream);
		RecordedTimeStamp rts = setup(clp);
		clp.processInput();
		ZonedDateTime zonedDateTime = rts.getCalendar().toZonedDateTime();
		ZonedDateTime expected = ZonedDateTime.of(1991, Month.MAY, 5, 11, 59, 31, 0, ZoneId.of("America/Los_Angeles"));
		assertEquals(expected, zonedDateTime);
	}

	@Test
	void testSetAmPm() throws Exception
	{
		String initialString = new StringBuilder("y 1991")
				.append("\n")
				.append("d 36")
				.append("\n")
				.append("tz America/Los_Angeles")
				.append("\n")
				.append("h 11")
				.append("\n")
				.append("m 59")
				.append("\n")
				.append("s 31")
				.append("\n")
				.append("mo 5")
				.append("\n")
				.append("a PM")
				.toString();
		InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
		CmdLineProcessor clp = new CmdLineProcessor(targetStream);
		RecordedTimeStamp rts = setup(clp);
		clp.processInput();
		ZonedDateTime zonedDateTime = rts.getCalendar().toZonedDateTime();
		ZonedDateTime expected = ZonedDateTime.of(1991, Month.FEBRUARY, 5, 23, 59, 31, 0, ZoneId.of("America/Los_Angeles"));
		assertEquals(expected, zonedDateTime);
	}
}
