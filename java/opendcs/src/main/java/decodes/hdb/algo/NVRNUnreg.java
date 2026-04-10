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
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description = "Navajo Unregulated Inflow Computation \n" +
"Sums Vallecito Delta Storage and Evaporation at t-1 \n" +
"adds it to Navajo Inflow to get Unregulated Inflow \n" +
"Adds Azotea tunnel volume to Navajo Unregulated flow to get Mod Unregulated Flow")
public class NVRNUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double VCRCDeltaStorage;
	@Input
	public double VCRCEvap;
	@Input
	public double NVRNInflow;
	@Input
	public double SJANVolume;

	@Output(type = Double.class)
	public NamedVariable unreg = new NamedVariable("unreg", 0);
	@Output(type = Double.class)
	public NamedVariable modunreg = new NamedVariable("modunreg", 0);

	@PropertySpec(value = "fail")
	public String VCRCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail")
	public String VCRCEvap_missing 		= "fail";
	@PropertySpec(value = "fail")
	public String NVRNInflow_missing 		= "fail";
	@PropertySpec(value = "fail")
	public String SJANVolume_missing 		= "fail";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
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
	protected void doAWTimeSlice()
		throws DbCompException
	{
		if (_sliceInputsDeleted) { //handle deleted values, since we have more than one output
			deleteAllOutputs();
			return;
		}
		double sum = VCRCDeltaStorage + VCRCEvap;
		
		log.trace("doAWTimeSlice, VCRCDeltaStorage={} VCRCEvap={} NVRNInflow={} sum={} SJANVolume={}",
				  VCRCDeltaStorage, VCRCEvap, NVRNInflow, sum, SJANVolume);

		setOutput(unreg, NVRNInflow + sum);
		setOutput(modunreg, NVRNInflow + sum + SJANVolume);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
