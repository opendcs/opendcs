/**
 * $Id$
 * 
 * $Log$
 */
package opendcs.opentsdb;

public class StorageTableSpec
{
	private int tableNum = -1;         // Suffix for table name
	private int storageType = 'N';     // 'N'umeric or 'S'tring
	private int numTsPresent = 0;      // Number of TSIDs currently assigned to table
	private int estAnnualValues = 0;   // Estimated annual values for all TS in this table

	public StorageTableSpec(char type)
	{
		storageType = type;
	}

	public int getTableNum()
	{
		return tableNum;
	}

	public void setTableNum(int tableNum)
	{
		this.tableNum = tableNum;
	}

	public int getStorageType()
	{
		return storageType;
	}

	public int getNumTsPresent()
	{
		return numTsPresent;
	}

	public void setNumTsPresent(int numTsPresent)
	{
		this.numTsPresent = numTsPresent;
	}

	public int getEstAnnualValues()
	{
		return estAnnualValues;
	}

	public void setEstAnnualValues(int estAnnualValues)
	{
		this.estAnnualValues = estAnnualValues;
	}

}
