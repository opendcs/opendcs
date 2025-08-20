/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2004/08/30 15:35:15  mjmaloney
*  Renamed BadIndexException to IndexRangeException because of class with ilex.
*  var class.
*
*/
package ilex.util;

/**
* This exception indicates that an object requested by index did not
* exist, or that the index was invalid.
*/
public class IndexRangeException extends Exception
{
	/**
	* Constructor
	* @param msg the message
	*/
	public IndexRangeException( String msg )
	{
		super(msg);
	}
}
