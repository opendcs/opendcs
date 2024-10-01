package decodes.dupdcpgui;

import lrgs.common.DcpAddress;

/**
 * This class stores a Controlling District object.
 */
public class ControllingDistrict
{
	private DcpAddress dcpAddress;
	private String district;
	
	/** Construct the Object */
	public ControllingDistrict(DcpAddress dcpAddress, String district)
	{
		this.dcpAddress = dcpAddress;
		this.district = district;
	}

	/** Return Dcp Address */
	public DcpAddress getDcpAddress()
	{
		return dcpAddress;
	}
	
	/** Set Dcp Address */
	public void setDcpAddress(DcpAddress dcpAddress)
	{
		this.dcpAddress = dcpAddress;
	}

	/** Return District */
	public String getDistrict()
	{
		return district;
	}

	/** Set District */
	public void setDistrict(String district)
	{
		this.district = district;
	}
}
