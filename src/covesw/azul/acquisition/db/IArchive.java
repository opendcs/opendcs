/*
 * $Id$
 * 
 * $Log$
 * 
 * Copyright 2014 Cove Software, LLC. All rights reserved.
 */
package covesw.azul.acquisition.db;

import lrgs.common.DcpMsg;

/**
 * Interface for raw message archives.
 */
public interface IArchive
{
	/**
	 * Initialize the archive. This is called once after construction.
	 */
	public void initialize();
	
	/**
	 * Shut down the archive. This is called once before program exit.
	 */
	public void shutdown();
	
	/**
	 * Archive a message.
	 * @param message the message to archive.
	 */
	public void archiveMessage(DcpMsg message);

}
