/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 15:44:00  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.3  2004/08/30 14:50:29  mjmaloney
*  Javadocs
*
*  Revision 1.2  2002/07/07 14:22:45  chris
*  Cosmetic changes only.
*
*  Revision 1.1  2001/07/25 13:15:27  mike
*  dev
*
*/
package ilex.util;

import java.util.Date;

/**
* Subclass of Logger that throws log messages away.
*/
public class NullLogger extends Logger
{
  /**
	* Constructor.  The procName argument is not used.
	* @param procName ignored
	*/
	public NullLogger( String procName )
	{
		super(procName);
	}

  /**
	* This class' implementation of the close() method.  This does nothing
	*/
	public void close( ) {}

  /**
	* This class' implementation of the doLog() method.  This does nothing.
	* @param priority ignored
	* @param text ignored
	*/
	protected void doLog( int priority, String text )
	{
	}
}

