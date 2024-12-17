/*
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
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms;

import decodes.db.Constants;

public class HecConstants
{
	    public static final int UNDEFINED_UTC_OFFSET = Integer.MAX_VALUE;
    public static final int NO_UTC_OFFSET = Integer.MIN_VALUE;

    public static boolean isValidValue(double v)
    {
        return !Double.isInfinite(v) && !Double.isNaN(v) && v != Constants.undefinedDouble;
    }
}
