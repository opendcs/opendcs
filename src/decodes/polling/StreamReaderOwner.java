package decodes.polling;

import java.io.IOException;

public interface StreamReaderOwner
{
	/**
	 * Called by the input StreamReader when an IOException has occurred.
	 * It means that the input stream from the station has failed.
	 * @param ex the exception thrown
	 */
	void inputError(IOException ex);
	
	/**
	 * Called by the input StreamReader when end of stream (-1) is received.
	 * This may be normal or abnormal.
	 */
	void inputClosed();
	
	public String getModule();

}
