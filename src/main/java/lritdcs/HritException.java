/**
 * $Id$
 * 
 * Copyright 2019 U.S. Government.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * $Log$
 */
package lritdcs;

import ilex.util.FailureException;

/**
 * Thrown when there is an error processing HRIT data
 */
public class HritException extends FailureException
{
	public enum ErrorCode 
	{
		WRONG_FILE_TYPE,
		INCOMPLETE_FILE,
		BAD_HEADER,
		BAD_FILE_CRC,
		BAD_MSG_CRC
	};
	
	private ErrorCode errorCode;
	
	public ErrorCode getErrorCode()
	{
		return errorCode;
	}

	public HritException(String msg, ErrorCode errorCode)
	{
		super(msg);
		this.errorCode = errorCode;
	}

}
