/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

/**
 * Encapsulates an Email Address used in an AlarmGroup
 */
public class EmailAddr
{
	private String addr = null;

	public EmailAddr(String addr)
	{
		super();
		this.addr = addr;
	}

	public String getAddr()
	{
		return addr;
	}
}
