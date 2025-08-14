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
package decodes.db;

import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Stack;


import decodes.util.DecodesException;

/**
Composite engineering unit converter.
Converts from one EU to another by combining other converters. For example,
to convert from inches to millimeters we might go:
<p>
inches -&gt; feet -&gt; meters -&gt; millimeters
*/
public class CompositeConverter extends UnitConverter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private Vector conversions;

	/**
	  Called from UnitConverterSet.findConversion.
	  @param from EU we're converting from.
	  @param to EU we're converting to.
	  @param conversions chain of UnitConverter objects to execute.
	*/
	private CompositeConverter(EngineeringUnit from, EngineeringUnit to,
		Vector conversions)
	{
		super(from, to);
		this.conversions = conversions;
	}

	/**
	  This method will not be called because composits are built explicitly
	  from other conversions.
	  @param coeff ignored
	*/
	public void setCoefficients(double[] coeff)
	{
	}

	/**
	  Perform the conversion on the passed value.
	  @param value the input value
	  @return the converted value
	*/
	/** default constructor */
	public double convert(double value)
		throws DecodesException
	{
		int n = conversions.size();
		for(int i = 0; i<n; i++)
		{
			UnitConverter uc = (UnitConverter)conversions.elementAt(i);
			value = uc.convert(value);
		}
		return value;
	}

	/**
	  Weight of a composit is the sum of the weights of components.
	  @return the weight of this conversion which is the sum of the components.
	*/
	public double getWeight()
	{
		double w = 0;
		int n = conversions.size();
		for(int i = 0; i<n; i++)
		{
			UnitConverter uc = (UnitConverter)conversions.elementAt(i);
			w += uc.getWeight();
		}
		return w;
	}

	/**
	  Attempts to build a composite converter from one EU to another, using
	  the current conversions defined in the singleton UnitConverterSet.
	  @param from EU we're converting from.
	  @param to EU we're converting to.
	  @return new CompositeConverter if successful, null if you can't get
	  there from here.
	*/
	public static synchronized UnitConverter
		build(EngineeringUnit from, EngineeringUnit to)
	{
		/* This method is only called from UnitConverterSet.get(), so
		 * assume that no direct conversion exists. Try to get there step
		 * by step.
		 */


		log.trace("Attempting to build composite converter from {}, to {}.", from, to);

		resetSearchedFlags();
		Stack<UnitConverter> callStack = new Stack<UnitConverter>();
		Vector solutions = new Vector();
		recursiveSearch(from, to, callStack, solutions);
		UnitConverter best = null;
		double bestWeight = Double.MAX_VALUE;
		for(int i = 0; i < solutions.size(); i++)
		{
			UnitConverter uc = (UnitConverter)solutions.elementAt(i);
			double w = uc.getWeight();
			if (w < bestWeight)
			{
				best = uc;
				bestWeight = w;
			}
		}
		return best;
	}

	private static void recursiveSearch(EngineeringUnit from,
		EngineeringUnit to, Stack<UnitConverter> callStack, Vector solutions)
	{
		from.cnvtSearched = true;

		// First look for direct conversion to target.
		for (Iterator it = Database.getDb().unitConverterSet.iteratorExec(); it.hasNext(); )
		{
			UnitConverter uc = (UnitConverter)it.next();
			if (!uc.getFrom().getAbbr().equalsIgnoreCase(from.getAbbr()))
				continue;
			if (uc.getTo().getAbbr().equalsIgnoreCase(to.getAbbr()))
			{
				callStack.push(uc);
				CompositeConverter cc = new CompositeConverter(
					(callStack.elementAt(0)).getFrom(), to, new Vector(callStack));
				solutions.add(cc);
				callStack.pop();
				return;
			}
		}

		// No direct conversion. Do recursive branching.
		for (Iterator it = Database.getDb().unitConverterSet.iteratorExec(); it.hasNext(); )
		{
			UnitConverter uc = (UnitConverter)it.next();

			// Skip if 'from' doesn't match or if I've already searched 'To'.
			if (!uc.getFrom().getAbbr().equalsIgnoreCase(from.getAbbr()) || uc.getTo().cnvtSearched)
				continue;

			callStack.push(uc);
			recursiveSearch(uc.getTo(), to, callStack, solutions);
			callStack.pop();
		}
	}

	private static void resetSearchedFlags()
	{
		for(Iterator<EngineeringUnit> it = Database.getDb().engineeringUnitList.iterator();
			it.hasNext(); )
		{
			EngineeringUnit eu = it.next();
			eu.cnvtSearched = false;
		}
   		for (Iterator<UnitConverter> it = Database.getDb().unitConverterSet.iteratorExec();
   			it.hasNext(); )
   		{
   			UnitConverter uc = it.next();
            uc.getFrom().cnvtSearched = false;
            uc.getTo().cnvtSearched = false;
   		}
	}
}
