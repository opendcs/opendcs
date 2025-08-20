/*
 * Open source software by Cove Software, LLC.
 * Prepared under contract to the U.S. Government.
 * Copyright 2014 United States Government, U.S. Geological Survey
 * 
 * $Id$
 * 
 * $Log$
*/
package lrgs.iridiumsbd;

/**
 * Thrown when a format problem is detected in an SBD message.
 */
public class SBDFormatException
    extends Exception
{
	public SBDFormatException(String msg)
	{
		super(msg);
	}
}
