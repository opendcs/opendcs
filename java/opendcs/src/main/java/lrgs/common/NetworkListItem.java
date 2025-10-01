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
		catch(Exception ex)
		{
			throw new IllegalArgumentException("Bad format for network list item '" + str + "'", ex);
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

	  Return: 0 if objects are equal, &lt; 0 if 'this' is less than the
	  passed object. Return &gt; 0 if 'this is greater than the passed object.
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
