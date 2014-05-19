/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.xml;

import java.io.*;

/**
Throw-away Utility to fix degree symbols.
Old XML writer didn't correctly write UTF for degree symbol.
This rendered the XML file unreadable.
This utility starts at the edit-db, recursively finds all xml files,
filters them by fixing any incorrect degree-symbols it finds.
*/
public class DegFixer
{
	public static void main(String args[])
		throws IOException
	{
		InputStream is = new FileInputStream(args[0]);
		OutputStream os = new FileOutputStream(args[1]);
		int c;
		while((c = is.read()) != -1)
		{
			if (c == 176) // degree symbol
				os.write(194); // preceed with 194 to make it valid UTF-8
			os.write(c);
		}
		is.close();
		os.close();
	}
}
