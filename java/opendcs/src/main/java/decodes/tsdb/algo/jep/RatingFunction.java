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
package decodes.tsdb.algo.jep;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import hec.lang.Const;

import java.util.Date;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.rating.CwmsRatingDao;

/**
 * This implements the rating function in the Expression Parser.
 *
 * The function takes two arguments: location and variable name.
 */
public class RatingFunction extends PostfixMathCommand
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String funcName = "rating";
	private JepContext ctx = null;
	private int numParms = 0;

	public RatingFunction(JepContext ctx)
	{
		super();
		this.ctx = ctx;
		this.numberOfParameters = -1;
	}

	@Override
	public boolean checkNumberOfParameters(int np)
	{
		numParms = np;
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

		double valueSet[] = new double[numParms-1];

		switch(numParms)
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

		if (ctx.getTsdb() == null)
		{
			double sum = 0.0;
			for(double v : valueSet)
				sum += v;
			log.warn("TEST-MODE: No database, rating returning sum of all inputs={}", sum);
			inStack.push(Double.valueOf(sum));
			return;
		}

		String what = "reading rating";
		CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)ctx.getTsdb());
		try
		{
			RatingSet ratingSet = crd.getRatingSet(specId);
			what = "performing rating";
			double d = ratingSet.rateOne(valueSet, tsbt.getTime());
			if (d == Const.UNDEFINED_DOUBLE)
			{
				throw new RatingException("input value(s) outside rating bounds.");
			}
			inStack.push(Double.valueOf(d));
		}
		catch (RatingException ex)
		{
			ParseException toThrow = new ParseException("Error " + what + " for '" + specId + "': " + ex);
			toThrow.addSuppressed(ex);
			throw toThrow;
		}
		finally
		{
			crd.close();
		}
	}
}
