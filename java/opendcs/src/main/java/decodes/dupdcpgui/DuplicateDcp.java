package decodes.dupdcpgui;

import lrgs.common.DcpAddress;

/**
 * This class represents a row of data in the DuplicateDcps GUI.
 *
 */
public class DuplicateDcp
{
	/** Corps DCP Name */
	private String dcpName;
	private DcpAddress dcpAddress;//Dcp Address
	private String description;//Corps Description
	private String duplicatedIn;//Shows all Districts were is duplicated
	private String controllingDist;//control office
	
	private String nwshb5Code;
	private String pdtOwner;//pdt agency
	private String pdtDescription;
	private String nwsDescription;
	private boolean duplicatedDcp;//indicates if it is a duplicate
	
	/** Constructor. Initialize all variables */
	public DuplicateDcp()
	{
		dcpName = null;
		dcpAddress = null;
		description = null;
		duplicatedIn = null;
		controllingDist = null;
		
		nwshb5Code = null;
		pdtOwner = null;//pdt agency
		pdtDescription = null;
		nwsDescription = null;
		duplicatedDcp = true;
	}
	
	/** Constructor. Initialize all variables with the given args */
	public DuplicateDcp(String dcpName, DcpAddress dcpAddress,
			String description, String duplicatedIn, 
			String controllingDist,	String nwshb5Code, 
			String pdtOwner, String pdtDescription, 
			String nwsDescription, boolean duplicatedDcp)
	{
		this.dcpName = dcpName;
		this.dcpAddress = dcpAddress;
		this.description = description;
		this.duplicatedIn = duplicatedIn; 
		this.controllingDist = controllingDist;
		
		this.nwshb5Code = nwshb5Code;
		this.pdtOwner = pdtOwner;//pdt agency
		this.pdtDescription = pdtDescription;
		this.nwsDescription = nwsDescription;
		this.duplicatedDcp = duplicatedDcp;
	}

	public boolean isDuplicatedDcp()
	{
		return duplicatedDcp;
	}

	public void setDuplicatedDcp(boolean duplicatedDcp)
	{
		this.duplicatedDcp = duplicatedDcp;
	}

	public String getNwsDescription()
	{
		return nwsDescription;
	}

	public void setNwsDescription(String nwsDescription)
	{
		this.nwsDescription = nwsDescription;
	}

	public String getNwshb5Code()
	{
		return nwshb5Code;
	}

	public void setNwshb5Code(String nwshb5Code)
	{
		this.nwshb5Code = nwshb5Code;
	}

	public String getPdtDescription()
	{
		return pdtDescription;
	}

	public void setPdtDescription(String pdtDescription)
	{
		this.pdtDescription = pdtDescription;
	}

	public String getPdtOwner()
	{
		return pdtOwner;
	}

	public void setPdtOwner(String pdtOwner)
	{
		this.pdtOwner = pdtOwner;
	}

	public String getDuplicatedIn()
	{
		return duplicatedIn;
	}

	public void setDuplicatedIn(String duplicatedIn)
	{
		this.duplicatedIn = duplicatedIn;
	}

	public String getControllingDist()
	{
		return controllingDist;
	}

	public void setControllingDist(String controllingDist)
	{
		this.controllingDist = controllingDist;
	}

	public String getDcpName()
	{
		return dcpName;
	}

	public void setDcpName(String dcpName)
	{
		this.dcpName = dcpName;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public DcpAddress getDcpAddress()
	{
		return dcpAddress;
	}

	public void setDcpAddress(DcpAddress dcpAddress)
	{
		this.dcpAddress = dcpAddress;
	}
	
	
}
