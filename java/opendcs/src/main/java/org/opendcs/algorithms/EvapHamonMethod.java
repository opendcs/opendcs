package org.opendcs.algorithms;


import java.util.GregorianCalendar;

import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.python.icu.impl.CalendarAstronomer;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;

@Algorithm(description = "Compute daily evap using the Hamon Method.")
public class EvapHamonMethod extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final String EVAP_ROLE = "evap";
	private static final String EVAPCODE_ROLE = "evapSourceTypeCode";

	@Input
	public double AirTemp;
	@Input
	public double WindSpeed;
	@Input
	public double SolarIrradiance;
	@Input
	public double RelativeHumidity;


	@Output
	public NamedVariable evap = new NamedVariable(EVAP_ROLE, 0);
	@Output(description = "For systems that desire tracking evap types by code, assign a times series. Otherwise leave it empty.")
	public NamedVariable evapSourceTypeCode = new NamedVariable(EVAPCODE_ROLE, 0);

	@PropertySpec(value = "5", description = "For systems that track this value by a type code, set it here. An integer is expected.")
	public int evapTypeCode = 5;

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( ) throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
    @Override
	protected void beforeTimeSlices() throws DbCompException
	{
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
    @Override
	protected void doAWTimeSlice() throws DbCompException
	{
		GregorianCalendar cal = new GregorianCalendar(aggTZ);
		cal.setTime(_timeSliceBaseTime);
		
		// Enter code to be executed at each time-slice.
		//unit conversions
		WindSpeed *= 24 ;  //convert from kilometers per hour to Kilometers per day
		SolarIrradiance = SolarIrradiance * (60.0 * 24.0)/698.0 ; //convert from watts/(m^2 * min) to Langley/day (a Langley is one thermochemical calorie per square centimeter). Known as Qs in eqn 2.13
		double xcompl ;
		xcompl = 1.0 - (RelativeHumidity/100.0) ;
		 
		double TdC ;
		TdC = AirTemp - ((14.55 + (0.114*AirTemp))*xcompl + Math.pow((2.5 + 0.007*AirTemp)*xcompl, 3.0) + (15.9 + 0.117*AirTemp)*Math.pow(xcompl, 14.0)) ;
		double del_lambda1 ;
		del_lambda1 = Math.pow(1.0 + (0.66/Math.pow(0.00815*AirTemp+ 0.8912, 7.0)), -1.0) ;
		 
		double del_lambda2 ;
		del_lambda2 = 1.0 - del_lambda1 ;
		double Qn ;
		Qn = 0.00714*SolarIrradiance + 0.00000526*SolarIrradiance*Math.pow(AirTemp+17.8, 1.87) + 0.00000394*Math.pow(SolarIrradiance, 2.0) - 0.00000000239*Math.pow(SolarIrradiance, 2.0)*Math.pow(AirTemp - 7.2, 2.0) - 1.02 ;
		double es_ea ;
		es_ea = 33.86 * (Math.pow(0.00738*AirTemp + 0.8072, 8.0) - Math.pow(0.00738*TdC + 0.8072, 8.0)) ;
		double Ea ;
		Ea = Math.pow(es_ea, 0.88) * (0.42 + 0.0029*WindSpeed) ;
		double daily_evap=0.0 ;
		daily_evap = (del_lambda1*Qn + del_lambda2*Ea) / 25.4 ;
		//don't allow negative values (originally I was told that this equation would never result in negatives...that was wrong!)
		if(daily_evap <= 0.0)
		{
			daily_evap = 0.0 ;
		}
		//set the output time series
		setOutput(evap, daily_evap) ;
		if (this.isAssigned(EVAPCODE_ROLE))
		{
			//set the output time series of evap codes
			setOutput(evapSourceTypeCode, evapTypeCode);
		}
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
    @Override
	protected void afterTimeSlices() throws DbCompException
	{
	}


}
