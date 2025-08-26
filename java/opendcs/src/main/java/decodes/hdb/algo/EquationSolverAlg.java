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
import decodes.tsdb.ParmRef;
import decodes.tsdb.TimeSeriesIdentifier;
// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;

import decodes.util.PropertySpec;
//AW:IMPORTS
import decodes.hdb.dbutils.*;
import decodes.hdb.HdbTsId;

import java.sql.Connection;
import java.text.SimpleDateFormat;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

//AW:IMPORTS_END

//AW:JAVADOC
/**
Takes up to 5 input values labeled input1 ... input5.
Takes the values of these inputs and replaces the reference of these
inputs in the computation's defined equation and the equation is then
submitted to ORACLE to be evaluated.  The equation specification is simple
yet must be an acceptable ORACLE statement.

ie  the three times the input1 squared equation is:   3*power(<<input1>>,2)
 */
//AW:JAVADOC_END
public class EquationSolverAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
//AW:INPUTS
	public double input1;	//AW:TYPECODE=i
	public double input2;	//AW:TYPECODE=i
	public double input3;	//AW:TYPECODE=i
	public double input4;	//AW:TYPECODE=i
	public double input5;	//AW:TYPECODE=i
	String _inputNames[] = { "input1", "input2", "input3", "input4", "input5" };
//AW:INPUTS_END

//AW:LOCALVARS
    boolean do_setoutput = true;
    Connection conn = null;

    private PropertySpec[] esaProps =
    {
    	new PropertySpec("equation", PropertySpec.LONGSTRING,
    		"The database equation to execute at each time slice"),
    	new PropertySpec("validation_flag", PropertySpec.STRING,
    		"If set, output values will be flagged with this flag.")
    };

    @Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return esaProps;
	}

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
    public String validation_flag = "";
    public String equation = "";

	String _propertyNames[] = { "equation", "validation_flag" };
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
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
//AW:TIMESLICE
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
		String new_equation = equation.replaceAll("<<INPUT", "<<input");
		String tsbt_time = "to_date('" + sdf.format(_timeSliceBaseTime) + "','dd-mon-yyyy HH24:mi')";
		new_equation = new_equation.replaceAll("<<tsbt>>", tsbt_time);

		// MJM 2017-04-30 replace with "null" if missing.
		// Note: if we get to here and value is missing, it means that missing action = ignore.
		String repl = !isMissing(input1) ? (Double.valueOf(input1)).toString() : "null";
		new_equation = new_equation.replaceAll("<<input1>>", repl);
		if (!isMissing(input1))
		{
			ParmRef pr = this.getParmRef("input1");
			TimeSeriesIdentifier tsid = pr.tsid;
			if (tsid != null) // shouldn't happen
			{
				HdbTsId hdbTsid = (HdbTsId)tsid;
				new_equation = new_equation.replaceAll("<<input1_sdi>>", "" + hdbTsid.getSdi());
				new_equation = new_equation.replaceAll("<<input1_interval>>", hdbTsid.getInterval());
				new_equation = new_equation.replaceAll("<<input1_modelid>>",
					"" + hdbTsid.getModelId());
				new_equation = new_equation.replaceAll("<<input1_modelrunid>>",
						"" + hdbTsid.getModelRunId());
			}
		}
		repl = !isMissing(input2) ? (Double.valueOf(input2)).toString() : "null";
		new_equation = new_equation.replaceAll("<<input2>>", repl);
		if (!isMissing(input2))
		{
			ParmRef pr = this.getParmRef("input2");
			TimeSeriesIdentifier tsid = pr.tsid;
			if (tsid != null) // shouldn't happen
			{
				HdbTsId hdbTsid = (HdbTsId)tsid;
				new_equation = new_equation.replaceAll("<<input2_sdi>>", "" + hdbTsid.getSdi());
				new_equation = new_equation.replaceAll("<<input2_interval>>", hdbTsid.getInterval());
				new_equation = new_equation.replaceAll("<<input2_modelid>>",
					"" + hdbTsid.getModelId());
				new_equation = new_equation.replaceAll("<<input2_modelrunid>>",
						"" + hdbTsid.getModelRunId());
			}
		}

		repl = !isMissing(input3) ? (Double.valueOf(input3)).toString() : "null";
		new_equation = new_equation.replaceAll("<<input3>>", repl);
		if (!isMissing(input3))
		{
			ParmRef pr = this.getParmRef("input3");
			TimeSeriesIdentifier tsid = pr.tsid;
			if (tsid != null) // shouldn't happen
			{
				HdbTsId hdbTsid = (HdbTsId)tsid;
				new_equation = new_equation.replaceAll("<<input3_sdi>>", "" + hdbTsid.getSdi());
				new_equation = new_equation.replaceAll("<<input3_interval>>", hdbTsid.getInterval());
				new_equation = new_equation.replaceAll("<<input3_modelid>>",
					"" + hdbTsid.getModelId());
				new_equation = new_equation.replaceAll("<<input3_modelrunid>>",
						"" + hdbTsid.getModelRunId());
			}
		}

		repl = !isMissing(input4) ? (Double.valueOf(input4)).toString() : "null";
		new_equation = new_equation.replaceAll("<<input4>>", repl);
		if (!isMissing(input4))
		{
			ParmRef pr = this.getParmRef("input4");
			TimeSeriesIdentifier tsid = pr.tsid;
			if (tsid != null) // shouldn't happen
			{
				HdbTsId hdbTsid = (HdbTsId)tsid;
				new_equation = new_equation.replaceAll("<<input4_sdi>>", "" + hdbTsid.getSdi());
				new_equation = new_equation.replaceAll("<<input4_interval>>", hdbTsid.getInterval());
				new_equation = new_equation.replaceAll("<<input4_modelid>>",
					"" + hdbTsid.getModelId());
				new_equation = new_equation.replaceAll("<<input4_modelrunid>>",
						"" + hdbTsid.getModelRunId());
			}
		}


		repl = !isMissing(input5) ? (Double.valueOf(input5)).toString() : "null";
		new_equation = new_equation.replaceAll("<<input5>>", repl);
		if (!isMissing(input5))
		{
			ParmRef pr = this.getParmRef("input5");
			TimeSeriesIdentifier tsid = pr.tsid;
			if (tsid != null) // shouldn't happen
			{
				HdbTsId hdbTsid = (HdbTsId)tsid;
				new_equation = new_equation.replaceAll("<<input5_sdi>>", "" + hdbTsid.getSdi());
				new_equation = new_equation.replaceAll("<<input5_interval>>", hdbTsid.getInterval());
				new_equation = new_equation.replaceAll("<<input5_modelid>>",
					"" + hdbTsid.getModelId());
				new_equation = new_equation.replaceAll("<<input5_modelrunid>>",
						"" + hdbTsid.getModelRunId());
			}
		}

		if (new_equation.contains("<<loading_application_id>>"))
			new_equation = new_equation.replaceAll("<<loading_application_id>>",
				tsdb.getAppId().toString());
		if (new_equation.contains("<<computation_id>>"))
			new_equation = new_equation.replaceAll("<<computation_id>>", comp.getId().toString());
		if (new_equation.contains("<<algorithm_id>>"))
			new_equation = new_equation.replaceAll("<<algorithm_id>>",
				comp.getAlgorithmId().toString());

		log.trace("doAWTimeSlice input1={}, input2={}", input1, input2);

		/* THis modification done by M. Bogner 30-SEP-2011 */
		/* need to reset the set output flag must be done for each time series */
		do_setoutput = true;


		if (do_setoutput)
		// Then continue with evaluation the equation
		{
			// get the connection and a few other classes so we can do some sql
			conn = tsdb.getConnection();
			DBAccess db = new DBAccess(conn);
			DataObject dbobj = new DataObject();
			String dt_fmt = "dd-MMM-yyyy HH:mm";

			String status = null;
			String query = "select " + new_equation + " result_value from dual";
			// now do the query for all the needed data
			status = db.performQuery(query, dbobj);
			log.trace(" SQL STRING:{}   DBOBJ: {} STATUS:  {}",
					  query, dbobj.toString(), status);

			// see if there was an error
			if (status.startsWith("ERROR"))
			{
				log.warn("EquationSolver:  FAILED due to following oracle error");
				log.warn("EquationSolver: {}", status);
				return;
			}
			//
			if (((String) dbobj.get("result_value")).length() == 0)
			{
				do_setoutput = false;
				;
				log.warn("EquationSolver:  {} : {}  Query FAILED due to NULL Equation result. ",
						 comp.getName(), query);
			}
			else
			{
				Double result_value = Double.valueOf(dbobj.get("result_value").toString());
				//
				/*
				 * added to allow users to automatically set the Validation
				 * column
				 */
				if (validation_flag.length() > 0)
					setHdbValidationFlag(output, validation_flag.charAt(1));
				setOutput(output, result_value);
			}
		}
		//
		// delete any existing value if this calculation failed
		if (!do_setoutput)
		{
			deleteOutput(output);
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
