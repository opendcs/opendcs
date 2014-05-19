/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.6  2008/09/26 20:49:02  mjmaloney
*  Added <all> and <production> network lists
*
*  Revision 1.5  2008/09/26 14:57:31  mjmaloney
*  Added <all> and <production> network lists
*
*  Revision 1.4  2008/09/25 15:02:11  mjmaloney
*  Fixed parsing for string DCP address.
*
*  Revision 1.3  2008/08/19 16:38:15  mjmaloney
*  DcpAddress stores internal value as String.
*
*  Revision 1.2  2008/08/06 19:40:58  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2001/03/05 22:11:17  mike
*  Improved parsing logic to handle more text variations.
*
*  Revision 1.6  2001/02/28 01:12:19  mike
*  Working network list editor.
*
*  Revision 1.5  2001/01/12 15:39:28  mike
*  Better error reporting when constructing a network list item. All
*  parsing exceptions should be funnelled into IllegalArgumentException
*
*  Revision 1.4  2000/03/02 20:58:58  mike
*  Added proper equals() and compareTo() methods.
*
*  Revision 1.3  1999/09/14 17:05:34  mike
*  9/14/1999
*
*  Revision 1.2  1999/09/03 17:22:57  mike
*  Put in new package hierarchy.
*
*  Revision 1.1  1999/09/03 15:34:52  mike
*  Initial checkin.
*
*
*/

package lrgs.common;

import java.io.Serializable;

/**
	Class NetworkListItem holds a single element from a network list
	file. This item prepresents a data collection platform by name,
	description, and numeric address.
*/

public class NetworkListItem implements Serializable, Cloneable, Comparable
{
    public String name;
    public String description;
    public char type;
    public DcpAddress addr;

	public static final int SORT_BY_ADDR = 0;
	public static final int SORT_BY_NAME = 1;

	/**
	  By setting the static integer sortBy to one of the constants,
	  SORT_BY_ADDR or SORT_BY_NAME, you can control the behavior of
	  the compareTo function.
	*/
	public static int sortBy = SORT_BY_ADDR;

    /**
    	Instantiate empty network list item.
    */
    public NetworkListItem()
    {
    	name = "";
    	description = "";
    	type = 'U';  // 'U' == unknown
    	addr = new DcpAddress();
    }

	/**
		Instantiate a network list item by parsing a string.
		See the 'fromString' method for a description of the required
		string format.
	*/
    public NetworkListItem(String s) throws IllegalArgumentException
    {
    	this();
    	fromString(s);
    }

	/**
	 * Copy constructor
	 */
	public NetworkListItem(NetworkListItem nli)
	{
		this();
	   	name = nli.name;
    	description = nli.description;
    	type = nli.type;  // 'U' == unknown
    	addr = new DcpAddress(nli.addr);
	}
	
	public NetworkListItem(DcpAddress addr, String name, String desc)
	{
		this();
		this.addr = addr;
		this.name = name;
		this.description = desc;
	}

    /**
    	Return a string representing this DCP in the format:
    		address:name description:type
    	This string is suitable for saving in a network list file.
    */
    public String toString()
    {
    	String desc = (description == null ? "" : description);
    	return addr.toString() + ":" + name +
    		(desc.length()>0 ? " " : "")
    		 + desc + ":" + type;
    }

    /**
    	Parse a string and load this object. The string should be in the format
    	suitable for storage in a network list file:
			address:name description:type
		where...
			'address' is an 8-hex-digit DCP address
			'name' is the first blank-delimited word after the colon.
			'description' is a brief text description of the DCP (e.g. location)
			'type' is a single character code describing the DCP type

		If the string cannot be parsed, an IllegalArgumentException is
		thrown.
	*/
	public void fromString(String str) throws IllegalArgumentException
	{
		try
		{
	    	name = "";
	    	description = "";
	    	type = 'u';

	    	str = str.trim();
	    	int i = str.indexOf(':');
	    	addr = new DcpAddress(i > 0 ? str.substring(0, i) : str);

	    	// Get new string after colon.
	    	if (i < 0)
	    		return; // No name or comment.

			int len = str.length();
			while (++i < len && Character.isWhitespace(str.charAt(i)))
				;

			if (i >= len)
				return; // No name present.

	    	str = str.substring(i);
			len = str.length();

	    	// First blank delimited word in description field is the name.
	    	for(i = 0; i < len
	    		      && !Character.isWhitespace(str.charAt(i))
	    		      && str.charAt(i) != ':'; ++i)
	    		;
	    	if (i > 0)
	    		name = str.substring(0, i);

			if (i >= len)
				return;

	    	str = str.substring(i);
			len = str.length();

	    	// Skip whitepsace between name and comment.
	    	for(i=0; i < len && str.charAt(i) == ' '; ++i);
			if (i >= len)
				return; // No description

	    	if (i > 0)
	    		str = str.substring(i);

	    	i = str.indexOf(':');
	    	if (i >= 0)
	    	{
	    		description = str.substring(0, i);
	    		type = ++i < str.length() ? str.charAt(i) : 'u';
	    	}
			else
				description = str;
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException(
				"Bad format for network list item '" + str + "': " + e);
		}
    }

	/**
	  Return true if the passed object is considered equal to 'this'.
	*/
    public boolean equals(Object obj)
	{
		// For equals() class types must match exactly.
		if (obj.getClass() != getClass())
			return false;
		return compareTo(obj) == 0;
	}

    /**
      Compare two Network List Items.

	  If the static integer sortBy is set to SORT_BY_ADDR (the default),
	  then the fields are compared in the following order:
		DCP-Address, DCP-Name, Description, type

	  Conversely, if sortBy is set to SORT_BY_NAME, then the fields are
	  compared in the following order:
		DCP-Name, DCP-Address, Description, type

	  Note that for name comparisons, a case-insensitive string compare is
	  done.

	  Return: 0 if objects are equal, <0 if 'this' is less than the
	  passed object. Return >0 if 'this is greater than the passed object.
     */
	public int compareTo(Object o)
	{
		if (this == o)
			return 0;
		if (o == null)
			return 1;     // A null object is always greater than a non-null.

		// The following will throw an exception if o is not the right type:
		NetworkListItem rhs = (NetworkListItem)o;

		int i = 0;
		if (sortBy == SORT_BY_ADDR)
		{
			i = addr.compareTo(rhs.addr);
			if (i != 0) return i;

			i = name.compareToIgnoreCase(rhs.name);
			if (i != 0) return i;
		}
		else
		{
			i = name.compareToIgnoreCase(rhs.name);
			if (i != 0) return i;

			i = addr.compareTo(rhs.addr);
			if (i != 0) return i;
		}

		i = description.compareTo(rhs.description);
		if (i != 0) return i;

		i = (int)type - (int)rhs.type;
		if (i != 0) return i;

		return 0; // All fields are equal!
	}
}
