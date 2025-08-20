/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:50:21  mjmaloney
*  Javadocs
*
*  Revision 1.2  2003/03/27 21:17:55  mjmaloney
*  drgs dev
*
*  Revision 1.1  1999/11/18 17:05:29  mike
*  Initial implementation.
*
*/
package ilex.jni;

/**
Abstract class for implementing a UNIX signal handler in Java.
@see SignalTrapper
*/
public abstract interface SignalHandler
{
	int SIGHUP = 1;
	int SIGINT = 2;
	int SIGUSR1 = 10;
	int SIGUSR2 = 12;
	int SIGTERM = 15;

	/**
	* Called when the passed signal was received.
	* @param sig the UNIX signal number.
	*/
	void handleSignal( int sig );
}

