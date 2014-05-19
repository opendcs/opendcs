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
}