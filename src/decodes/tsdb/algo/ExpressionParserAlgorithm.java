package decodes.tsdb.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import org.nfunk.jep.SymbolTable;

import ilex.var.IFlags;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.db.Constants;
import decodes.db.SiteName;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.jep.JepContext;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import decodes.tsdb.TimeSeriesIdentifier;

//AW:IMPORTS
// Place an import statements you need here.
//TODO Add the JEP stuff
import ilex.util.StringPair;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Allow up to 5 inputs labeled in1...in5 and two outputs labeled out1 and out2.
Properties can include:
- Time slice expressions labeled ex_<label>, where <label> is any string.
- Pre-time slice expressions labeled pre_<label>
- Post-time slice expressions labeled post_<label>

Expressions are executed in sort order.
 */
//AW:JAVADOC_END
public class ExpressionParserAlgorithm
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double in1;	//AW:TYPECODE=i
	public double in2;	//AW:TYPECODE=i
	public double in3;	//AW:TYPECODE=i
	public double in4;	//AW:TYPECODE=i
	public double in5;	//AW:TYPECODE=i
	String _inputNames[] = { "in1", "in2", "in3", "in4", "in5" };
//AW:INPUTS_END

//AW:LOCALVARS
	JepContext jepContext = null;
	public ExpressionParserAlgorithm()
	{
	}
	
	// Enter any local class variables needed by the algorithm.
	ArrayList<StringPair> preScript = new ArrayList<StringPair>();
	ArrayList<StringPair> timeSliceScript = new ArrayList<StringPair>();
	ArrayList<StringPair> postScript = new ArrayList<StringPair>();
	private void addSubBase(SymbolTable symTab, String name, String part, String id)
	{
		int hyphen = id.indexOf('-');
		if (hyphen > 0)
		{
			symTab.addVariable(name + ".sub" + part, id.substring(hyphen + 1));
			symTab.addVariable(name + ".base" + part, id.substring(0, hyphen));
		}
		else // id is the base part by itself
			symTab.addVariable(name + ".base" + part, id);
	}	
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable out1 = new NamedVariable("out1", 0);
	public NamedVariable out2 = new NamedVariable("out2", 0);
	String _outputNames[] = { "out1", "out2" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String pre_1 = "";
	public String ex_1 = "";
	public String post_1 = "";
	String _propertyNames[] = { "pre_1", "ex_1", "post_1" };
//AW:PROPERTIES_END

	private void executeScript(ArrayList<StringPair> script)
	{
		// Remove outputs from the symbol table so that I can detect assignments after execution.
		for(String nm: _outputNames)
			jepContext.getParser().getSymbolTable().remove(nm);
		int idx = 0;
		jepContext.setOnErrorLabel(null);
//		jepContext.setAllowAssignment(true);
//		jepContext.setAllowUndeclared(true);
		while(idx >= 0 && idx < script.size())
		{
			StringPair label_expr = script.get(idx);
			String expr = label_expr.second;
			debug2("Executing expression[" + idx + "]: " + expr);
			jepContext.reset();
			jepContext.getParser().parseExpression(expr);
			Object value = jepContext.getParser().getValueAsObject();

			if (jepContext.getParser().hasError() || value == null)
			{
				debug2("Expression '" + expr + "' resulted in error: " + jepContext.getParser().getErrorInfo());
				String lab = jepContext.getOnErrorLabel();
				if (lab != null)
					idx = findLabel(script, lab);
				else
					idx = -1;
			}
			else if (jepContext.getGotoLabel() != null)
				idx = findLabel(script, jepContext.getGotoLabel());
			else if (jepContext.isExitCalled())
				idx = -1;
			else
				idx++;
		}
	}
	
	private int findLabel(ArrayList<StringPair> script, String label)
	{
		for(int ret = 0; ret < script.size(); ret++)
			if (script.get(ret).first.equalsIgnoreCase(label))
				return ret;
		return -1;
	}


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
		jepContext = new JepContext(tsdb, this);

		// Extract the three scripts (pre, time-slice, and post) from the properties.
		Properties props = comp.getProperties();
		for(Object key : props.keySet())
		{
			String propName = key.toString();
			String value = props.getProperty(propName);
			
			if (propName.toLowerCase().startsWith("pre_"))
				preScript.add(new StringPair(propName, value));
			else if (propName.toLowerCase().startsWith("ex_"))
				timeSliceScript.add(new StringPair(propName, value));
			else if (propName.toLowerCase().startsWith("post_"))
				postScript.add(new StringPair(propName, value));
		}
		if (preScript.size() == 0 && timeSliceScript.size() == 0 && postScript.size() == 0)
			throw new DbCompException("ExpressionParser.init: No expressions found in properties.");
		// Sort all the scripts by label (i.e. prop name)
		Comparator<StringPair> spcomp = 
			new Comparator<StringPair>()
			{
				@Override
				public int compare(StringPair sp1, StringPair sp2)
				{
					return sp1.first.compareTo(sp2.first);
				}
			};
		Collections.sort(preScript, spcomp);
		Collections.sort(timeSliceScript, spcomp);
		Collections.sort(postScript, spcomp);
		
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
		
		// Prepopulate the symbol table with the info about the parameters.
		SymbolTable symTab = jepContext.getParser().getSymbolTable();
		symTab.clear();
		for(String inputName : _inputNames)
		{
			ParmRef pr = getParmRef(inputName);		
			if (pr != null && pr.compParm != null && pr.timeSeries != null 
			 && pr.timeSeries.getTimeSeriesIdentifier() != null)
			{
				TimeSeriesIdentifier tsid = pr.timeSeries.getTimeSeriesIdentifier();
				for(String part : tsid.getParts())
				{
					part = part.toLowerCase();
					symTab.addVariable(inputName + "." + part, tsid.getPart(part));
				}
				if (tsdb.isCwms())
				{
					// For CWMS, add sub/base for location, param, and version
					addSubBase(symTab, inputName, "loc", tsid.getSiteName());
					addSubBase(symTab, inputName, "param", tsid.getDataType().getCode());
					addSubBase(symTab, inputName, "version", tsid.getPart("version"));
				}
			}
		}
		ParmRef parmRef = getParmRef("out1");
		if (parmRef != null && parmRef.timeSeries != null
		 && parmRef.timeSeries.getTimeSeriesIdentifier() != null)
		{
			TimeSeriesIdentifier tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
			for(String part : tsid.getParts())
			{
				part = part.toLowerCase();
				symTab.addVariable("out1." + part, tsid.getPart(part));
			}
			addSubBase(symTab, "out1", "loc", tsid.getSiteName());
			addSubBase(symTab, "out1", "param", tsid.getDataType().getCode());
			addSubBase(symTab, "out1", "version", tsid.getPart("version"));
		}
		parmRef = getParmRef("out2");
		if (parmRef != null && parmRef.timeSeries != null
		 && parmRef.timeSeries.getTimeSeriesIdentifier() != null)
		{
			TimeSeriesIdentifier tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
			for(String part : tsid.getParts())
			{
				part = part.toLowerCase();
				symTab.addVariable("out2." + part, tsid.getPart(part));
			}
			addSubBase(symTab, "out2", "loc", tsid.getSiteName());
			addSubBase(symTab, "out2", "param", tsid.getDataType().getCode());
			addSubBase(symTab, "out2", "version", tsid.getPart("version"));
		}
		jepContext.setTimeSliceBaseTime(null);
		
		// The pre Script can't make any assignments. The user can use it to set
		// defaults, lookup meta data and set variables in the symbol table, etc.
		if (preScript.size() > 0)
			executeScript(preScript);

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
		jepContext.setTimeSliceBaseTime(_timeSliceBaseTime);

		// Add the inputs to the symbol table for this time slice.
		SymbolTable symTab = jepContext.getParser().getSymbolTable();
		for(String inputName : _inputNames)
		{
			double inputVal = inputName.equals("in1") ? in1 :
				inputName.equals("in1") ? in1 :
				inputName.equals("in2") ? in2 :
				inputName.equals("in3") ? in3 :
				inputName.equals("in4") ? in4 :
				inputName.equals("in5") ? in5 : Double.NEGATIVE_INFINITY;

			symTab.remove(inputName);
			String flagName = inputName + ".flags";
			symTab.remove(flagName);
			if (!isMissing(inputVal))
			{
				symTab.addVariable(inputName, new Double(inputVal));
				int f = getInputFlagBits(inputName);
				symTab.addVariable(flagName, new Double(f));

				debug1("" + debugSdf.format(_timeSliceBaseTime) + " " + inputName + "=" + inputVal
					+ ", " + flagName + "=0x" + Integer.toHexString(f));
			}
		}
		
		// Remove the outputs from the symbol table so we can detect assignments.
		// Any user-defined temporary vars stay in the table. The user can use these
		// for accumulating, counting, etc., across all time slices.
		symTab.remove("out1");
		symTab.remove("out1.flags");
		symTab.remove("out2");
		symTab.remove("out2.flags");
		
		// Execute the script should at some point set an output or input flags.
		executeScript(timeSliceScript);

		// Check to see if assignments were made to the output variables.
		Object out1value = jepContext.getParser().getSymbolTable().getValue("out1");
		Object out1Flags = jepContext.getParser().getSymbolTable().getValue("out1.flags");
		if (out1value != null)
		{
debug3("out1 was set to " + out1value);
			if (out1value instanceof Double)
				setOutput(out1, (Double)out1value);
			else if (out1value instanceof String)
				setOutput(out1, (String)out1value);
			if (out1Flags != null)
			{
				int f = (out1Flags instanceof Double) ? (int)(double)(Double)out1Flags
					: (out1Flags instanceof Long) ? (int)(long)(Long)out1Flags
					: 0;
debug3("out1.flags was set to 0x" + Integer.toHexString(f));
				clearNonReservedFlags(out1);
				setFlagBits(out1, f);
			}
else debug3("out1.flags not assigned.");
				
		}
		else
		{
			debug3("out1 was not assigned.");
			if (out1Flags != null)
				warning("Cannot set out1.flags without also ssetting out1 value. Ignored.");
		}
		
		
		Object out2value = jepContext.getParser().getSymbolTable().getValue("out2");
		Object out2Flags = jepContext.getParser().getSymbolTable().getValue("out2.flags");
		if (out2value != null)
		{
debug3("out2 was set to " + out2value);
			if (out2value instanceof Double)
				setOutput(out2, (Double)out2value);
			else if (out2value instanceof String)
				setOutput(out2, (String)out2value);
			if (out2Flags != null)
			{
				int f = (out2Flags instanceof Double) ? (int)(double)(Double)out2Flags
					: (out2Flags instanceof Long) ? (int)(long)(Long)out2Flags
					: 0;
				clearNonReservedFlags(out2);
				setFlagBits(out2, f);
			}
		}
		else
		{
			debug3("out2 was not assigned.");
			if (out2Flags != null)
				warning("Cannot set out2.flags without also ssetting out2 value. Ignored.");
		}
		
		// Check the input '.flags' values in the symbol table to see if any values were
		// changed. If so, set the new flags and cause them to be written to the database.
		for(String inputName : _inputNames)
		{
			String flagName = inputName + ".flags";
debug3("Checking for " + flagName);
			org.nfunk.jep.Variable v = symTab.getVar(flagName);
			if (v != null)
			{
debug3("...found");
				Object vv = v.getValue();
				if (vv instanceof Number)
				{
					int newFlags = ((Number)vv).intValue();
					int origFlags = this.getInputFlagBits(inputName);
debug3("origFlags=0x" + Integer.toHexString(origFlags) + ", newFlags=0x" + Integer.toHexString(newFlags));
					if (newFlags != origFlags)
					{
						debug1("" + debugSdf.format(_timeSliceBaseTime) + " " + flagName + 
							" changed from 0x" + Integer.toHexString(origFlags)
							+ " to 0x" + Integer.toHexString(newFlags));
						// Clear all application-level bits and set to the new values.
						setInputFlagBits(inputName, newFlags, 
							~(VarFlags.RESERVED_4_COMP|IFlags.RESERVED_MASK));
					}
				}
			}
else debug3("...not found");
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
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
		
		// Leave the base time alone. It should be set to the last base time seen 
		// in the time slices.
		executeScript(postScript);
		Object out1value = jepContext.getParser().getSymbolTable().getValue("out1");
		if (out1value != null)
		{
			if (out1value instanceof Double)
				setOutput(out1, (Double)out1value);
			else if (out1value instanceof String)
				setOutput(out1, (String)out1value);
		}
		Object out2value = jepContext.getParser().getSymbolTable().getValue("out2");
		if (out2value != null)
		{
			if (out2value instanceof Double)
				setOutput(out2, (Double)out2value);
			else if (out2value instanceof String)
				setOutput(out2, (String)out2value);
		}
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
