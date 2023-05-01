package decodes.platwiz;


/**
Thrown from one of the GUI panels back to the parent frame.
*/
public class PanelException extends Exception
{
	/** Default constructor. */
	public PanelException()
	{
	}

	/**
	  Construct with a message.
	  @param msg the message
	*/
	public PanelException(String msg)
	{
		super(msg);
	}

	public PanelException(String msg, Throwable cause)
	{
		super(msg,cause);
	}
}
