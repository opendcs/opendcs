package decodes.tsdb.algo;

import opendcs.dai.TimeSeriesDAI;

import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(
		description = "Adds the current value to the previous value in the database\n" +
				"\t\tand outputs the sum. Works on any time-series, any interval.\n" +
				"\t\tThis algorithm does assume that you are calling it with a \n" +
				"\t\tseries of contiguous values, like you would get out of a DCP\n" +
				"\t\tmessage.")

public class AddToPrevious extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input;
	private double prevVal = 0.0;
	private boolean justStarted = true;

	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm()
			throws DbCompException {
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
			throws DbCompException {
		justStarted = true;
	}

	/**
	 * Do the algorithm for a single time slice.
	 * AW will fill in user-supplied code here.
	 * Base class will set inputs prior to calling this method.
	 * User code should call one of the setOutput methods for a time-slice
	 * output variable.
	 *
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
			throws DbCompException {
		if (justStarted) {
			ParmRef inputParmRef = getParmRef("input");
			CTimeSeries inputTS = inputParmRef.timeSeries;
			TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
			try {
				TimedVariable prevInput =
						timeSeriesDAO.getPreviousValue(inputTS,
								inputParmRef.compParm.baseTimeToParamTime(
										this._timeSliceBaseTime, aggCal));
				prevVal = prevInput.getDoubleValue();
			} catch (Exception e) {
				warning("Can't get prev value, time-slice at "
						+ debugSdf.format(_timeSliceBaseTime));
				prevVal = 0.0;
			} finally {
				timeSeriesDAO.close();
			}
			justStarted = false;
		}
		setOutput(output, input + prevVal);
		prevVal = input;
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
			throws DbCompException {
	}
}
