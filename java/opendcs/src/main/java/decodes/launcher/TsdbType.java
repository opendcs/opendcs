/*
 * $Id$
 *
 * $Log$
 * Revision 1.1  2012/05/17 15:14:31  mmaloney
 * Initial implementation for USBR.
 *
 *
 * This is open-source software written by Sutron Corporation under
 * contract to the federal government. Anyone is free to copy and use this
 * source code for any purpos, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * Except for specific contractual terms between Sutron and the federal 
 * government, this source code is provided completely without warranty.
 */
package decodes.launcher;

/**
 * Time Series database type enumeration
 * @author mmaloney
 *
 */
public enum TsdbType
{
	CWMS("Corps Water Manangement System"),
	HDB("USBR Hydrologic Database"),
	OPENTSDB("Open Time Series Database"),
	NONE("No Time Series DB (DECODES Only)");
	
	private String desc;
	
	private TsdbType(String desc)
	{
		this.desc = desc;
	}
}
