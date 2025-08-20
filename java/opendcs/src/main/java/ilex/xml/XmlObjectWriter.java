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
*  Revision 1.4  2004/08/30 14:50:41  mjmaloney
*  Javadocs
*
*  Revision 1.3  2001/01/03 02:55:05  mike
*  dev
*
*  Revision 1.2  2000/12/24 02:41:10  mike
*  dev
*
*  Revision 1.1  2000/12/23 21:33:16  mike
*  Created XmlObjectWriter and XmlOutputStream
*
*
*/
package ilex.xml;

import java.io.IOException;
import ilex.xml.XmlOutputStream;

/**
* Interface for objects that can write to an XmlOutputStream.
*/
public abstract interface XmlObjectWriter
{
	/**
	* Writes this element to the xml output stream
	* @param xos the Xml Output Stream
	* @throws IOException on IO Error
	*/
	void writeXml( XmlOutputStream xos ) throws IOException;

	/**
	* @return my element name
	*/
	String myName( );
}

