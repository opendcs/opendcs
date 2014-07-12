/*
 * $Id$
 * 
 * $Log$
 * 
 * Copyright 2014 Cove Software, LLC. All rights reserved.
 */
package covesw.azul.acquisition.db;

import java.util.Date;

/**
 * Stores a raw logger message.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class LoggerMessage
	implements HasDbKey
{
	/** Surrogate key */
	private long key;
	
	/** Time that this message was locally acquired */
	private Date acquisitionTime = null;
	
	/** Time that this message was transmitted by the logger */
	private Date transmitTime = null;
	
	/** Specifies the type of the channel value, type of the medium ID, and format of header */
	private LoggerChannelType loggerChannelType = null;
	
	/** The channel value */
	private int channel = -1;
	
	/** Time that the channel became busy, if known */
	private Date carrierStart = null;
	
	/** Time that the channel became free, if known */
	private Date carrierStop = null;
	
	/** Depending on channel type, this can be DCP address, logger name, or something else */
	private String loggerId = null;
	
	/** Specifies quality of message, early/late, and other conditions */
	private String acquisitionFlags = "";
	
	/** The message data */
	private byte[] messageData = null;
	
	private float batteryVoltage = (float)0.0;
	
	@Override
	public long getKey()
	{
		return key;
	}

	@Override
	public void setKey(long key)
	{
		this.key = key;
	}

	public Date getAcquisitionTime()
	{
		return acquisitionTime;
	}

	public void setAcquisitionTime(Date acquisitionTime)
	{
		this.acquisitionTime = acquisitionTime;
	}

	public Date getTransmitTime()
	{
		return transmitTime;
	}

	public void setTransmitTime(Date transmitTime)
	{
		this.transmitTime = transmitTime;
	}

	public LoggerChannelType getLoggerChannelType()
	{
		return loggerChannelType;
	}

	public void setLoggerChannelType(LoggerChannelType loggerChannelType)
	{
		this.loggerChannelType = loggerChannelType;
	}

	public int getChannel()
	{
		return channel;
	}

	public void setChannel(int channel)
	{
		this.channel = channel;
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

	public String getLoggerId()
	{
		return loggerId;
	}

	public void setLoggerId(String loggerId)
	{
		this.loggerId = loggerId;
	}

	public String getAcquisitionFlags()
	{
		return acquisitionFlags;
	}

	public void setAcquisitionFlags(String acquisitionFlags)
	{
		this.acquisitionFlags = acquisitionFlags;
	}

	public byte[] getMessageData()
	{
		return messageData;
	}

	public void setMessageData(byte[] messageData)
	{
		this.messageData = messageData;
	}

	public float getBatteryVoltage()
	{
		return batteryVoltage;
	}

	public void setBatteryVoltage(float batteryVoltage)
	{
		this.batteryVoltage = batteryVoltage;
	}
	
	

}
