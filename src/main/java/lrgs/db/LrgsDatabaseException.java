/**
 * 
 */
package lrgs.db;

/**
 * Throw when database IO error occurs.
 */
public class LrgsDatabaseException extends Exception
{
	/** 
	  Constructor.
	  @param msg the message.
	*/
	public LrgsDatabaseException(String msg)
	{
		super(msg);
	}
}
