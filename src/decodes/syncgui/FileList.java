package decodes.syncgui;

import java.util.Vector;
import java.util.Iterator;

/**
 * Holds list of files of a particular type in the database.
 */
public class FileList
{
	/**
	 * The owning database
	 */
	DistrictDBSnap myDB;

	/**
	 * The type of files held herein (e.g. "config", "site").
	 */
	String fileType;

	/** The file names */
	private Vector fileNames;

	/**
	 * Constructor
	 * @param myDB my database snapshot
	 * @param fileType the type of files, i.e. the directory name
	 */
	public FileList( DistrictDBSnap myDB, String fileType )
	{
		this.myDB = myDB;
		this.fileType = fileType;
		fileNames = new Vector();
	}

	/** @return the snapshot holding this file list. */
	public DistrictDBSnap getSnap()
	{
		return myDB;
	}


	/** Clears the vector. */
	public void clear()
	{
		fileNames.clear();
	}

	/** @return # of elements in the list. */
	public int size()
	{
		return fileNames.size();
	}

	/**
	 * Adds a file name to this list.
	 * @param name the file name
	 */
	public void addName(String name)
	{
		fileNames.add(name);
	}

	/**
	 * @return the file type
	 */
	public String toString( ) { return fileType; }

	/** @return Vector containing the file names */
	public Vector getFileNames() { return fileNames; }

	/** Dumps the list to stdout for testing. */
	public void dump()
	{
		System.out.println("\t\tFiles of type '" + fileType + "'");
		for(Iterator it = fileNames.iterator(); it.hasNext(); )
		{
			String fn = (String)it.next();
			System.out.println("\t\t\t" + fn);
		}
	}
	
}
