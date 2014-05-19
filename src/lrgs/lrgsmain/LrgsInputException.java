/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2005/06/09 20:44:34  mjmaloney
*  Working on implementation of Java-Only Archive
*
*/
package lrgs.lrgsmain;

public class LrgsInputException extends Exception
{
	public LrgsInputException(String msg)
	{
		super(msg);
	}
}
