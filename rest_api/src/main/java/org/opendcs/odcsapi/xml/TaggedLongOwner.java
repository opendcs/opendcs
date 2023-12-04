/*
*  $Id: TaggedLongOwner.java,v 1.1 2023/05/15 18:36:26 mmaloney Exp $
*
*  $Source: /home/cvs/repo/odcsapi/src/main/java/org/opendcs/odcsapi/xml/TaggedLongOwner.java,v $
*
*  $State: Exp $
*
*  $Log: TaggedLongOwner.java,v $
*  Revision 1.1  2023/05/15 18:36:26  mmaloney
*  Added this odcsapi.xml package for parsing DDS message blocks and LRGS status.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
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
package org.opendcs.odcsapi.xml;

/**
* This interface is a companion to TaggedLongSetter.
* It is used for setting simple long or integer values within an object from XML
* elements.
*/
public interface TaggedLongOwner
{
	/**
	* Constructor.
	* @param tag the tag assigned to this element
	* @param value the value parsed from the data
	*/
	void set( int tag, long value );
}

