/*
* $Id$
*/
package lrgs.rtstat;

import java.util.ArrayList;
import ilex.util.AuthException;
import lrgs.ldds.DdsUser;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.ddsrecv.DdsRecvSettings;
import lrgs.drgs.DrgsInputSettings;
import lrgs.db.Outage;

/**
This interface is used by the real-time status GUIs to access the client
interface synchronously.
*/
public interface DdsClientIf
{
	/**
	 * Queries the server for status and returns it as an XML byte array.
	 * @return XML byte array containing status.
	 */
	public byte[] getStatus();

	/**
	 * @return list of DdsUser's defined on the server.
	 */
	public ArrayList<DdsUser> getUsers()
		throws AuthException;

	/**
	 * @return latest events on the server.
	 */
	public String[] getEvents();

	/**
	 * Modifies a user on the server.
	 * @param ddsUser the user data.
	 * @param pw the new password, or null to leave unchanged.
	 */
	public void modUser(DdsUser ddsUser, String pw)
		throws AuthException;

	/**
	 * Removes a user from the server.
	 */
	public void rmUser(String userName)
		throws AuthException;

	/**
	 * Sends the new LrgsConfiguration to the server.
	 */
	public void applyLrgsConfig(LrgsConfig lrgsConfig)
		throws AuthException;

	/**
	 * Sends the new DdsRecvSettings to the server.
	 */
	public void applyDdsRecvSettings(DdsRecvSettings settings)
		throws AuthException;

	/**
	 * Sends the new DrgsInputSettings to the server.
	 */
	public void applyDrgsInputSettings(DrgsInputSettings settings)
		throws AuthException;

	public void applyNetworkDcpSettings(DrgsInputSettings settings)
		throws AuthException;

	/**
	 * @return a list of network lists that exist on the server.
	 */
	public String[] getNetlistList()
		throws AuthException;

	/**
	 * @return the data in a particular network list.
	 */
	public byte[] getNetlist(String listname)
		throws AuthException;

	/**
	 * Install a network list on the server.
	 */
	public void installNetlist(String listname, byte[] data)
		throws AuthException;

	/**
	 * Delete a network list from the server.
	 */
	public void deleteNetlist(String listname)
		throws AuthException;

	/**
	 * @return list of outages from the server.
	 */
	public ArrayList<Outage> getOutages()
		throws AuthException;

	/**
	 * Assert (or reassert) outages.
	 * @param outages the outages
	 */
	public void assertOutages(ArrayList<Outage> outages)
		throws AuthException;
	
	/** @return the hostname of the server. */
	public String getServerHost();
}
