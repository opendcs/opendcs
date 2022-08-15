package opendcs.opentsdb.hydrojson.beans;

import decodes.tsdb.compedit.ComputationInList;

public class ComputationRef
{
	Long computationId = null;
	String name = null;
	Long algorithmId = null;
	String algorithmName = null;
	Long processId = null;
	String processName = null;
	boolean enabled = false;
	String description = null;
	
	public ComputationRef() {}
	
	public ComputationRef(ComputationInList cil)
	{
		this.computationId = cil.getComputationId().getValue();
		this.name = cil.getComputationName();
		this.algorithmId = cil.getAlgorithmId().getValue();
		this.algorithmName = cil.getAlgorithmName();
		this.processId = cil.getProcessId().getValue();
		this.processName = cil.getProcessName();
		this.enabled = cil.isEnabled();
		this.description = cil.getDescription();
	}
	public Long getComputationId()
	{
		return computationId;
	}
	public void setComputationId(Long computationId)
	{
		this.computationId = computationId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public Long getAlgorithmId()
	{
		return algorithmId;
	}
	public void setAlgorithmId(Long algorithmId)
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
	public Long getProcessId()
	{
		return processId;
	}
	public void setProcessId(Long processId)
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
