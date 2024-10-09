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
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2005/06/06 21:15:26  mjmaloney
*  Added new Java-Only Archiving Package
*
*/
package lrgs.archive;

import ilex.util.FailureException;

public class InvalidArchiveException extends FailureException
{
	public InvalidArchiveException(String msg)
	{
		super(msg);
	}
}
