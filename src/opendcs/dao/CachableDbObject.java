/*
 * $Id$
 * 
 * $Log$
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
