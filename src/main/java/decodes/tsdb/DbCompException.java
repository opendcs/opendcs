/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb;

/**
Thrown when a computation's apply method fails. Could be due to
meta-data inconsistencies or things like divide-by-zero.
*/
public class DbCompException extends TsdbException
{
	/**
	 * Constructor.
	 * @param msg explanatory message.
	 */
	public DbCompException(String msg)
	{
		super(msg);
	}

	/**
	 * Constructor with cause.
	 * @param msg explanatory message
	 * @param cause upstream exception with additional details.
	 */
	public DbCompException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
