package decodes.cwms.validation;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import decodes.cwms.CwmsFlags;
import decodes.cwms.CwmsTsId;
import decodes.tsdb.CTimeSeries;
import ilex.var.TimedVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Database-free checks of ScreeningCriteria.doChecks and Screening season selection.
 *
 * Each input series is built by hand relative to a fixed evaluation time T, so the
 * expected flags can be worked out from the check limits written beside each test.
 */
final class ScreeningCriteriaTest
{
	private static final long MINUTE_MS = 60_000L;
	private static final long HOUR_MS = 60 * MINUTE_MS;
	/** 2024-01-01T12:00:00Z */
	private static final Date T = new Date(1_704_110_400_000L);

	private static final String HOURLY = "TestLoc.Stage.Inst.hour.0.test";
	private static final String FIFTEEN_MINUTE = "TestLoc.Stage.Inst.minute*15.0.test";
	private static final String TWELVE_HOUR = "TestLoc.Stage.Inst.hour*12.0.test";
	private static final String HOURLY_INCREMENTAL = "TestLoc.Precip.Total.hour.hour.test";

	private static final int OKAY = CwmsFlags.SCREENED | CwmsFlags.VALIDITY_OKAY;
	private static final int QUESTIONABLE = CwmsFlags.SCREENED | CwmsFlags.VALIDITY_QUESTIONABLE;
	private static final int REJECTED = CwmsFlags.SCREENED | CwmsFlags.VALIDITY_REJECTED;

	@Test
	void absoluteValueWithinLimitsIsOkay()
	{
		assertEquals(OKAY, check(absoluteCriteria(), series(HOURLY), 50.0));
	}

	@ParameterizedTest
	@ValueSource(doubles = {5.0, 95.0})
	void absoluteValueOutsideQuestionLimitsIsQuestionable(double value)
	{
		// Questionable range is [10, 90]; reject range is [0, 100].
		assertEquals(QUESTIONABLE | CwmsFlags.TEST_ABSOLUTE_VALUE, check(absoluteCriteria(), series(HOURLY), value));
	}

	@ParameterizedTest
	@ValueSource(doubles = {-1.0, 101.0})
	void rejectWinsOverQuestionableRegardlessOfCheckOrder(double value)
	{
		ScreeningCriteria rejectFirst = new ScreeningCriteria();
		rejectFirst.addAbsCheck(new AbsCheck('R', 0.0, 100.0));
		rejectFirst.addAbsCheck(new AbsCheck('Q', 10.0, 90.0));

		ScreeningCriteria questionFirst = new ScreeningCriteria();
		questionFirst.addAbsCheck(new AbsCheck('Q', 10.0, 90.0));
		questionFirst.addAbsCheck(new AbsCheck('R', 0.0, 100.0));

		assertEquals(REJECTED | CwmsFlags.TEST_ABSOLUTE_VALUE, check(rejectFirst, series(HOURLY), value));
		assertEquals(REJECTED | CwmsFlags.TEST_ABSOLUTE_VALUE, check(questionFirst, series(HOURLY), value));
	}

	@Test
	void missingValueIsFlaggedMissingWithoutRunningChecks()
	{
		// Current behavior: a missing value returns VALIDITY_MISSING alone, without the SCREENED bit.
		assertEquals(CwmsFlags.VALIDITY_MISSING, check(absoluteCriteria(), series(HOURLY), Double.NEGATIVE_INFINITY));
	}

	@ParameterizedTest
	@ValueSource(doubles = {6.0, 14.0})
	void hourlyRateOfChangeWithinLimitsIsOkay(double value)
	{
		// Previous value 10 one hour earlier; allowed change is -5..+5 per hour.
		CTimeSeries input = series(HOURLY);
		add(input, HOUR_MS, 10.0);
		add(input, 0, value);
		assertEquals(OKAY, check(rocCriteria(), input, value));
	}

	@ParameterizedTest
	@ValueSource(doubles = {4.0, 16.0})
	void hourlyRateOfChangeOutsideLimitsIsQuestionable(double value)
	{
		// Previous value 10 one hour earlier; a change of 6 exceeds the 5 per hour limit.
		CTimeSeries input = series(HOURLY);
		add(input, HOUR_MS, 10.0);
		add(input, 0, value);
		assertEquals(QUESTIONABLE | CwmsFlags.TEST_RATE_OF_CHANGE, check(rocCriteria(), input, value));
	}

	@Test
	void rateOfChangeIsSkippedWhenPreviousValueIsRejected()
	{
		CTimeSeries input = series(HOURLY);
		add(input, HOUR_MS, 10.0, CwmsFlags.VALIDITY_REJECTED);
		add(input, 0, 16.0);
		assertEquals(OKAY, check(rocCriteria(), input, 16.0));
	}

	@Test
	void rateOfChangeIsSkippedWhenThereIsNoPreviousValue()
	{
		CTimeSeries input = series(HOURLY);
		add(input, 0, 16.0);
		assertEquals(OKAY, check(rocCriteria(), input, 16.0));
	}

	@Test
	void subHourlyRateOfChangeComparesAgainstTheValueOneHourEarlier()
	{
		// The last 15-minute step is only +0.5, but the change over the hour is 16 - 10 = 6,
		// which exceeds the 5 per hour limit.
		CTimeSeries input = series(FIFTEEN_MINUTE);
		add(input, 60 * MINUTE_MS, 10.0);
		add(input, 45 * MINUTE_MS, 11.0);
		add(input, 30 * MINUTE_MS, 12.0);
		add(input, 15 * MINUTE_MS, 15.5);
		add(input, 0, 16.0);
		assertEquals(QUESTIONABLE | CwmsFlags.TEST_RATE_OF_CHANGE, check(rocCriteria(), input, 16.0));
	}

	@Test
	void twelveHourRateOfChangeWithinPerHourLimitIsOkay()
	{
		// (70 - 10) / 12 hours = 5 per hour, exactly at the limit.
		CTimeSeries input = series(TWELVE_HOUR);
		add(input, 12 * HOUR_MS, 10.0);
		add(input, 0, 70.0);
		assertEquals(OKAY, check(rocCriteria(), input, 70.0));
	}

	@Test
	void twelveHourRateOfChangeOutsidePerHourLimitIsQuestionable()
	{
		// (100 - 10) / 12 hours = 7.5 per hour, above the 5 per hour limit.
		CTimeSeries input = series(TWELVE_HOUR);
		add(input, 12 * HOUR_MS, 10.0);
		add(input, 0, 100.0);
		assertEquals(QUESTIONABLE | CwmsFlags.TEST_RATE_OF_CHANGE, check(rocCriteria(), input, 100.0));
	}

	@Test
	void constantValueForTheWholeDurationIsFlagged()
	{
		CTimeSeries input = series(HOURLY);
		for (int hoursBefore = 3; hoursBefore >= 0; hoursBefore--)
		{
			add(input, hoursBefore * HOUR_MS, 5.0);
		}
		assertEquals(QUESTIONABLE | CwmsFlags.TEST_CONSTANT_VALUE, check(constCriteria(), input, 5.0));
	}

	@Test
	void changingValueIsNotFlaggedConstant()
	{
		CTimeSeries input = series(HOURLY);
		add(input, 3 * HOUR_MS, 5.0);
		add(input, 2 * HOUR_MS, 5.0);
		add(input, HOUR_MS, 5.0);
		add(input, 0, 6.0);
		assertEquals(OKAY, check(constCriteria(), input, 6.0));
	}

	@Test
	void constantValueShorterThanTheDurationIsNotFlagged()
	{
		// Only two hours of flat data for a three hour check.
		CTimeSeries input = series(HOURLY);
		add(input, 2 * HOUR_MS, 5.0);
		add(input, HOUR_MS, 5.0);
		add(input, 0, 5.0);
		assertEquals(OKAY, check(constCriteria(), input, 5.0));
	}

	@Test
	void incrementalAccumulationAboveDurationLimitIsRejected()
	{
		// 1.0 + 1.0 + 1.0 = 3.0 over three hours, above the 2.0 limit.
		CTimeSeries input = series(HOURLY_INCREMENTAL);
		add(input, 2 * HOUR_MS, 1.0);
		add(input, HOUR_MS, 1.0);
		add(input, 0, 1.0);
		assertEquals(REJECTED | CwmsFlags.TEST_DURATION_VALUE, check(durMagCriteria(), input, 1.0));
	}

	@Test
	void incrementalAccumulationWithinDurationLimitIsOkay()
	{
		// 0.5 + 0.5 + 0.5 = 1.5 over three hours, within the 0..2.0 limit.
		CTimeSeries input = series(HOURLY_INCREMENTAL);
		add(input, 2 * HOUR_MS, 0.5);
		add(input, HOUR_MS, 0.5);
		add(input, 0, 0.5);
		assertEquals(OKAY, check(durMagCriteria(), input, 0.5));
	}

	@Test
	void screeningSelectsCriteriaForTheSeasonContainingTheDate()
	{
		ScreeningCriteria winter = new ScreeningCriteria();
		winter.setSeasonStart(Calendar.JANUARY, 1);
		ScreeningCriteria summer = new ScreeningCriteria();
		summer.setSeasonStart(Calendar.JUNE, 1);

		Screening screening = new Screening();
		screening.add(summer);
		screening.add(winter);

		TimeZone utc = TimeZone.getTimeZone("UTC");
		assertSame(winter, screening.findForDate(utcDate(2024, Calendar.MARCH, 15), utc));
		assertSame(summer, screening.findForDate(utcDate(2024, Calendar.JUNE, 1), utc));
		assertSame(summer, screening.findForDate(utcDate(2024, Calendar.DECEMBER, 31), utc));
	}

	private static ScreeningCriteria absoluteCriteria()
	{
		ScreeningCriteria crit = new ScreeningCriteria();
		crit.addAbsCheck(new AbsCheck('R', 0.0, 100.0));
		crit.addAbsCheck(new AbsCheck('Q', 10.0, 90.0));
		return crit;
	}

	private static ScreeningCriteria rocCriteria()
	{
		ScreeningCriteria crit = new ScreeningCriteria();
		crit.addRocPerHourCheck(new RocPerHourCheck('Q', -5.0, 5.0));
		return crit;
	}

	private static ScreeningCriteria constCriteria()
	{
		ScreeningCriteria crit = new ScreeningCriteria();
		crit.addConstCheck(new ConstCheck('Q', "hour*3", 0.0, 0.01, 0));
		return crit;
	}

	private static ScreeningCriteria durMagCriteria()
	{
		ScreeningCriteria crit = new ScreeningCriteria();
		crit.addDurCheckPeriod(new DurCheckPeriod('R', "hour*3", 0.0, 2.0));
		return crit;
	}

	private static int check(ScreeningCriteria crit, CTimeSeries input, double value)
	{
		return crit.doChecks(null, input, T, new ScreeningAlgorithm(), value);
	}

	private static CTimeSeries series(String tsidStr)
	{
		CwmsTsId tsid = new CwmsTsId();
		tsid.setUniqueString(tsidStr);
		return new CTimeSeries(tsid);
	}

	private static void add(CTimeSeries ts, long msBeforeT, double value)
	{
		add(ts, msBeforeT, value, 0);
	}

	private static void add(CTimeSeries ts, long msBeforeT, double value, int flags)
	{
		ts.addSample(new TimedVariable(new Date(T.getTime() - msBeforeT), value, flags));
	}

	private static Date utcDate(int year, int month, int day)
	{
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.clear();
		cal.set(year, month, day, 12, 0, 0);
		return cal.getTime();
	}
}
