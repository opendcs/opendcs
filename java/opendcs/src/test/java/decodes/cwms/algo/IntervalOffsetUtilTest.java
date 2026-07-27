package decodes.cwms.algo;

import java.util.Date;

import decodes.cwms.CwmsTsId;
import decodes.tsdb.CTimeSeries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the general CWMS and InflowEstimation-specific output-boundary policies.
 * The calculation framework can offer a running aggregation window at every
 * input timestamp (for example, every five minutes), even when inflow is a
 * 15-minute output. These checks prove that only timestamps with the selected
 * interval offset are eligible to publish output.
 */
final class IntervalOffsetUtilTest
{
	private static final Date HOUR = new Date(60 * 60 * 1000L);
	private static final Date HOUR_AND_A_HALF = new Date(90 * 60 * 1000L);

	@Test
	void generalPolicyUsesTheDefinedCwmsOffset()
	{
		CTimeSeries output = outputSeries(0);

		// A defined zero-second offset allows exact hour boundaries, not :30.
		assertTrue(IntervalOffsetUtil.matchesIntervalOffset(output, HOUR));
		assertFalse(IntervalOffsetUtil.matchesIntervalOffset(output, HOUR_AND_A_HALF));
	}

	@Test
	void inflowPolicyKeepsOneOffsetForANewOutputSeries()
	{
		CTimeSeries output = outputSeries(null);

		// A new output series has no CWMS offset yet. The algorithm chooses the
		// first eligible one for this run, which prevents one save batch from
		// mixing incompatible offsets (the source of the CWMS save failure).
		assertTrue(IntervalOffsetUtil.matchesInflowIntervalOffset(output, HOUR, null));
		// Subsequent values must use that same offset (zero seconds here).
		assertTrue(IntervalOffsetUtil.matchesInflowIntervalOffset(output, HOUR, 0));
		assertFalse(IntervalOffsetUtil.matchesInflowIntervalOffset(output, HOUR_AND_A_HALF, 0));
	}

	private static CTimeSeries outputSeries(Integer utcOffset)
	{
		CwmsTsId identifier = new CwmsTsId();
		identifier.setUniqueString("BLU.Flow-In.Ave.15Minutes.3Hours.TEST");
		// The unit-test interval list contains built-in calendar names, not CWMS aliases.
		identifier.setInterval("hour");
		identifier.setUtcOffset(utcOffset);
		return new CTimeSeries(identifier);
	}
}
