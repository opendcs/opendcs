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
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;


import java.lang.Math;

@Algorithm(description = "Computes flow through a Parshall Flume according to the\n" + 
   "width property value assigned to the calculation using this\n" + 
   "algorithm and the logic:\n\n" + 

   "If width is greater than 9.0 then:\n" + 
   "Area = 3.6875 * width + 2.5\n" + 
   "Flow = Area * (stage ^ 1.6)\n\n" + 

   "If width is less than or equal to 9.0 then:\n" + 
   "Flow = 4 * width * stage ^ (1.522 * (width ^ 0.026))\n\n" +  

   "Required Properties:  width (ie width=7.0 set in computation property)\n\n" + 

   "Required Inputs: stage (observed gauge height)\n\n" + 

   "Output variables: flow (Calculated as determined above)")  

   // Programmed by A. Gilmore July 2008
   // modified by  M. Bogner  April 2009 to calculate for widths &lt;= 9.0
public class ParshallFlume
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input	
	public double stage;

	double area;
// the algorithm version moded by M. Bogner March 2013 for the CP 5.3 project where
// the surrogate keys were changed to a DbKey object
	String alg_ver = "1.0.03";


	@Output(type = Double.class)
	public NamedVariable flow = new NamedVariable("flow", 0);

	@PropertySpec(value = "25") 
	public double width = 25;
	@PropertySpec(value = "") 
	public String validation_flag = "";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
		area = 3.6875 * width + 2.5;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
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
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
	}
}
