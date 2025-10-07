/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package ilex.util;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipFile;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.zip.ZipEntry;

/**
A collection of utilities to manipulate files.
*/
public final class FileUtil
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		int total = 0;
		try (FileOutputStream fos = new FileOutputStream(to);
			 FileInputStream fis = new FileInputStream(from);)
		{
			byte buf[] = new byte[4096];
			int len;
			while((len = fis.read(buf)) > 0)
			{
				total += len;
				fos.write(buf, 0, len);
			}
		}
		return total;
	}

	/**
	 * Retrieve contents of given file as a String.
	 *
	 * @param f The file desired.
	 * @return contents if it can be read.
	 * @throws IOException
	 */
	public static String getFileContents(File f)
		throws IOException
	{
		StringBuilder sb = new StringBuilder();
		try(FileReader fr = new FileReader(f);)
		{
			int c;
			while((c = fr.read()) != -1)
			{
				sb.append((char)c);
			}
			return sb.toString();
		}

	}

	public static byte[] getfileBytes(File f)
		throws IOException
	{
		byte ret[] = new byte[(int)f.length()];
		try( BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f)))
		{
			int b = 0;
			for(int idx = 0; idx < ret.length && (b = bis.read()) != -1; idx++)
			{
				ret[idx] = (byte)b;
			}
		}
		return ret;
	}

	/**
	  Copies an input stream to an output stream.
	  Both streams are closed after the transfer is complete.
	  TODO: update all usages to handle stream closing and remove from this block.
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
			if (len > 0)
			{
				out.write(buffer, 0, len);
				total += len;
			}
			else // read returned 0, pause before trying again
			{
				try { Thread.sleep(100L); } catch(InterruptedException ex) {}
			}
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

		try (ZipFile zipfile = new ZipFile(zipname))
		{
			int numFiles = 0;
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
			log.atError().setCause(ex).log("Unzip failed.");
			if (monitor != null)
			{
				monitor.zipFailed(ex);
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
