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
// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description = "Crystal Unregulated Inflow Computation\n" +
"Sums Delta Storages and Evaporations at the current timestep from these reservoirs\n" +
"Blue Mesa\n" +
"Morrow Point\n\n" +
"Also adds Taylor Park Delta Storage from t-1, and\n" +
"adds it to Crystal Inflow to get Unregulated Inflow")
public class CRRCUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double TPRCDeltaStorage;
	@Input
	public double BMDCDeltaStorage;
	@Input
	public double BMDCEvap;
	@Input
	public double MPRCDeltaStorage;
	@Input
	public double MPRCEvap;
	@Input
	public double CRRCInflow;

	@Output(type = Double.class)
	public NamedVariable unreg = new NamedVariable("unreg", 0);

	@PropertySpec(value = "fail")
	public String TPRCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail")
	public String BMDCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail")
	public String BMDCEvap_missing = "fail";
	@PropertySpec(value = "fail")
	public String MPRCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail")
	public String MPRCEvap_missing = "fail";
	@PropertySpec(value = "fail")
	public String CRRCInflow_missing = "fail";

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
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
		double sum = 0.0;
		sum = TPRCDeltaStorage;
		sum += BMDCDeltaStorage + BMDCEvap;
		sum += MPRCDeltaStorage + MPRCEvap;

		log.trace("doAWTimeSlice, TPRCDeltaStorage={} BMDCDeltaStorage={} BMDCEvap={} MPRCDeltaStorage={}" +
				  " MPRCEvap={} CRRCInflow={} sum ={}",
				  TPRCDeltaStorage, BMDCDeltaStorage, BMDCEvap, MPRCDeltaStorage, MPRCEvap, CRRCInflow, sum);

		setOutput(unreg, CRRCInflow + sum);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
