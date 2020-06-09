/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:51:49  mjmaloney
*  Javadocs
*
*  Revision 1.2  2000/01/19 14:34:52  mike
*  Debug messages to detect garbage collection.
*
*  Revision 1.1  2000/01/07 19:10:00  mike
*  Generalizing client interface
*
*
*/
package lrgs.ldds;

/**
  ProtocolError is used by the various LRGS Servers and the corresponding
  client interfaces.  
  It either means that the message received could not be parsed (bad
  format) or that it was not the expected message type. 
*/
public class ProtocolError extends Exception
{
	/**
	  Constructor.
	  @param msg the message
	*/
	public ProtocolError(String msg)
	{
		super(msg);
	}

	/** @return string representation of this exception. */
	public String toString()
	{
		return "Protocol Error: " + super.toString();
	}

}

