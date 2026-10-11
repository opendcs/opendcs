package decodes.tsdb.algo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Site;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import ilex.var.NamedVariable;

/**
 * Calculates daily Hamon evaporation from air temperature and site latitude.
 * The monthly properties provide the local adjustment coefficient.
 */
public class HamonMethod extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	public double input;
	private final String[] inputNames = new String[] { "input" };
	public NamedVariable output = new NamedVariable("output", 0);
	private final String[] outputNames = new String[] { "output" };
	public double jan = 0.0;
	public double feb = 0.0;
	public double mar = 0.0;
	public double apr = 0.0;
	public double may = 0.0;
	public double jun = 0.0;
	public double jul = 0.0;
	public double aug = 0.0;
	public double sep = 0.0;
	public double oct = 0.0;
	public double nov = 0.0;
	public double dec = 0.0;
	private final String[] propertyNames = new String[]
		{ "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec" };

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
		// Hamon has no state to initialize before processing time slices.
	}

	@Override
	protected void doAWTimeSlice() throws DbCompException
	{
		ParmRef inputParm = getParmRef("input");
		CTimeSeries inputTimeSeries = inputParm.timeSeries;
		Site site = inputTimeSeries.getTimeSeriesIdentifier().getSite();
		double temperatureCelsius = toCelsius(input, inputTimeSeries.getUnitsAbbr());

		// Hamon uses the Julian day to estimate solar declination and daylight length.
		String dayOfYearText = new SimpleDateFormat("DDD HH:MM:SS yyyy", Locale.US)
			.format(_timeSliceBaseTime).substring(0, 3);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(_timeSliceBaseTime);
		int month = calendar.get(Calendar.MONTH);
		int dayOfYear = Integer.parseInt(dayOfYearText);
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

		setOutput(output, baseHamonEvaporation * monthlyCoefficient);
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
		switch (month)
		{
		case Calendar.JANUARY:
			return jan;
		case Calendar.FEBRUARY:
			return feb;
		case Calendar.MARCH:
			return mar;
		case Calendar.APRIL:
			return apr;
		case Calendar.MAY:
			return may;
		case Calendar.JUNE:
			return jun;
		case Calendar.JULY:
			return jul;
		case Calendar.AUGUST:
			return aug;
		case Calendar.SEPTEMBER:
			return sep;
		case Calendar.OCTOBER:
			return oct;
		case Calendar.NOVEMBER:
			return nov;
		case Calendar.DECEMBER:
			return dec;
		default:
			throw new IllegalStateException("Unexpected calendar month: " + month);
		}
	}

	private double toCelsius(double temperature, String units)
	{
		if (units == null)
		{
			return temperature;
		}

		if ("F".equalsIgnoreCase(units)
			|| "degF".equalsIgnoreCase(units)
			|| "Fahrenheit".equalsIgnoreCase(units))
		{
			return (temperature - 32.0) * 5.0 / 9.0;
		}

		return temperature;
	}

	@Override
	public String[] getInputNames()
	{
		return inputNames;
	}

	@Override
	public String[] getOutputNames()
	{
		return outputNames;
	}

	@Override
	public String[] getPropertyNames()
	{
		return propertyNames;
	}

	@Override
	protected void afterTimeSlices() throws DbCompException
	{
		// Hamon has no state to finalize after processing time slices.
	}
}
