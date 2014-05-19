/**
 * $Id$
 * 
 * $Log$
 * Revision 1.2  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 * Revision 1.1  2012/08/17 19:46:00  mmaloney
 * Refactoring for correct implementation of listcompsForGUI.
 *
 * 
 * This is open-source software written by Sutron Corporation and
 * Cove Software, LLC under contract to the federal government. 
 * You are free to copy and use this
 * source code for your own purposes, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * Except for specific contractual terms between ILEX and the federal 
 * government, this source code is provided completely without warranty.
 * 
 * $Log$
 * Revision 1.2  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.hdb;

import decodes.sql.DbKey;

/**
 * Represents an entry in the HDB_SITE_DATATYPE table.
 * An entry is immutable.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class HdbSiteDatatype
{
	/** HDB Site Datatype ID */
	private DbKey sdi;
	
	/** HDB_SITE.SITE_ID */
	private DbKey siteId;
	
	/* HDB_DATATYPE.DATATYPE_ID */
	private DbKey datatypeId;

	public HdbSiteDatatype(DbKey sdi, DbKey siteId, DbKey datatypeId)
	{
		super();
		this.sdi = sdi;
		this.siteId = siteId;
		this.datatypeId = datatypeId;
	}

	public DbKey getSdi()
	{
		return sdi;
	}

	public DbKey getSiteId()
	{
		return siteId;
	}

	public DbKey getDatatypeId()
	{
		return datatypeId;
	}
}
