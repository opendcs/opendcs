/**
 * $Id$
 * 
 * $Log$
 * 
 */
package decodes.tsdb.algo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DynamicPropertiesOwner;
import decodes.util.PropertySpec;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements the Jython Python interpreter.

 */
//AW:JAVADOC_END
public class PythonAlgorithm
	extends decodes.tsdb.algo.AW_AlgorithmBase
	implements DynamicPropertiesOwner
{
//AW:INPUTS
	public double dummyin;	//AW:TYPECODE=i
	String _inputNames[] = { "dummyin" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable dummyout = new NamedVariable("dummyout", 0);
	String _outputNames[] = { "dummyout" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double dummyprop = 123.456;
	String _propertyNames[] = { "dummyprop" };
//AW:PROPERTIES_END

	private ArrayList<PropertySpec> dynamicPropSpecs = new ArrayList<PropertySpec>();
	
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
		// Code here will be run once, after the algorithm object is created.

		DbCompAlgorithm algo = comp.getAlgorithm();
		// TODO use algo.getParms and algo.getProperties to reset my constant
		// arrays for _inputNames, _outputNames, and _propertyNames
		
		
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		// For Aggregating algorithms, this is done before each aggregate
		// period.
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
		// Enter code to be executed at each time-slice.
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
	
	/**
	 * Called from AW_AlgorithmBase if no field is found matching the variable
	 * name. This will be the case for the python variables.
	 * @param name
	 * @param value
	 */
	public void setTimeSliceInput(String name, double value)
	{
		
	}

	@Override
	public boolean dynamicPropsAllowed()
	{
		return true;
	}

	@Override
	public Collection<PropertySpec> getDynamicPropSpecs()
	{
		return dynamicPropSpecs;
	}

	@Override
	public void setDynamicPropDescription(String propName, String description)
	{
		for(PropertySpec ps : dynamicPropSpecs)
			if (ps.getName().equals(propName))
			{
				ps.setDescription(description);
				return;
			}
		PropertySpec ps = new PropertySpec(propName, PropertySpec.STRING, description);
		ps.setDynamic(true);
		dynamicPropSpecs.add(ps);
	}

	@Override
	public String getDynamicPropDescription(String propName)
	{
		for(PropertySpec ps : dynamicPropSpecs)
			if (ps.getName().equals(propName))
				return ps.getDescription();
		return null;
	}
}
