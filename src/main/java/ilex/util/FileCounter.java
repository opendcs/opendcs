/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2007/08/08 17:46:38  mmaloney
*  *** empty log message ***
*
*  Revision 1.5  2004/08/30 15:44:00  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.4  2004/08/30 14:50:26  mjmaloney
*  Javadocs
*
*  Revision 1.3  2001/06/30 20:49:01  mike
*  dev
*
*  Revision 1.2  2001/06/16 20:22:54  mike
*  dev
*
*  Revision 1.1  2001/06/13 02:01:41  mike
*  dev
*
*
*/
package ilex.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
* Implements a file-based persistent counter for returning unique IDs.
*/
public class FileCounter implements Counter
{
	File file;

	/**
	* Constructor.
	* @param filename the file containing the integer value.
	* @throws IOException if can't read or write.
	*/
	public FileCounter( String filename ) throws IOException
	{
		file = new File(filename);

		// The file stores the next value to be returned.
		if (!file.exists())
			writeValue(1);
	}

	/**
	* @return the next integer value and increments the counter,
	* or -1 if IO error.
	*/
	public int getNextValue( )
	{
		int v;
		try
		{
			v = readValue();
			setNextValue(v+1);
		}
		catch(Exception e)
		{
			Logger.instance().log(Logger.E_FAILURE, 
				"Cannot read file counter '" + file.getPath() + "': " + e);
			v = -1;
		}
		return v;
	}

	/**
	* Sets the counter so that the next call to getNextValue() will return
	* the passed integer.
	* @param value next integer value.
	*/
	public void setNextValue( int value )
	{
		try
		{
			writeValue(value);
		}
		catch(Exception e)
		{
			Logger.instance().log(Logger.E_FAILURE, 
				"Cannot increment file counter '" + file.getPath() + "': " + e);
		}
	}

	/**
	* @param value
	* @throws IOException
	*/
	private void writeValue( int value ) throws IOException
	{
		FileWriter fos = new FileWriter(file);
		String str = "" + value;
		fos.write(str, 0, str.length());
		fos.close();
	}

	/**
	* @return @throws IOException
	* @throws NumberFormatException
	*/
	private int readValue( ) throws IOException, NumberFormatException
	{
		FileReader fr = new FileReader(file);
		char buf[] = new char[20];
		int len = fr.read(buf, 0, 20);
		fr.close();
		String str = new String(buf, 0, len).trim();
		return Integer.parseInt(str);
	}

	/**
	* Main is a simple utility for testing & manipulating File Counter.
	* Two or three arguments must be supplied on the command line:
	* <ul>
	* <li>g filename - Get next value from file & increment</li>
	* <li>s filename value - Set next value of file to value</li>
	* </ul
	* @param args
	* @throws IOException
	*/
	public static void main( String[] args ) throws IOException
	{
		if (args[0].equals("g") && args.length == 2)
		{
			FileCounter fc = new FileCounter(args[1]);
			System.out.println("" + fc.getNextValue());
		}
		else if (args[0].equals("s") && args.length == 3)
		{
			FileCounter fc = new FileCounter(args[1]);
			int i = Integer.parseInt(args[2]);
			fc.setNextValue(i);
		}
		else
			System.err.println("Usage: FileCounter [g|s] filename [value]");
	}

}
