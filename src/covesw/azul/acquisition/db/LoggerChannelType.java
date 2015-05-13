/**
 * Copyright 2015 Cove Software, LLC
 * All Rights Reserved.
 */
package covesw.azul.acquisition.db;

public enum LoggerChannelType
{
	GOES('G'),
	Modem('M'),
	TCP('T');
	
	private char channelTypeCode;
	
	private LoggerChannelType(char code)
	{
		this.channelTypeCode = code;
	}

	/**
	 * @return the channelTypeCode
	 */
	public char getChannelTypeCode()
	{
		return channelTypeCode;
	}

}
