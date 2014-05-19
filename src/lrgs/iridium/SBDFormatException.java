/**
 * 
 */
package lrgs.iridium;

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
