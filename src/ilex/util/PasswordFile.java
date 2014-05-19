/*
*  $Id$
*/
package ilex.util;

import java.util.Hashtable;
import java.util.Enumeration;
import java.text.ParsePosition;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.FileWriter;

/**
* PasswordFile implements a custom security mechanism for storing usernames,
* roles, and hashed passwords in an ASCII file.
* This class provides utilities for reading, writing, & modifying the file.
* Care should be taken to make sure that the file is secure on the target
* system. That is, it should be readable & writable only by the appropriate
* administrative user.
*/
public class PasswordFile implements FileParser
{
	private File passwordFile;
	private Hashtable entries;
    private FileExceptionList exlist;

	/**
	* Constructs a PasswordFile object with the given file name.
	* You must explicitely call read() after construction to load this object
	* with the entries from the file.
	* @param f the File
	*/
	public PasswordFile( File f )
	{
		passwordFile = f;
	}

	/**
	* Retrieves the File object naming this password file.
	* @return the File
	*/
	public File getFile( )
	{
		return passwordFile;
	}

	/**
	* Reads the password file and stores the entries internally.
	* Returns the number of entries successfully read.
	* <p>
	* Parsing the file may result in one or more non-fatal errors. After
	* calling the read method, call getWarnings to get a list of file
	* exceptions.
	* 
	* @return  the number of entries successfully read.
	* @throws IOException if file cannot be accessed.
	*/
	public int read( ) throws IOException
	{
		entries = new Hashtable();
		parseFile(passwordFile);
		return entries.size();
	}

	/**
	* Writes the current entries out to the specified file.
	* This overwrites the previous contents of the file.
	* 
	* @throws IOException if file cannot be accessed.
	*/
	public void write( ) throws IOException
	{
		if (entries == null || entries.size() == 0)
			return;

		FileWriter fw = new FileWriter(passwordFile);
		Enumeration e = entries.elements();
		while(e.hasMoreElements())
			fw.write(e.nextElement().toString() + "\n");
		fw.flush();
		fw.close();
	}

	/**
	* Retrieves the entry for a specific user.
	* Return null if entry does not exist.
	* @param username the user name
	* @return the PasswordFileEntry or null if no match
	*/
	public PasswordFileEntry getEntryByName( String username )
	{
		if (entries == null)
			return null;
		return (PasswordFileEntry)entries.get(username);
	}

	/**
	* Removes an entry for a specific user.
	* Return true if user was present in the file, false if not.
	* @param username the user name
	* @return true if entry was deleted, false if no match.
	*/
	public boolean rmEntryByName( String username )
	{
		if (entries == null)
			return false;
		return entries.remove(username) == null ? false : true;
	}

	/**
	* Adds an entry to the file.
	* @param entry the new entry.
	*/
	public void addEntry( PasswordFileEntry entry )
	{
		if (entries == null)
			entries = new Hashtable();
		entries.put(entry.getUsername(), entry);
	}


	//=============================================================
	// Methods inherited from FileParser
	//=============================================================
	
	/**
	* Parses the file.
	* @param input the File
	* @return true if parse was successful
	* @throws IOException on IO error
	*/
	public boolean parseFile( File input ) throws IOException
	{
		exlist = new FileExceptionList(input);
   		LineNumberReader rdr = new LineNumberReader(new FileReader(input));
   		String ln;
   		while( (ln = rdr.readLine()) != null)
   		{
			// Skip blank and comment lines.
			ParsePosition pp = new ParsePosition(0);
			if (TextUtil.skipWhitespace(ln, pp) == false
			 || ln.charAt(pp.getIndex()) == '#')
				continue;

			PasswordFileEntry pfe = new PasswordFileEntry();
			try 
			{
				pfe.parseLine(ln); 
				entries.put(pfe.getUsername(), pfe);
			}
			catch (AuthException e)
			{
				exlist.add(new FileException(rdr.getLineNumber(),e));
			}
		}
		rdr.close();

		return true;
	}
	
	/**
	* @see FileParser
	*/
	public FileExceptionList getWarnings( )
	{
		return exlist;
	}
}

