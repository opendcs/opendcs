/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation;

import java.io.File;

/**
 * Keeps track of a single file containing DSS to CWMS path mappings
 */
public class LoadedFile
	extends File
{
	/** msec value when the file was last read into memory */
	private long lastRead = 0L;

	/**
	 * @param theFile
	 */
	public LoadedFile(String path)
	{
		super(path);
		lastRead = 0L;
	}

	public long getLastRead()
	{
		return lastRead;
	}

	public void setLastRead(long lastRead)
	{
		this.lastRead = lastRead;
	}
}
