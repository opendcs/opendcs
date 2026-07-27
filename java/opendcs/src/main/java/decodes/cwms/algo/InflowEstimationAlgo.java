package decodes.cwms.algo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import decodes.cwms.CwmsConstants;
import decodes.cwms.HecConstants;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.ParmRef;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.util.PropertySpec;
import decodes.util.TSUtil;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.algorithms.NotEnoughDataException;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;


@Algorithm(
		description = "Reservoir Inflow calculation based on an algorithm developed by NWP," +
				" Which utilizes storage change, releases, and additional outflows to calculate release " +
				" based on the output time series duration.")
public final class InflowEstimationAlgo extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private CwmsRatingDao ratingDao;
	private Connection conn;
	private TimeSeriesDAI timeSeriesDAO;

	// Required: the annotated role name and Java field name must match.
	@Output(name = "inflow", description = "Inflow rate")
	public NamedVariable inflow = new NamedVariable("inflow", "");

	// Required: AW_AlgorithmBase accepts primitive input fields. The time series
	// themselves are obtained below from the matching ParmRefs.
	@Input(name = "tailwaterTsId",
			description = "tailwater timeseries id. If provided, the tailwater_to_release_rating rating curve must be supplied. Can be regular or irregular. " +
					"If irregular, values will be averaged by weighting each irregular timestep to the second. Data evaluated as end of period. " +
					"Example: ARDB.Elev-Tailwater.Inst.0.0.BCHYDRO-RAW")
	public double tailwaterTsId;
	@org.opendcs.annotations.PropertySpec(name = "tailwaterToReleaseRating", propertySpecType = PropertySpec.STRING,
			description = "Rating Curve specification for tailwater/release curve, Example: BLUO.Stage;Flow.Linear.USGS-NWIS")
	public String tailwaterToReleaseRating;
	@Input(name = "releaseTsId",
			description = "Time Series Identifier for release time series. Can be used instead of the tailwater_tsid and tailwater_to_release_rating, " +
					"Example: BLU.Flow-Out.Inst.0.0.MIXED-COMPUTED-REV")
	public double releaseTsId;
	@Input(name = "stagePoolTsId",
			description = "The elevation timeseries id. If provided, the elevation stage_pool_storage_rating rating curve must be supplied. " +
					"Can be regular or irregular. If irregular, values will be averaged by weighting each irregular timestep to the second. Data evaluated as end of period.")
	public double stagePoolTsId;
	@org.opendcs.annotations.PropertySpec(name = "stagePoolStorageRating", propertySpecType = PropertySpec.STRING,
			description = "Rating Curve specification for stage stag pool/release curve, Example: BLUO.Stage;Flow.Linear.USGS-NWIS")
	public String stagePoolStorageRating;
	@Input(name = "storageTsId",
			description = "The time series id for reservoir storage. If stage pool time series is provided, this parameter is optional. Data evaluated as end of period.")
	public double storageTsId;

	@Input(name = "outflowTsId1",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public double outflowTsId1;
	@Input(name = "outflowTsId2",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public double outflowTsId2;
	@Input(name = "outflowTsId3",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public double outflowTsId3;
	@Input(name = "outflowTsId4",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public double outflowTsId4;
	@Input(name = "outflowTsId5",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public double outflowTsId5;
	@Input(name = "outflowTsId6",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public double outflowTsId6;

	private CTimeSeries inflowTs;
	private CTimeSeries outflowTs1;
	private CTimeSeries outflowTs2;
	private CTimeSeries outflowTs3;
	private CTimeSeries outflowTs4;
	private CTimeSeries outflowTs5;
	private CTimeSeries outflowTs6;
	private CTimeSeries tailwaterTs;
	private CTimeSeries releaseTs;
	private CTimeSeries stagePoolTs;
	private CTimeSeries storageTs;
	private RatingSet tailwaterReleaseRatingSet;
	private RatingSet stageStorRatingSet;
	private String durationPeriod;
	// Required for new CWMS output series, whose persisted UTC offset is initially undefined.
	private Integer effectiveOutputOffsetSeconds;

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm() throws DbCompException
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		aggLowerBoundClosed = false;
		aggUpperBoundClosed = true;
	}

	@Override
	protected void beforeTimeSlices() throws DbCompException
	{
		// No per-time-slice initialization is required.
	}

	@Override
	protected void doAWTimeSlice() throws DbCompException
	{
		// Intentionally left blank. The inflow calculation is performed in afterTimeSlices() to ensure that
		// all time slices are available for the aggregation.
	}

	private void validateOutput() throws DbCompException
	{
		inflowTs = getParmRef("inflow").timeSeries;
		TimeSeriesIdentifier timeSeriesIdentifier = inflowTs.getTimeSeriesIdentifier();
		if(!(timeSeriesIdentifier instanceof CwmsTsId))
		{
			throw new DbCompException("InflowEstimationAlgo requires a CwmsTsId time series identifier");
		}
		CwmsTsId cwmsTsId = (CwmsTsId) timeSeriesIdentifier;
		int intervalSeconds = IntervalCodes.getIntervalSeconds(cwmsTsId.getInterval());
		if(intervalSeconds == 0)
		{
			throw new DbCompException("InflowEstimationAlgo cannot calculate inflow for an irregular time series");
		}
		String paramType = cwmsTsId.getParamType();
		if(!paramType.equalsIgnoreCase(CwmsConstants.PARAM_TYPE_AVE))
		{
			throw new DbCompException("InflowEstimationAlgo can only calculate inflow for an average time series");
		}
		int durationSeconds = IntervalCodes.getIntervalSeconds(cwmsTsId.getDuration());
		if(durationSeconds == 0)
		{
			throw new DbCompException("InflowEstimationAlgo cannot calculate inflow for a zero duration period");
		}
		aggPeriodInterval = cwmsTsId.getInterval();
		// A computation instance can be reused; do not carry a previous run's selected offset forward.
		effectiveOutputOffsetSeconds = null;
	}

	private void validateInputs() throws DbCompException
	{
		ParmRef outflow1 = getParmRef("outflowTsId1");
		ParmRef outflow2 = getParmRef("outflowTsId2");
		ParmRef outflow3 = getParmRef("outflowTsId3");
		ParmRef outflow4 = getParmRef("outflowTsId4");
		ParmRef outflow5 = getParmRef("outflowTsId5");
		ParmRef outflow6 = getParmRef("outflowTsId6");
		ParmRef releaseTsid = getParmRef("releaseTsId");
		ParmRef tailwaterTsid = getParmRef("tailwaterTsId");
		ParmRef stagePoolTsid = getParmRef("stagePoolTsId");
		ParmRef storageTsid = getParmRef("storageTsId");
		// Alternative roles may be omitted from the computation XML. In that case
		// getParmRef returns null; the null-safe getTimeSeries calls below allow the
		// alternative-input validation to report a useful configuration error.
		inflowTs = getParmRef("inflow").timeSeries;
		outflowTs1 = getTimeSeries(outflow1);
		outflowTs2 = getTimeSeries(outflow2);
		outflowTs3 = getTimeSeries(outflow3);
		outflowTs4 = getTimeSeries(outflow4);
		outflowTs5 = getTimeSeries(outflow5);
		outflowTs6 = getTimeSeries(outflow6);
		releaseTs = getTimeSeries(releaseTsid);
		tailwaterTs = getTimeSeries(tailwaterTsid);
		stagePoolTs = getTimeSeries(stagePoolTsid);
		storageTs = getTimeSeries(storageTsid);

		if((tailwaterTs == null || tailwaterTs.getTimeSeriesIdentifier() == null)
				&& (releaseTs == null || releaseTs.getTimeSeriesIdentifier() == null))
		{
			throw new DbCompException("InflowEstimationAlgo requires either tailwaterTsId or releaseTsId to be set");
		}
		if((tailwaterTs != null && tailwaterTs.getTimeSeriesIdentifier() != null)
				&& tailwaterToReleaseRating == null)
		{
			throw new DbCompException("InflowEstimationAlgo requires tailwaterToReleaseRating to be set when tailwaterTsId is set");
		}
		if((stagePoolTs == null || stagePoolTs.getTimeSeriesIdentifier() == null)
				&& (storageTs == null || storageTs.getTimeSeriesIdentifier() == null))
		{
			throw new DbCompException("InflowEstimationAlgo requires either stagePoolTsId or storageTsId to be set");
		}
		if(stagePoolTs != null && stagePoolTs.getTimeSeriesIdentifier() != null && stagePoolStorageRating == null)
		{
			throw new DbCompException("InflowEstimationAlgo requires stagePoolStorageRating to be set when stagePoolTsId is set");
		}
		TimeSeriesIdentifier timeSeriesIdentifier = inflowTs.getTimeSeriesIdentifier();
		if(!(timeSeriesIdentifier instanceof CwmsTsId))
		{
			throw new DbCompException("InflowEstimationAlgo requires a CwmsTsId time series identifier");
		}
		aggPeriodInterval = timeSeriesIdentifier.getInterval();
		durationPeriod = ((CwmsTsId) timeSeriesIdentifier).getDuration();
	}

	/** Returns no series for an omitted optional role rather than dereferencing a null ParmRef. */
	private CTimeSeries getTimeSeries(ParmRef parmRef)
	{
		return parmRef == null ? null : parmRef.timeSeries;
	}

	@Override
	public void beforeAllTimeSlices() throws DbCompException
	{
		validateInputs();
		validateOutput();
		timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		try
		{
			conn = tsdb.getConnection();
		}
		catch(SQLException ex)
		{
			throw new DbCompException("Unable to acquire required connection.", ex);
		}

		ratingDao = new CwmsRatingDao((CwmsTimeSeriesDb) tsdb);
		loadRatingSets();
	}

	private void loadRatingSets() throws DbCompException
	{
		if(stagePoolStorageRating != null)
		{
			try
			{
				stageStorRatingSet = ratingDao.getRatingSet(stagePoolStorageRating);
			}
			catch(RatingException ex)
			{
				throw new DbCompException("Failed to load rating table for: " + stagePoolStorageRating, ex);
			}
		}
		if(tailwaterToReleaseRating != null)
		{
			try
			{
				tailwaterReleaseRatingSet = ratingDao.getRatingSet(tailwaterToReleaseRating);
			}
			catch(RatingException ex)
			{
				throw new DbCompException("Failed to load rating table for: " + tailwaterToReleaseRating, ex);
			}
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
			throws DbCompException
	{
		log.atDebug().log("InflowEstimationAlgo::afterTimeSlices");
		if(!matchesInflowIntervalOffset(inflowTs, _aggregatePeriodEnd))
		{
			return;
		}
		try
		{
			double release = calculateRelease();
			double holdout = calculateHoldout();
			List<Double> constituents = new ArrayList<>();
			constituents.add(release);
			constituents.add(holdout);
			for(CTimeSeries additionalOutflow : Arrays.asList(outflowTs1, outflowTs2, outflowTs3, outflowTs4,
					outflowTs5, outflowTs6))
			{
				if(additionalOutflow != null && additionalOutflow.getTimeSeriesIdentifier() != null)
				{
					extendTimeSeries(additionalOutflow);
					TSUtil.convertUnits(additionalOutflow, additionalOutflow.getTimeSeriesIdentifier().getStorageUnits());
					double averagedOutflow = averageOverTimestep(getValues(additionalOutflow));
					constituents.add(averagedOutflow);
				}
			}
			aggregateAllTimeSeries(constituents);
		}
		catch(NoConversionException ex)
		{
			throw new DbCompException("Error calculating inflow", ex);
		}
		catch(NotEnoughDataException ex)
		{
			log.atDebug().setCause(ex).log("Not enough data found to perform inflow calculation");
		}
	}

	@Override
	public void alwaysAfterTimeSlices()
	{
		// closing connection and releasing resources here as this hook runs even when computation fails.
		if(conn != null)
		{
			try
			{
				// connection object returned by getConnection will be a WrappedConnection that correctly
				// handles if the instance needs to be closed or not
				conn.close();
			}
			catch(SQLException ex)
			{
				log.atWarn().setCause(ex).log("Unable to close inflow-estimation connection");
			}
		}
		if(ratingDao != null)
		{
			ratingDao.close();
		}
		if(timeSeriesDAO != null)
		{
			timeSeriesDAO.close();
		}
	}

	private void aggregateAllTimeSeries(List<Double> constituents)
	{

		double flow = 0.0;
		for(Double constituent : constituents)
		{
			flow += constituent;
		}
		setOutput(inflow, flow);
	}

	private boolean matchesInflowIntervalOffset(CTimeSeries timeSeries, Date date)
	{
		TimeSeriesIdentifier timeSeriesIdentifier = timeSeries.getTimeSeriesIdentifier();
		if(timeSeriesIdentifier instanceof CwmsTsId)
		{
			CwmsTsId cwmsTsId = (CwmsTsId) timeSeriesIdentifier;
			Integer utcOffset = cwmsTsId.getUtcOffset();
			if(utcOffset == null
					|| utcOffset == HecConstants.UNDEFINED_UTC_OFFSET
					|| utcOffset == HecConstants.NO_UTC_OFFSET)
			{
				// A new CWMS output does not yet have a persisted offset. Select the
				// first eligible one so all values saved in this run use one offset.
				int candidateOffset = IntervalOffsetUtil.getIntervalOffsetForTime(
						IntervalCodes.getInterval(timeSeries.getInterval()), date);
				if(effectiveOutputOffsetSeconds == null)
				{
					effectiveOutputOffsetSeconds = candidateOffset;
					log.info("Output '{}' has no defined CWMS UTC offset; using offset={} seconds from "
							+ "the first eligible period end {} for this computation run.",
							cwmsTsId.getUniqueString(), effectiveOutputOffsetSeconds, date);
				}
				boolean matches = IntervalOffsetUtil.matchesInflowIntervalOffset(timeSeries, date,
						effectiveOutputOffsetSeconds);
				if(!matches)
				{
					log.trace("Skipping period end {} with interval offset={} seconds; run offset={} seconds.",
							date, candidateOffset, effectiveOutputOffsetSeconds);
				}
				return matches;
			}
		}
		return IntervalOffsetUtil.matchesInflowIntervalOffset(timeSeries, date, effectiveOutputOffsetSeconds);
	}

	private double averageOverTimestep(NavigableMap<Long, Double> values) throws NotEnoughDataException
	{
		return averageEndOfPeriodValues(values, _aggregatePeriodEnd,
				IntervalCodes.getIntervalSeconds(durationPeriod));
	}

	/**
	 * Calculates a duration-weighted average of end-of-period stepped values.
	 * Each sample applies from the preceding sample through its timestamp.
	 */
	static double averageEndOfPeriodValues(NavigableMap<Long, Double> values, Date windowEnd,
			long durationSeconds) throws NotEnoughDataException
	{
		double weightedSum = 0.0;
		double totalWeights = 0.0;

		long windowEndMs = windowEnd.getTime();
		long durationMs = durationSeconds * 1000L;
		long windowStart = windowEndMs - durationMs;
		//Calculate as end-of-period stepped
		Long s = values.ceilingKey(windowStart);
		while(s != null)
		{
			Long prev = values.lowerKey(s);
			long segStart = Math.max(prev == null ? Long.MIN_VALUE : prev, windowStart);
			long segEnd = Math.min(s, windowEndMs);

			if(segEnd > segStart)
			{
				double weight = (double) (segEnd - segStart) / (double) durationMs;
				weightedSum += weight * values.get(s);
				totalWeights += weight;
			}

			if(s >= windowEndMs)
			{
				break;
			}
			s = values.higherKey(s);
		}
		if(totalWeights == 0.0)
		{
			throw new NotEnoughDataException(
					"Not enough data to perform period average computation over " + durationSeconds + " seconds."
			);
		}
		return weightedSum / totalWeights;
	}

	private void extendTimeSeries(CTimeSeries timeSeries) throws DbCompException
	{
		Date start = Date.from(_aggregatePeriodEnd.toInstant().minusSeconds(IntervalCodes.getIntervalSeconds(durationPeriod)));
		TSUtil.extendTimeSeries(timeSeriesDAO, timeSeries, start, _aggregatePeriodEnd);
	}

	private double calculateHoldout() throws DbCompException, NotEnoughDataException, NoConversionException
	{
		NavigableMap<Long, Double> storageRaw;
		if(stagePoolTs != null && stagePoolTs.getTimeSeriesIdentifier() != null)
		{
			extendTimeSeries(stagePoolTs);
			TSUtil.convertUnits(stagePoolTs, stagePoolTs.getTimeSeriesIdentifier().getStorageUnits());
			storageRaw = rate(conn, stagePoolTs, stageStorRatingSet);
			log.atDebug().log(_aggregatePeriodEnd + ": rated storage: " + storageRaw);
		}
		else
		{
			extendTimeSeries(storageTs);
			TSUtil.convertUnits(storageTs, storageTs.getTimeSeriesIdentifier().getStorageUnits());
			storageRaw = getValues(storageTs);
			log.atDebug().log(_aggregatePeriodEnd + ": storage ts: " + storageRaw);
		}
		//Units are in m3 - calculated to cms
		double holdout = calculateHoldout(storageRaw);
		log.atDebug().log(_aggregatePeriodEnd + ": averaged holdout: " + holdout);
		return holdout;
	}

	private double calculateHoldout(NavigableMap<Long, Double> values) throws NotEnoughDataException
	{
		return calculateStorageChangeRate(values, _aggregatePeriodEnd,
				IntervalCodes.getIntervalSeconds(durationPeriod));

	}

	/** Calculates the storage-change contribution to inflow in cubic metres per second. */
	static double calculateStorageChangeRate(NavigableMap<Long, Double> values, Date windowEnd,
			long durationSeconds) throws NotEnoughDataException
	{
		double endingStorage = findInterp(values, windowEnd);
		Date windowStart = Date.from(windowEnd.toInstant().minusSeconds(durationSeconds));
		double startingStorage = findInterp(values, windowStart);
		return (endingStorage - startingStorage) / durationSeconds;
	}

	private static double findInterp(NavigableMap<Long, Double> values, Date time) throws NotEnoughDataException
	{
		Long prev = values.floorKey(time.getTime());
		if(prev == null)
		{
			throw new NotEnoughDataException("No previous value found for time: " + time);
		}
		Long next = values.ceilingKey(time.getTime());
		if(next == null)
		{
			throw new NotEnoughDataException("No next value found for time: " + time);
		}
		long timeRange = next - prev;
		if(timeRange == 0)
		{
			return values.get(time.getTime());
		}

		// Fraction of the interval from the preceding sample to the requested time.
		double pos = (double) (time.getTime() - prev) / (double) timeRange;

		double prevVal = values.get(prev);
		double nextVal = values.get(next);
		return prevVal + (nextVal - prevVal) * pos;
	}

	private double calculateRelease() throws DbCompException, NotEnoughDataException, NoConversionException
	{
		NavigableMap<Long, Double> releaseRaw;
		if(tailwaterTs != null && tailwaterTs.getTimeSeriesIdentifier() != null)
		{
			extendTimeSeries(tailwaterTs);
			TSUtil.convertUnits(tailwaterTs, tailwaterTs.getTimeSeriesIdentifier().getStorageUnits());
			releaseRaw = rate(conn, tailwaterTs, tailwaterReleaseRatingSet);
			log.atDebug().log(_aggregatePeriodEnd + ": rated release: " + releaseRaw);
		}
		else
		{
			extendTimeSeries(releaseTs);
			TSUtil.convertUnits(releaseTs, releaseTs.getTimeSeriesIdentifier().getStorageUnits());
			releaseRaw = getValues(releaseTs);
			log.atDebug().log(_aggregatePeriodEnd + ": release ts: " + releaseRaw);
		}
		double release = averageOverTimestep(releaseRaw);
		log.atDebug().log(_aggregatePeriodEnd + ": averaged release: " + release);
		return release;
	}

	private NavigableMap<Long, Double> getValues(CTimeSeries timeSeries) throws NoConversionException
	{
		NavigableMap<Long, Double> retval = new TreeMap<>();
		for(int i = 0; i < timeSeries.size(); i++)
		{
			TimedVariable timedVariable = timeSeries.sampleAt(i);
			retval.put(timedVariable.getTime().getTime(), timedVariable.getDoubleValue());
		}
		return retval;
	}


	private static NavigableMap<Long, Double> rate(Connection conn, CTimeSeries timeSeries, RatingSet ratingSet) throws DbCompException
	{
		try
		{
			String depUnits;
			if(ratingSet.getRatingSpec().getDepParameter().startsWith("Stor"))
			{
				depUnits = "m3";
			}
			else
			{
				depUnits = "cms";
			}
			ratingSet.setDataUnits(conn, new String[]{timeSeries.getUnitsAbbr(), depUnits});
			long[] times = new long[timeSeries.size()];
			double[] values = new double[timeSeries.size()];
			NavigableMap<Long, Double> retval = new TreeMap<>();
			for(int i = 0; i < timeSeries.size(); i++)
			{
				TimedVariable timedVariable = timeSeries.sampleAt(i);
				times[i] = timedVariable.getTime().getTime();
				values[i] = timedVariable.getDoubleValue();
			}
			//Should be faster to iterate twice over the data set and rate the whole block
			//than rating each value at a time
			double[] rate = ratingSet.rate(conn, times, values);
			for(int i = 0; i < times.length; i++)
			{
				retval.put(times[i], rate[i]);
			}
			return retval;
		}
		catch(RatingException | NoConversionException e)
		{
			throw new DbCompException("Failed to rate time series", e);
		}
	}

}
