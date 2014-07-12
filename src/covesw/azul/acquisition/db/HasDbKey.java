/*
 * $Id$
 * 
 * $Log$
 * 
 * Copyright 2014 Cove Software, LLC. All rights reserved.
 */
package covesw.azul.acquisition.db;

/**
 * Implemented by objects stored in the database that can be referenced
 * by a surrogate key.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public interface HasDbKey
{
	/**
	 * @return the surrogate key value
	 */
	public long getKey();
	
	/**
	 * @param key the key value to set
	 */
	public void setKey(long key);
}
