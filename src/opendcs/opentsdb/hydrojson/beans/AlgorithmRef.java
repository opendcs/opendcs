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
package opendcs.opentsdb.hydrojson.beans;

import decodes.sql.DbKey;
import decodes.tsdb.compedit.AlgorithmInList;


/**
 * This class holds the info for an algorithm in the on-screen list.
 */
public class AlgorithmRef
{
	private long algorithmId = DbKey.NullKey.getValue();
	private String algorithmName = "";
	private String execClass = "";
	private int numCompsUsing = 0;
	private String description = "";
	
	public AlgorithmRef()
	{
		
	}
	
	/**
	 * Convert from DECODES object so we can use DECODES DAO.
	 * @param ail
	 */
	public AlgorithmRef(AlgorithmInList ail)
	{
		setAlgorithmId(ail.getAlgorithmId().getValue());
		setAlgorithmName(ail.getAlgorithmName());
		setExecClass(ail.getExecClass());
		setNumCompsUsing(ail.getNumCompsUsing());
		setDescription(ail.getDescription());
	}
	
	/**
	 * Convert from bean to DECODES object so we can use DECODES DAO.
	 * @return
	 */
	public AlgorithmInList toDecodes()
	{
		return new AlgorithmInList(
			DbKey.createDbKey(this.algorithmId),
			this.algorithmName, this.execClass, this.numCompsUsing, this.description);
	}
	
	
	public long getAlgorithmId()
	{
		return algorithmId;
	}
	public void setAlgorithmId(long algorithmId)
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
