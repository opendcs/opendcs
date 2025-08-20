/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/09/02 13:09:04  mjmaloney
*  javadoc
*
*  Revision 1.1  2003/04/09 19:38:03  mjmaloney
*  impl
*
*/
package lrgs.drgs;

import java.util.Date;

/**
  Data structure to hold an event, as defined in the DRGS (DAMS-NT) interface.
*/
public class DrgsEvent
{
	/** priority of the event */
	public int priority;
	/** date/time stamp of the event */
	public Date timestamp;
	/** text of the event */
	public String text;

	/** 
	  Constructor.
	  @param prio priority of the event
	  @param ts date/time stamp of the event
	  @param tx text of the event
	*/
	DrgsEvent(int prio, Date ts, String tx)
	{
		priority = prio;
		timestamp = ts;
		text = tx;
	}
}
