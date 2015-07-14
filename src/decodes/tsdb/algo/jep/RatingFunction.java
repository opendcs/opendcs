package decodes.tsdb.algo.jep;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;

import java.util.Date;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.rating.CwmsRatingDao;

/**
 * This implements the lookupMeta(location, varname) function for the JEP parser when run inside
 * the ExpressionParserAlgorithm.
 * The function takes two arguments: location and variable name.
 */
public class RatingFunction
	extends PostfixMathCommand
{
	public static final String funcName = "rating";
	private JepContext ctx = null;

	public RatingFunction(JepContext ctx)
	{
		super();
		this.ctx = ctx;
		this.numberOfParameters = -1;
	}
	
	@Override
	public boolean checkNumberOfParameters(int np)
	{
		// Required arg table name followed by 1...9 indeps
		return np >= 2 && np <= 10;
	}
	
	private double getArgAsDouble(Object o, int pos)
		throws ParseException
	{
		if (o instanceof Number)
			return ((Number) o).doubleValue();
		throw new ParseException("invalid independent parameter in position " + pos + ": " + o);
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

		int np = this.getNumberOfParameters();
		double valueSet[] = new double[np-1];

		switch(np)
		{
		case 10: valueSet[8] = getArgAsDouble(inStack.pop(), 9);
		case  9: valueSet[7] = getArgAsDouble(inStack.pop(), 8);
		case  8: valueSet[6] = getArgAsDouble(inStack.pop(), 7);
		case  7: valueSet[5] = getArgAsDouble(inStack.pop(), 6);
		case  6: valueSet[4] = getArgAsDouble(inStack.pop(), 5);
		case  5: valueSet[3] = getArgAsDouble(inStack.pop(), 4);
		case  4: valueSet[2] = getArgAsDouble(inStack.pop(), 3);
		case  3: valueSet[1] = getArgAsDouble(inStack.pop(), 2);
		case  2: valueSet[0] = getArgAsDouble(inStack.pop(), 1);
		}
		
		String specId = inStack.pop().toString();
		
		String what = "reading rating";
		try
		{
			CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)ctx.getTsdb());
			RatingSet ratingSet = crd.getRatingSet(specId);
			what = "performing rating";
			inStack.push(new Double(ratingSet.rateOne(valueSet, tsbt.getTime())));
		}
		catch (RatingException ex)
		{
			throw new ParseException("Error " + what + " for '" + specId + "': " + ex);
		}
	}
}
