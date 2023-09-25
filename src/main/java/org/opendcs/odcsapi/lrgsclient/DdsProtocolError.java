/*
*  $Id: DdsProtocolError.java,v 1.1 2023/05/15 18:33:56 mmaloney Exp $
*/
package org.opendcs.odcsapi.lrgsclient;

/**
  ProtocolError is used by the various LRGS Servers and the corresponding
  client interfaces.  
  It either means that the message received could not be parsed (bad
  format) or that it was not the expected message type. 
*/
public class DdsProtocolError extends Exception
{
	/**
	  Constructor.
	  @param msg the message
	*/
	public DdsProtocolError(String msg)
	{
		super(msg);
	}

	/** @return string representation of this exception. */
	public String toString()
	{
		return "Protocol Error: " + super.toString();
	}
}

