package decodes.hdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
// this import was added by M. Bogner March 2013 for the CP upgrade project 5.3
// surrogate keys where changed to a DbKey object instead of ject a long/
import decodes.sql.DbKey;
import decodes.tsdb.algo.AWAlgoType;


//AW:IMPORTS
import java.lang.Math;
//AW:IMPORTS_END

//AW:JAVADOC
/**
   Computes flow through a Parshall Flume according to the 
  width property value assigned to the calculation using this
  algorithm and the logic:

   If width is greater than 9.0 then:
   Area = 3.6875 * width + 2.5
   Flow = Area * (stage ^ 1.6)

   If width is less than or equal to 9.0 then:
   Flow = 4 * width * stage ^ (1.522 * (width ^ 0.026)) 

   Required Properties:  width (ie width=7.0 set in computation property)

   Required Inputs: stage (observed gauge height)

   Output variables: flow (Calculated as determined above)

   Programmed by A. Gilmore July 2008;
   modified by  M. Bogner  April 2009 to calculate for widths <= 9.0;

 */
//AW:JAVADOC_END
public class ParshallFlume
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double stage;	//AW:TYPECODE=i
	String _inputNames[] = { "stage" };
//AW:INPUTS_END

//AW:LOCALVARS
	double area;
// the algorithm version moded by M. Bogner March 2013 for the CP 5.3 project where
// the surrogate keys were changed to a DbKey object
	String alg_ver = "1.0.03";

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable flow = new NamedVariable("flow", 0);
	String _outputNames[] = { "flow" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double width = 25;
	public String validation_flag = "";
	String _propertyNames[] = { "width", "validation_flag" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
		area = 3.6875 * width + 2.5;
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
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
		double result;
		double algChange;
		if (!isMissing(stage))
		{
			debug3("ParshalFlume -" + alg_ver + " SDI: " + getSDI("stage") + "  Width: " + width + " Stage: " + stage);
			algChange = 9.0;
			result = Math.pow(stage,1.6) * area;
			if (Double.compare(width,algChange) <= 0)
			{
				Double tnumber = Math.pow(width,0.026) * 1.522;
				result = Math.pow(stage,tnumber) * width * 4.0;
		 
			}
		        if (validation_flag.length() > 0)
			 setHdbValidationFlag(flow,validation_flag.charAt(1));
			setOutput(flow, result);
		}
		else
		{
			deleteOutput(flow);
		}
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
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
