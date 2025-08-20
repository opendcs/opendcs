/*
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.

 */
package opendcs.dao;

import decodes.sql.DbKey;

/**
 * Interface for objects stored in a cache accessible by ID or Unique Name
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public interface CachableDbObject
{
	/** @return the unique database surrogate key */
	public DbKey getKey();
	
	/** @return the unique name */
	public String getUniqueName();
}
