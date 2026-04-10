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
package decodes.hdb.algo;

import java.util.GregorianCalendar;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.sql.DbKey;
import decodes.tsdb.RatingStatus;
import decodes.hdb.HDBRatingTable;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description = "Implements the Powell Evaporation Computation.\n\n" +

"net evap = gross evap - river evap - streamside evap - terrace evap - remaining evap\n" + 
"areas for river, streamside, and terrace cover from separate ratings\n" +
"river evap is river area * gross evap coeff\n" +
"streamside evap is streamside area * streamside evap coeff * ave temp\n" +
"terrace evap is terrace area * terrace evap coeff * ave temp\n" +
"remaining evap is (area - all areas) * ave precip\n\n" +

"Inputs are:\n" +
"area from reservoir elevation lookup\n" +
"reservoir elevation\n\n" +

"These are SDI pointers to lookup coefficients\n" +
"from the stat tables.\n" +
"gross evap coeff\n" +
"streamside evap coeff\n" +
"terrace evap coeff\n" +
"average monthly rainfall\n" +
"average monthly temperature\n\n" +

"Output is net evaporation as a volume, plus the component evaporations of\n" + 
"gross, river, streamside, terrace, remaining.\n\n" +

"<p>Properties include: \n" +
"<ul>\n" +
"<li>ignoreTimeSeries - completely ignore changes to any timeseries value from evapCoeff, and always lookup from database.\n" +
"</li>\n" +
"</ul>")
public class GLDAEvap extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double area;
	@Input
	public double elev;
	@Input
	public double grossEvapCoeff;
	@Input
	public double streamEvapCoeff;
	@Input
	public double terraceEvapCoeff;
	@Input
	public double aveTemp;
	@Input
	public double avePrecip;

	HDBRatingTable riverRatingTable = null;
	HDBRatingTable streamRatingTable = null;
	HDBRatingTable terraceRatingTable = null;
	GregorianCalendar cal = new GregorianCalendar();
	private boolean firstCall = true;

	@Output(type = Double.class)
	public NamedVariable netEvap = new NamedVariable("netEvap", 0);
	@Output(type = Double.class)
	public NamedVariable grossEvap = new NamedVariable("grossEvap", 0);
	@Output(type = Double.class)
	public NamedVariable riverEvap = new NamedVariable("riverEvap", 0);
	@Output(type = Double.class)
	public NamedVariable streamsideEvap = new NamedVariable("streamsideEvap", 0);
	@Output(type = Double.class)
	public NamedVariable terraceEvap = new NamedVariable("terraceEvap", 0);
	@Output(type = Double.class)
	public NamedVariable remainingEvap = new NamedVariable("remainingEvap", 0);

	@PropertySpec(value = "true") 
	public boolean ignoreTimeSeries = true;
	@PropertySpec(value = "fail") 
	public String area_missing = "fail";
	@PropertySpec(value = "fail") 
	public String elev_missing = "fail";
	@PropertySpec(value = "ignore") 
	public String grossEvapCoeff_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String streamEvapCoeff_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String terraceEvapCoeff_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String aveTemp_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String avePrecip_missing = "ignore";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		if (firstCall)
		{
			firstCall = false;
			
			// Find the name for the input parameter.
			// Cast to int was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
			// Cast to int was moded by M. Bogner March 2013 for the 5.3 CP upgrade project
			// because the surrogate keys where changed to a dbkey object 
			//int elev_sdi = (int) getSDI("elev").getValue();
			DbKey elev_sdi = getSDI("elev");
			log.trace("Constructing HDB ratings for evap ratings");
			riverRatingTable = new HDBRatingTable(tsdb,"Lake Powell River Area",elev_sdi);
			streamRatingTable = new HDBRatingTable(tsdb,"Lake Powell Streamside Area",elev_sdi);
			terraceRatingTable = new HDBRatingTable(tsdb,"Lake Powell Terrace Area",elev_sdi);

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
	protected void doAWTimeSlice()
		throws DbCompException
	{
		cal.setTime(_timeSliceBaseTime);
		int daysInMonth = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		if (_sliceInputsDeleted) { //handle deleted values, since we have more than one output
			deleteAllOutputs();
			return;
		}
		if (ignoreTimeSeries) {	
			grossEvapCoeff = getCoeff("grossEvapCoeff");
			streamEvapCoeff= getCoeff("streamEvapCoeff");
			terraceEvapCoeff = getCoeff("terraceEvapCoeff");
			aveTemp = getCoeff("aveTemp");
			avePrecip = getCoeff("avePrecip");
		}
		//gross evap is whole area times coeff divided by length of current month in days * 12 in/ft
		double gevap = area*grossEvapCoeff/(daysInMonth*12); 

		//river evap is river area times coeff divided by length of current month in days * 12 in/ft
		RatingStatus rs = riverRatingTable.doRating(elev, _timeSliceBaseTime);
		double riverArea = rs.dep;
		double rivevap = riverArea*grossEvapCoeff/(daysInMonth*12);
		
		//streamside evap is streamside area times coeff times ave temp divided by length of current month in days * 12 in/ft
		rs = streamRatingTable.doRating(elev, _timeSliceBaseTime);
		double streamArea = rs.dep;
		double sevap =  streamArea*streamEvapCoeff*aveTemp/(daysInMonth*12);
		
		//terrace evap is terrace area times coeff times ave temp divided by length of current month in days * 12 in/ft
		rs = terraceRatingTable.doRating(elev, _timeSliceBaseTime);
		double terraceArea = rs.dep;
		double tevap = terraceArea*terraceEvapCoeff*aveTemp/(daysInMonth*12);
		
		//remaining evap is remaining area * avePrecip divided by length of current month in days * 12 in/ft,
		//all assumed to evaporate
		double remevap = (area - riverArea - streamArea- terraceArea)*
		                 avePrecip/(daysInMonth*12);

		log.trace("doAWTimeSlice gevap={}, rivevap={}, sevap={} tevap={}, remevap={}",
				  gevap, rivevap, sevap, tevap, remevap);
				
		setOutput(grossEvap, gevap);
		setOutput(riverEvap, rivevap);
		setOutput(streamsideEvap, sevap);
		setOutput(terraceEvap, tevap);
		setOutput(remainingEvap, remevap);
		
		setOutput(netEvap, gevap - rivevap - sevap - tevap - remevap);		
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
	}
}
