/*
*  $Id$
*/

package lrgs.common;

import java.util.Comparator;
import java.util.Date;
import java.util.Vector;
import java.io.LineNumberReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

import ilex.util.*;


/**
	A NetworkList stores a list of data collection platforms. For each
	platform the list holds the name, description, and a unique numeric
	DCP address. With this class you can read and write network list files
	and examine the file contents.
*/
@SuppressWarnings("serial")
public class NetworkList extends Vector<NetworkListItem>
	implements FileParser, Cloneable
{
	/** The File from which this list was read. */
    public File file;

    private FileExceptionList exlist;

	/** opaque handle associated with this list. */
	private Object handle;

	/** The time this list was loaded from the file. */
	private Date lastReadTime; 

	/**
		Instantiate an empty NetworkList object.
	*/
    public NetworkList()
    {
    	file = null;
    	exlist = null;
		handle = null;
		lastReadTime = null;
    }

    public NetworkList(File file) throws IOException
    {
    	this();
    	this.file = file;
    	parseFile(file);
    }

	public String makeFileName()
	{
		if (file == null)
			return "list-in-memory.nl";
		else
			return file.getName();
	}

    /**
       Read specified text file and populate this network list.
       The list is not flushed before read. Subsequent reads can
       be used to combine network list files.
     */
	public boolean parseFile(File input) throws IOException
    {
		if (file == null)
			file = input;
    	exlist = null;    // Toss old exceptions, if any.
   		LineNumberReader rdr = new LineNumberReader(
   			new FileReader(input));
   		String ln;
   		while( (ln = rdr.readLine()) != null)
   		{
			if (ln.length() <= 0
   			 || ln.charAt(0) == '#') // skip comment lines.
   				continue;
   			else
   			{
   				NetworkListItem nli;
   				try
   				{
   					nli = new NetworkListItem(ln);
   					addElement(nli); // Add new element to the end of vector
   				}
   				catch(IllegalArgumentException e)
   				{
   					if (exlist == null)
   						exlist = new FileExceptionList(input);
   					exlist.add(new FileException(rdr.getLineNumber(), e));
   					//System.err.println("Line " + rdr.getLineNumber()
   					//	+ ": " + e);
   				}
   			}
   		}
		rdr.close();
		lastReadTime = new Date();
    	return true;
    }

    public void saveFile() throws IOException
	{
		if (file == null)
			throw new IOException("No file defined");
		saveFile(file);
	}

    public void saveFile(File output) throws IOException
    {
    	FileWriter fw = new FileWriter(output);
    	fw.write(toFileString());
    	fw.flush();
    	fw.close();
    }

    public int getNumExceptions() {
    	if (exlist == null)
    		return 0;
    	else
    		return exlist.size();
    }

    public FileExceptionList getWarnings()
    {
    	return exlist;
    }

    /**
      Return the entire list in the format that it would be stored
      in a text file.
    */
    public String toFileString()
	{
		StringBuffer ret = new StringBuffer();
		for(int i = 0; i < size(); i++)
			ret.append(elementAt(i).toString() + '\n');
		return new String(ret);
	}

	/**
	  Sort the network list by DCP address in ascending order.
	*/
	public void sortByAddress()
	{
		java.util.Collections.sort(this, new cmpByAddr());
	}

	/**
	  Sort the network list by DCP Name in ascending order.
	*/
	public void sortByName()
	{
		java.util.Collections.sort(this, new cmpByName());
	}

	/**
	  Sort the network list by description in ascending order.
	*/
	public void sortByDescription()
	{
		java.util.Collections.sort(this, new cmpByDescription());
	}

	/**
	  Inner class for comparing network list items by DCP address.
	*/
	class cmpByAddr implements Comparator<NetworkListItem>
	{
		NetworkListItem cmp;

		cmpByAddr()
		{
			cmp = null;
		}

		cmpByAddr(NetworkListItem c)
		{
			this();
			cmp = c;
		}

		public int compare(NetworkListItem nli1, NetworkListItem nli2)
		{
			return nli1.addr.compareTo(nli2.addr);
		}

		public boolean equals(Object o)
		{
			if (cmp == null)
				return false;
			return cmp.equals(o);
		}
	}

	/**
	  Inner class for comparing network list items by name.
	*/
	class cmpByName implements Comparator<NetworkListItem>
	{
		NetworkListItem cmp;

		cmpByName()
		{
			cmp = null;
		}

		cmpByName(NetworkListItem c)
		{
			this();
			cmp = c;
		}

		public int compare(NetworkListItem nli1, NetworkListItem nli2)
		{
			return nli1.name.compareTo(nli2.name);
		}

		public boolean equals(Object o)
		{
			if (cmp == null)
				return false;
			NetworkListItem nli = (NetworkListItem)o;
			return cmp.name.equals(nli.name);
		}
	}
	/**
	  Inner class for comparing network list items by description.
	*/
	class cmpByDescription implements Comparator<NetworkListItem>
	{
		public int compare(NetworkListItem nli1, NetworkListItem nli2)
		{
			return nli1.description.compareTo(nli2.description);
		}
	}


	/**
	 * Set the handle associated with this network list.
	 * @param handle the handle
	 */
	public void setHandle(Object handle)
	{
		this.handle = handle;
	}

	/**
	 * Get the handle associated with this network list.
	 * @return the handle
	 */
	public Object getHandle()
	{
		return handle;
	}

	/**
	 * @return the date/time this list was last loaded from the file.
	 */
	public Date getLastReadTime()
	{
		return lastReadTime;
	}

	/**
	 * @return the File from which this network list was loaded. 
	 */
	public File getFile() { return file; }
	
	public boolean containsDcpAddr(DcpAddress addr)
	{
		for(NetworkListItem nli : this)
			if (addr.equals(nli.addr))
				return true;
		return false;
	}
}

