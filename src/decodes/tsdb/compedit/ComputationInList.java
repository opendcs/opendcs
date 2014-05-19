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

public class ComputationInList
{

	private DbKey computationId = Constants.undefinedId;
	private String computationName = "";
	private DbKey algorithmId = Constants.undefinedId;
	private String algorithmName = "";
	private DbKey processId = Constants.undefinedId;
	private String processName = "";
	private boolean enabled = false;
	private String description = "";
	
	/**
	 * @param computationId
	 * @param computationName
	 * @param algorithmId
	 * @param processId
	 * @param enabled
	 * @param description
	 */
	public ComputationInList(DbKey computationId, String computationName,
		DbKey algorithmId, DbKey processId, boolean enabled, String description)
	{
		super();
		this.computationId = computationId;
		this.computationName = computationName;
		this.algorithmId = algorithmId;
		this.processId = processId;
		this.enabled = enabled;
		this.description = description;
	}

	public DbKey getComputationId()
	{
		return computationId;
	}
	public void setComputationId(DbKey computationId)
	{
		this.computationId = computationId;
	}
	public String getComputationName()
	{
		return computationName;
	}
	public void setComputationName(String computationName)
	{
		this.computationName = computationName;
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
	public DbKey getProcessId()
	{
		return processId;
	}
	public void setProcessId(DbKey processId)
	{
		this.processId = processId;
	}
	public String getProcessName()
	{
		return processName;
	}
	public void setProcessName(String processName)
	{
		this.processName = processName;
	}
	public boolean isEnabled()
	{
		return enabled;
	}
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
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
