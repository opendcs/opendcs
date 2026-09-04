/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.cwms.algo;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.var.NamedVariable;
import decodes.cwms.CwmsSiteDAO;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.cwms.CwmsVerticalDatumDao;
import decodes.cwms.NoVerticalDatumMappingException;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.Site;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.util.DecodesException;
import decodes.util.PropertySpec;

/**
 * Convert vertical values between CWMS vertical datums using
 * cwms_loc.get_vertical_datum_offset (datum1 -> datum2), or explicitly
 * convert stage to the location's native datum using CWMS_V_LOC.elevation
 * and CWMS_V_LOC.vertical_datum before optionally converting onward to
 * datum2.
 *
 * Typical usage:
 *   - Input: stage/local-datum series (valueInDatum1).
 *   - Output: NAVD88 or other elevation datum (valueInDatum2).
 */
@Algorithm(
	description = "Convert vertical values between CWMS vertical datums using "
			+ "cwms_loc.get_vertical_datum_offset (datum1 -> datum2), or "
			+ "explicitly convert STAGE to the location native datum using "
			+ "CWMS_V_LOC.elevation and CWMS_V_LOC.vertical_datum before "
			+ "optionally converting onward to datum2. Typical use: "
			+ "stage/local datum to NAVD88 elevation.")
public class CwmsVerticalDatumConversion extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static final String CWMS_TIME_ZONE_UTC = "UTC";
	private static final String CWMS_LOCATION_ELEVATION_UNIT = "m";
	private static final String MODE_CWMS_DATUM_OFFSET = "cwmsDatumOffset";
	private static final String MODE_LOCATION_ELEVATION_OFFSET = "locationElevationOffset";
	private static final String STAGE_DATUM = "STAGE";

	//AW:INPUTS
	@Input
	public double valueInDatum1;       //AW:TYPECODE=i
	String _inputNames[] = { "valueInDatum1" };
	//AW:INPUTS_END

	//AW:OUTPUTS
	@Output
	public NamedVariable valueInDatum2 =
		new NamedVariable("valueInDatum2", 0);
	String _outputNames[] = { "valueInDatum2" };
	//AW:OUTPUTS_END

	//AW:PROPERTIES
	@org.opendcs.annotations.PropertySpec(
		name = "datum1",
		propertySpecType = PropertySpec.STRING,
		description = "Input vertical datum (e.g. LOCAL, NGVD29, NAVD88).For "
			+ "conversionMode=locationElevationOffset, use datum1=STAGE to convert from stage "
			+ "to use the location's native datum from CWMS-VUE Locations")
	public String datum1;

	@org.opendcs.annotations.PropertySpec(
		name = "datum2",
		propertySpecType = PropertySpec.STRING,
		description = "Output vertical datum (e.g., NAVD88, MSL1912). For "
			+ "conversionMode=locationElevationOffset, leave blank to use "
			+ "CWMS_V_LOC.vertical_datum.")
	public String datum2;

	@org.opendcs.annotations.PropertySpec(
		name = "officeId",
		propertySpecType = PropertySpec.STRING,
		description = "Optional CWMS office ID override; if blank, uses DB office.")
	public String officeId;

	@org.opendcs.annotations.PropertySpec(
		name = "effectiveDateMode",
		propertySpecType = PropertySpec.STRING,
		description = "Offset selection mode: 'latestOnOrBefore' (default) or 'latestOverall'.")
	public String effectiveDateMode = "latestOnOrBefore";

	@org.opendcs.annotations.PropertySpec(
		name = "conversionMode",
		propertySpecType = PropertySpec.STRING,
		description = "Conversion source: 'cwmsDatumOffset' (default) uses "
			+ "cwms_loc.get_vertical_datum_offset; 'locationElevationOffset' "
			+ "uses CWMS_V_LOC.elevation and CWMS_V_LOC.vertical_datum from CWMS-VUE Locations tab for "
			+ "STAGE to native-datum elevation before any native-to-datum2 "
			+ "CWMS conversion.")
	public String conversionMode = MODE_CWMS_DATUM_OFFSET;

	//AW:PROPERTIES_END

	// Local fields
	private CwmsVerticalDatumDao verticalDatumDao;
	private String normalizedDatum1;
	private String normalizedDatum2;
	private String normalizedConversionMode;
	private String inputUnit;
	private String locationIdFromTs;
	private LocationElevationInfo locationElevationInfo;
	private UnitConverter locationElevationUnitConverter;
	private LocationSiteLoader locationSiteLoader = this::readLocationSiteFromCwmsLoc;

	static final class LocationElevationInfo
	{
		final double elevationOffsetInInputUnit;
		final double elevationInMeters;
		final String verticalDatum;

		LocationElevationInfo(double elevationOffsetInInputUnit, double elevationInMeters,
			String verticalDatum)
		{
			this.elevationOffsetInInputUnit = elevationOffsetInInputUnit;
			this.elevationInMeters = elevationInMeters;
			this.verticalDatum = verticalDatum;
		}
	}

	/**
	 * Local seam around site loading so this algorithm can unit test the
	 * location-elevation path without bootstrapping the full CWMS DAO stack.
	 * The default implementation still reads directly from CWMS_V_LOC.
	 */
	@FunctionalInterface
	interface LocationSiteLoader
	{
		Site load(String locationId, String officeId) throws DbCompException;
	}

	@Override
	protected void initAWAlgorithm() throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
		if (datum1 == null)
		{
			throw new DbCompException("CwmsVerticalDatumConversion requires 'datum1'.");
		}

		normalizedDatum1 = datum1.trim().toUpperCase(Locale.US);
		normalizedDatum2 = normalizeOptionalDatum(datum2);

		if (effectiveDateMode == null || effectiveDateMode.trim().isEmpty())
		{
			effectiveDateMode = "latestOnOrBefore";
		}
		effectiveDateMode = effectiveDateMode.trim();

		if (conversionMode == null || conversionMode.trim().isEmpty())
		{
			conversionMode = MODE_CWMS_DATUM_OFFSET;
		}
		normalizedConversionMode = conversionMode.trim();
		if (!MODE_CWMS_DATUM_OFFSET.equalsIgnoreCase(normalizedConversionMode)
			&& !MODE_LOCATION_ELEVATION_OFFSET.equalsIgnoreCase(normalizedConversionMode))
		{
			throw new DbCompException(
				"Unknown conversionMode '" + conversionMode + "'. Expected '"
				+ MODE_CWMS_DATUM_OFFSET + "' or '" + MODE_LOCATION_ELEVATION_OFFSET + "'.");
		}

		if (isLocationElevationOffsetMode() && !STAGE_DATUM.equals(normalizedDatum1))
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
			  + "requires datum1=STAGE.");
		}
		if (!isLocationElevationOffsetMode() && normalizedDatum2 == null)
		{
			throw new DbCompException("CwmsVerticalDatumConversion requires 'datum2' when "
				+ "conversionMode=" + MODE_CWMS_DATUM_OFFSET + ".");
		}
	}

	@Override
	public void beforeAllTimeSlices() throws DbCompException
	{
		if (!(tsdb instanceof CwmsTimeSeriesDb))
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion requires a CWMS time series database (CwmsTimeSeriesDb).");
		}

		verticalDatumDao = new CwmsVerticalDatumDao((CwmsTimeSeriesDb) tsdb);
	}

	@Override
	protected void beforeTimeSlices() throws DbCompException
	{
		// Determine input units once and set output units to match.
		if (inputUnit == null)
		{
			inputUnit = getInputUnitsAbbr("valueInDatum1");
		}
		if (inputUnit == null || inputUnit.trim().isEmpty() ||
			"unknown".equalsIgnoreCase(inputUnit))
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion cannot determine units for input 'valueInDatum1'. "
			  + "Ensure the input time series has defined engineering units.");
		}

		setOutputUnitsAbbr("valueInDatum2", inputUnit);

		// Derive CWMS location ID from the input time series identifier.
		if (locationIdFromTs == null)
		{
			locationIdFromTs = resolveLocationIdFromInput();
		}

		if (isLocationElevationOffsetMode())
		{
			initializeLocationElevationUnitConverter(
				CWMS_LOCATION_ELEVATION_UNIT,
				inputUnit);
			ensureLocationElevationInfoLoaded();
		}
	}

	@Override
	protected void doAWTimeSlice() throws DbCompException
	{
		if (isMissing(valueInDatum1))
		{
			// Follow standard AW behavior: skip this timeslice if input is missing.
			return;
		}

		if (isLocationElevationOffsetMode())
		{
			LocationElevationInfo locationInfo = ensureLocationElevationInfoLoaded();
			double outVal = valueInDatum1 + locationInfo.elevationOffsetInInputUnit;
			if (!locationInfo.verticalDatum.equals(normalizedDatum2))
			{
				outVal += computeVerticalDatumOffset(locationInfo.verticalDatum, normalizedDatum2);
			}
			setOutput(valueInDatum2, outVal, _timeSliceBaseTime);
			return;
		}

		// 1. Identity short-circuit
		if (normalizedDatum1.equals(normalizedDatum2))
		{
			log.trace("CwmsVerticalDatumConversion: identity mapping {}->{}, copying value.",
				      normalizedDatum1, normalizedDatum2);
			setOutput(valueInDatum2, valueInDatum1, _timeSliceBaseTime);
			return;
		}

		// 2. Call CWMS via DAO and apply offset
		double outVal = valueInDatum1 + computeVerticalDatumOffset(normalizedDatum1, normalizedDatum2);
		setOutput(valueInDatum2, outVal, _timeSliceBaseTime);
	}

	@Override
	protected void afterTimeSlices() throws DbCompException
	{
		// No per-period finalization required.
	}

	@Override
	public void alwaysAfterTimeSlices()
	{
		if (verticalDatumDao != null)
		{
			try
			{
				verticalDatumDao.close();
			}
			catch (Exception ex)
			{
				log.warn("Error closing CwmsVerticalDatumDao", ex);
			}
			verticalDatumDao = null;
		}
		locationElevationInfo = null;
		locationElevationUnitConverter = null;
	}

	private boolean isLocationElevationOffsetMode()
	{
		return MODE_LOCATION_ELEVATION_OFFSET.equalsIgnoreCase(normalizedConversionMode);
	}

	private LocationElevationInfo ensureLocationElevationInfoLoaded() throws DbCompException
	{
		if (locationElevationInfo == null)
		{
			locationElevationInfo = loadLocationElevationInfo();
		}

		if (normalizedDatum2 == null)
		{
			normalizedDatum2 = locationElevationInfo.verticalDatum;
			log.info("CwmsVerticalDatumConversion resolved datum2={} from CWMS_V_LOC.vertical_datum "
				   + "for location={}.", normalizedDatum2, locationIdFromTs);
		}

		validateLocationElevationInfo();
		return locationElevationInfo;
	}

	private void validateLocationElevationInfo() throws DbCompException
	{
		if (locationElevationInfo == null)
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
			  + "could not load CWMS_V_LOC metadata for location=" + locationIdFromTs + ".");
		}

		if (normalizedDatum2 == null)
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
			  + "could not determine datum2 for location=" + locationIdFromTs + ".");
		}
	}

	private LocationElevationInfo loadLocationElevationInfo() throws DbCompException
	{
		String office = effectiveOfficeId();
		Site site = loadLocationSite(locationIdFromTs, office);
		double elevationInMeters = site.getElevation();
		if (elevationInMeters == Constants.undefinedDouble)
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
			  + "requires CWMS_V_LOC.elevation for location=" + locationIdFromTs + ".");
		}

		String verticalDatum = site.getProperty("vertical_datum");
		if (verticalDatum == null || verticalDatum.trim().isEmpty())
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
			  + "requires CWMS_V_LOC.vertical_datum for location=" + locationIdFromTs + ".");
		}

		String normalizedVerticalDatum = verticalDatum.trim().toUpperCase(Locale.US);
		double elevationOffsetInInputUnit =
			convertUnits(elevationInMeters, CWMS_LOCATION_ELEVATION_UNIT, inputUnit);
		log.info("CwmsVerticalDatumConversion using locationElevationOffset for location={} "
			   + "with CWMS_V_LOC.elevation={} {} and CWMS_V_LOC.vertical_datum={}.",
			   locationIdFromTs, elevationInMeters, CWMS_LOCATION_ELEVATION_UNIT,
			   normalizedVerticalDatum);
		return new LocationElevationInfo(
			elevationOffsetInInputUnit,
			elevationInMeters,
			normalizedVerticalDatum);
	}

	private Site readLocationSiteFromCwmsLoc(String locationId, String office)
		throws DbCompException
	{
		try (CwmsSiteDAO siteDao = new CwmsSiteDAO((CwmsTimeSeriesDb) tsdb, office))
		{
			DbKey siteId = siteDao.lookupSiteID(locationId);
			if (DbKey.isNull(siteId))
			{
				throw new DbCompException(
					"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
				  + "could not find a CWMS_V_LOC row for location=" + locationId
				  + " (office=" + office + ").");
			}

			// Read directly from CWMS_V_LOC. readSite() merges site_property
			// values onto the Site object, which can override vertical_datum.
			return siteDao.readSiteFromCwmsLoc(siteId);
		}
		catch (DbIoException | NoSuchObjectException ex)
		{
			throw new DbCompException(
				"Failed to load CWMS_V_LOC.elevation and CWMS_V_LOC.vertical_datum "
			  + "for location=" + locationId + ".",
				ex);
		}
	}

	protected Site loadLocationSite(String locationId, String officeId)
		throws DbCompException
	{
		return locationSiteLoader.load(locationId, officeId);
	}

	private String effectiveOfficeId()
	{
		if (officeId != null && !officeId.trim().isEmpty())
		{
			return officeId.trim();
		}
		return ((CwmsTimeSeriesDb) tsdb).getDbOfficeId();
	}

	private String effectiveOfficeIdOverride()
	{
		return officeId == null || officeId.trim().isEmpty()
			? null
			: officeId.trim();
	}

	private double computeVerticalDatumOffset(String fromDatum, String toDatum)
		throws DbCompException
	{
		Date offsetTime = determineOffsetTime();
		String office = effectiveOfficeIdOverride();
		CwmsVerticalDatumDao dao = getVerticalDatumDao();

		try
		{
			return dao.getVerticalDatumOffset(
				locationIdFromTs,
				fromDatum,
				toDatum,
				offsetTime,
				CWMS_TIME_ZONE_UTC,
				inputUnit,
				office
			);
		}
		catch (NoVerticalDatumMappingException ex)
		{
			String msg =
				"No vertical datum mapping for location=" + locationIdFromTs +
				", " + fromDatum + "->" + toDatum +
				" at " + offsetTime + " (unit=" + inputUnit +
				", office=" + (office != null ? office : "default") + ")";
			throw new DbCompException(msg, ex);
		}
		catch (DbIoException ex)
		{
			throw new DbCompException(
				"Database error calling cwms_loc.get_vertical_datum_offset "
			  + "for location=" + locationIdFromTs + ", " + fromDatum + "->" + toDatum,
				ex);
		}
	}

	protected CwmsVerticalDatumDao getVerticalDatumDao() throws DbCompException
	{
		if (verticalDatumDao == null)
		{
			throw new DbCompException("CwmsVerticalDatumConversion vertical datum DAO not initialized.");
		}
		return verticalDatumDao;
	}

	protected String resolveLocationIdFromInput() throws DbCompException
	{
		if (getParmRef("valueInDatum1") == null
			|| getParmRef("valueInDatum1").timeSeries == null
			|| getParmRef("valueInDatum1").timeSeries.getTimeSeriesIdentifier() == null)
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion requires a bound CWMS time series for 'valueInDatum1'.");
		}

		if (!(getParmRef("valueInDatum1").timeSeries.getTimeSeriesIdentifier() instanceof CwmsTsId))
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion requires 'valueInDatum1' to use a CwmsTsId TSID.");
		}

		CwmsTsId tsId = (CwmsTsId) getParmRef("valueInDatum1").timeSeries.getTimeSeriesIdentifier();
		return tsId.getSiteName();
	}

	private Date determineOffsetTime() throws DbCompException
	{
		if ("latestOverall".equalsIgnoreCase(effectiveDateMode))
		{
			// Far-future date so CWMS selects the latest effective_date
			return new GregorianCalendar(3000, Calendar.JANUARY, 1).getTime();
		}
		if ("latestOnOrBefore".equalsIgnoreCase(effectiveDateMode))
		{
			return _timeSliceBaseTime;
		}

		throw new DbCompException(
			"Unknown effectiveDateMode '" + effectiveDateMode
		  + "'. Expected 'latestOnOrBefore' or 'latestOverall'.");
	}

	private double convertUnits(double value, String fromUnit, String toUnit)
		throws DbCompException
	{
		if (fromUnit.equalsIgnoreCase(toUnit))
		{
			return value;
		}


		if (locationElevationUnitConverter == null)
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
			  + "cannot convert CWMS_V_LOC.elevation from '" + fromUnit + "' to '"
			  + toUnit + "'.");
		}

		try
		{
			return locationElevationUnitConverter.convert(value);
		}
		catch (DecodesException ex)
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
			  + "failed converting CWMS_V_LOC.elevation from '" + fromUnit + "' to '"
			  + toUnit + "'.",
				ex);
		}
	}

	private void initializeLocationElevationUnitConverter(String fromUnit, String toUnit)
		throws DbCompException
	{
		if (fromUnit == null || toUnit == null || fromUnit.equalsIgnoreCase(toUnit))
		{
			locationElevationUnitConverter = null;
			return;
		}

		Database db = Database.getDb();
		if (db == null || db.unitConverterSet == null)
		{
			throw new DbCompException(
				"CwmsVerticalDatumConversion conversionMode=locationElevationOffset "
			  + "cannot convert CWMS_V_LOC.elevation from '" + fromUnit + "' to '"
			  + toUnit + "' because engineering unit converters are not initialized.");
		}

		EngineeringUnit fromEu = EngineeringUnit.getEngineeringUnit(fromUnit);
		EngineeringUnit toEu = EngineeringUnit.getEngineeringUnit(toUnit);
		locationElevationUnitConverter = db.unitConverterSet.get(fromEu, toEu);
	}

	private String normalizeOptionalDatum(String datum)
	{
		if (datum == null)
		{
			return null;
		}
		String trimmed = datum.trim();
		return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.US);
	}
}
