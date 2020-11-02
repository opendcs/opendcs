/*
 * $Id$
 */
package lrgs.common;

import lrgs.common.LrgsErrorCode;

/**
Thrown when a search could not be performed because of some kind of syntax
error in the request, the search criteria, or the network lists.
*/
public class SearchSyntaxException extends ArchiveException
{
	/**
	 * Constructor.
	 * @param msg the message
	 */
	public SearchSyntaxException(String msg, int errorCode)
	{
		super(msg, errorCode, false);
	}
}
