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
*  Revision 1.2  2004/08/30 14:50:38  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/12/31 15:56:56  mike
*  dev
*
*/
package ilex.xml;

/**
* This interface is a companion to TaggedBooleanSetter.
* It is used for setting simple boolean values within an object from XML
* elements.
*/
public abstract interface TaggedBooleanOwner
{
	/**
	* Called from TaggedBooleanSetter.
	* @param tag the integer tag.
	* @param value the value.
	*/
	void set( int tag, boolean value );
}

