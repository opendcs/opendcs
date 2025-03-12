package org.opendcs.algorithms;

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.util.DecodesException;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;

@Algorithm(description =
    "Evaporation Calculation using the Hamon Method. Only 24 hour interval data is currently support." +
    "The equation expects the following units:\n" + //
    "    param          |   units\n" + //
    "  -----------------+----------\n" + //
    "   Solar Radiation |  Watts per M^2   (enter w/m2 in the units field)\n" + //
    "   WindSpeed       |  kilometers per hour\n" + //
    "   Humidity        |  %\n" + //
    "   AirTemp         |  degrees Celsius\n" + //
    "\n" + //
    "   Evap            |  millimeter (you may specify different units now. it will affect only the display in compedit )\n"
)
public class EvapHamon extends decodes.tsdb.algo.AW_AlgorithmBase
{
    @Input
    public double tempAir24hour;
    @Input
    public double windSpeed24hour;
    @Input
    public double solarRadition24hour;
    @Input
    public double relativeHumidity24hour;
    

    @Output
    public NamedVariable evap24hour = new NamedVariable("evap24hour", 0);
    
    private UnitConverter evapUc;
    
    @Override
    protected void initAWAlgorithm( )
            throws DbCompException
    {
        _awAlgoType = AWAlgoType.TIME_SLICE;
    }
    
    @Override
    protected void beforeTimeSlices() throws DbCompException
    {
        EngineeringUnit eu = EngineeringUnit.getEngineeringUnit(getParmUnitsAbbr("tempAir24hour"));
        if ( evapUc == null )
        {
            evapUc = decodes.db.CompositeConverter.build(EngineeringUnit.getEngineeringUnit("mm"), eu);
        }
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
//AW:TIMESLICE
        // Enter code to be executed at each time-slice.
        //unit conversions
        double windSpeed24hourKmD  = windSpeed24hour * 24;  //convert from kilometers per hour to Kilometers per day
        //convert from watts/(m^2 * min) to Langley/day (a Langley is one thermochemical calorie per square centimeter). Known as Qs in eqn 2.13
        solarRadition24hour = solarRadition24hour * (60.0 * 24.0)/698.0; 
        double xcompl = 1.0 - (relativeHumidity24hour/100.0);

        double TdC = tempAir24hour - //NOSONAR
                        (
                        (14.55 + (0.114*tempAir24hour))*xcompl +
                            Math.pow((2.5 + 0.007*tempAir24hour)*xcompl, 3.0) +
                            (15.9 + 0.117*tempAir24hour)*Math.pow(xcompl, 14.0)
                    );
        double delLambda1 = Math.pow(1.0 + (0.66/Math.pow(0.00815*tempAir24hour+ 0.8912, 7.0)), -1.0);

        double delLambda2 = 1.0 - delLambda1;
        double Qn = 0.00714*solarRadition24hour + //NOSONAR
                    0.00000526*solarRadition24hour*Math.pow(tempAir24hour+17.8, 1.87) +
                    0.00000394*Math.pow(solarRadition24hour, 2.0) - 
                    0.00000000239*Math.pow(solarRadition24hour, 2.0)*Math.pow(tempAir24hour - 7.2, 2.0) - 1.02;
        double es_ea = 33.86 * (Math.pow(0.00738*tempAir24hour + 0.8072, 8.0) - Math.pow(0.00738*TdC + 0.8072, 8.0)); //NOSONAR
        double Ea = Math.pow(es_ea, 0.88) * (0.42 + 0.0029*windSpeed24hourKmD); //NOSONAR
        double dailyEvapMilliMeters = (delLambda1*Qn + delLambda2*Ea);
        double dailyEvap;
        try 
        {
            dailyEvap = evapUc.convert(dailyEvapMilliMeters);
        }
        catch (DecodesException ex)
        {
            throw new DbCompException("Unable to convert evap value from mm to " + evapUc.getToAbbr(), ex);
        }
        //don't allow negative values (originally I was told that this equation would never result in negatives...that was wrong!)
        if (dailyEvap <= 0.0)
        {
            dailyEvap = 0.0 ;
        }
        setOutput(evap24hour, dailyEvap);
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
            throws DbCompException
    {
        /* nothing to do */
    }
}
