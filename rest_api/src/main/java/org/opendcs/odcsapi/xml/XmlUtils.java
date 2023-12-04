/*
*  $Id: XmlUtils.java,v 1.1 2023/05/15 18:36:26 mmaloney Exp $
*
*  $Log: XmlUtils.java,v $
*  Revision 1.1  2023/05/15 18:36:26  mmaloney
*  Added this odcsapi.xml package for parsing DDS message blocks and LRGS status.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:41  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/04/26 20:02:27  mjmaloney
*  Dev.
*
*/
package org.opendcs.odcsapi.xml;

import org.xml.sax.Attributes;

/**
* This class contains static methods that provide convenince functions for
* writing XML parsers and writers.
*/
public class XmlUtils
{
	/**
	* Returns an attribute value with the given name, without regard
	* to the case of the name-string.
	* @param atts attributes
	* @param name name to search for
	* @return value or null if not found.
	*/
	public static String getAttrIgnoreCase( Attributes atts, String name )
	{
		int len = atts.getLength();
		for(int i=0; i<len; i++)
		{
			String nm = atts.getQName(i);
			if (nm.equalsIgnoreCase(name))
				return atts.getValue(i);
		}
		return null;
	}
}
	
