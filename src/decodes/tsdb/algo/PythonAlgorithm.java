/**
 * $Id$
 * 
 * $Log$
 * Revision 1.20  2019/01/29 20:28:58  mmaloney
 * dev
 *
 * Revision 1.19  2019/01/29 19:03:54  mmaloney
 * dev
 *
 * Revision 1.18  2019/01/29 16:45:17  mmaloney
 * dev
 *
 * Revision 1.17  2019/01/22 21:29:43  mmaloney
 * dev
 *
 * Revision 1.16  2019/01/17 15:24:39  mmaloney
 * HDB 646 Set variable with full double precision into Python namespace.
 *
 * Revision 1.15  2019/01/04 15:01:34  mmaloney
 * Added rolename.tskey, and for HDB, rolename.sdi
 *
 * Revision 1.14  2018/07/31 16:59:47  mmaloney
 * isPresent also returns false on Deleted data.
 *
 * Revision 1.13  2018/06/19 13:18:27  mmaloney
 * In the init script, add computation_id to the python namespace. HDB 492.
 *
 * Revision 1.12  2018/06/13 19:17:41  mmaloney
 * dev
 *
 * Revision 1.11  2018/06/13 19:00:02  mmaloney
 * Can't use 'NV' for missing values because can't mix strings and doubles. The expressions
 * won't compile.
 *
 * Revision 1.10  2018/05/31 18:44:19  mmaloney
 * Allow optional parameters.
 *
 * Revision 1.9  2018/05/30 20:23:38  mmaloney
 * Add "tsbt" time slice base time to name space for time slice scripts.
 *
 * Revision 1.8  2017/06/01 18:19:01  mmaloney
 * Fixed cwms logic bug in isPresent.
 *
 * Revision 1.7  2017/06/01 14:49:16  mmaloney
 * Guard against null ptr bug in parmRef.
 *
 * Revision 1.6  2017/05/31 21:29:58  mmaloney
 * Refactoring for HDB.
 *
 * Revision 1.5  2017/02/16 14:42:04  mmaloney
 * Close CwmsRatingDao in final block.
 *
 * Revision 1.4  2016/09/23 15:59:14  mmaloney
 * Only set MISSING to IGNORE if not explicitely set to something else.
 *
 * Revision 1.3  2016/04/22 14:28:49  mmaloney
 * Parse AlgorithmType from Init Script before executing.
 *
 * Revision 1.2  2016/03/24 19:16:06  mmaloney
 * Added for Python Algorithm
 *
 * Revision 1.1  2015/10/26 12:45:34  mmaloney
 * PythonAlgorithm
 *
 * 
 */
package decodes.tsdb.algo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeSet;

import opendcs.dai.TimeSeriesDAI;

import org.python.core.PyFloat;
import org.python.util.PythonInterpreter;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.var.IFlags;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.Variable;
import decodes.comp.ComputationParseException;
import decodes.comp.LookupTable;
import decodes.comp.RdbRatingReader;
import decodes.comp.TabRatingReader;
import decodes.comp.TableBoundsException;
import decodes.cwms.CwmsFlags;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.validation.DatchkReader;
import decodes.cwms.validation.Screening;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.TsidScreeningAssignment;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.hdb.HdbFlags;
import decodes.sql.DbKey;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.MissingAction;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.NoValueException;
import decodes.tsdb.ScriptType;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.compedit.PythonAlgoTracer;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;

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
//	implements DynamicPropertiesOwner
{
//AW:INPUTS
	public double dummyin;	//AW:TYPECODE=i
	String _inputNames[] = { "dummyin" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
	private static PythonAlgorithm runningInstance = null;
	
	/** 
	 * This method is called from Jython code to get the current running instance.
	 * This gives it access to all of the infrastructure methods.
	 */
	public static PythonAlgorithm getRunningInstance() { return runningInstance; }
	private boolean firstTsGroup = true;
	private PythonInterpreter pythonIntepreter = null;
	private String linesep = System.getProperty("line.separator");
	
	DatchkReader datchkReader = null;
	HashSet<String> datchkInitialized = new HashSet<String>();
	HashMap<String, Screening> cwmsScreenings = new HashMap<String, Screening>();
	PythonAlgoTracer tracer = null;
	private HashMap<String, LookupTable> filename2table = new HashMap<String, LookupTable>();
	private LookupTable errorTable = new LookupTable();
	private NumberFormat pyNumFmt = NumberFormat.getNumberInstance();
	private double missingValue = -9000000000000.;
	private double missingLimit = -8999999999900.;
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable dummyout = new NamedVariable("dummyout", 0);
	String _outputNames[] = { "dummyout" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double dummyprop = 123.456;
	String _propertyNames[] = { "dummyprop" };
//AW:PROPERTIES_END

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
		pyNumFmt.setGroupingUsed(false);
		pyNumFmt.setMaximumFractionDigits(5);

		// Replace the dummy param names with the ones defined in the param record.
		ArrayList<String> inputs = new ArrayList<String>();
		ArrayList<String> outputs = new ArrayList<String>();
		for(Iterator<DbAlgoParm> parmit = comp.getAlgorithm().getParms(); parmit.hasNext(); )
		{
			DbAlgoParm parm = parmit.next();
			String role = parm.getRoleName();
			if (parm.getParmType().toLowerCase().startsWith("i"))
				inputs.add(role);
			else
				outputs.add(role);
		}
		_inputNames = new String[inputs.size()];
		inputs.toArray(_inputNames);
		_outputNames = new String[outputs.size()];
		outputs.toArray(_outputNames);
		if (tracer != null)
		{
			tracer.traceMsg("Inputs:");
			for(String name : inputs)
				tracer.traceMsg("\t" + name);
			tracer.traceMsg("Outputs:");
			for(String name : inputs)
				tracer.traceMsg("\t" + name);
		}

		runningInstance = this;
		firstTsGroup = true;
		debug3("initAWAlgorithm: Installed " + _inputNames.length 
			+ " inputs and " + _outputNames.length + " outputs.");
		
		for(DbCompAlgorithmScript script : comp.getAlgorithm().getScripts())
			if (script.getScriptType() == ScriptType.PY_Init)
			{
				Properties initProps = new Properties();
				try
				{
					initProps.load(new StringReader(script.getText()));
					String s = PropertiesUtil.getIgnoreCase(initProps, "AlgorithmType");
					if (s != null)
					{
						_awAlgoType = AWAlgoType.fromString(s);
						debug1("AlgorithmType set to " + _awAlgoType);
						
						// set _aggPeriodVarRoleName to the first output parameter.
						if (_awAlgoType == AWAlgoType.AGGREGATING
						 && _outputNames.length > 0)
						{
							_aggPeriodVarRoleName = _outputNames[0];
							debug1("set _aggPeriodVarRoleName to " + _aggPeriodVarRoleName);
						}
					}
				}
				catch (IOException ex)
				{
					warning("Error parsing init script '" + script.getText() + "': " + ex);
				}
				break;
			}

//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		debug3("beforeTimeSlices()");
		// on the first call of beforeTimeSlices after initAWAlgorithm...
		if (firstTsGroup)
			firstBeforeTimeSlices();
		
		// Execute the before script
		DbCompAlgorithmScript beforeScript = comp.getAlgorithm().getScript(ScriptType.PY_BeforeTimeSlices);
		if (beforeScript != null && beforeScript.getText() != null && beforeScript.getText().length() > 0)
		{
			try
			{
				debug3("Executing beforeScript:" + linesep + beforeScript.getText());
				pythonIntepreter.exec(beforeScript.getText());
			}
			catch(Exception ex)
			{
				String msg = "Error executing beforeScript : " + ex;
				warning(msg + linesep + beforeScript.getText());
				throw new DbCompException(msg);
			}
		}
		
//AW:BEFORE_TIMESLICES_END
	}
	
	public void firstBeforeTimeSlices()
		throws DbCompException
	{
		// Python algorithms have special behavior for MISSING actions.
		// The default missing action for python should be IGNORE. This allows a multi-step
		// computation to run when intermediate products do not yet exist. The algorithm
		// can use the isPresent method to control execution of dependent blocks of code.
		// So, set the Missing Action to ignore unless there is an explicit property setting
		// to something else.
		for(String roleName : this.getInputNames())
		{
			ParmRef parmRef = this.getParmRef(roleName);
			if (parmRef == null)
				continue;
			
			String missingPropval = comp.getProperty(roleName + "_MISSING");
				if (missingPropval == null)
					parmRef.setMissingAction(MissingAction.IGNORE);
debug3("Missing action for '" + roleName + "' now set to " + parmRef.missingAction);
		}
		
		// Instantiate the PythonInterpreter
		debug3("... Creating PythonInterpreter");
		pythonIntepreter = new PythonInterpreter();
		
		// Execute the canned init stuff.
		FileInputStream fis = null;
		String fn = EnvExpander.expand("$DCSTOOL_HOME/python/PyAlgoEnv.py");
		try
		{
			fis = new FileInputStream(fn);
			debug3("... Executing " + fn);
			pythonIntepreter.execfile(fis, "PyAlgoEnv");
		}
		catch(Exception ex)
		{
			String msg = "Cannot execute '" + fn + "': " + ex;
			warning(msg);
			throw new DbCompException(msg);
		}
		finally
		{
			if (fis != null)
				try { fis.close(); } catch(Exception ex) {}
		}
		
		String parmInitScript = makeInitScript();
		debug3("... Executing parmInitScript:" + linesep + parmInitScript);
		try
		{
			pythonIntepreter.exec(parmInitScript);
		}
		catch(Exception ex)
		{
			String msg = "Error executing parmInitScript : " + ex;
			warning(msg + linesep + parmInitScript);
			throw new DbCompException(msg);
		}
		firstTsGroup = false;
	}
	
	public String makeInitScript()
	{
		// Initialize all the variables.
		// Put value and TSID info into the namespace for each param.
		StringBuilder sb = new StringBuilder();
		for(Iterator<DbAlgoParm> parmit = comp.getAlgorithm().getParms(); parmit.hasNext(); )
		{
			DbAlgoParm parm = parmit.next();
debug3("Checking parm '" + parm.getRoleName() + "' with type " + parm.getParmType());
			String role = parm.getRoleName();
			ParmRef parmRef = getParmRef(role);
			TimeSeriesIdentifier tsid = this.getParmTsId(role);
			if (tsid != null)
			{
				// This defines the object with tsid and current value.
				// Use 0.0 as a placeholder for now.
				sb.append(role + " = AlgoParm('" + tsid.getUniqueString() + "')" + linesep);
				for(String part : tsid.getParts())
					sb.append(role + "." + part.toLowerCase() + " = '" + tsid.getPart(part) + "'" + linesep);
				
				// MJM 20180104 add .tsid and .sdi
				sb.append(role + ".tskey = " + (DbKey.isNull(tsid.getKey()) ? -1 : tsid.getKey()) + linesep);
				DbCompParm compParm = comp.getParm(role);
				sb.append(role + "sdi = " + 
					(compParm != null && !DbKey.isNull(compParm.getSiteDataTypeId()) ? 
						compParm.getSiteDataTypeId() : -1)
					+ linesep);
				
				if (tsdb.isCwms() || tsdb.isOpenTSDB())
				{
					// Add baselocation, sublocation, baseparam, subparam, baseversion, subversion
					for(String partname : new String[]{ "location", "param", "version" })
					{
						String fullpart = tsid.getPart(partname);
						int hyphen = fullpart.indexOf('-');
						// If no hyphen, then base is the full part.
						String basepart = hyphen < 0 ? fullpart : fullpart.substring(0, hyphen);
						sb.append(role + ".base" + partname + " = '" + basepart + "'" + linesep);
						if (hyphen > 0 && fullpart.length() > hyphen + 1)
							sb.append(role + ".sub" + partname + " = '" 
								+ fullpart.substring(hyphen+1) + "'" + linesep);
					}
				}
			}
			else
			{
				sb.append(role + " = AlgoParm('undefined')" + linesep);
				debug1("No time series assigned to role " + parm.getRoleName());
				debug1("parmRef for '" + parm.getRoleName() + "' " + 
					(parmRef == null || parmRef.timeSeries == null ?
						"HAS NO TIME SERIES." : "HAS A TIME SERIES"));
				
//				if (parmRef.timeSeries!= null)
//					debug1("... TSID for time series is " + parmRef.timeSeries.getTimeSeriesIdentifier().getUniqueString());
			}
		}
		
		// Add the properties to the script
		NumberFormat propFmt = NumberFormat.getInstance();
		propFmt.setGroupingUsed(false);
		propFmt.setMaximumFractionDigits(4);
		for(Enumeration<?> pnenum = comp.getPropertyNames(); pnenum.hasMoreElements(); )
		{
			String pname = pnenum.nextElement().toString();
			String pval = comp.getProperty(pname).trim();
			if (pval.equalsIgnoreCase("true"))
				sb.append(pname + "=True" + linesep);
			else if (pval.equalsIgnoreCase("false"))
				sb.append(pname + "=False" + linesep);
			else
			{
				try { sb.append(pname + "=" + propFmt.format(Double.parseDouble(pval)) + linesep); }
				catch(NumberFormatException ex)
				{
					if ((pval.startsWith("'") && pval.endsWith("'"))
					 || (pval.startsWith("\"") && pval.endsWith("\"")))
						sb.append(pname + "=" + pval + linesep);
					else
						sb.append(pname + "='" + pval + "'" + linesep);
				}
			}
		}
		
		// HDB 492, add the computation_id to the python environment
		if (this.comp != null)
			sb.append("computation_id=" + this.comp.getId() + linesep);
		
		return sb.toString();
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
		setTSBT();
		
		// The setTimeSliceInput method below was called by AW_AlgorithmBase
		// to put all the slice values into the python interpreter.
		// Now execute the script.
		// Execute the before script
		DbCompAlgorithmScript tsScript = comp.getAlgorithm().getScript(ScriptType.PY_TimeSlice);
		if (tsScript != null && tsScript.getText() != null && tsScript.getText().length() > 0)
		{
			try
			{
				debug3("Executing tsScript:" + linesep + tsScript.getText());
				pythonIntepreter.exec(tsScript.getText());
			}
			catch(Exception ex)
			{
				String msg = "Error executing tsScript : " + ex;
				warning(msg + linesep + tsScript.getText());
//				throw new DbCompException(msg);
			}
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
		DbCompAlgorithmScript afterScript = comp.getAlgorithm().getScript(ScriptType.PY_AfterTimeSlices);
		if (afterScript != null && afterScript.getText() != null && afterScript.getText().length() > 0)
		{
			try
			{
				debug3("Executing afterScript:" + linesep + afterScript.getText());
				pythonIntepreter.exec(afterScript.getText());
			}
			catch(Exception ex)
			{
				String msg = "Error executing afterScript : " + ex;
				warning(msg + linesep + afterScript.getText());
				throw new DbCompException(msg);
			}
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
	
	/**
	 * Called from AW_AlgorithmBase at the beginning of a time slice.
	 * Set the variables value in the algorithm's namespace
	 * @param nv null means there's no value at this time slice.
	 */
	public void setTimeSliceInput(String varName, NamedVariable nv)
	{
		String expr = null;
		if (nv == null || nv.getStringValue().trim().length() == 0 
			|| nv.getStringValue().equalsIgnoreCase("NA")
			|| (nv.getFlags() & (IFlags.IS_ERROR|IFlags.IS_ERROR|CwmsFlags.VALIDITY_MISSING)) != 0)
			expr = varName + ".value = " + missingValue + linesep
				+  varName + ".qual = 0x40000000" + linesep;
		else
		{
			try
			{
				// MJM 20190116 set value into Python name space with full double precision
				// Note setting name.value directly from set() does NOT work. Use intermediate variable.
				this.pythonIntepreter.set("___x___", new PyFloat(nv.getDoubleValue()));
				expr = 
					varName + ".value = ___x___" + linesep +
					varName + ".qual = 0x" + Integer.toHexString(nv.getFlags()) + linesep;
			}
			catch(NoConversionException ex)
			{
				expr = varName + ".value = " + missingValue + linesep
					+  varName + ".qual = 0x40000000" + linesep;
			}
		}
		debug3("Executing:\n" + expr);
		this.pythonIntepreter.exec(expr);
		
	}
	
	private void setTSBT()
	{
		String expr = "tsbt = " + ((double)this._timeSliceBaseTime.getTime() / 1000.0);
		debug3("Executing:\n" + expr);
		this.pythonIntepreter.exec(expr);
	}
	
	public void setOutput(String rolename, double value)
	{
		debug1("setOutput(" + rolename + ", " + value + ")");
		if (tracer != null)
			return;
		NamedVariable nv = new NamedVariable(rolename, value);
		
		// Don't overwrite a triggering value:
		if (VarFlags.wasAdded(nv))
			return;

		int f = nv.getFlags();
		if (value < missingLimit)
		{
			value = 0.0;
			f |= IFlags.IS_MISSING;
		}

		if ((f & IFlags.IS_MISSING) != 0)
			nv.setFlags(f & (~IFlags.IS_MISSING));
		this.setOutput(nv, value);

		// See docs in PythonWritten.java for explanation of the following:
		ParmRef parmRef = this.getParmRef(rolename);
		ComputationApp app = ComputationApp.instance();
		if (parmRef.compParm.getAlgoParmType().startsWith("i") && app != null)
			app.getResolver().pythonWrote(comp.getId(), parmRef.timeSeries.getTimeSeriesIdentifier().getKey());
	}
	
	/**
	 * Return true if the named param has a value in the current timeslice.
	 * Return false if there is no value or it is flagged as missing or for deletion.
	 * @param rolename
	 * @return
	 */
	public boolean isPresent(String rolename)
	{
		debug1("isPresent(" + rolename + ")");
		if (tracer != null)
			return true;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
		{
			debug3("isPresent(" + rolename + ") - no variable in timeslice - returning false");
			return false;
		}
		return isPresent(nv);
	}
	
	public boolean isPresent(Variable v)
	{
		int f = v.getFlags();
		if ((f & (IFlags.IS_MISSING | VarFlags.TO_DELETE | VarFlags.DB_DELETED)) != 0)
		{
			debug3("isPresent - Flags indicate missing or deleted -- returning false.");
			return false;
		}
		try
		{
			double value = v.getDoubleValue();
			if (value < missingLimit)
				return true;
		}
		catch (NoConversionException e)
		{
		}
		
		if (tsdb.isCwms())
			return (f & CwmsFlags.VALIDITY_MISSING) == 0;
		debug3("isPresent - returning TRUE");
		return true;
	}

	/**
	 * Return true if the named param has a value in the current timeslice
	 * and that value is flagged as questionable. Return false if no value
	 * or if it is not flagged questionable.
	 * This method only works for CWMS.
	 * @param rolename
	 * @return
	 */
	public boolean isQuestionable(String rolename)
	{
		debug1("isQuestionable(" + rolename + ")");
		if (tracer != null)
			return false;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			return false;
		int f = nv.getFlags();
		if (tsdb.isCwms())
			return (f & CwmsFlags.VALIDITY_QUESTIONABLE) != 0;
		else if (tsdb.isHdb())
			return HdbFlags.isQuestionable(f);
		else return false;
	}

	
	/**
	 * Return true if the named param has a value in the current timeslice
	 * and that value is flagged as rejected. Return false if no value
	 * or if it is not flagged rejected.
	 * This method only works for CWMS.
	 * @param rolename
	 * @return
	 */
	public boolean isRejected(String rolename)
	{
		debug1("isRejected(" + rolename + ")");
		if (tracer != null)
			return false;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			return false;
		return isRejected(nv);
	}
	
	public boolean isRejected(Variable v)
	{
		int f = v.getFlags();
		if (!tsdb.isCwms())
			return false;
		if (tsdb.isCwms())
			return (f & CwmsFlags.VALIDITY_REJECTED) != 0;
		else if (tsdb.isHdb())
			return HdbFlags.isRejected(f);
		return false;
	}

	/**
	 * Return true if the named param has a value in the current timeslice
	 * and that value is flagged as good quality. That is, it is not flagged
	 * as questionable, rejected, missing, or to-delete.
	 * This method only works for CWMS.
	 * @param rolename
	 * @return
	 */
	public boolean isGoodQuality(String rolename)
	{
		debug1("isGoodQuality(" + rolename + ")");
		if (tracer != null)
			return true;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			return false;
		int f = nv.getFlags();
		try
		{
			double value = nv.getDoubleValue();
			if (value < missingLimit)
				return false;
		}
		catch (NoConversionException e)
		{
		}
		
		if (tsdb.isCwms())
		{
			boolean r = (f & 
				(CwmsFlags.VALIDITY_REJECTED | CwmsFlags.VALIDITY_QUESTIONABLE
					| IFlags.IS_MISSING | VarFlags.TO_DELETE)) == 0;
			debug3("   checking cwms flag value " + Integer.toHexString(f) + " and returning " + r);
			return r;
		}
		else if (tsdb.isHdb())
		{
			return HdbFlags.isGoodQuality(f);
		}
		else
			return false;
	}
	
	/**
	 * Return true if the named param is either a triggering value for this
	 * computation, or was just computed and is flagged to write to the database.
	 * This can be used to avoid unnecessary steps in a multi-input computation.
	 * @param rolename
	 * @return
	 */
	public boolean isNew(String rolename)
	{
		debug1("isNew(" + rolename + ")");
		if (tracer != null)
			return true;
		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			return false;
		int f = nv.getFlags();
		boolean ret = (f & (VarFlags.DB_ADDED | VarFlags.TO_WRITE)) != 0;
		debug1("    ... returning " + ret);
		return ret;
	}

	/**
	 * Return the running average of the named param over the specified duration.
	 * @param name The role-name of the parameter to average
	 * @param duration Must be a valid duration string in this database
	 * @param boundaries (default="(]", meaning open at the beginning and closed
	 *        at the end). Paren means open, i.e. boundary value not included.
	 *        Square bracket means closed, i.e. boundary is included.
	 * @throws NoSuchObjectException if duration or boundaries are invalid
	 * @throws NoValueException if there are no values of the named time series in
	 *         the specified duration
	 * @return the running average
	 */
	public double runningAverage(String name, String duration, String boundaries)
		throws NoSuchObjectException, NoValueException
	{
		debug3("runningAverage(" + name + ", " + duration + ", " + boundaries + ")");

		TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		ParmRef parmRef = this.getParmRef(name);
		if (parmRef == null || parmRef.timeSeries == null)
			throw new NoSuchObjectException("runningAverage: no time series for role '"
				+ name + "'");
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(aggCal.getTimeZone());
		Date until = parmRef.compParm.baseTimeToParamTime(_timeSliceBaseTime, cal);
		IntervalIncrement iinc = IntervalCodes.getIntervalCalIncr(duration);
		cal.add(iinc.getCalConstant(), -iinc.getCount());
		Date since = cal.getTime();
		boolean aggLowerBoundClosed = false;
		boolean aggUpperBoundClosed = true;
		if (boundaries != null && boundaries.length() >= 1 && boundaries.charAt(0) == '[')
			aggLowerBoundClosed = true;
		if (boundaries != null && boundaries.length() >= 2 && boundaries.charAt(1) == ')')
			aggUpperBoundClosed = false;
		try
		{
			timeSeriesDAO.fillTimeSeries(parmRef.timeSeries, since, until,
				aggLowerBoundClosed, aggUpperBoundClosed, false);
			double tally = 0.0;
			double count = 0;
			for(int idx = 0; idx < parmRef.timeSeries.size(); idx++)
			{
				TimedVariable tv = parmRef.timeSeries.sampleAt(idx);
				if ((aggLowerBoundClosed && tv.getTime().before(since))
				 || (!aggLowerBoundClosed && !tv.getTime().after(since)))
					continue;
				if ((aggUpperBoundClosed && tv.getTime().after(until))
				 || (!aggUpperBoundClosed && !tv.getTime().before(until)))
				{
					break;
				}
				// skip missing or rejected values
				if (!isPresent(tv) || isRejected(tv))
					continue;
				try
				{
					tally += tv.getDoubleValue();
					count++;
				}
				catch(NoConversionException ex)
				{
					
				}
			}
			if (count == 0)
				return 0.0;
			else
				return tally / count;
		}
		catch (Exception ex)
		{
			String msg = "Error in runningAverage...fillTimeSeries("
				+ parmRef.timeSeries.getTimeSeriesIdentifier().getUniqueString() + ", "
				+ debugSdf.format(since) + ", " + debugSdf.format(until) + "): " + ex;
			warning(msg);
			throw new NoSuchObjectException(msg);
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}
	
	/**
	 * Perform the CWMS DATCHK functions on the named variable.
	 * See docs on Datchk algorithm on how datchk is configured.
	 * @param rolename
	 */
	public int datchk(String rolename)
	{
		debug1("datchk(" + rolename + ")");
		if (tracer != null)
			return 0;
		
		if (!tsdb.isCwms())
			return 0;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			return 0;
		if (!isPresent(nv))
			return nv.getFlags();

		if (datchkReader == null)
			datchkReader = DatchkReader.instance();

		ParmRef parmRef = getParmRef(rolename);
		TimeSeriesIdentifier tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		if (tsid == null)
		{
			warning("datchk(" + rolename + ") -- no time series assigned to this role.");
			return nv.getFlags();
		}
		
		Screening screening = null;
		try { screening = datchkReader.getScreening(tsid); }
		catch (DbCompException ex)
		{
			warning("datchk(" + rolename + ") Bad DATCHK config: " + ex);
			return nv.getFlags();
		}
		if (screening == null)
		{
			warning("datchk(" + rolename + ") No screening defined for tsid '" 
				+ tsid.getUniqueString() + "'");
			return nv.getFlags();
		}

		// If this is the first time I've seen this screening in this run of the
		// comp, initialize it.
		if (!datchkInitialized.contains(tsid.getUniqueString()))
		{
			screeningFirstTime(screening, parmRef);
			datchkInitialized.add(tsid.getUniqueString());
		}
		doScreening(screening, nv, parmRef);
		return nv.getFlags();
	}
	
	/**
	 * Called for either DATCHK or CWMS-resident screening to do the actual screening.
	 * @param screening
	 * @param nv
	 * @param parmRef
	 */
	private void doScreening(Screening screening, NamedVariable nv, ParmRef parmRef)
	{
		ScreeningCriteria crit = screening.findForDate(
			parmRef.compParm.baseTimeToParamTime(_timeSliceBaseTime, aggCal));
		if (crit == null)
		{
			warning("No criteria for time=" + debugSdf.format(_timeSliceBaseTime));
			// Treat no criteria the same as a screening where everything passes.
			int flags = nv.getFlags();
			if ((flags & CwmsFlags.PROTECTED) != 0)
				return;
			flags &= (~(CwmsFlags.VALIDITY_MASK | CwmsFlags.TEST_MASK));
			flags |= (CwmsFlags.SCREENED | CwmsFlags.VALIDITY_OKAY);
			if (flags == nv.getFlags())
				return; // No changes to flags. Do not write output.
			nv.setFlags(flags);
			VarFlags.setToWrite(nv);
			_saveOutputCalled = true;
			return;
		}

		crit.executeChecks(dc, parmRef.timeSeries, _timeSliceBaseTime, nv, this);
	}
	
	private void screeningFirstTime(Screening screening, ParmRef parmRef)
	{
		// Using the tests, determine the amount of past-data needed at each time-slice.
		TreeSet<Date> needed = new TreeSet<Date>();
		TimeSeriesIdentifier tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		
		IntervalIncrement tsinc = IntervalCodes.getIntervalCalIncr(tsid.getInterval());
		boolean inputIrregular = tsinc == null || tsinc.getCount() == 0;

		debug3("Retrieving additional data needed for screening.");
		ScreeningCriteria prevcrit = null;
		for(int idx = 0; idx < parmRef.timeSeries.size(); idx++)
		{
			TimedVariable tv = parmRef.timeSeries.sampleAt(idx);
			if (VarFlags.wasAdded(tv))
			{
				ScreeningCriteria crit = screening.findForDate(tv.getTime());
				Site site = tsid.getSite();
				if (site != null && site.timeZoneAbbr != null && site.timeZoneAbbr.length() > 0)
				{
					TimeZone tz = TimeZone.getTimeZone(site.timeZoneAbbr);
					screening.setSeasonTimeZone(tz);
				}
				if (crit == null || crit == prevcrit)
					continue;
				crit.fillTimesNeeded(parmRef.timeSeries, needed, aggCal, this);
				prevcrit = crit;
			}
		}
		debug3("additional data for screening, #times needed=" + needed.size());
		
		if (needed.size() > 0)
		{
			TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
			try
			{
				// Optimization: if >= 50 values within 4 days,
				// use a range retrieval.
				Date start = needed.first();
				Date end = needed.last();
				if (inputIrregular
				 || (needed.size() >= 50 && end.getTime() - start.getTime() <= (4*24*3600*1000L)))
				{
					timeSeriesDAO.fillTimeSeries(parmRef.timeSeries, start, end, true, true, false);
				}
				else
					timeSeriesDAO.fillTimeSeries(parmRef.timeSeries, needed);
			}
			catch (Exception ex)
			{
				warning("screeningFirstTime -- error retrieving time series data for '"
					+ tsid.getUniqueString() + "': " + ex);
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}
		
		String euAbbr = screening.getCheckUnitsAbbr();
		if (euAbbr != null 
		 && !euAbbr.equalsIgnoreCase(parmRef.timeSeries.getUnitsAbbr()))
		{
			// In Python, can't change the units because user has already
			// set them and there may be other dependencies in the python
			// code. Therefore convert the screening units.
			try
			{
				screening.convertUnits(parmRef.timeSeries.getUnitsAbbr());
			}
			catch (decodes.db.NoConversionException ex)
			{
				warning("screeningFirstTime -- error converting screening for '"
					+ tsid.getUniqueString() + "' from " + screening.getCheckUnitsAbbr()
					+ " to " + parmRef.timeSeries.getUnitsAbbr() + ": " + ex);
			}
		}
	}

	/**
	 * Perform the CWMS Screening functions on the named variable.
	 * @param rolename
	 * @return the new flag value after screening
	 */
	public int screening(String rolename)
	{
		debug1("screening(" + rolename + ")");
		if (tracer != null)
			return 0;
		
		if (!tsdb.isCwms())
			return 0;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
		{
debug3("... no named variable for '" + rolename + "' in this timeslice.");
			return 0;
		}
		if (!isPresent(nv))
		{
debug3("... variable for '" + rolename + "' NOT PRESENT in this timeslice. flags=0x" + 
Integer.toHexString(nv.getFlags()));
			return nv.getFlags();
		}

		ParmRef parmRef = getParmRef(rolename);
		TimeSeriesIdentifier tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		if (tsid == null)
		{
			warning("screening(" + rolename + ") -- no time series assigned to this role.");
			return nv.getFlags();
		}
debug3("screening(" + rolename + ") tsid='" + tsid.getUniqueString() + "'");
		
		Screening screening = cwmsScreenings.get(tsid.getUniqueString());
		if (screening == null)
		{
			ScreeningDAI screeningDAO = null;
			try
			{
				screeningDAO = ((CwmsTimeSeriesDb)tsdb).makeScreeningDAO();
				debug3("Attempting to read screening for TSID '" + tsid.getUniqueString() + "'");
				TsidScreeningAssignment tsa = screeningDAO.getScreeningForTS(tsid);
				screening = tsa != null && tsa.isActive() ? tsa.getScreening() : null;
				if (screening != null)
					cwmsScreenings.put(tsid.getUniqueString(), screening);
				else
				{
					warning("screening(" + rolename + ") No screening defined for tsid '" 
						+ tsid.getUniqueString() + "'");
					return nv.getFlags();
				}
				screeningFirstTime(screening, parmRef);
			}
			catch (DbIoException ex)
			{
				warning("screening(" + rolename + ") Error while reading screening for '" 
					+ tsid.getUniqueString() + "': " + ex);
				return nv.getFlags();
			}
			finally
			{
				if (screeningDAO != null)
					screeningDAO.close();
			}
		}
		
		if (screening == null)
		{
			warning("screening(" + rolename + ") No screening defined for tsid '" 
				+ tsid.getUniqueString() + "'");
			return nv.getFlags();
		}

		doScreening(screening, nv, parmRef);
		return nv.getFlags();
	}
	
	/**
	 * Completely set the flag bits of a parameter.
	 * @param rolename
	 * @param qual
	 */
	public void setQual(String rolename, int qual)
	{
		debug1("setQual(" + rolename + ", 0x" + Integer.toHexString(qual) + ")");
		if (tracer != null)
			return;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			return;
		nv.setFlags(qual);
		VarFlags.setToWrite(nv);
		_saveOutputCalled = true;
		
		// See docs in PythonWritten.java for explanation of the following:
		ParmRef parmRef = this.getParmRef(rolename);
		ComputationApp app = ComputationApp.instance();
		if (parmRef.compParm.getAlgoParmType().startsWith("i") && app != null)
			app.getResolver().pythonWrote(comp.getId(), parmRef.timeSeries.getTimeSeriesIdentifier().getKey());
	}
	
	/**
	 * Sets the output and quality bits in one go.
	 * @param rolename
	 * @param value
	 * @param qual
	 */
	public void setOutputAndQual(String rolename, double value, int qual)
	{
		debug1("setOutputAndQual(" + rolename + ", " + value + ", 0x" + Integer.toHexString(qual) + ")");
		if (tracer != null)
			return;
		
		if (value < missingLimit)
			qual |= IFlags.IS_MISSING;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
		{
			nv = new NamedVariable(rolename, value);
			_timeSliceVars.add(nv);
		}
		else if (VarFlags.wasAdded(nv))
			return; // Value exists already in this timeslice. Don't overwrite a triggering value.
		else
			nv.setValue(value);
		nv.setFlags(qual);
		VarFlags.setToWrite(nv);
		_saveOutputCalled = true;
		
		// See docs in PythonWritten.java for explanation of the following:
		ParmRef parmRef = this.getParmRef(rolename);
		ComputationApp app = ComputationApp.instance();
		if (parmRef.compParm.getAlgoParmType().startsWith("i") && app != null)
			app.getResolver().pythonWrote(comp.getId(), parmRef.timeSeries.getTimeSeriesIdentifier().getKey());
	}
	
	/**
	 * Return the change in the specified parameter from the
	 * value at the specified duration in the past to the
	 * currently evaluating time slice. Thus a positive
	 * value means the value has risen.
	 * @param rolename the parameter's role name
	 * @param duration a valid duration in this database
	 * @return
	 */
	public double changeSince(String rolename, String duration)
		throws NoSuchObjectException, NoValueException
	{
		debug1("changeSince(" + rolename + ", " + duration + ")");
		if (tracer != null)
			return 5.0;

		NamedVariable nv = _timeSliceVars.findByName(rolename);
		if (nv == null)
			throw new NoValueException("changeSince: No value for '" + rolename
				+ " in time slice base time " + debugSdf.format(_timeSliceBaseTime));

		ParmRef parmRef = this.getParmRef(rolename);
		if (parmRef == null || parmRef.timeSeries == null)
			throw new NoSuchObjectException("changeSince: no time series for role '"
				+ rolename + "'");

		IntervalIncrement tsinc = IntervalCodes.getIntervalCalIncr(duration);
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(aggCal.getTimeZone());
		parmRef.compParm.baseTimeToParamTime(_timeSliceBaseTime, cal);
		cal.add(tsinc.getCalConstant(), -tsinc.getCount());
		Date needed = cal.getTime();
		TimedVariable tv = parmRef.timeSeries.findWithin(needed, roundSec);
		if (tv == null)
		{
			try
			{
				ArrayList<Date> qd = new ArrayList<Date>();
				qd.add(needed);
				if (tsdb.fillTimeSeries(parmRef.timeSeries, qd) == 1)
					tv = parmRef.timeSeries.findWithin(needed, roundSec);
				
				if (tv == null || !isPresent(tv))
				{
					TimedVariable prev = tsdb.getPreviousValue(parmRef.timeSeries, needed);
					TimedVariable next = tsdb.getNextValue(parmRef.timeSeries, needed);
					if (prev == null || next == null)
						throw new NoValueException("changeSince(" + rolename
							+ ") no value at time " + debugSdf.format(needed)
							+ " and can't interpolate.");
					// The prev & next should now be in the time series.
					tv = parmRef.timeSeries.findInterp(needed.getTime()/1000);
					// shouldn't happen but just in case:
					if (tv == null)
						throw new NoValueException("changeSince(" + rolename
							+ ") no value at time " + debugSdf.format(needed)
							+ " and interpolation failed.");
				}
			}
			catch (Exception ex)
			{
				throw new NoValueException("changeSince(" + rolename
					+ ") no value at time " + debugSdf.format(needed)
					+ " and error reading db to interpolate: " + ex);
			}
		}
		
		// Now we have tv as the previous time.
		try
		{
			return nv.getDoubleValue() - tv.getDoubleValue();
		}
		catch(NoConversionException ex)
		{
			throw new NoSuchObjectException("changeSince(" + rolename
				+ ") no value at time " + debugSdf.format(needed)
				+ " and error fetching values as numbers: " + ex);
		}
	}
	
	/**
	 * Attempts rating from table in the time series database
	 * @param specId
	 * @param indeps
	 * @return
	 * @throws NoValueException
	 */
	public double rating(String specId, double... indeps)
		throws NoValueException
	{
		StringBuilder sb = new StringBuilder("rating(" + specId + ", with " + indeps.length + " independents" + "):");
		for(double d : indeps)
			sb.append(" " + d);

		debug1(sb.toString());
		if (tracer != null)
			return 100.0;

		try
		{
			return tsdb.rating(specId, _timeSliceBaseTime, indeps);
		}
		catch(Exception ex)
		{
			throw new NoValueException("rating(" + specId + ") failed: " + ex);
		}
	}
	
	public double rdbrating(String tabfile, double indep)
		throws NoValueException
	{
		String tabFileExp = EnvExpander.expand(tabfile);
		debug1("rdbrating(" + tabfile + ", " + pyNumFmt.format(indep) + ")");
		if (tracer != null)
			return 100.0;

		LookupTable lookupTable = filename2table.get(tabFileExp);
		if (lookupTable == errorTable)
			throw new NoValueException("rdbrating: Cannot read table '" + tabFileExp + "'");
		if (lookupTable == null)
		{
			// first time for this table. Attempt to read it.
			RdbRatingReader tableReader = new RdbRatingReader(tabFileExp);

			lookupTable = new LookupTable();
			try
			{
				tableReader.readRatingTable(lookupTable);
			}
			catch(ComputationParseException ex)
			{
				String msg = "rdbrating Cannot read RDB rating table: " + ex;
				warning(msg);
				filename2table.put(tabFileExp, errorTable);
				throw new NoValueException(msg);
			}
			filename2table.put(tabFileExp, lookupTable);
		}
		
		try
		{
			return lookupTable.lookup(indep);
		}
		catch(TableBoundsException ex)
		{
			throw new NoValueException("rdbrating " + ex);
		}
	}
	
	public double tabrating(String tabfile, double indep)
		throws NoValueException
	{
		String tabFileExp = EnvExpander.expand(tabfile);
		debug1("tabrating(" + tabfile + ", " + pyNumFmt.format(indep) + ")");
		if (tracer != null)
			return 100.0;

		LookupTable lookupTable = filename2table.get(tabFileExp);
		if (lookupTable == errorTable)
			throw new NoValueException("tabrating: Cannot read table '" + tabFileExp + "'");
		if (lookupTable == null)
		{
			// first time for this table. Attempt to read it.
			TabRatingReader tableReader = new TabRatingReader(tabFileExp);
			
			lookupTable = new LookupTable();
			try
			{
				tableReader.readRatingTable(lookupTable);
			}
			catch(ComputationParseException ex)
			{
				String msg = "tabrating Cannot read TAB rating table: " + ex;
				warning(msg);
				filename2table.put(tabFileExp, errorTable);
				throw new NoValueException(msg);
			}
			filename2table.put(tabFileExp, lookupTable);
		}
		
		try
		{
			return lookupTable.lookup(indep);
		}
		catch(TableBoundsException ex)
		{
			throw new NoValueException("tabrating " + ex);
		}
	}


	
	public void trace(String msg)
	{
		if (tracer != null)
			tracer.traceMsg(msg);
	}
	
	public void debug3(String msg)
	{
		if (tracer != null)
			tracer.traceMsg(Logger.instance().standardMessage(Logger.E_DEBUG3, msg));
		super.debug3(msg);
	}
	public void debug2(String msg)
	{
		if (tracer != null)
			tracer.traceMsg(Logger.instance().standardMessage(Logger.E_DEBUG2, msg));
		super.debug2(msg);
	}
	public void debug1(String msg)
	{
		if (tracer != null)
			tracer.traceMsg(Logger.instance().standardMessage(Logger.E_DEBUG1, msg));
		super.debug1(msg);
	}
	public void info(String msg)
	{
		if (tracer != null)
			tracer.traceMsg(Logger.instance().standardMessage(Logger.E_INFORMATION, msg));
		super.info(msg);
	}
	public void warning(String msg)
	{
		if (tracer != null)
			tracer.traceMsg(Logger.instance().standardMessage(Logger.E_WARNING, msg));
		super.warning(msg);
	}

	public PythonInterpreter getPythonIntepreter()
	{
		return pythonIntepreter;
	}

	public void setTracer(PythonAlgoTracer tracer)
	{
		this.tracer = tracer;
	}
	
	public void abortComp(String msg)
		throws DbCompException
	{
		throw new DbCompException(msg);
	}
	
}
