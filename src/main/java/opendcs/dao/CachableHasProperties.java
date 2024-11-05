/**
 * $Id$
 * 
 * $Log$
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

import ilex.util.HasProperties;

/**
 * Combines CachableDbObject with HasProperties
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public interface CachableHasProperties extends CachableDbObject, HasProperties
{

}
