/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:30  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/04/09 19:34:52  mjmaloney
*  Created.
*
*/
package ilex.util;

/**
* This interface works along with ServerLock to provide a more controlled
* way of exiting rather than just calling System.exit().
* If one of these objects is provided to ServerLock, it will call the
* lockFileRemoved() method rather than calling System.exit(1).
*/
public abstract interface ServerLockable
{
	/** Override this method to get notification when lock file was removed. */
	void lockFileRemoved( );
}
