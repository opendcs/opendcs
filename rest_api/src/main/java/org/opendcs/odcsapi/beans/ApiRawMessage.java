package org.opendcs.odcsapi.beans;

import java.util.Date;

/** Used to encapsulate a raw message returned by GET message or sent to POST decode */
public class ApiRawMessage
{
	// Attributes - always present:
	private long flags = 0L;
	private String platformId = null;
	
	// GOES fields:
	private Integer sequenceNum = null;
	private Date localRecvTime = null;
	private Date carrierStart = null;
	private Date carrierStop = null;
	private Integer baud = null;
	private Double goodPhasePct = null;
	private Double freqOffset = null;
	private Double signalStrength= null;
	private Double phaseNoise = null;	
	private Date xmitTime = null;

	// Iridium fields:
	private Integer momsn = null;
	private Integer mtmsn = null;
	private Long cdrReference = null;
	private Integer sessionStatus = null;

	// Base64 encoded binary message to preserve original whitespace
	private String base64 = null;

	public String getBase64()
	{
		return base64;
	}

	public void setBase64(String base64)
	{
		this.base64 = base64;
	}

	public long getFlags()
	{
		return flags;
	}

	public void setFlags(long flags)
	{
		this.flags = flags;
	}

	public String getPlatformId()
	{
		return platformId;
	}

	public void setPlatformId(String platformId)
	{
		this.platformId = platformId;
	}

	public Integer getSequenceNum()
	{
		return sequenceNum;
	}

	public void setSequenceNum(Integer sequenceNum)
	{
		this.sequenceNum = sequenceNum;
	}

	public Date getLocalRecvTime()
	{
		return localRecvTime;
	}

	public void setLocalRecvTime(Date localRecvTime)
	{
		this.localRecvTime = localRecvTime;
	}

	public Date getCarrierStart()
	{
		return carrierStart;
	}

	public void setCarrierStart(Date carrierStart)
	{
		this.carrierStart = carrierStart;
	}

	public Date getCarrierStop()
	{
		return carrierStop;
	}

	public void setCarrierStop(Date carrierStop)
	{
		this.carrierStop = carrierStop;
	}

	public Integer getBaud()
	{
		return baud;
	}

	public void setBaud(Integer baud)
	{
		this.baud = baud;
	}

	public Double getGoodPhasePct()
	{
		return goodPhasePct;
	}

	public void setGoodPhasePct(Double goodPhasePct)
	{
		this.goodPhasePct = goodPhasePct;
	}

	public Double getFreqOffset()
	{
		return freqOffset;
	}

	public void setFreqOffset(Double freqOffset)
	{
		this.freqOffset = freqOffset;
	}

	public Double getSignalStrength()
	{
		return signalStrength;
	}

	public void setSignalStrength(Double signalStrength)
	{
		this.signalStrength = signalStrength;
	}

	public Double getPhaseNoise()
	{
		return phaseNoise;
	}

	public void setPhaseNoise(Double phaseNoise)
	{
		this.phaseNoise = phaseNoise;
	}

	public Date getXmitTime()
	{
		return xmitTime;
	}

	public void setXmitTime(Date xmitTime)
	{
		this.xmitTime = xmitTime;
	}

	public Integer getMomsn()
	{
		return momsn;
	}

	public void setMomsn(Integer momsn)
	{
		this.momsn = momsn;
	}

	public Integer getMtmsn()
	{
		return mtmsn;
	}

	public void setMtmsn(Integer mtmsn)
	{
		this.mtmsn = mtmsn;
	}

	public Long getCdrReference()
	{
		return cdrReference;
	}

	public void setCdrReference(Long cdrReference)
	{
		this.cdrReference = cdrReference;
	}

	public Integer getSessionStatus()
	{
		return sessionStatus;
	}

	public void setSessionStatus(Integer sessionStatus)
	{
		this.sessionStatus = sessionStatus;
	}

}
