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
}

