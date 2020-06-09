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
*  Revision 1.2  2007/02/03 20:47:41  mmaloney
*  Implemented sequence range retrieval & index-storage.
*
*  Revision 1.1  2005/06/23 15:47:18  mjmaloney
*  Java archive search algorithms.
*
*/
package lrgs.ldds;

public interface StatLogger
{
	/**
      Log status for an LddsThread object.
      @param lt the thread object
    */
	public void logStat(LddsThread lt);
	public void incrNumAuth();
	public void incrNumUnAuth();
	public void incrBadPasswords();
	public void incrBadUsernames();
}
