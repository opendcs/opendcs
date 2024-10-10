/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.2  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */
package decodes.tsdb.compedit;

import decodes.db.Constants;
import decodes.sql.DbKey;


/**
 * This class holds the info for an algorithm in the on-screen list.
 */
public class AlgorithmInList
{
	private DbKey algorithmId = Constants.undefinedId;
	private String algorithmName = "";
	private String execClass = "";
	private int numCompsUsing = 0;
	private String description = "";
	
	public AlgorithmInList(DbKey algorithmId, String algorithmName,
		String execClass, int numCompsUsing, String description)
	{
		this.algorithmId = algorithmId;
		this.algorithmName = algorithmName;
		this.execClass = execClass;
		this.numCompsUsing = numCompsUsing;
		this.description = description;
	}

	public DbKey getAlgorithmId()
	{
		return algorithmId;
	}
	public void setAlgorithmId(DbKey algorithmId)
	{
		this.algorithmId = algorithmId;
	}
	public String getAlgorithmName()
	{
		return algorithmName;
	}
	public void setAlgorithmName(String algorithmName)
	{
		this.algorithmName = algorithmName;
	}
	public String getExecClass()
	{
		return execClass;
	}
	public void setExecClass(String execClass)
	{
		this.execClass = execClass;
	}
	public int getNumCompsUsing()
	{
		return numCompsUsing;
	}
	public void setNumCompsUsing(int numCompsUsing)
	{
		this.numCompsUsing = numCompsUsing;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	
}
