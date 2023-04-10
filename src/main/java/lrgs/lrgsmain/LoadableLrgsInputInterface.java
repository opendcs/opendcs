package lrgs.lrgsmain;

import lrgs.archive.MsgArchive;

public interface LoadableLrgsInputInterface extends LrgsInputInterface
{
	/**
	 * The interface is assigned a unique name in the config file:
	 * LrgsInput.<InterfaceName>.class=<fully-qualified java class name>
	 * @param name
	 */
	public void setInterfaceName(String name);

	/**
	 * Sets a config parameter read from the lrgs.conf file.
	 * Config params are in the form: LrgsInput.<InterfaceName>.<paramName>=<value>
	 * @param name the param name
	 * @param value the param value
	 */
	public void setConfigParam(String name, String value);

	/**
	 * Allow interaction with the message archive.
	 *
	 * @param archive The message archive to store what we've received.
	 */
	public void setMsgArchive(MsgArchive archive);
}
