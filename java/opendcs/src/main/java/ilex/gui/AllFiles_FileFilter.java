/*
*  $Id$
*
*  $Source$
*
*  $State
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:18  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/04/27 11:56:01  mike
*  ExtensionFileFilter should return true for directories to allow navigation.
*  This is a problem only on some platforms.
*  Created the "All Files" filter.
*
*/
package ilex.gui;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
File filter that does not filtering.
*/
public class AllFiles_FileFilter extends FileFilter
{
	/** Default constructor. */
	public AllFiles_FileFilter( )
	{
		super();
	}
	
	/**
	* @param f the File to check
	* @return true
	*/
	public boolean accept( File f )
	{
		return true;
	}
	
	/**
	* @return description of this filter
	*/
	public String getDescription( )
	{
		return "All files";
	}
}
