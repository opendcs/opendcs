/*
*  $Id: TaggedDoubleOwner.java,v 1.1 2023/05/15 18:36:26 mmaloney Exp $
*
*  $Source: /home/cvs/repo/odcsapi/src/main/java/org/opendcs/odcsapi/xml/TaggedDoubleOwner.java,v $
*
*  $State: Exp $
*
*  $Log: TaggedDoubleOwner.java,v $
*  Revision 1.1  2023/05/15 18:36:26  mmaloney
*  Added this odcsapi.xml package for parsing DDS message blocks and LRGS status.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
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
package org.opendcs.odcsapi.xml;

/**
* This interface is a companion to TaggedDoubleSetter.
* It is used for setting simple double or float values within an object from XML
* elements.
*/
public interface TaggedDoubleOwner
{
	/**
	* Called from Tagged---Setter.
	* @param tag the integer tag.
	* @param value the value.
	*/
	void set( int tag, double value );
}

