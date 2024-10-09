/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2007/01/30 21:32:29  mmaloney
*  Added SimpleCounter.
*
*  Revision 1.2  2004/08/30 14:50:25  mjmaloney
*  Javadocs
*
*  Revision 1.1  2001/06/13 02:01:41  mike
*  dev
*
*
*/
package ilex.util;

/**
* Interface for implementing persistent counters used for Unique IDs.
*/
public interface Counter
{
	/**
	* @return the next integer value and increments the counter.
	*/
	public int getNextValue( );

	/**
	* Sets the counter so that the next call to getNextValue() will return
	* the passed integer.
	* @param value next value to return.
	*/
	public void setNextValue( int value );
}
