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
 * Defines event numbers used in log messages by the Iridium SBD module.
 * @author mmaloney
 */
public enum IridiumEvent
{
	BadIridiumConfig(1, false),
	CannotListen(2, false),
	BadHeaderFormat(3, true),
	BadMessageFormat(4, true),
	ClientHangup(5, true);

	private int evtNum;
	private boolean autoRecover = false;
	
	private IridiumEvent(int evtNum, boolean autoRecover)
	{
		this.evtNum = evtNum;
		this.autoRecover = autoRecover;
	}
	
	public int getEvtNum() { return evtNum; }
	public boolean isAutoRecover() { return autoRecover; }
}
