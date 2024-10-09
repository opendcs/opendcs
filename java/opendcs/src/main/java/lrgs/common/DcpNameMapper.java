/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2009/04/30 17:40:46  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.1  1999/09/14 17:05:34  mike
*  9/14/1999
*
*
*/

package lrgs.common;

/**
  The DcpNameMapper interface is used by SearchCriteria and
  other classes to map DCP names to addresses. On the LRGS
  server side there is a native implementation that uses the
  global LRGS shared memory. On the client side there is a
  pure-java implementation that uses locally defined network
  lists to map names.
*/
public interface DcpNameMapper
{
	/**
	  Return numeric DCP address associated with name.
	  Return -1 if there is no mapping for this name.
	*/
	public DcpAddress dcpNameToAddress(String name);

//	/**
//	  Return the String name for the passed DCP address.
//	  Return null if there is no mapping for this address.
//	*/
//	public String dcpAddressToName(long address);
}