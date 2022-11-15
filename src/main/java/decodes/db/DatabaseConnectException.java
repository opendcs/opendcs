/*
*  $Id$
*/
package decodes.db;

/**
Thrown when can't connect to database because of missing or incorrect
connection parameters.
*/
public class DatabaseConnectException extends DatabaseException
{
	/** 
	  constructor.
	  @param msg the message.
 	*/
	public DatabaseConnectException(String msg)
	{
		super(msg);
	}

	/** 
	  constructor with cause
	  @param msg the message.
	  @param 
 	*/
	 public DatabaseConnectException(String msg, Throwable ex)
	 {
		 super(msg,ex);
	 }
}
