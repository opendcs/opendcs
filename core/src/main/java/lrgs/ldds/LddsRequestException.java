/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2005/03/07 21:33:52  mjmaloney
*  dev
*
*  Revision 1.2  2004/08/30 14:51:48  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/06/09 18:56:44  mjmaloney
*  Created.
*
*/
package lrgs.ldds;

import lrgs.common.ArchiveException;

/**
  This is thrown internally to indicate that a request failed.
  The hangup flag will be set indicating whether or not the server
  should hangup on the client or not as a result of this error.
*/
public class LddsRequestException extends ArchiveException
{
	/**
	  Constructor.
	  @param msg the explanation.
	  @param errorCode application-specific error code
	  @param hangup true if this exception warrants a hangup on the client
	*/
	public LddsRequestException(String msg, int errorCode, boolean hangup)
	{
		super(msg, errorCode, hangup);
	}

//	/**
//	  @return a backward-compatible string version of the message
//	*/
//	public String toString()
//	{
//		return "?" + errorCode + ",0," + super.getMessage();
//	}

	/** 
	 * Sets the hangup flag. 
	 * @param tf the new flag value
	 */
	public void setHangup(boolean tf) { hangup = tf; }
}
