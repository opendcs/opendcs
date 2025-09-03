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

import ilex.var.NamedVariable;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.DbCompException;
// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
Computes the Powell inflow from the three upstream gages and a side inflow
coefficient. If any gages are missing, estimates inflow using mass balance
and an assumed delta bankstorage coefficient.

This algorithm does not actually write to delta bank storage, but the resulting
estimated inflow will result in a delta bank storage that matches the value
used here.

Inputs:
bffu = San Juan River at Bluff
clru = Colorado River at Cisco
grvu = Green River at Green River, UT
evap = Powell Evaporation
delta_storage = Powell change in storage
total_release = Powell total release volume

Output:
inflow = Powell total inflow
 */
//AW:JAVADOC_END
public class EstGLDAInflow extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
//AW:INPUTS
	public double bffu;	//AW:TYPECODE=i
	public double clru;	//AW:TYPECODE=i
	public double grvu;	//AW:TYPECODE=i
	public double evap; //AW:TYPECODE=i
	public double delta_storage; //AW:TYPECODE=i
	public double total_release; //AW:TYPECODE=i
	public double inflowCoeff; //AW:TYPECODE=i
	String _inputNames[] = { "bffu", "clru", "grvu", "evap", "delta_storage", "total_release", "inflowCoeff" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable inflow = new NamedVariable("inflow", 0);
	String _outputNames[] = { "inflow" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String bffu_MISSING = "ignore";
	public String clru_MISSING = "ignore";
	public String grvu_MISSING = "ignore";
	public String evap_MISSING = "fail";
	public String delta_storage_MISSING = "fail";
	public String total_release_MISSING = "fail";
	public String inflowCoeff_MISSING = "ignore";
	public double bscoeff = 0.04;
	String _propertyNames[] = { "bffu_MISSING", "clru_MISSING", "grvu_MISSING", "evap_MISSING", "delta_storage_MISSING",
			"total_release_MISSING", "inflowCoeff_MISSING", "bscoeff" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
	}

	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
	{
//AW:BEFORE_TIMESLICES
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
	protected void doAWTimeSlice()
		throws DbCompException
	{
//AW:TIMESLICE
		if (!(isMissing(bffu) || isMissing(clru) || isMissing(grvu))) {
			// all inflow values are present, no estimate necessary
			inflowCoeff = getCoeff("inflowCoeff");
			double in = bffu + clru + grvu;
			in += in * inflowCoeff;

			log.trace("doAWTimeSlice bffu={}, clru={}, gvru={}, inflowCoeff={}, inflow={}",
			 		  bffu, clru, grvu, inflowCoeff, in);
			setOutput(inflow, in);
			return;
		}
		else {
			//one or more of the gages is missing, do an estimate
			log.debug("GLDA Estimated Inflow computation entered for {}", _timeSliceBaseTime);
			double dBS = delta_storage * bscoeff;
			double invol = delta_storage + dBS + evap + total_release;
			double in = invol * 43560/86400; //convert to cfs

			log.trace("doAWTimeSlice Estimated Inflow! dBS={}, invol={}, in={}", dBS, invol, in);
			setHdbDerivationFlag(inflow, "E");
			setOutput(inflow, in);
			return;
		}
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
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
}
