package decodes.cwms;

import fixtures.NonPoolingConnectionOwner;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CwmsTimeSeriesDAOTest
{
	@Test
	public void testExceedsMaxTimeGap()
	{
		try (CwmsTimeSeriesDAO cwmsTimeSeriesDAO = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			Calendar first = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			first.set(2015, Calendar.JANUARY, 1, 0, 0, 0);
			Calendar second = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			second.set(2015, Calendar.JANUARY, 9, 0, 0, 0);
			assertTrue(cwmsTimeSeriesDAO.exceedsMaxTimeGap(first.getTime(), second.getTime()));
			second.set(2015, Calendar.JANUARY, 8, 0, 0, 0);
			assertTrue(cwmsTimeSeriesDAO.exceedsMaxTimeGap(first.getTime(), second.getTime()));
		}
	}

	@Test
	public void testWithinMaxTimeGap()
	{
		try (CwmsTimeSeriesDAO cwmsTimeSeriesDAO = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			Calendar first = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			first.set(2015, Calendar.JANUARY, 1, 0, 0, 0);
			Calendar second = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			second.set(2015, Calendar.JANUARY, 7, 23, 59, 59);
			assertFalse(cwmsTimeSeriesDAO.exceedsMaxTimeGap(first.getTime(), second.getTime()));
		}
	}
}
