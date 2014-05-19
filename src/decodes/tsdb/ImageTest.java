/*
* $Id$
*
* $Log$
* Revision 1.3  2012/12/26 17:32:02  shweta
* changed long to int at ps.setBinaryStream method as postgres jar giving error that method is not implemented with long
*
* Revision 1.2  2012/12/21 20:29:45  mmaloney
* dev
*
* Revision 1.1  2012/12/21 20:22:07  mmaloney
* Created.
*
*/
package decodes.tsdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import lrgs.gui.DecodesInterface;

import ilex.cmdline.*;
import ilex.util.Logger;

import decodes.db.Constants;
import decodes.util.CmdLineArgs;


/**
 * Usage:
 * 	decj decodes.tsdb.ImageTest [-w] -f ImageFileName -n ImageName
*/
public class ImageTest extends TsdbAppTemplate
{
	private StringToken imageFileArg = new StringToken("f", "image-file",
		"", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	private StringToken imageNameArg = new StringToken("n", "image-name",
		"", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	private BooleanToken writeArg = new BooleanToken("w", 
		"write the image (default = read)",
		"", TokenOptions.optSwitch, false);;


//	private StringToken grpNameArg;

	public ImageTest()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(writeArg);
		cmdLineArgs.addToken(imageFileArg);
		cmdLineArgs.addToken(imageNameArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new ImageTest();
		DecodesInterface.silent = true;
		tp.execute(args);
	}

	protected void runApp()
		throws Exception
	{
		// Table defined as:
		// CREATE TABLE images (imgname text, img bytea);
		
		if (writeArg.getValue())
		{
			// Get file and write it to database table
			File file = new File(imageFileArg.getValue());
			FileInputStream fis = new FileInputStream(file);
			PreparedStatement ps = theDb.getConnection().prepareStatement(
				"INSERT INTO images VALUES (?, ?)");
			ps.setString(1, imageNameArg.getValue());
			ps.setBinaryStream(2, fis, (int)file.length());
			ps.executeUpdate();
			ps.close();
			fis.close();
		}
		else
		{
			// Read the image from the database and write it to the file.
			PreparedStatement ps = theDb.getConnection().prepareStatement(
				"SELECT img FROM images WHERE imgname=?");
			ps.setString(1, imageNameArg.getValue());
			ResultSet rs = ps.executeQuery();
			if (rs != null)
			{
			    if (rs.next())
			    {
			        byte[] imgBytes = rs.getBytes(1);
			        FileOutputStream fos = new FileOutputStream(
			        	imageFileArg.getValue());
			        fos.write(imgBytes);
			        fos.close();
			    }
			    rs.close();
			}
			ps.close();
		}
	}
}
