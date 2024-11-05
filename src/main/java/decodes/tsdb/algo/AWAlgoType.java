/**
 * 
 */
package decodes.tsdb.algo;

public enum AWAlgoType 
{
	// Code dependency: first chars must be 'T', 'A', 'R'. Do not change.
	TIME_SLICE("Time Slice"),
	AGGREGATING("Aggregating"),
	RUNNING_AGGREGATE("Running Aggregate");
	
	private String displayName = null;
	
	AWAlgoType(String displayName)
	{
		this.displayName = displayName;
	}
	
	public String getDisplayName() { return displayName; }
	
	public static AWAlgoType fromString(String s)
	{
		if (s == null || s.length() == 0)
			return null;
		char c = s.charAt(0);
		if (c == 'T' || c == 't')
			return TIME_SLICE;
		else if (c == 'A' || c == 'a')
			return AGGREGATING;
		else if (c == 'R' || c == 'r')
			return RUNNING_AGGREGATE;
		return TIME_SLICE;
	}
}