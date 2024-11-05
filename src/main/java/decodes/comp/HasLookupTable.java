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
package decodes.comp;

import java.util.Date;

/**
This interface defines the public interface for accessing lookup tables.
It is used by the RDB and simple-ascii-table reader classes.
*/
public interface HasLookupTable
{
	/**
	 * Sets a property.
	 */
	public void setProperty(String name, String value);

	/**
	 * Adds an independent/dependent point pair to the lookup table.
	 * @param indep the independent value
	 * @param dep the dependent value
	 */
	public void addPoint(double indep, double dep);

	/**
	 * Adds an independent/shift point pair to the lookup table.
	 * @param indep the independent value
	 * @param shift the shift value
	 */
	public void addShift(double indep, double shift);

	/**
	* Sets the X offset used for logarithmic interpolation.
	* @param xo the X offset
	*/
	public void setXOffset(double xo);

	/**
	* Sets the begin time for this rating.
	* @param bt the begin time
	*/
	public void setBeginTime( Date bt );

	/**
	* Sets the end time for this rating.
	* @param et the end time
	*/
	public void setEndTime( Date et );

	/** Clears the table. */
	public void clearTable();
}
