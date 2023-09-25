/*
*  $Id: TaggedBooleanOwner.java,v 1.1 2023/05/15 18:36:26 mmaloney Exp $
*
*  $Source: /home/cvs/repo/odcsapi/src/main/java/org/opendcs/odcsapi/xml/TaggedBooleanOwner.java,v $
*
*  $State: Exp $
*
*  $Log: TaggedBooleanOwner.java,v $
*  Revision 1.1  2023/05/15 18:36:26  mmaloney
*  Added this odcsapi.xml package for parsing DDS message blocks and LRGS status.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
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
package org.opendcs.odcsapi.xml;

/**
* This interface is a companion to TaggedBooleanSetter.
* It is used for setting simple boolean values within an object from XML
* elements.
*/
public interface TaggedBooleanOwner
{
	/**
	* Called from TaggedBooleanSetter.
	* @param tag the integer tag.
	* @param value the value.
	*/
	void set( int tag, boolean value );
}

