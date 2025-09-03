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

@Algorithm(description = "This algorithm is an Basic mass balance calculation for inflow as: \n" +  
"Delta Storage + Total Release \n\n" +  

"If inputs Delta Storage or Total Release do not exist or have been \n" +  
"deleted and the Delta_STORAGE_MISSING or the TOTAL_RELEASE_MISSING \n" +  
"properties are set to \"fail\" then the inflow will not be calculated \n" +  
"and/or the inflow will be deleted. \n\n" +  

"If all of the inputs do not exist because of a delete the inflow will \n" +  
"be deleted if the output exists regardless of the property settings. \n\n" +  

"This algorithm written by M. Bogner, August 2008 \n" +  
"Modified by M. Bogner May 2009 to add additional delete logic and version control")
public class InflowBasicAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double total_release;
	@Input
	public double delta_storage;

// Version 1.0.04 was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
	String alg_ver = "1.0.04";
        boolean do_setoutput = true;
	double inflow_calculation = 0.0;


	@Output(type = Double.class)
	public NamedVariable inflow = new NamedVariable("inflow", 0);

	@PropertySpec(value = "ignore")
	public String total_release_missing = "ignore";
	@PropertySpec(value = "ignore")
	public String delta_storage_missing = "ignore";
	@PropertySpec(value = "")
    public String validation_flag = "";

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
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
	inflow_calculation = 0.0;
	do_setoutput = true;
	if (!isMissing(total_release))
		inflow_calculation = inflow_calculation + total_release;
	if (!isMissing(delta_storage))
		inflow_calculation = inflow_calculation + delta_storage;

	// now test existence of inputs and their missing flags and setoutput accordingly
	if (isMissing(total_release) && isMissing(delta_storage)) do_setoutput = false;
	if (isMissing(total_release) && total_release_missing.equalsIgnoreCase("fail")) do_setoutput = false;
	if (isMissing(delta_storage) && delta_storage_missing.equalsIgnoreCase("fail")) do_setoutput = false;

	if (do_setoutput)
	{
		log.trace("InflowBasicAlg-{}: total_release={}, delta_storage={}",
				  alg_ver, total_release, delta_storage);
		/* added to allow users to automatically set the Validation column  */
		if (validation_flag.length() > 0) setHdbValidationFlag(inflow,validation_flag.charAt(1));
		setOutput(inflow,inflow_calculation);
	}
	else
	{
		log.trace("InflowBasicAlg-{}: Deleting inflow output", alg_ver);
		deleteOutput(inflow);
	}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
