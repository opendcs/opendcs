package decodes.tsdb.algo.jep;

import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.util.Date;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import decodes.cwms.CwmsFlags;
import decodes.cwms.validation.DatchkReader;
import decodes.cwms.validation.Screening;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.TsidScreeningAssignment;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.tsdb.DbIoException;
import decodes.tsdb.ParmRef;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * This implements the CWMS screening() function in the Expression Parser.
 * The function takes a single argument which is the algorithm role name.
 * It can only be called during a time slice.
 * It finds the value at the current time slice, performs the screening
 * specified in CWMS database, and returns the flag value.
 */
public class ScreeningFunction
	extends PostfixMathCommand
{
	public static final String funcName = "screening";
	private JepContext ctx = null;

	public ScreeningFunction(JepContext ctx)
	{
		super();
		this.ctx = ctx;
		this.numberOfParameters = 1;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);
		Date tsbt = ctx.getTimeSliceBaseTime();
		if (tsbt == null)
			throw new ParseException(funcName + " can only be called from within a time-slice script.");

		// Get input name (1st arg)
		String name = inStack.pop().toString();
		
		// Find the Screening record
		ParmRef inputParm = ctx.getAlgo().getParmRef(name);
		if (inputParm.timeSeries == null)
			throw new ParseException("No input time-series for '" + name + "'!");
		TimeSeriesIdentifier inputTsid = inputParm.timeSeries.getTimeSeriesIdentifier();
		if (inputTsid == null)
			throw new ParseException("No input time-series identifier associated with '" + name + "'!");
		
		ScreeningDAI screeningDAO = null;
		Screening screening = null;
		try
		{
			screeningDAO = ((CwmsTimeSeriesDb)ctx.getTsdb()).makeScreeningDAO();
			TsidScreeningAssignment tsa = screeningDAO.getScreeningForTS(inputTsid);
			screening = tsa != null && tsa.isActive() ? tsa.getScreening() : null;
		}
		catch(DbIoException ex)
		{
			ctx.getAlgo().warning(funcName + ": error reading screening: " + ex);
		}
		if (screening == null)
			throw new ParseException("No active screening found for " + inputTsid.getUniqueString());
		
		int retFlags = 0;
		TimedVariable tv = inputParm.timeSeries.findWithin(tsbt, ctx.getAlgo().roundSec);
		if (tv == null)
		{
			ctx.getAlgo().warning(funcName + "(" + name + ") tsid=" + inputTsid.getUniqueString()
				+ " no value to screen at time " + ctx.getAlgo().debugSdf.format(tsbt));
		}
		else
		{
			try
			{
				double value = tv.getDoubleValue();
				ctx.initForScreening(name, screening);
				
				ScreeningCriteria crit = screening.findForDate(tsbt);
				if (crit == null)
				{
					ctx.getAlgo().debug1(funcName + "(" + name + ") tsid=" + inputTsid.getUniqueString() 
						+ " no criteria for sample at time " + ctx.getAlgo().debugSdf.format(tsbt));
					retFlags = CwmsFlags.SCREENED | CwmsFlags.VALIDITY_OKAY;
				}
				else
				{
					retFlags = crit.doChecks(ctx.getAlgo().getDataCollection(), 
						inputParm.timeSeries, tsbt, ctx.getAlgo(), value);
					ctx.getAlgo().debug1(funcName + " result flags for '" + name + "'=0x" 
						+ Integer.toHexString(retFlags));
				}
			}
			catch(NoConversionException ex)
			{
				ctx.getAlgo().warning(funcName + "(" + name + ") tsid="
					+ inputTsid.getUniqueString()
					+ " value at time " + ctx.getAlgo().debugSdf.format(tsbt)
					+ " is not a number '" + tv.getStringValue() + "'");
			}
		}
		inStack.push(new Double(retFlags));
	}
}
