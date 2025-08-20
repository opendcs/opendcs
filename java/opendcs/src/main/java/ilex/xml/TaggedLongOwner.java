/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:40  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/12/21 21:30:11  mike
*  Created
*
*
*/
package ilex.xml;

/**
* This interface is a companion to TaggedLongSetter.
* It is used for setting simple long or integer values within an object from XML
* elements.
*/
public abstract interface TaggedLongOwner
{
	/**
	* Constructor.
	* @param tag the tag assigned to this element
	* @param value the value parsed from the data
	*/
	void set( int tag, long value );
}

