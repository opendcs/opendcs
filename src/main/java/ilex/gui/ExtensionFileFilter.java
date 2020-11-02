/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 14:50:19  mjmaloney
*  Javadocs
*
*  Revision 1.3  2003/06/17 15:25:06  mjmaloney
*  Updated library versions to 3.4
*
*  Revision 1.2  2000/04/27 11:56:01  mike
*  ExtensionFileFilter should return true for directories to allow navigation.
*  This is a problem only on some platforms.
*  Created the "All Files" filter.
*
*  Revision 1.1  2000/04/04 19:45:11  mike
*  created
*
*/
package ilex.gui;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
Filters files based on one or more string file extensions.
That is, only files with names ending with one of the extensions will
be returned.
*/
public class ExtensionFileFilter extends FileFilter
{
	/** the extensions to pass */
	String[] extensions;
	/** 
	  Description of this filter. Example, extensions ".txt", and ".text"
	  might have the description "Text Files".
	*/
	String description;
	
	/**
	* Constructor for multiple extensions.
	* @param extensions the extensions.
	* @param description the description
	*/
	public ExtensionFileFilter( String[] extensions, String description )
	{
		super();
		this.extensions = extensions;
		this.description = description;
	}

	/**
	* Constructor for a single extension.
	* @param extension the extension
	* @param description the description
	*/
	public ExtensionFileFilter( String extension, String description )
	{
		super();
		this.extensions = new String[1];
		this.extensions[0] = extension;
		this.description = description;
	}
	
	/**
	* Typically called from the JFileChooser, returns true if the passed
	* file passes the filter.
	* @param f the file to check
	* @return true if passes.
	*/
	public boolean accept( File f )
	{
		// Always allow directories so that user can navigate.
		if (f.isDirectory())
			return true;
			
		String nm = f.getName();
		for(int i=0; i<extensions.length; i++)
			if (nm.endsWith(extensions[i]))
				return true;
		return false;
	}
	
	/**
	* @return the description for this filter.
	*/
	public String getDescription( )
	{
		StringBuffer ret = new StringBuffer(description + " (");
		for(int i=0; i<extensions.length; i++)
		{
			ret.append("*."+extensions[i]);
			if (i < extensions.length - 1)
				ret.append(", ");
		}
		ret.append(')');
		return ret.toString();
	}
}
