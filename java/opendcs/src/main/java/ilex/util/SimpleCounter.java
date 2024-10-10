/*
*  $Id$
*/
package ilex.util;

/**
* Interface for implementing persistent counters used for Unique IDs.
*/
public class SimpleCounter implements Counter
{
	private int nextValue;

	public SimpleCounter(int initValue)
	{
		setNextValue(initValue);
	}

	/**
	* @return the next integer value and increments the counter.
	*/
	public int getNextValue( )
	{
		return nextValue++;
	}

	/**
	* Sets the counter so that the next call to getNextValue() will return
	* the passed integer.
	* @param value next value to return.
	*/
	public void setNextValue( int value )
	{
		nextValue = value;
	}
}
