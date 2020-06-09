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
*  Revision 1.2  2004/08/30 14:50:39  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/12/21 21:30:11  mike
*  Created
*
*
*/
package ilex.xml;

/**
* This interface is a companion to TaggedDoubleSetter.
* It is used for setting simple double or float values within an object from XML
* elements.
*/
public abstract interface TaggedDoubleOwner
{
	/**
	* Called from Tagged---Setter.
	* @param tag the integer tag.
	* @param value the value.
	*/
	void set( int tag, double value );
}

