package opendcs.opentsdb;

/**
 * Determines what do do when writing time series data and an offset
 * error is detected. The choices are:
 * <ul>
 *   <li>ignore - Ignore the error and write data as-is.</li>
 *   <li>reject - Reject the data. Do not write to database.</li>
 *   <li>round - Round the data to the nearest acceptable time.</li>
 * </ul>
 *   
 * @author mmaloney Mike Maloney, Cove Software, LLC
 *
 */
public enum OffsetErrorAction
{
	IGNORE, 
	REJECT, 
	ROUND
}
