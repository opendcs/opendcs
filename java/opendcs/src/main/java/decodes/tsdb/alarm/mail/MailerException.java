/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/05/17 20:37:12  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.mail;

@SuppressWarnings("serial")
public class MailerException extends Exception
{
	public MailerException(String message)
	{
		super(message);
	}
}
