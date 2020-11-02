/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/31 21:08:36  mjmaloney
*  javadoc
*
*  Revision 1.2  2000/06/06 18:45:41  mike
*  Have MessageBrowser use DcpMsgOutputThread mechanism for screen display.
*  This fixes the problem whereby the GUI was hanging while waiting for a
*  response to a message request. It will also facilitate the future implementation
*  of an abort button.
*
*  Revision 1.1  2000/04/28 18:56:49  mike
*  Fixed LRGS3.0.0 DRs 21, and 22. Added feature to Message Browser to
*  save messages to a local file.
*
*/
package lrgs.gui;

import lrgs.common.DcpMsg;

/**
Objects that create and use DcpMsgOutputThread objects must implement this
interface. The output thread will use these methods to report status back
to the parent, and to determine if it's OK to continue.
<p>
The MessageBrowser implements this class in order to display messages
in its JTextArea on the screen.
@see DcpMsgOutputThread
*/
public interface DcpMsgOutputMonitor
{
	/**
	  Called after each message has been saved.
	  This is typically used by the monitor to display status.
	  @param msg the DcpMsg just received.
	*/
	public void dcpMsgOutputStatus(DcpMsg msg);
	
	/**
	 * Called from output thread after each message has been
	 * saved to determine if user has pressed the PAUSE button.
	
	  @return true if output should pause, false if it should continue
	 */
	public boolean dcpMsgOutputIsPaused();
	
	/**
	  Called if the output thread encounters an error, and cannot continue.
	  @param msg Explanation for display in dialog to user or log file.
	*/
	public void dcpMsgOutputError(String msg);

	/**
	  Called if until time is reached, meaning that all messages
	  have been successfully retrieved and written to the output
	  stream.
	*/
	public void dcpMsgOutputDone();

	/**
	  Called if timeout waiting for a message from the server interface.
	*/
	public void dcpMsgTimeout();
	
}
