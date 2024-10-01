/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/30 15:44:01  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.4  2004/08/30 14:50:31  mjmaloney
*  Javadocs
*
*  Revision 1.3  2002/07/07 14:23:28  chris
*  Change to use the new 'standardMessage' method of the base class.
*
*  Revision 1.2  2001/06/30 20:49:01  mike
*  dev
*
*  Revision 1.1  2001/04/13 19:38:41  mike
*  Added logger classes.
*
*/
package ilex.util;

/**
* Concrete subclass of Logger that Logs messages to standard error
* (i.e. System.err).
*/
public class StderrLogger extends Logger
{
    /**
	* Construct with a process name.
	* If you don't want to use the process name string, pass the
	* empty string.
	* @param procName the process name
	*/
	public StderrLogger( String procName )
	{
		super(procName);
	}

    /**
	* For this implementation of Logger, this does nothing.
	*/
	public void close( ) {}

    /**
	* Logs a message.
	* @param priority the priority
	* @param text the formatted text
	*/
	public void doLog( int priority, String text )
	{
		System.err.println(standardMessage(priority, text));
	}
}

