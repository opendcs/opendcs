package decodes.tsdb.algo;

import java.util.Calendar;
import java.util.Locale;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.Site;
import decodes.db.UnitConverter;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import decodes.util.DecodesException;
import ilex.var.NamedVariable;

/**
 * Calculates daily Hamon evaporation from air temperature and site latitude.
 * The monthly properties provide the local adjustment coefficient.
 */
@Algorithm(description = "Calculates daily Hamon evaporation from air temperature, site latitude, and monthly adjustment coefficients.")
public class HamonMethod extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static final DateTimeFormatter DAY_OF_YEAR_FORMATTER =
		DateTimeFormatter.ofPattern("DDD", Locale.getDefault());
	private UnitConverter temperatureConverter;
	private UnitConverter evaporationConverter;

	@Input
	public double input;

	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	@PropertySpec(value = "0.0")
	public double jan = 0.0;
	@PropertySpec(value = "0.0")
	public double feb = 0.0;
	@PropertySpec(value = "0.0")
	public double mar = 0.0;
	@PropertySpec(value = "0.0")
	public double apr = 0.0;
	@PropertySpec(value = "0.0")
	public double may = 0.0;
	@PropertySpec(value = "0.0")
	public double jun = 0.0;
	@PropertySpec(value = "0.0")
	public double jul = 0.0;
	@PropertySpec(value = "0.0")
	public double aug = 0.0;
	@PropertySpec(value = "0.0")
	public double sep = 0.0;
	@PropertySpec(value = "0.0")
	public double oct = 0.0;
	@PropertySpec(value = "0.0")
	public double nov = 0.0;
	@PropertySpec(value = "0.0")
	public double dec = 0.0;

	public HamonMethod()
	{
		// The algorithm framework instantiates this class reflectively.
	}

	@Override
	protected void initAWAlgorithm() throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}

	@Override
	protected void beforeTimeSlices() throws DbCompException
	{
		temperatureConverter = getConverter(getInputUnitsAbbr("input"), "C");
		evaporationConverter = getConverter("in", getParmUnitsAbbr("output"));
	}

	@Override
	protected void doAWTimeSlice() throws DbCompException
	{
		ParmRef inputParm = getParmRef("input");
		CTimeSeries inputTimeSeries = inputParm.timeSeries;
		Site site = inputTimeSeries.getTimeSeriesIdentifier().getSite();
		double temperatureCelsius = convert(input, temperatureConverter, "temperature", "C");

		// Hamon uses the Julian day to estimate solar declination and daylight length.
		ZonedDateTime localTime = _timeSliceBaseTime.toInstant().atZone(aggTZ.toZoneId());
		int month = localTime.getMonthValue() - 1;
		int dayOfYear = Integer.parseInt(DAY_OF_YEAR_FORMATTER.format(localTime));
		double latitudeDegrees = parseLatitude(site);

		// Solar declination and sunset hour angle are expressed in radians.
		double solarDeclinationRadians = 0.4093
			* Math.sin(0.01721420632103996 * dayOfYear - 1.405);
		double sunsetHourAngleRadians = Math.acos(
			-Math.tan(Math.toRadians(latitudeDegrees)) * Math.tan(solarDeclinationRadians));
		double daylightHours = 7.639437268410976 * sunsetHourAngleRadians;

		// Convert air temperature to saturation vapor pressure and density.
		double saturationVaporPressureKpa = 0.6108
			* Math.pow(Math.E, 17.27 * temperatureCelsius / (237.3 + temperatureCelsius));
		double temperatureKelvin = temperatureCelsius + 273.15;
		double saturationVaporDensity = 2166.74
			* (saturationVaporPressureKpa / temperatureKelvin);

		// This is the unadjusted Hamon estimate before the monthly coefficient.
		double baseHamonEvaporation = 0.55
			* Math.pow(daylightHours / 12.0, 2.0)
			* (saturationVaporDensity / 100.0);
		double monthlyCoefficient = getMonthlyCoefficient(month);

		double evaporationInches = baseHamonEvaporation * monthlyCoefficient;
		double evaporation = convert(evaporationInches, evaporationConverter, "evaporation", "inches");
		setOutput(output, evaporation);
	}

	private double parseLatitude(Site site)
	{
		try
		{
			return Double.parseDouble(site.latitude);
		}
		catch (NumberFormatException exception)
		{
			log.warn("Latitude is not a decimal number. Please convert latitude for site {} to a decimal number",
				site.getDisplayName());
			return 0.0;
		}
	}

	private double getMonthlyCoefficient(int month)
	{
		return switch (month)
		{
		case Calendar.JANUARY -> jan;
		case Calendar.FEBRUARY -> feb;
		case Calendar.MARCH -> mar;
		case Calendar.APRIL -> apr;
		case Calendar.MAY -> may;
		case Calendar.JUNE -> jun;
		case Calendar.JULY -> jul;
		case Calendar.AUGUST -> aug;
		case Calendar.SEPTEMBER -> sep;
		case Calendar.OCTOBER -> oct;
		case Calendar.NOVEMBER -> nov;
		case Calendar.DECEMBER -> dec;
		default -> throw new IllegalStateException("Unexpected calendar month: " + month);
		};
	}

	private UnitConverter getConverter(String fromUnits, String toUnits) throws DbCompException
	{
		if (fromUnits == null || toUnits == null || fromUnits.equalsIgnoreCase(toUnits)
			|| fromUnits.equalsIgnoreCase("unknown") || toUnits.equalsIgnoreCase("unknown"))
		{
			return null;
		}

		EngineeringUnit from = EngineeringUnit.getEngineeringUnit(fromUnits);
		EngineeringUnit to = EngineeringUnit.getEngineeringUnit(toUnits);
		UnitConverter converter = Database.getDb().unitConverterSet.get(from, to);
		if (converter == null)
		{
			throw new DbCompException("No Hamon unit converter is available from "
				+ fromUnits + " to " + toUnits);
		}
		return converter;
	}

	private double convert(double value, UnitConverter converter, String valueName, String units)
		throws DbCompException
	{
		if (converter == null)
		{
			return value;
		}

		try
		{
			return converter.convert(value);
		}
		catch (DecodesException exception)
		{
			throw new DbCompException("Unable to convert Hamon " + valueName + " to " + units, exception);
		}
	}

	@Override
	protected void afterTimeSlices() throws DbCompException
	{
		// Hamon has no state to finalize after processing time slices.
	}
}
