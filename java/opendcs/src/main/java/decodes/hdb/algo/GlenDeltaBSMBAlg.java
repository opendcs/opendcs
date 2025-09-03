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

@Algorithm(description = "This algorithm calculates the Glen Canyon Bank Storage Mass Balance \n\n" +

"The calculation for the Bank storage is Inflow minus the total Releases, \n" +
"delta storage, and evaporation \n\n" +

"If inputs Delta Storage or Total Release Above or Total Release Below \n" +
"or if the Evap do not exist or have been deleted and the \n" +
"DELTA_STORAGE_MISSING or the TOTAL_RELEASE_MISSING or EVAP_MISSING, \n" +
"or INFLOW_MISSING properties are set to \"fail\" then the BANK STORAGE \n" +
"will not be calculated and/or the BANK STORAGE will be deleted. \n\n" +

"If all of the inputs do not exist because of a delete the BANK STORAGE \n" +
"will be deleted if the output exists regardless of the property settings. \n\n" +

"This algorithm written by M. Bogner, August 2008 \n" +
"Modified by M. Bogner May 2009 to add additional delete logic and version control")
public class GlenDeltaBSMBAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double inflow;
	@Input
	public double total_release;
	@Input
	public double delta_storage;
	@Input
	public double evap;

// Version 1.0.03 was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
	String alg_ver = "1.0.03";
        boolean do_setoutput = true;
	double bs_calculation = 0.0;

	@Output(type = Double.class)
	public NamedVariable delta_bs = new NamedVariable("delta_bs", 0);

	@PropertySpec(value = "ignore") 
	public String total_release_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String delta_storage_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String evap_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String inflow_missing = "ignore";
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
	   bs_calculation = 0;
	   do_setoutput = true;
	   if (!isMissing(total_release))
             bs_calculation = bs_calculation - total_release;
	   if (!isMissing(delta_storage))
             bs_calculation = bs_calculation - delta_storage;
	   if (!isMissing(evap))
             bs_calculation = bs_calculation - evap;
	   if (!isMissing(inflow))
             bs_calculation = bs_calculation + inflow;

        // now test existence of inputs and their missing flags and setoutput accordingly
        if (isMissing(total_release) && isMissing(inflow) && isMissing(delta_storage)  && isMissing(evap)) do_setoutput = false;
        if (isMissing(total_release) && total_release_missing.equalsIgnoreCase("fail")) do_setoutput = false;
        if (isMissing(inflow) && inflow_missing.equalsIgnoreCase("fail")) do_setoutput = false;
        if (isMissing(delta_storage) && delta_storage_missing.equalsIgnoreCase("fail")) do_setoutput = false;
        if (isMissing(evap) && evap_missing.equalsIgnoreCase("fail")) do_setoutput = false;

        if (do_setoutput)
        {
			log.trace("GlenDeltaBSMBAlg-{}: total_release= {}, delta_storage= {}",
					  alg_ver, total_release, delta_storage);
			log.trace("GlenDeltaBSMBAlg-{}: inflow= {} evap= {}", alg_ver, inflow, evap);
			log.trace("GlenDeltaBSMBAlg-{}: bs_calculation= {}", alg_ver, bs_calculation);
                /* added to allow users to automatically set the Validation column  */
            if (validation_flag.length() > 0)
			{ 
				setHdbValidationFlag(delta_bs,validation_flag.charAt(1));
			}
			setOutput(delta_bs,bs_calculation);
        }
        else
        {
            log.trace("GlenDeltaBSMBAlg-{}: Deleting Delta Bank Storage output", alg_ver);
            deleteOutput(delta_bs);
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
