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
Throws when a time series could not be processed because of bad
internal linkages.
*/
public class BadTimeSeriesException extends TsdbException
{
	/**
	 * Constructor.
	 * @param msg explanatory message.
	 */
	public BadTimeSeriesException(String msg)
	{
		super(msg);
	}

    public BadTimeSeriesException(String msg, Throwable ex)
	{
		super(msg,ex);
    }
}
