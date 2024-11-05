package decodes.dcpmon;

import java.util.Date;

import decodes.sql.DbKey;

/**
 * This class is used to read a subset of the DCP_TRANS_NN tables
 * for populating the Oriole displays.
 */
public class XmitRecSpec
{
	private long recordId = -1;
	private char mediumType = 0;
	private String mediumId = null;
	private Date xmitTime = null;
	private String failureCodes = "";
	private int goesChannel = 0;
	
	public XmitRecSpec(long recordId)
	{
		this.recordId = recordId;
	}

	public char getMediumType()
	{
		return mediumType;
	}

	public void setMediumType(char mediumType)
	{
		this.mediumType = mediumType;
	}

	public String getMediumId()
	{
		return mediumId;
	}

	public void setMediumId(String mediumId)
	{
		this.mediumId = mediumId;
	}

	public Date getXmitTime()
	{
		return xmitTime;
	}

	public void setXmitTime(Date xmitTime)
	{
		this.xmitTime = xmitTime;
	}

	public String getFailureCodes()
	{
		return failureCodes;
	}

	public void setFailureCodes(String failureCodes)
	{
		this.failureCodes = failureCodes;
	}

	public long getRecordId()
	{
		return recordId;
	}

	public int getGoesChannel()
	{
		return goesChannel;
	}

	public void setGoesChannel(int goesChannel)
	{
		this.goesChannel = goesChannel;
	}
	
}
