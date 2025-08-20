/*
* $Id$
*/
package lrgs.dqm;

/**
Encapsulates all exceptions that can be thrown by the serial interface.
*/
public class DqmSerialException extends Exception 
{
	public DqmSerialException(String message)
	{
		super(message);
	}
}
