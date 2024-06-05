package org.opendcs.algorithms;

import decodes.util.DecodesException;
import java.util.Date;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;

//AW:IMPORTS
// Place an import statements you need here.
import decodes.cwms.CwmsFlags;
import decodes.db.EngineeringUnit;
import java.util.GregorianCalendar;
import java.util.Calendar;
//AW:IMPORTS_END

import org.slf4j.LoggerFactory;

//AW:JAVADOC
/**
 * <pre>
Computes the Penmann Monteith evap calculation. Based on the document "The ASCE STANDARDIZED REFERENCE EVAPOTRANSPIRATION EQUATION" December 21, 2001   revised july 9, 2002
The equation expects the following units:
    param          |   units
  -----------------+----------
   Solar Radiation |  Watts per M^2   ( enter w/m2 in CCP ) ( NOTE: short wave radiation value expected )
   WindSpeed       |  meters per second
   Humidity        |  %
   AirTemp         |  degrees Celsius
   BaroPressure    |  kPa

   Evap            |  millimeter (you may specify different units now. it will affect only the display in compedit )

 the algorithm assumes 15 minute data
 output should be daily

  value e_a ( actual saturation vapor pressure ) will be calculated using equation 41 from the reference

  If not every set of data is present data will be marked Quesitionable
  E.G. if an Rel Humdity value was missing, but the Temp wasn't, the average temp will include all Temp values
  but e_a will not include that period.
  e_a is calculated with the e_naught equation for every timeslice instead of using the min and max Humidity values

  For Net Long Wave radiation the algorithm uses the entire comp from the reference above. The sutron calculation uses an arbitrary 64 langleys/min
  in it's Net Radiation equation then converts to Megajoules.
 * </pre>
 * @author l2eddman
 */
//AW:JAVADOC_END
public class EvapPennmanMonteith extends decodes.tsdb.algo.AW_AlgorithmBase
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(EvapPennmanMonteith.class);
//AW:INPUTS
    // input values, declare a variable, and add the string of the variable name to the _inputNames array
    public double SolarRadiation; //AW:TYPECODE=i
    public double RelativeHumidity; //AW:TYPECODE=i
    public double WindSpeed; //AW:TYPECODE=i
    public double AirTemp; //AW:TYPECODE=i
    public double BaroPressure; //AW:TYPECODE=i
    String _inputNames[] = { "AirTemp", "WindSpeed", "SolarRadiation", "RelativeHumidity", "BaroPressure"};
//AW:INPUTS_END

//AW:LOCALVARS
    // Enter any local class variables needed by the algorithm.
        /**
         * the initial evap calculation
         */
    public double evap = 0.0; //output value
    /**
     * number of good periods of data we have
     */
    public int count = 0; // total count
    /**
     * quality flags, based mostly on the quality flags of the input data
     */
    public int flags = 0; // data validity flags
    /**
     * The Latitude of the station in radians, the user supplies the property in decimal degeress
     * and the algorithm initialization function will convert to radians
     */
    public double radian_latitude = 0.0; // site latitude in radians
    public double radian_latitude_center_tz = 0.0; // site latitude in radians

    /**
     * total solar energy into the system
     */
    public double total_radiation = 0.0;
    public int count_rad_values = 0;


    public double avg_temp = 0.0;
    public double max_temp = -1*Double.MAX_VALUE;
    public double min_temp = Double.MAX_VALUE;
    public int count_temp_values = 0;

    /**
     * Average pressure for the day
     */
    public double avg_pressure = 0.0;
    public int    avg_pressure_counts = 0;

    public double windspeed_avg = 0.0;
    public double count_wind_values = 0;

    /**
     * \@ 1.5 to 2.5 meters in kPa, daily timestep, average of saturation vapor press at max/min air temp
     */
    public double saturation_vapor_pressure = 0.0;
    /**
     * \@ 1.5 to 2.5 meters in kPa, ( will
     */
    public double mean_actual_vapor_pressure = 0.0;
    /**
     * required temp and RH to exist for each timestep
     */
    public int    count_actual_vapor_pressure_values = 0;
    /*
     * numerator constant, changes with reference (water,grass, etc) and time step
     */
    public double C_n = 900.0;
    /*
     * denomanator constant, changes with reference (water,grass, etc) and time step
     */
    public double C_d = 0.34;

    /*
     * Solar Constant, 4.92 MJ/m2/h
     */
    public final double Gsc = 4.92; //
    /*
     *  Stefan-Boltzmann constant 4.903*0.000000001 (MJ/k4/m2/d)
     */
    public final double sigma =    4.903*0.000000001; // Stefan-Boltzmann constant (MJ/k4/m2/d)
    public final double sigma_hr = sigma/24;

    /*
     *  We need to know if we're on an hourly time step
     */
    public boolean isHourly = false;

    /* helper stuff
        *
        */
    decodes.db.UnitConverter uc = null;
//AW:LOCALVARS_END

//AW:OUTPUTS
    // created a NameVariable with the name you want, and add the string of that name to the array
    public NamedVariable Evap = new NamedVariable( "Evap", 0 );
    String _outputNames[] = { "Evap" };
//AW:OUTPUTS_END

//AW:PROPERTIES
    public boolean UsingNetRadiation = false;
    public double  Elevation = 0.0;
    public double  WindSpeedHeight = 0.0;
    public double  Albedo = 0.06;
    public double  MinSamples = 72; // assume okay if we have more than 75% of the values
    public double  latitude = 0.0;
    public double  latitude_center_tz = 120.0;
    String _propertyNames[] = { "UsingNetRadiation", "Elevation", "WindSpeedHeight", "Albedo", "latitude", "MinSamples", "latitude_center_tz" };
//AW:PROPERTIES_END

    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    protected void initAWAlgorithm( )
        throws DbCompException
    {
//AW:INIT
    _awAlgoType = AWAlgoType.AGGREGATING;
    // create an output variable and give it's name here
    // this variable will determine the output interval
    _aggPeriodVarRoleName = "Evap";
//AW:INIT_END

//AW:USERINIT

//AW:USERINIT_END
    }

    /**
     * This method is called once before iterating all time slices.
     */
    protected void beforeTimeSlices() throws DbCompException
    {
//AW:BEFORE_TIMESLICES
        // This code will be executed once before each group of time slices.
        // For TimeSlice algorithms this is done once before all slices.
        // For Aggregating algorithms, this is done before each aggregate
        // period.
        avg_temp = 0;
        max_temp = -1*Double.MAX_VALUE;
        min_temp = Double.MAX_VALUE;
        count_temp_values = 0;
        count = 0;
        count_actual_vapor_pressure_values = 0;
        count_rad_values = 0;
        count_wind_values = 0;
        total_radiation = 0;
        mean_actual_vapor_pressure = 0.0;
        windspeed_avg = 0.0;
        flags = 0;
        evap = 0.0;
        avg_pressure = 0.0;
        avg_pressure_counts = 0;
        //setOutputUnitsAbbr("Evap", "mm");
        decodes.db.EngineeringUnit eu = EngineeringUnit.getEngineeringUnit(getParmUnitsAbbr("Evap"));
        if ( uc == null )
        {
            uc = decodes.db.CompositeConverter.build(EngineeringUnit.getEngineeringUnit("mm"), eu);
        }

        radian_latitude = (Math.PI/180.0)*latitude;
        radian_latitude_center_tz = (Math.PI/180.0)*latitude_center_tz;
        String interval = getInterval("Evap");
        isHourly = interval.equalsIgnoreCase("1hour");
        if (isHourly)
        {
            C_n = 37;
        }

//AW:BEFORE_TIMESLICES_END
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
    protected void doAWTimeSlice() throws DbCompException
    {
//AW:TIMESLICE
        // Enter code to be executed at each time-slice.
        if (isMissing(AirTemp) && isMissing(RelativeHumidity)
         && isMissing(WindSpeed) && isMissing(SolarRadiation) && isMissing(BaroPressure))
        {
            return;
        }

        if (log.isTraceEnabled())
        {
            log.trace("Inputs this time slice:\t  input  |  value ");
            log.trace("                       \t AirTemp |  {}", AirTemp);
            log.trace("                       \t  RH     |  {}", RelativeHumidity);
            log.trace("                       \t WindSpd |  {}", WindSpeed);
            log.trace("                       \t SolRad  |  {}", SolarRadiation);
            log.trace("                       \t Pressure|  {}", BaroPressure);
        }

        if (!isMissing(AirTemp))
        {
            avg_temp += AirTemp;

            max_temp = Math.max(AirTemp, max_temp);
            min_temp = Math.min(AirTemp, min_temp);
            count_temp_values += 1;
            if (!isMissing(RelativeHumidity))
            {
                // this assumes instantaneous 15min data for AirTemp
                mean_actual_vapor_pressure += (RelativeHumidity/100)*e_naught(AirTemp); //e_naught will be factored in after the timeslice
                log.trace("e_a now: {} \t added {}", mean_actual_vapor_pressure, (RelativeHumidity/100)*e_naught( AirTemp ));
                count_actual_vapor_pressure_values +=1;
            }
            else
            {
                flags |= CwmsFlags.VALIDITY_QUESTIONABLE;
            }

        }
        else
        {
            flags |= CwmsFlags.VALIDITY_QUESTIONABLE;
        }

        if( !isMissing( SolarRadiation ))
        {
            if (SolarRadiation > 0.0)
            {
                total_radiation += 900*SolarRadiation;  //total energy in W/m2 * 900 seconds = J/s/m^2 * 900 s = J/m^2
            }
            count_rad_values += 1; // TODO: should this count be upped even if the radiation is <0.0 ( maybe check for night?)
        }
        else
        {
            flags |= CwmsFlags.VALIDITY_QUESTIONABLE;
        }

        if (!isMissing(WindSpeed))
        {
            windspeed_avg += WindSpeed;
            count_wind_values += 1;
        }
        else
        {
            flags |= CwmsFlags.VALIDITY_QUESTIONABLE;
        }

        if(!isMissing(BaroPressure))
        {
            avg_pressure += BaroPressure;
            avg_pressure_counts +=1;
        }

        count += 1;
//AW:TIMESLICE_END
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {
//AW:AFTER_TIMESLICES
        // This code will be executed once after each group of time slices.
        // For TimeSlice algorithms this is done once after all slices.
        // For Aggregating algorithms, this is done after each aggregate
        // period.
        log.trace("****Calculating evap for period****");
        double T = (max_temp+min_temp)*0.5;

        log.trace("Median Temp( Celsius ): {}", T);
        avg_temp = avg_temp/count_temp_values;
        log.trace("Average Temp( Celsius): ", avg_temp);

        double P = avg_pressure/avg_pressure_counts;  // 101.3*Math.pow( (293-0.0065*Elevation)/293, 5.26  );
        log.trace("Atmo Pressure at station leval (kPa): {}", P);

        double psychrometric_constant = 0.000665*P;
        double delta = 2504*Math.exp(((17.27*T))/(T+237.3) )/( Math.pow(T+237.3, 2) );
        log.trace("gamma (kPa/C): {}", psychrometric_constant);
        log.trace("delta (kPa/C): {}", delta);
        //double e_naught = 0.6108*Math.exp( (17.21*T)/(T+237.3) );
        //debug3( " e0(kpA):" + e_naught );
        double e_s = ( e_naught(max_temp) + e_naught(min_temp) )*0.5;
        log.trace("e_s (kPa): {}", e_s);

        double e_a = mean_actual_vapor_pressure/count_actual_vapor_pressure_values;
        log.trace("e_a (kPa): {}", e_a);

        log.trace("Total Solar Rad  (MJ/m^2/day): {}", total_radiation/1000000.0);
        double Rns = (  (1-Albedo)*total_radiation)/1000000.0; // convert the Joules per m^2 to megajoules per m^2 per day
        log.trace("Net Short wave Rad (MJ/m^2/day): {}", Rns);

        double r_a;
        // calculate the net longwave radiation
        if(isHourly)
        {
            r_a = r_a_hr(this.julian(_aggregatePeriodBegin));
        }
        else
        {
            r_a = r_a( this.julian(_timeSliceBaseTime));
        }

        double r_so = r_so(r_a);
        double Rnl;
        if (isHourly)
        {
            Rnl = r_nl(avg_temp+273.16, total_radiation/1000000.0, r_so, e_a);
        }
        else
        {
            Rnl = r_nl(max_temp+273.16, min_temp+273.16, total_radiation/1000000.0, r_so, e_a);
        }
        log.trace("Net Long Wave Radiation (MJ/m^2/day): {}", Rnl);
        log.trace("Computed from R_a = {} and R_so = {}", r_a, r_so);

        //double Rn = Rns-2.678912; // This is as defined in the sutron provided spreadsheet
        double Rn = Rns - Rnl;
        log.trace("Net Radiation (MJ/m^2/day): {}", Rn);

        double uz = windspeed_avg/count_wind_values;
        double u2 = uz*(  4.87/Math.log(67.8*WindSpeedHeight-5.42));
        log.trace("WindSpeedAvg went from {} to {} (m/s)", uz, u2);

        // according to documentation solar heat flux influx ( G ) is allowed to be zero for daily data
        double numerator = 0.0;
        if (isHourly)
        {
            // minor variation with hourly data
            // can't ignore G
            // NOTE: this is for soil flux, we care about water. Will likely need to do more research
            // but right now I'm just testing.
            // Perhaps these two constants should just be a property.
            double G;
            if( Rn < 0 )
            {
                G = .5*Rn; //Night
            }
            else
            {
                G = .1*Rn; //Daylight
            }
            log.trace( "Hourly TimeStemp, Soil Flux( G ) = {}", G);
            numerator = 0.408*delta*(Rn-G) + psychrometric_constant*(C_n/(T+273) )*u2*(e_naught(avg_temp)-e_a);
        }
        else
        {
            numerator = 0.408*delta*Rn + psychrometric_constant*(C_n/(T+273) )*u2*(e_s-e_a);
        }
        double denominator = delta + psychrometric_constant*(1+C_d*u2);

        log.trace("Numerator Calc: {}", numerator);
        log.trace("Denominator Calc: {}", denominator);


        evap = numerator/denominator;
        evap = Math.max(0,evap); // evap is never negative
        log.trace("Final Evap Calc(mm): {}", evap);
        if (!(( count == count_actual_vapor_pressure_values ) &&
              ( count == count_rad_values) &&
              ( count == count_temp_values) &&
              ( count == count_wind_values)
             )
            )
        {
            log.warn( "We did not have an equal number of data sets across an values, number likely bogus");
        }

        if (count >= MinSamples)
        {
            try
            {
                evap = uc.convert(evap);
                setOutput(Evap, evap);
                setFlagBits(Evap, flags);
            }
            catch (DecodesException ex)
            {
                throw new DbCompException( "No Conversion", ex);
            }
        }
        else
        {
            log.warn("There where not enough values present, assuming the evap number calculated is junk, sorry");
        }
//AW:AFTER_TIMESLICES_END
    }

    /**
     * Required method returns a list of all input time series names.
     */
    public String[] getInputNames()
    {
        return _inputNames;
    }

    /**
     * Required method returns a list of all output time series names.
     */
    public String[] getOutputNames()
    {
        return _outputNames;
    }

    /**
     * Required method returns a list of properties that have meaning to
     * this algorithm.
     */
    public String[] getPropertyNames()
    {
        return _propertyNames;
    }

    // Pennmann Monteith support equations

    /**
     * saturation vapor pressure function
     * @param T current temperature in degrees Celsius
     * @return capacity of air to hold water vapor
     */
    public double e_naught(double T)
    {
        return 0.6108*Math.exp( (17.21*T)/(T+237.3) );
    }
    /**
     *
     * @param date
     * @return julian day of the year
     */
    public double julian(Date date)
    {
        // fortunately, java handles date/time better than an ASCE document
        // this replaces equation 25 in the above referenced document
        GregorianCalendar cal = new GregorianCalendar( this.aggTZ);
        cal.setTime(date);
        double tmp = cal.get( Calendar.DAY_OF_YEAR);
        return tmp;
    }
    /**
     *
     * @param J julian day of the year
     * @return inverse distance factor used int the R_a calculation
     */
    public double inverse_distance_factor(double J)
    {
        return 1+0.033*Math.cos(((2*Math.PI)/365 )*J);
    }

    /**
     *
     * @param J julian day of the year
     * @return solar declination for the given day
     */
    public double solar_declination(double J)
    {
        return 0.409*Math.sin(((2*Math.PI)/365 )*J - 1.39);
    }

    /**
     *
     * @param latitude     latitude in radians
     * @param declination  also in radians ( value from solar declination )
     * @return the sunset hour angle
     */
    public double sunset_hour_angle(double latitude, double declination)
    {
        return Math.acos((-1)*Math.tan(latitude)*Math.tan(declination));
    }

    /**
     * calculates the estimated extraterrestrial radiation ( solar radiation before getting into our atmosphere )
     * @param J julian day of the year
     * @return extraterrestrial radiation
     */
    public double r_a( double J )
    {
        double d_r = inverse_distance_factor(J);
        double delta = solar_declination(J);
        double omega_s = sunset_hour_angle(radian_latitude,delta);

        log.trace("   ***calculating R_a***");
        log.trace("     d_r    : {} (inverse relative distance factor ( sqaured ))", d_r);
        log.trace("     delta  : {} (solar declination in radians)", delta);
        log.trace("     omega_s: {} (sunset hour angle in radians)", omega_s);
        log.trace("     J      : {} (julian day of the year)", J);

        return (24/Math.PI)*Gsc*d_r*( omega_s*Math.sin(radian_latitude)*Math.sin(delta)
             + Math.cos(radian_latitude)*Math.cos(delta)*Math.sin(omega_s)  );
    }
    /**
     * calculates the estimated extraterrestrial radiation ( solar radiation before getting into our atmosphere )
     * @param J julian day of the year
     * @return extraterrestrial radiation
     */
    public double r_a_hr( double J )
    {
        double d_r = inverse_distance_factor(J);
        double delta = solar_declination(J);
        double omega_s = sunset_hour_angle(radian_latitude,delta);

        log.trace("   ***calculating R_a (hourly)***");
        log.trace("     d_r    : {} (inverse relative distance factor ( sqaured ))", d_r);
        log.trace("     delta  : {} (solar declination in radians)", delta);
        log.trace("     omega_s: {} (sunset hour angle in radians)", omega_s);
        log.trace("     J      : {} (julian day of the year)", J);

        GregorianCalendar cal = new GregorianCalendar(this.aggTZ);
        cal.setTime(this._aggregatePeriodBegin);
        int t1 = cal.get(Calendar.HOUR_OF_DAY);
        cal.setTime(this._aggregatePeriodEnd);
        int t2 = cal.get(Calendar.HOUR_OF_DAY);
        double t = (t1+t2)/2.0;

        if ( t2 == 0 && t1 == 23)
        {
            t = 23.5;
        }

        log.trace("     t1= {}", t1);
        log.trace("     t2= {}", t2);
        log.trace("     t = {}", t);
        /* determine S_c */
        double b = 2.0*Math.PI*(J-81)/364.0;
        double S_c = .1645*Math.sin(2*b)-.1255*Math.cos(b)-Math.sin(b);
        double omega = (Math.PI/12.0)*(  (t+.06667*(radian_latitude_center_tz-radian_latitude)+S_c)-12 );
        double omega_1 = omega - Math.PI/24.0;
        double omega_2 = omega + Math.PI/24.0;

        log.trace("     omega  = {}", omega);
        log.trace("     omega_s-.79 < omega < omega_s -.52 = {}", ((omega_s-.79 <= omega) && (omega <= omega_s-0.52)));
        log.trace("     before bounds");
        log.trace("     omega_1 = {}", omega_1);
        log.trace("     omega_2 = {}", omega_2);
        log.trace("     S_c     = {}", S_c);

        /* now we bound omeage 1 and 2 */
        if (omega_1 < -omega_s) omega_1 = -omega_s;
        if (omega_2 < -omega_s) omega_2 = -omega_s;
        if (omega_1 >  omega_s) omega_1 = omega_s;
        if (omega_2 >  omega_s) omega_2 = omega_s;
        if (omega_1 >  omega_2) omega_1 = omega_2;
        double tmp = (12/Math.PI)*Gsc*d_r*((omega_2-omega_1)*Math.sin(radian_latitude)*Math.sin(delta)
                   + Math.cos(radian_latitude)*Math.cos(delta)*(Math.sin(omega_2)-Math.sin(omega_1)));
        log.trace("     after bounds");
        log.trace("     omega_1 = {}", omega_1);
        log.trace("     omega_2 = {}", omega_2);
        log.trace("     r_a     = {}", tmp );
        return tmp;
    }

    /**
     * uses the Elevation property, which must be in meters
     * @param r_a   estimated extraterrestrial radiation
     * @return      estimated clear sky radiation
     */
    public double r_so( double r_a)
    {
        return (0.75+(0.00002*Elevation))*r_a;
    }
    /**
     * Calculates the net long wave radiation
     * @param T_k_max   max temperature in Kelvin
     * @param T_k_min   min       "      "    "
     * @param r_s       measured short wave radiation
     * @param r_so      estimated clear sky radiation
     * @param e_a       actual vapor pressure
     * @return          estimated net long wave radiation
     */
    public double r_nl( double T_k_max, double T_k_min, double r_s, double r_so, double e_a)
    {
        double rs_rso = Math.max(.25+Double.MIN_VALUE, Math.min(1.0, r_s/r_so) ); // ratio of r_s/r_so bounded to (.25,1.0]
        double T_max_fourth = Math.pow(T_k_max,4);
        double T_min_fourth = Math.pow(T_k_min,4);

        log.trace("   ***Calculating Rnl***");
        log.trace("   Rs      :  {}", r_s );
        log.trace("   Rso     :  {}", r_so );
        log.trace("   Rs/Rso  :  {}", rs_rso);
        log.trace("   TMax^4  :  {}", T_max_fourth);
        log.trace("   TMin^4  :  {}", T_min_fourth);
        log.trace("   sigma   :  {}", sigma);

        double p1 = sigma*( (T_max_fourth+T_min_fourth)/2 );
        double p2 =  0.34 - 0.14*Math.sqrt(e_a) ;
        double p3 = 1.35*rs_rso-0.35;

        log.trace("   sigma*( Tmax^4 + Tmin^4)/2:  {}", p1);
        log.trace("   0.34 - 0.14*sqrt(e_a)     :  {}", p2);
        log.trace("   1.35*r_s/r_so-0.35)       :  {}", p3);
        return p1*p2*p3;
    }
    /**
     * Calculates the net long wave radiation (Hourly Time Step)
     * @param T_k_max   max temperature in Kelvin
     * @param T_k_min   min       "      "    "
     * @param r_s       measured short wave radiation
     * @param r_so      estimated clear sky radiation
     * @param e_a       actual vapor pressure
     * @return          estimated net long wave radiation
     */
    public double r_nl( double T_hr, double r_s, double r_so, double e_a)
    {
        double rs_rso;//= Math.max(.25+Double.MIN_VALUE, Math.min(1.0, r_s/r_so) ); // ratio of r_s/r_so bounded to (.25,1.0]
        if( r_s == 0.0 )
        {
            /* it's night, assume a value */
            /*
             * The ASCE manual suggests .7 to .8 for arid/semi arid climates, which is correct for CA.
             * This should be moved to a user configurable parameter after testing
             * (Plus maybe we can find an average of all the "night" values of this number which that manual says is 2-3 hours before sunset)
             */
            rs_rso = .75;
        }
        else if( r_so == 0.0)
        {
            rs_rso = 1.0;
        }
        else
        {
            rs_rso = Math.max(.25+Double.MIN_VALUE, Math.min(1.0, r_s/r_so) ); // ratio of r_s/r_so bounded to (.25,1.0]
        }
        double T_avg_fourth = Math.pow(T_hr,4);

        log.trace("   ***Calculating Rnl (hourly)***");
        log.trace("   Rs      :  {}", r_s );
        log.trace("   Rso     :  {}", r_so );
        log.trace("   Rs/Rso  :  {}",  rs_rso);
        log.trace("   Tavg    :  {}", T_hr );
        log.trace("   Tavg^4  :  {}", T_avg_fourth);

        log.trace("   sigma   :  " + sigma_hr);
        double p1 = sigma_hr*T_avg_fourth;
        double p2 =  0.34 - 0.14*Math.sqrt(e_a) ;
        double p3 = 1.35*rs_rso-0.35;
        log.trace("   sigma*( Tavg^4):  {}", p1);
        log.trace("   0.34 - 0.14*sqrt(e_a)     :  {}", p2);
        log.trace("   1.35*r_s/r_so-0.35)       :  {}", p3);
        return p1*p2*p3;
    }
}
