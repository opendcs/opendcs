/**
 * @(#) PlatListEntry.java
 */

package decodes.syncgui;

import java.util.Vector;
import java.util.Iterator;
import java.text.NumberFormat;

/**
The PlatList holds a collection of these objects.
*/
public class PlatListEntry
{
	/** Platform ID */
	public int id;
	
	/** Owning Agency Name */
	public String agency;
	
	/** Configuration Name */
	public String configName;
	
	/** Vector of Strings, representing the transport media */
	public Vector media;
	
	/** Site Name (preferred type) */
	public String siteNameType;
	
	/** Site Name Value */
	public String siteNameValue;

	/** The description */
	public String desc;

	/** The expiration time as a string */
	public String expiration;

	protected static NumberFormat platIdFormat;
	static
	{
		platIdFormat = NumberFormat.getNumberInstance();
		platIdFormat.setMaximumIntegerDigits(5);
		platIdFormat.setMinimumIntegerDigits(5);
		platIdFormat.setGroupingUsed(false);
	}

	/** 
	  Constructor -- sets all descriptive attributes to null.
	  @param id the platform ID.
	*/
	public PlatListEntry(int id)
	{
		this.id = id;
		agency = "";
		configName = "";
		media = new Vector();
		siteNameType = "";
		siteNameValue = "";
		desc = "";
		expiration = "";
	}

	/**
	  Adds a transport medium in the form type:id.
	  @param med the medium.
	*/
	public void addMedium(String med)
	{
		media.add(med);
	}

	/** @return string containing the platform info. */
	public String toString()
	{
		return "id=" + id + ", site=" + siteNameType + ":" + siteNameValue
			+ ", agency=" + agency + ", desc=" + desc;
	}

	/** @return iterator into transport media */
	public Iterator getTMIterator()
	{
		return media.iterator();
	}

	/** @return String representing the (1st) or default medium */
	public String getDefaultMedium()
	{
		if (media.size() == 0)
			return "(no transport media)";
		else
		{
			String med = (String)media.elementAt(0);
			int idx = med.indexOf(':');
			return idx != -1 ? med.substring(idx+1) : med;
		}
	}

	public String makeFileName()
	{
		return "p" + platIdFormat.format(id) + ".xml";
	}
}
