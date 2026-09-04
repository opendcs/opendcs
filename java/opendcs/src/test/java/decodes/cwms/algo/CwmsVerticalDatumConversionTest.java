package decodes.cwms.algo;

import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsVerticalDatumDao;
import decodes.cwms.NoVerticalDatumMappingException;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.Site;
import decodes.db.UnitConverterDb;
import decodes.tsdb.DbCompException;
import mil.army.usace.hec.metadata.UnitUtil;

/**
 * Database-free unit tests for CwmsVerticalDatumConversion.
 *
 * These tests exercise the core behaviors:
 *   - Identity short-circuit (datum1 == datum2) skips the CWMS call.
 *   - Non-identity path adds the CWMS offset to the input value.
 *   - effectiveDateMode selects the correct datetime passed to the DAO.
 */
final class CwmsVerticalDatumConversionTest
{
	@BeforeAll
	static void setupUnitConversions() throws Exception
	{
		Database database = new Database();
		Database.setDb(database);
		String[] availableUnits = UnitUtil.getAvailableUnits();
		for (String unit : availableUnits)
		{
			List<String> allConvertTo = UnitUtil.getAllUnitsThatCanConvertTo(unit, UnitUtil.ENGLISH);
			for (String convertTo : allConvertTo)
			{
				UnitConverterDb unitConverterDb = new UnitConverterDb(unit, convertTo);
				unitConverterDb.algorithm = Constants.eucvt_linear;
				unitConverterDb.coefficients = new double[]
					{ UnitUtil.getScalarFactor(unit, convertTo), 0.0 };
				database.unitConverterSet.addDbConverter(unitConverterDb);
			}

			allConvertTo = UnitUtil.getAllUnitsThatCanConvertTo(unit, UnitUtil.SI);
			for (String convertTo : allConvertTo)
			{
				UnitConverterDb unitConverterDb = new UnitConverterDb(unit, convertTo);
				unitConverterDb.algorithm = Constants.eucvt_linear;
				database.unitConverterSet.addDbConverter(unitConverterDb);
			}
		}
	}

	/**
	 * Stub CwmsVerticalDatumDao that records the last call and returns a fixed offset.
	 */
	private static final class StubVerticalDatumDao extends CwmsVerticalDatumDao
	{
		String lastLocationId;
		String lastDatum1;
		String lastDatum2;
		Date lastDatetime;
		String lastTimeZone;
		String lastUnit;
		String lastOfficeId;
		double offsetToReturn = 0.0;
		int callCount = 0;

		StubVerticalDatumDao()
		{
			super(new CwmsTimeSeriesDb());
		}

		@Override
		public double getVerticalDatumOffset(
			String locationId,
			String datum1,
			String datum2,
			Date datetime,
			String timeZone,
			String unit,
			String officeIdOverride
		) throws NoVerticalDatumMappingException
		{
			callCount++;
			lastLocationId = locationId;
			lastDatum1 = datum1;
			lastDatum2 = datum2;
			lastDatetime = datetime;
			lastTimeZone = timeZone;
			lastUnit = unit;
			lastOfficeId = officeIdOverride;
			return offsetToReturn;
		}
	}

	private static final class TestableCwmsVerticalDatumConversion extends CwmsVerticalDatumConversion
	{
		final StubVerticalDatumDao stubDao = new StubVerticalDatumDao();
		String testInputUnit = "ft";
		String testLocationId = "TEST";
		Site siteToLoad = null;
		String loadedLocation = null;
		String loadedOffice = null;

		@Override
		protected String getInputUnitsAbbr(String rolename)
		{
			if ("valueInDatum1".equals(rolename))
			{
				return testInputUnit;
			}
			return super.getInputUnitsAbbr(rolename);
		}

		@Override
		protected String resolveLocationIdFromInput() throws DbCompException
		{
			return testLocationId;
		}

		@Override
		protected CwmsVerticalDatumDao getVerticalDatumDao() throws DbCompException
		{
			return stubDao;
		}

		@Override
		protected Site loadLocationSite(String locationId, String officeId) throws DbCompException
		{
			loadedLocation = locationId;
			loadedOffice = officeId;
			if (siteToLoad == null)
			{
				throw new DbCompException("No test site configured.");
			}
			return siteToLoad;
		}

		void runTimeSlice(double inputValue, Date baseTime) throws DbCompException
		{
			valueInDatum1 = inputValue;
			_timeSliceBaseTime = baseTime;
			doAWTimeSlice();
		}
	}

	@Test
	void identityConversionCopiesInputAndSkipsDao() throws Exception
	{
		TestableCwmsVerticalDatumConversion algo = new TestableCwmsVerticalDatumConversion();
		algo.datum1 = "NAVD88";
		algo.datum2 = "navd88"; // different case on purpose
		algo.effectiveDateMode = "latestOnOrBefore";
		algo.initAWAlgorithm();
		algo.testInputUnit = "ft";
		algo.testLocationId = "FOO";
		algo.beforeTimeSlices();

		Date baseTime = new Date(1_700_000_000_000L);
		algo.runTimeSlice(123.45, baseTime);

		// DAO should not be called for identity conversions.
		assertEquals(0, algo.stubDao.callCount);
		// Output NamedVariable value should equal input.
		assertEquals(123.45, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	@Test
	void nonIdentityConversionAddsOffsetAndUsesBaseTime() throws Exception
	{
		TestableCwmsVerticalDatumConversion algo = new TestableCwmsVerticalDatumConversion();
		algo.datum1 = "LOCAL";
		algo.datum2 = "NAVD88";
		algo.effectiveDateMode = "latestOnOrBefore";
		algo.initAWAlgorithm();
		algo.stubDao.offsetToReturn = 5.0; // CWMS offset
		algo.testInputUnit = "ft";
		algo.testLocationId = "BAR";
		algo.beforeTimeSlices();

		Date baseTime = new Date(1_700_000_000_000L);
		algo.runTimeSlice(10.0, baseTime);

		// DAO should be called exactly once with expected parameters.
		assertEquals(1, algo.stubDao.callCount);
		assertEquals("BAR", algo.stubDao.lastLocationId);
		assertEquals("LOCAL", algo.stubDao.lastDatum1);
		assertEquals("NAVD88", algo.stubDao.lastDatum2);
		assertEquals("ft", algo.stubDao.lastUnit);
		assertEquals("UTC", algo.stubDao.lastTimeZone);
		assertNull(algo.stubDao.lastOfficeId);
		assertEquals(baseTime, algo.stubDao.lastDatetime);

		// Output = input + offset
		assertEquals(15.0, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	@Test
	void latestOverallUsesFarFutureDate() throws Exception
	{
		TestableCwmsVerticalDatumConversion algo = new TestableCwmsVerticalDatumConversion();
		algo.datum1 = "LOCAL";
		algo.datum2 = "NAVD88";
		algo.effectiveDateMode = "latestOverall";
		algo.initAWAlgorithm();
		algo.stubDao.offsetToReturn = 1.0;
		algo.testInputUnit = "m";
		algo.testLocationId = "BAZ";
		algo.beforeTimeSlices();

		// Base time should be ignored for latestOverall.
		Date baseTime = new Date(1_600_000_000_000L);
		algo.runTimeSlice(2.0, baseTime);

		assertEquals(1, algo.stubDao.callCount);
		// expected far-future date of 3000-01-01
		Calendar expected = new GregorianCalendar(3000, Calendar.JANUARY, 1);
		Calendar actual = Calendar.getInstance();
		actual.setTime(algo.stubDao.lastDatetime);
		assertEquals(expected.get(Calendar.YEAR), actual.get(Calendar.YEAR));
		assertEquals(expected.get(Calendar.MONTH), actual.get(Calendar.MONTH));
		assertEquals(expected.get(Calendar.DAY_OF_MONTH), actual.get(Calendar.DAY_OF_MONTH));
		assertEquals(3.0, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	@Test
	void locationElevationOffsetAddsConfiguredSiteElevation() throws Exception
	{
		TestableCwmsVerticalDatumConversion algo = new TestableCwmsVerticalDatumConversion();
		algo.datum1 = "STAGE";
		algo.datum2 = "LOCAL";
		algo.conversionMode = "locationElevationOffset";
		algo.officeId = "TEST";
		algo.initAWAlgorithm();
		algo.testInputUnit = "m";
		algo.testLocationId = "LOCKDAM_03";
		algo.siteToLoad = siteWithElevation(182.88, "LOCAL");
		algo.beforeTimeSlices();

		Date baseTime = new Date(1_700_000_000_000L);
		algo.runTimeSlice(4.0, baseTime);

		assertEquals(0, algo.stubDao.callCount);
		assertEquals(186.88, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	@Test
	void locationElevationOffsetAllowsBlankDatum2AndResolvesFromLocation() throws Exception
	{
		TestableCwmsVerticalDatumConversion algo = new TestableCwmsVerticalDatumConversion();
		algo.datum1 = "STAGE";
		algo.conversionMode = "locationElevationOffset";
		algo.officeId = "LRL";
		algo.initAWAlgorithm();
		algo.testInputUnit = "ft";
		algo.testLocationId = "LOCKDAM_03";
		algo.siteToLoad = siteWithElevation(100.0, "navd88");

		algo.beforeTimeSlices();

		assertEquals("LOCKDAM_03", algo.loadedLocation);
		assertEquals("LRL", algo.loadedOffice);

		Date baseTime = new Date(1_700_000_000_000L);
		algo.runTimeSlice(4.0, baseTime);

		assertEquals(0, algo.stubDao.callCount);
		assertEquals(332.0839895, algo.valueInDatum2.getDoubleValue(), 1e-7);
	}

	@Test
	void locationElevationOffsetRequiresStageDatum1() throws Exception
	{
		CwmsVerticalDatumConversion algo = new CwmsVerticalDatumConversion();
		algo.datum1 = "LOCAL";
		algo.datum2 = "MSL1912";
		algo.conversionMode = "locationElevationOffset";

		DbCompException ex = assertThrows(DbCompException.class, algo::initAWAlgorithm);
		assertTrue(ex.getMessage().contains("datum1=STAGE"));
	}

	@Test
	void locationElevationOffsetConvertsFromNativeDatumToRequestedTarget() throws Exception
	{
		TestableCwmsVerticalDatumConversion algo = new TestableCwmsVerticalDatumConversion();
		algo.datum1 = "STAGE";
		algo.datum2 = "NAVD88";
		algo.conversionMode = "locationElevationOffset";
		algo.officeId = "TEST";
		algo.initAWAlgorithm();
		algo.stubDao.offsetToReturn = 5.0;
		algo.testInputUnit = "m";
		algo.testLocationId = "LOCKDAM_03";
		algo.siteToLoad = siteWithElevation(182.88, "LOCAL");
		algo.beforeTimeSlices();

		Date baseTime = new Date(1_700_000_000_000L);
		algo.runTimeSlice(4.0, baseTime);

		assertEquals(1, algo.stubDao.callCount);
		assertEquals("LOCKDAM_03", algo.stubDao.lastLocationId);
		assertEquals("LOCAL", algo.stubDao.lastDatum1);
		assertEquals("NAVD88", algo.stubDao.lastDatum2);
		assertEquals(baseTime, algo.stubDao.lastDatetime);
		assertEquals(191.88, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	private static Site siteWithElevation(double elevation, String verticalDatum)
	{
		Site site = new Site();
		site.setElevation(elevation);
		site.setProperty("vertical_datum", verticalDatum);
		return site;
	}
}
