package decodes.tsdb;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimedCompParseTest
{
	@Test
	void testTimedCompParse()
	{
		String timeZoneId = "America/Los_Angeles";
		TimeZone dbtz = TimeZone.getTimeZone(timeZoneId);
		String timedCompInterval = "1d";
		String timedCompOffset = "30m";
		ZonedDateTime now = ZonedDateTime.of(2023, 2, 1, 5, 1, 1, 0, ZoneId.of(timeZoneId));
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(dbtz);
		Date nextExec = ComputationApp.computeNextRunTime(timedCompInterval, timedCompOffset, cal, Date.from(now.toInstant()));
		ZonedDateTime expected = ZonedDateTime.of(2023, 2, 2, 0, 30, 0, 0, ZoneId.of(timeZoneId));
		assertEquals(expected.toInstant(), nextExec.toInstant());
	}

}
