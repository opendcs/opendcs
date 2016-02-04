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
package lrgs.ldds;

import decodes.util.ResourceFactory;

/**
 * This class documents the DDS versions and what features they support.
 */
public class DdsVersion
{
	/** Current version of servers & clients compiled with this code. */
	public static final int DdsVersionNum = 13;
	
	/**
	 * Version 13:
	 * Addition of set pw functions.
	 */
	public static final int version_13 = 13;
	
	/**
	 * Version 12:
	 * Allow the following new SOURCE names: GOES_SELFTIMED GOES_RANDOM
	 */
	public static final int version_12 = 12;

	/**
	 * Version 11:
	 * 	Allow specifying "single mode" in search criteria.
	 * 	Include <LocalRecvTime> element in xml message block.
	 */
	public static final int version_11 = 11;

	/**
	 * Version 10 adds support for Iridium messages, which have a very
	 * different header structure from GOES.
	 * Servers MUST NOT send iridium messages to clients who can't handle it.
	 */
	public static final int version_10 = 10;
	
	public static final int version_9 = 9;
	
	/**
	 * Version 8: Extensible (XML) format for message exchange.
	 */
	public static final int version_8 = 8;
	
	public static final int version_7 = 7;

	/**
	 * Version 6: For rtstat - method to retrieve LRGS event messages.
	 */
	public static final int version_6 = 6;
	
	/**
	 * Version 5: Multi-mode retrieval (multiple messages returned in response).
	 */
	public static final int version_5 = 5;
	
	/**
	 * Version 4: Authentication
	 */
	public static final int version_4 = 4;
	
	public static final int version_3 = 3;

	public static final int version_1 = 1;
	
	public static final int version_unknown = 0;
	


	public static String getVersion()
	{
		return "" + DdsVersionNum +
			ResourceFactory.instance().getDdsVersionSuffix();
	}
}
