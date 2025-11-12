package org.opendcs.algorithms;

public final class AlgorithmUtilties
{
    private AlgorithmUtilties()
    {
        // static helpers only
    }
    /**
	 * Returns true if this variable is flagged as either missing or deleted.
	 * The algorithm should not then use it's value in an equation.
	 * @return true if this variable is flagged as either missing or deleted.
	 */
	public static boolean isMissing(double var)
	{
		return var == Double.MIN_VALUE || var == Double.NEGATIVE_INFINITY;
	}

	/**
	 * Returns true if this variable is flagged as either missing or deleted.
	 * The algorithm should not then use it's value in an equation.
	 * @return true if this variable is flagged as either missing or deleted.
	 */
	public static boolean isMissing(long var)
	{
		return var == Long.MIN_VALUE;
	}

	/**
	 * Returns true if this variable is flagged as either missing or deleted.
	 * The algorithm should not then use it's value in an equation.
	 * @return true if this variable is flagged as either missing or deleted.
	 */
	public static boolean isMissing(String var)
	{
		return var == null;
	}
}
