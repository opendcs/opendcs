/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2014/10/07 12:49:20  mmaloney
*  added getFileBytes
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.3  2011/01/14 21:03:03  sparab
*  *** empty log message ***
*
*  Revision 1.2  2010/11/15 21:30:51  sparab
*  Added copyStream(InputStream in, OutputStream out,int timeout) for copying streams till a timeout is reached
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2008/01/14 14:57:46  mmaloney
*  dev
*
*  Revision 1.6  2006/07/19 21:29:59  mmaloney
*  dev
*
*  Revision 1.5  2005/12/10 21:43:57  mmaloney
*  dev
*
*  Revision 1.4  2005/10/10 19:47:47  mmaloney
*  dev
*
*  Revision 1.3  2004/11/14 21:51:28  mjmaloney
*  dos2unix
*
*  Revision 1.2  2004/11/10 16:27:10  mjmaloney
*  Added unzip capability
*
*  Revision 1.1  2004/08/26 14:14:37  mjmaloney
*  Added FileUtil
*
*/
package ilex.util;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
A collection of utilities to manipulate files.
*/
public class FileUtil
{
	/**
	  Copies a file.
	  @param from the file being copied
	  @param to the file we're copying to
	  @return number of bytes copied
	  @throws IOException on error.
	*/
	public static int copyFile(File from, File to)
		throws IOException
	{
		if (from.equals(to))
			return 0;

		FileOutputStream fos = null;
		FileInputStream fis = null;
		int total = 0;
		try
		{
			fos = new FileOutputStream(to);
			fis = new FileInputStream(from);
			byte buf[] = new byte[4096];
			int len;
			while((len = fis.read(buf)) > 0)
			{
				total += len;
				fos.write(buf, 0, len);
			}
			fos.close();
			fis.close();
		}
		finally
		{
			if (fis != null)
			{
				try { fis.close(); }
				catch(IOException ex) {}
			}
			if (fos != null)
			{
				try { fos.close(); }
				catch(IOException ex) {}
			}
		}
		return total;
	}

	public static String getFileContents(File f)
		throws IOException
	{
		StringBuilder sb = new StringBuilder();
		FileReader fr = new FileReader(f);
		int c;
		while((c = fr.read()) != -1)
			sb.append((char)c);
		fr.close();
		return sb.toString();
	}
	
	public static byte[] getfileBytes(File f)
		throws IOException
	{
		byte ret[] = new byte[(int)f.length()];
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
		int b = 0;
		for(int idx = 0; idx < ret.length && (b = bis.read()) != -1; idx++)
			ret[idx] = (byte)b;
		bis.close();
		return ret;
	}

	/**
	  Copies an input stream to an output stream.
	  Both streams are closed after the transfer is complete.
	  @param in the intput stream
	  @param out the output stream
	*/
	public static void copyStream(InputStream in, OutputStream out)
		throws IOException
	{
		byte buffer[] = new byte[4096];
		int len;
		int total = 0;
		while((len = in.read(buffer)) >= 0)
		{
			out.write(buffer, 0, len);
			total += len;
		}
		in.close();
		out.close();
	}

	/**
	  Copies an input stream to an output stream until timeout or 
	  the End of stream is reached.
	  I/O streams are not closed after the copying completes.
	  @param in the intput stream
	  @param out the output stream
	  @param timeout time in milliseconds to timeout 
	*/
	public static void copyStream(InputStream in, OutputStream out,long timeout)
		throws IOException
	{
		byte buffer[] = new byte[4096];
		int len;
		int total = 0;
		long start_time = System.currentTimeMillis();
		long elapsed = 0L;
		
		while((len = in.read(buffer)) >= 0 && elapsed < timeout)
		{
			out.write(buffer, 0, len);
			total += len;
			elapsed = System.currentTimeMillis() - start_time;
		}
	}
	

	/**
	  Moves a file.
	  @param from the file being moved
	  @param to the file we're moving to
	  @return number of bytes moved
	  @throws IOException on error.
	*/
	public static int moveFile(File from, File to)
		throws IOException
	{
		if (from.renameTo(to))
			return (int)to.length();
		int ret = copyFile(from, to);
		from.delete();
		return ret;
	}

	/**
	  Deletes a non-empty directory by recursively deleting the contents.
	  @param f the File object representing the file or directory to be deleted.
	  @return true if directory was successfully deleted.
	*/
	public static boolean deleteDir(File f) 
	{
		if (f.isDirectory()) 
		{
			String[] children = f.list();
			for (int i=0; i<children.length; i++) 
			{
				boolean success = deleteDir(new File(f, children[i]));
				if (!success)
					return false;
			}
		}
		// We now have either a regular file or an empty directory. Delete it
		return f.delete();
	}

	/**
	  Unzips a file into a target directory.
	  @param zipname the name of the zip file
	  @param targetdir the target directory
	  @param monitor if not null, call back with status.
	*/
	public static void unzip(String zipname, String targetdir, 
		ZipMonitor monitor)
	{
		if (monitor != null)
			monitor.setZipStatus("Opening " + zipname);
		ZipFile zipfile = null;
		try
		{
			int numFiles = 0;
			zipfile = new ZipFile(zipname);
			if (monitor != null)
				monitor.setNumZipEntries(zipfile.size());
			for(Enumeration entries = zipfile.entries();
				entries.hasMoreElements(); )
			{
				ZipEntry entry = (ZipEntry)entries.nextElement();
				String name = entry.getName();
				File file = new File(targetdir + File.separator + name);
				if (monitor != null)
					monitor.setZipStatus("Unzipping " + name);
				if (entry.isDirectory())
					file.mkdirs();
				else
				{
					copyStream(zipfile.getInputStream(entry),
						new FileOutputStream(file));
				}
				if (monitor != null)
					monitor.setZipProgress(++numFiles);
			}
			if (monitor != null)
				monitor.zipComplete();
		}
		catch(IOException ex)
		{
			if (monitor != null)
				monitor.zipFailed(ex);
			else
				System.err.println("Unzip failed: " + ex);
		}
		finally
		{
			if (zipfile != null)
			{
				try { zipfile.close(); }
				catch(IOException ex) {}
			}
		}
	}


	// test main
	public static void main(String args[])
		throws IOException
	{
		if (args.length != 3)
		{
			System.err.println("Usage java classname [c|m] from to");
			System.exit(1);
		}
		File from = new File(args[1]);
		File to = new File(args[2]);
		int ret = 0;
		if (args[0].charAt(0) == 'c')
			ret = copyFile(from, to);
		else if (args[0].charAt(0) == 'm')
			ret = moveFile(from, to);
		else
		{
			System.err.println("Usage java classname [c|m] from to");
			System.exit(1);
		}
		System.out.println("Done, file size = " + ret + " bytes.");
	}
}
