/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.db;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
A NetworkList is a collection of transport media. The most common
type is a list of GOES DCP addresses.
 */
public class NetworkList extends IdDatabaseObject
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	/** Unique name of this network list. */
	public String name;

	/** Type of transport medium stored in this network list. */
	public String transportMediumType;

	/** Preferred name type for this network list. */
	public String siteNameTypePref;

	/**
	 * This HashMap stores the NetworkListEntry objects, indexed by their
	 * transportId's.  The transportId's are converted to uppercase before
	 * being used as a key in this HashMap.
	 * This data member is never null.
	 */
	public HashMap<String, NetworkListEntry> networkListEntries
		= new HashMap<String, NetworkListEntry>();

	/** Time that this network list was last modified in the database. */
	public Date lastModifyTime;

	/** Executable Links */
	public lrgs.common.NetworkList legacyNetworkList;

	/** Optional directory in which to store legacy netlist file. */
	public static String legacyNetlistDir = null;

	/** Dummy placeholder for when user adds <all> to network list */
	public static final NetworkList dummy_all =
		new NetworkList("<all>", Constants.medium_Goes);

	/** Dummy placeholder for when user adds <production> to network list */
	public static final NetworkList dummy_production =
		new NetworkList("<production>", Constants.medium_Goes);

	/**
	 * Constructor.
	 */
	public NetworkList()
	{
		super(); // sets _id to Constants.undefinedId;

		name = null;
		transportMediumType = null;
		siteNameTypePref = null;
		legacyNetworkList = null;
		lastModifyTime = null;
	}

	/**
	 * Construct with a name.
	  @param name the name of this list
	 */
	public NetworkList(String name)
	{
		this();
		this.name = name;
	}

	public NetworkList(String name, String tmType)
	{
		this(name);
		transportMediumType = tmType;
	}

	/**
	 * @return a deep copy of this object, that is, the list and all of its
	 * members are copied.
	 */
	public NetworkList copy()
	{
		NetworkList ret = new NetworkList(name);
		try { ret.setId(getId()); }
		catch(DatabaseException ex) {} // won't happen.

		ret.transportMediumType = transportMediumType;
		ret.siteNameTypePref = siteNameTypePref;
		ret.lastModifyTime = lastModifyTime;
		for(Iterator<NetworkListEntry> it = iterator(); it.hasNext(); )
		{
			NetworkListEntry nle = it.next();
			ret.addEntry(nle.copy(ret));
		}
		return ret;
	}

	/**
	 * Remove all the network list entries from this NetworkList.
	 */
	public void clear()
	{
		networkListEntries.clear();
	}

	/**
	 * @return a string out of this object's name, suitable for use as a
	 * filename.
	 */
	public String makeFileName()
	{
		if (this == dummy_all)
			return "<all>";
		else if (this == dummy_production)
			return "<production>";

		StringBuffer ret = new StringBuffer(name);
		for(int i=0; i<ret.length(); i++)
			if (Character.isWhitespace(ret.charAt(i)))
				ret.setCharAt(i, '-');
		return ret.toString();
	}

	/**
	 * This overrides the DatabaseObject's method.
	 * @return "NetworkList".
	 */
	public String getObjectType() {
		return "NetworkList";
	}

	/**
	 * Add a NetworkListEntry to this list.
	  @param nle the entry to add
	 */
	public void addEntry(NetworkListEntry nle)
	{
		networkListEntries.put(nle.transportId.toUpperCase(), nle);
	}

	/**
	 * Retrieve a NetworkListEntry by transportId.
	  @param transportId the ID to search for.
	  @return the entry or null if no match
	 */
	public NetworkListEntry getEntry(String transportId)
	{
		return (NetworkListEntry) networkListEntries.get(
			transportId.toUpperCase());
	}

	/**
	 * Remove the entry with the passed transport ID.
	 * @param transportId the ID to remove
	 * @return true if entry was deleted, false if it was not in the list.
	 */
	public boolean removeEntry(String transportId)
	{
		return networkListEntries.remove(transportId.toUpperCase()) != null;
	}

	/**
	  @return an iterator with which to step through the entries.
	 */
	public Iterator<NetworkListEntry> iterator()
	{
		return networkListEntries.values().iterator();
	}

	/**
	 * @return the number of network list entries in this list.
	 */
	public int size()
	{
		return networkListEntries.size();
	}

	/**
	 * @return the list of NetworkListEntries as a Collection.
	 */
	public Collection<NetworkListEntry> values()
	{
		return networkListEntries.values();
	}

	/**
	 * This compares one NetworkList with another.
	  @param ob the other object
	 */
	public boolean equals(Object ob)
	{
		if (!(ob instanceof NetworkList))
			return false;
		NetworkList nl = (NetworkList)ob;
		if (nl == this)
			return true;

		if (!name.equalsIgnoreCase(nl.name))
			return false;
		if (!transportMediumType.equalsIgnoreCase(nl.transportMediumType))
			return false;

		if (!siteNameTypePref.equalsIgnoreCase(nl.siteNameTypePref))
			return false;

		for(Iterator<String> it1 = networkListEntries.keySet().iterator();
			it1.hasNext();)
		{
			Object key = it1.next();

			NetworkListEntry nle1 = networkListEntries.get(key);
			NetworkListEntry nle2 = nl.networkListEntries.get(key);

			// Note: We're iterating 'this', so assume that nle1 is not null.
			if (nle2 == null)
				return false;
			if (!nle1.equals(nle2))
				return false;
		}
		return true;
	}

	/**
	 * From DatabaseObject interface,
	 */
	public void prepareForExec()
		throws InvalidDatabaseException
	{
		//Feature added to allow different type of DCP Address (less than 8
		//hex digits) other than GOES
		//If transport medium type is one of these types:

		// MJM 5/30/2014 Why not allow it to create a legacy netlist regardless
		// of type? There may be some use for this later as LRGS becomes more flexible.


		// Translate new NetworkList into old LRGS netlist format:
		legacyNetworkList = new lrgs.common.NetworkList();

		String path = legacyNetlistDir != null ?
			(legacyNetlistDir + File.separator) : "";
			path = path + name + ".nl";

		legacyNetworkList.file = new File(path);

		for (Iterator<NetworkListEntry> it = iterator(); it.hasNext(); )
		{
			NetworkListEntry nle = it.next();

			lrgs.common.NetworkListItem nli =
				new lrgs.common.NetworkListItem();
			nli.name = nle.getPlatformName() == null ? "" : nle.getPlatformName();
			nli.description =
				nle.getDescription() == null ? "" : nle.getDescription();

			// Make sure the description is only a single line.
			int idx = nli.description.indexOf('\r');
			if (idx == -1)
				idx = nli.description.indexOf('\n');
			if (idx != -1)
				nli.description = nli.description.substring(0, idx);

			nli.type = 'U';
			try
			{
				nli.addr = new lrgs.common.DcpAddress(nle.transportId);
				legacyNetworkList.add(nli);
			}
			catch(NumberFormatException nfe)
			{
				log.atWarn()
				   .setCause(nfe)
				   .log("Network List '{}' has improper DCP address '{}' - must be 8 hex digits -- skipped.",
				        name, nle.transportId);
			}
		}
	}

	/**
	 * From DatabaseObject interface.
	  @return true if prepareForExec was previously called.
	 */
	public boolean isPrepared()
	{
		return legacyNetworkList != null;
	}

	/**
	 * From DatabaseObject interface; this does nothing.
	 */
	public void validate()
	throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	 * From DatabaseObject interface; this reads (or re-reads) the NetworkList
	 * from the database.
	 * In the XML database, this uses the object's name member (not its ID)
	 * to uniquely identify the record in the database.
	 */
	public synchronized void read()
	throws DatabaseException
	{
		if (this == dummy_all || this == dummy_production)
		{
			this.lastModifyTime = new Date();
			return;
		}
		clear();
		myDatabase.getDbIo().readNetworkList(this);
	}

	/**
	 * From DatabaseObject interface; this writes the NetworkList
	 * back out to the database.
	 * In the XML database, this uses the object's name member (not its ID)
	 * to uniquely identify the record in the database.
	 */
	public void write()
	throws DatabaseException
	{
		lastModifyTime = new Date();
		myDatabase.getDbIo().writeNetworkList(this);
	}

	public String getDisplayName()
	{
		return name;
	}

	/**
	 * Return true if this list contains a medium id matching the passed platform
	 * @param p the platform
	 * @return true if this list contains a medium id matching the passed platform
	 */
	public boolean contains(Platform p)
	{
		return getEntry(p) != null;
	}

	/**
	 * If this list contains an entry matching a transport medium in the passed
	 * platform, return it. Otherwise return null.
	 * @param p the platform
	 * @return the matching entry or null if none found.
	 */
	public NetworkListEntry getEntry(Platform p)
	{
		TransportMedium platTM = null;
		if (this.transportMediumType == null)
			platTM = p.transportMedia.size() == 0 ? null : p.transportMedia.firstElement();
		else
		{
			boolean _isGoes = isGoes();
			for(Iterator<TransportMedium> tmit = p.getTransportMedia(); tmit.hasNext(); )
			{
				TransportMedium _tm = tmit.next();
				if ((_isGoes && _tm.isGoes())
				 || transportMediumType.equalsIgnoreCase(_tm.getMediumType()))
				{
					platTM = _tm;
					break;
				}
			}
		}
		if (platTM == null)
			return null;

		return getEntry(platTM.getMediumId());
	}

	public boolean contains(TransportMedium tm)
	{
		if ((tm.isGoes() && this.isGoes()
		 || transportMediumType.equalsIgnoreCase(tm.getMediumType())))
		{
			String mediumId = tm.getMediumId();
			for(Iterator<NetworkListEntry> nleit = iterator(); nleit.hasNext(); )
			{
				NetworkListEntry nle = nleit.next();
				if (mediumId.equalsIgnoreCase(nle.getTransportId()))
					return true;
			}
		}
		return false;
	}

	public boolean isGoes()
	{
		return transportMediumType != null
			&& transportMediumType.toLowerCase().startsWith("goes");
	}

}
