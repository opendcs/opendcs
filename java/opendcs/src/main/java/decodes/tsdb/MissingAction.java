package decodes.tsdb;

/**
 * The MissingAction determines how the comp processor treats input values
 * that are missing at a given time-slice.
 */
public enum MissingAction 
{
	/** (default) FAIL means do not execute comp if this input is missin. */
	FAIL,
	/** IGNORE means attempt to execute anyway - algorithm deals with it. */
	IGNORE,
	/** PREV means retrieve last value before the time-slice */
	PREV,
	/** NEXT means retrieve next value after the time-slice */
	NEXT,
	/** INTERP means linear interpolation between PREV and NEXT */
	INTERP,
	/** CLOSEST means retrieve either PREV or NEXT, whichever is closest in time */
	CLOSEST;
	
	/** Converts a string to a MissingAction, non-case sensitive */
	public static MissingAction fromString(String s)
	{
		if (s == null)
			return FAIL;
		s = s.trim();
		if (s.length() == 0)
			return FAIL;
		for(MissingAction ma : values())
			if (ma.toString().equalsIgnoreCase(s))
				return ma;
		return FAIL;
	}
}