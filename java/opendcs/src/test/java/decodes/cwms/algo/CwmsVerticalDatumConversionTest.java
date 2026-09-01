package decodes.cwms.algo;

import java.lang.reflect.Field;
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

	@Test
	void identityConversionCopiesInputAndSkipsDao() throws Exception
	{
		CwmsVerticalDatumConversion algo = new CwmsVerticalDatumConversion();
		algo.datum1 = "NAVD88";
		algo.datum2 = "navd88"; // different case on purpose
		algo.effectiveDateMode = "latestOnOrBefore";
		algo.initAWAlgorithm();

		StubVerticalDatumDao stubDao = new StubVerticalDatumDao();
		setPrivateField(algo, "verticalDatumDao", stubDao);
		setPrivateField(algo, "inputUnit", "ft");
		setPrivateField(algo, "locationIdFromTs", "FOO");

		Date baseTime = new Date(1_700_000_000_000L);
		setPrivateField(algo, "_timeSliceBaseTime", baseTime);

		algo.valueInDatum1 = 123.45;
		algo.doAWTimeSlice();

		// DAO should not be called for identity conversions.
		assertEquals(0, stubDao.callCount);
		// Output NamedVariable value should equal input.
		assertEquals(123.45, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	@Test
	void nonIdentityConversionAddsOffsetAndUsesBaseTime() throws Exception
	{
		CwmsVerticalDatumConversion algo = new CwmsVerticalDatumConversion();
		algo.datum1 = "LOCAL";
		algo.datum2 = "NAVD88";
		algo.effectiveDateMode = "latestOnOrBefore";
		algo.initAWAlgorithm();

		StubVerticalDatumDao stubDao = new StubVerticalDatumDao();
		stubDao.offsetToReturn = 5.0; // CWMS offset
		setPrivateField(algo, "verticalDatumDao", stubDao);
		setPrivateField(algo, "inputUnit", "ft");
		setPrivateField(algo, "locationIdFromTs", "BAR");

		Date baseTime = new Date(1_700_000_000_000L);
		setPrivateField(algo, "_timeSliceBaseTime", baseTime);

		algo.valueInDatum1 = 10.0;
		algo.doAWTimeSlice();

		// DAO should be called exactly once with expected parameters.
		assertEquals(1, stubDao.callCount);
		assertEquals("BAR", stubDao.lastLocationId);
		assertEquals("LOCAL", stubDao.lastDatum1);
		assertEquals("NAVD88", stubDao.lastDatum2);
		assertEquals("ft", stubDao.lastUnit);
		assertEquals("UTC", stubDao.lastTimeZone);
		assertNull(stubDao.lastOfficeId);
		assertEquals(baseTime, stubDao.lastDatetime);

		// Output = input + offset
		assertEquals(15.0, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	@Test
	void latestOverallUsesFarFutureDate() throws Exception
	{
		CwmsVerticalDatumConversion algo = new CwmsVerticalDatumConversion();
		algo.datum1 = "LOCAL";
		algo.datum2 = "NAVD88";
		algo.effectiveDateMode = "latestOverall";
		algo.initAWAlgorithm();

		StubVerticalDatumDao stubDao = new StubVerticalDatumDao();
		stubDao.offsetToReturn = 1.0;
		setPrivateField(algo, "verticalDatumDao", stubDao);
		setPrivateField(algo, "inputUnit", "m");
		setPrivateField(algo, "locationIdFromTs", "BAZ");

		// Base time should be ignored for latestOverall.
		Date baseTime = new Date(1_600_000_000_000L);
		setPrivateField(algo, "_timeSliceBaseTime", baseTime);

		algo.valueInDatum1 = 2.0;
		algo.doAWTimeSlice();

		assertEquals(1, stubDao.callCount);
		// expected far-future date of 3000-01-01
		Calendar expected = new GregorianCalendar(3000, Calendar.JANUARY, 1);
		Calendar actual = Calendar.getInstance();
		actual.setTime(stubDao.lastDatetime);
		assertEquals(expected.get(Calendar.YEAR), actual.get(Calendar.YEAR));
		assertEquals(expected.get(Calendar.MONTH), actual.get(Calendar.MONTH));
		assertEquals(expected.get(Calendar.DAY_OF_MONTH), actual.get(Calendar.DAY_OF_MONTH));
		assertEquals(3.0, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	@Test
	void locationElevationOffsetAddsConfiguredSiteElevation() throws Exception
	{
		CwmsVerticalDatumConversion algo = new CwmsVerticalDatumConversion();
		algo.datum1 = "STAGE";
		algo.conversionMode = "locationElevationOffset";
		algo.initAWAlgorithm();

		setPrivateField(algo, "normalizedDatum2", "LOCAL");

		StubVerticalDatumDao stubDao = new StubVerticalDatumDao();
		setPrivateField(algo, "verticalDatumDao", stubDao);
		setPrivateField(algo, "inputUnit", "m");
		setPrivateField(algo, "locationIdFromTs", "LOCKDAM_03");
		setPrivateField(
			algo,
			"locationElevationInfo",
			new CwmsVerticalDatumConversion.LocationElevationInfo(182.88, 182.88, "LOCAL"));

		Date baseTime = new Date(1_700_000_000_000L);
		setPrivateField(algo, "_timeSliceBaseTime", baseTime);

		algo.valueInDatum1 = 4.0;
		algo.doAWTimeSlice();

		assertEquals(0, stubDao.callCount);
		assertEquals(186.88, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	@Test
	void locationElevationOffsetAllowsBlankDatum2AndResolvesFromLocation() throws Exception
	{
		CwmsVerticalDatumConversion algo = new CwmsVerticalDatumConversion();
		algo.datum1 = "STAGE";
		algo.conversionMode = "locationElevationOffset";
		algo.officeId = "LRL";
		algo.initAWAlgorithm();

		setPrivateField(algo, "inputUnit", "ft");
		setPrivateField(algo, "locationIdFromTs", "LOCKDAM_03");

		String[] loadedLocation = new String[1];
		String[] loadedOffice = new String[1];
		setPrivateField(algo, "locationSiteLoader",
			(CwmsVerticalDatumConversion.LocationSiteLoader)(locationId, officeId) ->
			{
				loadedLocation[0] = locationId;
				loadedOffice[0] = officeId;
				Site site = new Site();
				site.setElevation(100.0);
				site.setProperty("vertical_datum", "navd88");
				return site;
			});

		Date baseTime = new Date(1_700_000_000_000L);
		setPrivateField(algo, "_timeSliceBaseTime", baseTime);

		algo.beforeTimeSlices();

		assertEquals("LOCKDAM_03", loadedLocation[0]);
		assertEquals("LRL", loadedOffice[0]);
		assertEquals("NAVD88", getPrivateField(algo, "normalizedDatum2"));

		algo.valueInDatum1 = 4.0;
		algo.doAWTimeSlice();

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
		CwmsVerticalDatumConversion algo = new CwmsVerticalDatumConversion();
		algo.datum1 = "STAGE";
		algo.datum2 = "NAVD88";
		algo.conversionMode = "locationElevationOffset";
		algo.initAWAlgorithm();

		StubVerticalDatumDao stubDao = new StubVerticalDatumDao();
		stubDao.offsetToReturn = 5.0;
		setPrivateField(algo, "verticalDatumDao", stubDao);
		setPrivateField(algo, "inputUnit", "m");
		setPrivateField(algo, "locationIdFromTs", "LOCKDAM_03");
		setPrivateField(
			algo,
			"locationElevationInfo",
			new CwmsVerticalDatumConversion.LocationElevationInfo(182.88, 182.88, "LOCAL"));

		Date baseTime = new Date(1_700_000_000_000L);
		setPrivateField(algo, "_timeSliceBaseTime", baseTime);

		algo.valueInDatum1 = 4.0;
		algo.doAWTimeSlice();

		assertEquals(1, stubDao.callCount);
		assertEquals("LOCKDAM_03", stubDao.lastLocationId);
		assertEquals("LOCAL", stubDao.lastDatum1);
		assertEquals("NAVD88", stubDao.lastDatum2);
		assertEquals(baseTime, stubDao.lastDatetime);
		assertEquals(191.88, algo.valueInDatum2.getDoubleValue(), 1e-9);
	}

	private static void setPrivateField(Object target, String fieldName, Object value)
		throws NoSuchFieldException, IllegalAccessException
	{
		Class<?> cls = target.getClass();
		Field f;
		while (true)
		{
			try
			{
				f = cls.getDeclaredField(fieldName);
				break;
			}
			catch (NoSuchFieldException ex)
			{
				cls = cls.getSuperclass();
				if (cls == null)
				{
					throw ex;
				}
			}
		}
		f.setAccessible(true);
		f.set(target, value);
	}

	private static Object getPrivateField(Object target, String fieldName)
		throws NoSuchFieldException, IllegalAccessException
	{
		Class<?> cls = target.getClass();
		Field f;
		while (true)
		{
			try
			{
				f = cls.getDeclaredField(fieldName);
				break;
			}
			catch (NoSuchFieldException ex)
			{
				cls = cls.getSuperclass();
				if (cls == null)
				{
					throw ex;
				}
			}
		}
		f.setAccessible(true);
		return f.get(target);
	}
}
