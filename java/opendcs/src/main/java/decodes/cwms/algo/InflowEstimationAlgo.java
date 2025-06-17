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
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.MissingAction;
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
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;


@Algorithm(
		description = "Preform Reservoir Evaporation calculation based on an algorithm developed by NWDM," +
				" Which utilizes air temp, air speed, solar radiation, and water temperature profiles to return" +
				" evaporation rates and total evaporation as flow")
public final class InflowEstimationAlgo extends AW_AlgorithmBase
{
	private static final Logger LOGGER = LoggerFactory.getLogger(InflowEstimationAlgo.class.getName());

	private CwmsRatingDao ratingDao;
	private Connection conn;
	private TimeSeriesDAI timeSeriesDAO;

	@Output(name = "inflow", description = "Inflow rate")
	public NamedVariable inflowTsId = new NamedVariable("inflow", "");

	@Input(name = "tailwater_tsid",
			description = "tailwater timeseries id. If provided, the tailwater_to_release_rating rating curve must be supplied. Can be regular or irregular. " +
					"If irregular, values will be averaged by weighting each irregular timestep to the second. Data evaluated as end of period. " +
					"Example: ARDB.Elev-Tailwater.Inst.0.0.BCHYDRO-RAW")
	public NamedVariable stagePoolTsId;
	@org.opendcs.annotations.PropertySpec(name = "tailwater_to_release_rating", propertySpecType = PropertySpec.STRING,
			description = "Rating Curve specification for tailwater/release curve, Example: BLUO.Stage;Flow.Linear.USGS-NWIS")
	public String tailwater_to_release_rating;
	@Input(name = "release_tsid",
			description = "Time Series Identifier for release time series. Can be used instead of the tailwater_tsid and tailwater_to_release_rating, " +
					"Example: BLU.Flow-Out.Inst.0.0.MIXED-COMPUTED-REV")
	public NamedVariable releaseTsId;
	@Input(name = "stage_pool_tsid",
			description = "The elevation timeseries id. If provided, the elevation stage_pool_storage_rating rating curve must be supplied. " +
					"Can be regular or irregular. If irregular, values will be averaged by weighting each irregular timestep to the second. Data evaluated as end of period.")
	public NamedVariable stagePool;
	@org.opendcs.annotations.PropertySpec(name = "stage_pool_storage_rating", propertySpecType = PropertySpec.STRING,
			description = "Rating Curve specification for stage stag pool/release curve, Example: BLUO.Stage;Flow.Linear.USGS-NWIS")
	public String stage_pool_storage_rating;
	@Input(name = "storage_tsid",
			description = "The time series id for reservoir storage. If stage pool time series is provided, this parameter is optional. Data evaluated as end of period.")
	public NamedVariable storageTsId;

	@Input(name = "outflow_1",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public NamedVariable outflowTsId1;
	@Input(name = "outflow_2",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public NamedVariable outflowTsId2;
	@Input(name = "outflow_3",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public NamedVariable outflowTsId3;
	@Input(name = "outflow_4",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public NamedVariable outflowTsId4;
	@Input(name = "outflow_5",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public NamedVariable outflowTsId5;
	@Input(name = "outflow_6",
			description = "Time Series Identifier for additional outflow time series, Example FTPK.Flow-Evap.Inst.0.0.Rev-NWO-Evap")
	public NamedVariable outflowTsId6;

	private final List<Double> constituents = new ArrayList<>();
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
		debug2("InflowEstimationAlgo::beforeTimeSlices");
	}

	@Override
	protected void doAWTimeSlice() throws DbCompException
	{
		debug2("InflowEstimationAlgo::doAWTimeSlice");
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
	}

	private void validateInputs() throws DbCompException
	{
		ParmRef outflow1 = getParmRef("outflow_1");
		ParmRef outflow2 = getParmRef("outflow_2");
		ParmRef outflow3 = getParmRef("outflow_3");
		ParmRef outflow4 = getParmRef("outflow_4");
		ParmRef outflow5 = getParmRef("outflow_5");
		ParmRef outflow6 = getParmRef("outflow_6");
		ParmRef releaseTsid = getParmRef("release_tsid");
		ParmRef tailwaterTsid = getParmRef("tailwater_tsid");
		ParmRef stagePoolTsid = getParmRef("stage_pool_tsid");
		ParmRef storageTsid = getParmRef("storage_tsid");
		outflow1.setMissingAction(MissingAction.IGNORE);
		outflow2.setMissingAction(MissingAction.IGNORE);
		outflow3.setMissingAction(MissingAction.IGNORE);
		outflow4.setMissingAction(MissingAction.IGNORE);
		outflow5.setMissingAction(MissingAction.IGNORE);
		outflow6.setMissingAction(MissingAction.IGNORE);
		releaseTsid.setMissingAction(MissingAction.IGNORE);
		tailwaterTsid.setMissingAction(MissingAction.IGNORE);
		stagePoolTsid.setMissingAction(MissingAction.IGNORE);
		storageTsid.setMissingAction(MissingAction.IGNORE);
		inflowTs = getParmRef("inflow").timeSeries;
		outflowTs1 = outflow1.timeSeries;
		outflowTs2 = outflow2.timeSeries;
		outflowTs3 = outflow3.timeSeries;
		outflowTs4 = outflow4.timeSeries;
		outflowTs5 = outflow5.timeSeries;
		outflowTs6 = outflow6.timeSeries;
		releaseTs = releaseTsid.timeSeries;
		tailwaterTs = tailwaterTsid.timeSeries;
		stagePoolTs = stagePoolTsid.timeSeries;
		storageTs = storageTsid.timeSeries;

		if((tailwaterTs == null || tailwaterTs.getTimeSeriesIdentifier() == null)
				&& (releaseTs == null || releaseTs.getTimeSeriesIdentifier() == null))
		{
			throw new DbCompException("InflowEstimationAlgo requires either tailwater_tsid or release_tsid to be set");
		}
		if((tailwaterTs != null && tailwaterTs.getTimeSeriesIdentifier() != null)
				&& tailwater_to_release_rating == null)
		{
			throw new DbCompException("InflowEstimationAlgo requires tailwater_to_release_rating to be set when tailwater_tsid is set");
		}
		if((stagePoolTs == null || stagePoolTs.getTimeSeriesIdentifier() == null)
				&& (storageTs == null || storageTs.getTimeSeriesIdentifier() == null))
		{
			throw new DbCompException("InflowEstimationAlgo requires either stage_pool_tsid or storage_tsid to be set");
		}
		if(stagePoolTs != null && stagePoolTs.getTimeSeriesIdentifier() != null && stage_pool_storage_rating == null)
		{
			throw new DbCompException("InflowEstimationAlgo requires stage_pool_storage_rating to be set when stage_pool_tsid is set");
		}
		TimeSeriesIdentifier timeSeriesIdentifier = inflowTs.getTimeSeriesIdentifier();
		if(!(timeSeriesIdentifier instanceof CwmsTsId))
		{
			throw new DbCompException("InflowEstimationAlgo requires a CwmsTsId time series identifier");
		}
		aggPeriodInterval = timeSeriesIdentifier.getInterval();
		durationPeriod = ((CwmsTsId) timeSeriesIdentifier).getDuration();
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
		if(stage_pool_storage_rating != null)
		{
			try
			{
				stageStorRatingSet = ratingDao.getRatingSet(stage_pool_storage_rating);
			}
			catch(RatingException ex)
			{
				throw new DbCompException("Failed to load rating table for: " + stage_pool_storage_rating, ex);
			}
		}
		if(tailwater_to_release_rating != null)
		{
			try
			{
				tailwaterReleaseRatingSet = ratingDao.getRatingSet(tailwater_to_release_rating);
			}
			catch(RatingException ex)
			{
				throw new DbCompException("Failed to load rating table for: " + tailwater_to_release_rating, ex);
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
		debug2("InflowEstimationAlgo::afterTimeSlices");
		if(!IntervalOffsetUtil.matchesIntervalOffset(inflowTs, _aggregatePeriodEnd))
		{
			return;
		}
		try
		{
			double release = calculateRelease();
			double holdout = calculateHoldout();
			constituents.clear();
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
			LOGGER.atDebug().setCause(ex).log("Not enough data found to perform inflow calculation");
			debug1("Not enough data found to perform inflow calculation: " + ex.getMessage());
		}
	}

	@Override
	public void afterAllTimeSlices()
	{
		tsdb.freeConnection(conn);
		ratingDao.close();
		timeSeriesDAO.close();
	}

	private void aggregateAllTimeSeries(List<Double> constituents)
	{

		double flow = 0.0;
		for(Double constituent : constituents)
		{
			flow += constituent;
		}
		setOutput(inflowTsId, flow);
	}

	private double averageOverTimestep(NavigableMap<Long, Double> values) throws NotEnoughDataException
	{
		double weightedSum = 0.0;
		double totalWeights = 0.0;

		long windowEnd = _aggregatePeriodEnd.getTime();
		long durationMs = IntervalCodes.getIntervalSeconds(durationPeriod) * 1000L;
		long windowStart = windowEnd - durationMs;
		//Calculate as end-of-period stepped
		Long s = values.ceilingKey(windowStart);
		while(s != null)
		{
			Long prev = values.lowerKey(s);
			long segStart = Math.max(prev == null ? Long.MIN_VALUE : prev, windowStart);
			long segEnd = Math.min(s, windowEnd);

			if(segEnd > segStart)
			{
				double weight = (double) (segEnd - segStart) / (double) durationMs;
				weightedSum += weight * values.get(s);
				totalWeights += weight;
			}

			if(s >= windowEnd)
			{
				break;
			}
			s = values.higherKey(s);
		}
		if(totalWeights == 0.0)
		{
			throw new NotEnoughDataException(
					"Not enough data to perform period average computation to interval seconds: " + durationPeriod
			);
		}
		return weightedSum / totalWeights;
	}

	private void extendTimeSeries(CTimeSeries timeSeries) throws DbCompException
	{
		Date start = Date.from(_aggregatePeriodEnd.toInstant().minusSeconds(IntervalCodes.getIntervalSeconds(durationPeriod)));
		if(timeSeries.findWithin(start, 0) != null && timeSeries.findWithin(_aggregatePeriodEnd, 0) != null)
		{
			return;
		}
		try
		{
			timeSeriesDAO.fillTimeSeries(timeSeries, start, _aggregatePeriodEnd);
		}
		catch(DbIoException | BadTimeSeriesException e)
		{
			throw new DbCompException("Could not retrieve time series: " + timeSeries.getTimeSeriesIdentifier(), e);
		}
	}

	private double calculateHoldout() throws DbCompException, NotEnoughDataException, NoConversionException
	{
		NavigableMap<Long, Double> storageRaw;
		if(stagePoolTs != null && stagePoolTs.getTimeSeriesIdentifier() != null)
		{
			extendTimeSeries(stagePoolTs);
			TSUtil.convertUnits(stagePoolTs, stagePoolTs.getTimeSeriesIdentifier().getStorageUnits());
			storageRaw = rate(conn, stagePoolTs, stageStorRatingSet);
			debug2(_aggregatePeriodEnd + ": rated storage: " + storageRaw);
		}
		else
		{
			extendTimeSeries(storageTs);
			TSUtil.convertUnits(storageTs, storageTs.getTimeSeriesIdentifier().getStorageUnits());
			storageRaw = getValues(storageTs);
			debug2(_aggregatePeriodEnd + ": storage ts: " + storageRaw);
		}
		//Units are in m3 - calculated to cms
		double holdout = calculateHoldout(storageRaw);
		debug2(_aggregatePeriodEnd + ": averaged holdout: " + holdout);
		return holdout;
	}

	private double calculateHoldout(NavigableMap<Long, Double> values) throws NotEnoughDataException
	{
		double value = findInterp(values, _aggregatePeriodEnd);
		Date previousInterval = Date.from(_aggregatePeriodEnd.toInstant()
				.minusSeconds(IntervalCodes.getIntervalSeconds(durationPeriod)));
		double previousVariable = findInterp(values, previousInterval);
		return (value - previousVariable) / IntervalCodes.getIntervalSeconds(durationPeriod);

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
			throw new NotEnoughDataException("No previous value found for time: " + time);
		}
		long timeRange = next - prev;
		if(timeRange == 0)
		{
			return values.get(time.getTime());
		}

		double pos = (double) (next - prev) / (double) timeRange;

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
			debug2(_aggregatePeriodEnd + ": rated release: " + releaseRaw);
		}
		else
		{
			extendTimeSeries(releaseTs);
			TSUtil.convertUnits(releaseTs, releaseTs.getTimeSeriesIdentifier().getStorageUnits());
			releaseRaw = getValues(releaseTs);
			debug2(_aggregatePeriodEnd + ": release ts: " + releaseRaw);
		}
		double release = averageOverTimestep(releaseRaw);
		debug2(_aggregatePeriodEnd + ": averaged release: " + release);
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
