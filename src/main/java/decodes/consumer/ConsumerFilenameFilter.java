/*
*  $Id$
*
*  $State$
*
*/
package decodes.consumer;

import java.io.File;
import java.io.FilenameFilter;

/**
  ConsumerFilenameFilter filters files according to a filename template.
  If the template is a prefix of the filename, the file is accepted.
*/
public class ConsumerFilenameFilter implements FilenameFilter
{
	private String fileTemplate;

	/** default constructor */
	public ConsumerFilenameFilter()
	{
		super();
		fileTemplate = null;
	}

    /** 
	  Constructs filter given a filename template.
	  @param t the template
	*/
	public ConsumerFilenameFilter(String t)
	{
		super();
		fileTemplate = t;
	}	

	/**
	  Determines whether the passed file should be processed.
	  All files for which the template is a prefix of the filename are accepted.
	  @param dir the directory containing the file
	  @param name the name of the file
	  @return true if the file should be processed.
	*/
	public boolean accept(File dir, String name )
	{
           if ( fileTemplate == null )
             return true;
           else 
             return( fileTemplate.regionMatches(0, name, 0, fileTemplate.length() ) );

	}
}

