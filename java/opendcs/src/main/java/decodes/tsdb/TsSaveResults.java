package decodes.tsdb;

/**
 * This bean-class is returned by the TSDB saveTimeSeries method.
 * It contains the results of the operation.
 * @author mmaloney Mike Maloney, Cove Software LLC
 *
 */
public class TsSaveResults
{
	private int numSaved = 0;
	private int numSaveErrors = 0;
	private int numDeleted = 0;
	private int numDeleteErrors = 0;
	
	public TsSaveResults()
	{
		
	}
	
	public TsSaveResults(int numSaved, int numSaveErrors, int numDeleted, int numDeleteErrors)
	{
		this.numSaved = numSaved;
		this.numSaveErrors = numSaveErrors;
		this.numDeleted = numDeleted;
		this.numDeleteErrors = numDeleteErrors;
	}

	/**
	 * @return number of time-series values successfully saved to the database.
	 */
	public int getNumSaved()
	{
		return numSaved;
	}

	/**
	 * @return Number of errors encountered attempting to save data.
	 */
	public int getNumSaveErrors()
	{
		return numSaveErrors;
	}

	/**
	 * @return Number of time-series values successfully deleted from the database.
	 */
	public int getNumDeleted()
	{
		return numDeleted;
	}

	/**
	 * @return Number of errors encountered attempting to delete data.
	 */
	public int getNumDeleteErrors()
	{
		return numDeleteErrors;
	}
	
	/**
	 * Add the passed results to this. Used for aggregating several save-calls.
	 * @param rhs the results to add to this one.
	 */
	public void add(TsSaveResults rhs)
	{
		numSaved += rhs.numSaved;
		numSaveErrors += rhs.numSaveErrors;
		numDeleted += rhs.numDeleted;
		numDeleteErrors += rhs.numDeleteErrors;
	}
}
