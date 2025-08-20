package decodes.dbeditor;

/**
 * This interface is used when loading a sample DCP message.
 */
public interface SampleMessageOwner
{
	/**
	 * After data is read from a file, this fills in the sample message area.
	 * @param msgData the message data.
	 */
	public void setRawMessage(String msgData);
}
