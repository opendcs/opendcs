/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.8  2004/08/25 19:31:12  mjmaloney
*  Added javadocs & deprecated unused code.
*
*  Revision 1.7  2003/12/07 20:36:49  mjmaloney
*  First working implementation of EDL time stamping.
*
*  Revision 1.6  2001/09/27 18:18:55  mike
*  Finished rounding rules & eu conversions.
*
*  Revision 1.5  2001/09/27 00:57:23  mike
*  Work on presentation elements.
*
*  Revision 1.4  2001/08/12 17:36:54  mike
*  Slight architecture change for unit converters. The UnitConverterDb objects
*  are now full-fledged DatabaseObjects and not derived from UnitConverter.
*  This necessitated changes to DB parsing code and prepareForExec code.
*
*  Revision 1.3  2001/06/30 13:37:21  mike
*  dev
*
*  Revision 1.2  2001/03/23 20:22:53  mike
*  Collection classes are no longer static monostate. Access them through
*  the current database (Database.getDb().collectionName)
*
*  Revision 1.1  2001/01/13 14:59:33  mike
*  Implemented EU Conversions
*
*/
package decodes.db;

import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;

import ilex.util.Logger;

import decodes.util.DecodesException;

/**
Composit engineering unit converter.
Converts from one EU to another by combining other converters. For example,
to convert from inches to millimeters we might go:
<p>
inches -> feet -> meters -> millimeters
*/
public class CompositeConverter extends UnitConverter
{
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
//		UnitConverter ret = Database.getDb().unitConverterSet.get(from, to);
//		if (ret != null)
//			return ret;

		Logger.instance().log(Logger.E_DEBUG3, 
			"Attempting to build composite converter from " + from + " to "
			+ to);

		resetSearchedFlags();
		Stack callStack = new Stack();
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
		EngineeringUnit to, Stack callStack, Vector solutions)
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
					((UnitConverter)callStack.elementAt(0)).getFrom(),
					to, new Vector(callStack));
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
		for(Iterator it = Database.getDb().engineeringUnitList.iterator();
			it.hasNext(); )
		{
			EngineeringUnit eu = (EngineeringUnit)it.next();
			eu.cnvtSearched = false;
		}
	}
}
