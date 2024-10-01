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
*  Revision 1.3  2004/08/30 14:50:40  mjmaloney
*  Javadocs
*
*  Revision 1.2  2000/12/31 15:56:56  mike
*  dev
*
*  Revision 1.1  2000/12/21 21:30:11  mike
*  Created
*
*
*/
package ilex.xml;

import org.xml.sax.SAXException;

/**
* This interface is a companion to TaggedStringSetter.
* It is used for setting simple string values within an object from XML
* elements.
*/
public abstract interface TaggedStringOwner
{
	/**
	* Classes implementing this interface provide a function to store the
	* parsed string. If the string does not have an acceptable value, the
	* method may throw a SAXException.
	* @param tag the tag assigned to this element
	* @param value the value parsed from the data
	* @throws SAXException
	*/
	void set( int tag, String value ) throws SAXException;
}

