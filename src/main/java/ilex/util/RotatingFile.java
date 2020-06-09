/*
* $Id$
*/
package ilex.util;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;

/**
* This class used for log file & summary files. It allows a file to grow to a fixed
* size. Upon reaching the limit, the file is renamed with a ".old" extension and a
* new file is created. File locking is used to allow output from multiple JVMs.
*/
public class RotatingFile
{
	private long sizeLimit;
	private File file;
	private FileOutputStream stream = null;
	private FileChannel channel = null;

	/**
	 * Opens the file for appending output.
	 * @param path the full name of the file.
	 * @param sizeLimit rotation happens when this limit is reached.
	 */
	public RotatingFile(String path, long sizeLimit)
		throws IOException
	{
		this.sizeLimit = sizeLimit;
		file = new File(path);
		stream = new FileOutputStream(file, true);
		channel = stream.getChannel();
	}

	/**
	 * Write a string to the file.
	 * @param str the String
	 */
	public void write(String str)
		throws IOException
	{
		FileLock flock = null;
		try
		{
			flock = channel.tryLock();
			if (file.length() >= sizeLimit)
			{
				File oldFile = new File(file.getPath() + ".old");
				if (oldFile.exists())
					oldFile.delete();
				file.renameTo(oldFile);
				FileOutputStream newStream = new FileOutputStream(file, false);
				FileChannel newChannel = newStream.getChannel();
				FileLock newLock = newChannel.tryLock();
				try { flock.release(); } catch(Exception ex0) {}
				try { stream.close(); } catch(Exception ex1) {}
				try { channel.close(); } catch(Exception ex2) {}
				stream = newStream;
				channel = newChannel;
				flock = newLock;
			}
			stream.write(str.getBytes());
		}
		catch(IOException ex)
		{
			Logger.instance().warning("IO Error writing to '" + file.getPath()
				+ "': " + ex);
			throw ex;
		}
		finally
		{
			if (flock != null)
				try { flock.release(); } catch(Exception ex) {}
		}
	}

	/**
	 * Release all resources associated with this file.
	 */
	public void close()
	{
		try { stream.close(); } catch(Exception ex1) {}
		try { channel.close(); } catch(Exception ex2) {}
	}

	/**
	 * @return the path of the file.
	 */
	public String getPath()
	{
		return file.getPath();
	}

	/**
	 * Test main that continually writes dates to the file to demonstrate roll-over.
	 * @param args call with filename idstring
	 */
	public static void main(String[] args)
		throws IOException
	{
		RotatingFile rf = new RotatingFile(args[0], 2000000);
		while(true)
		{
			rf.write(args[1] + " " + (new java.util.Date()) + "\n");
			try { Thread.sleep(10L); } catch(InterruptedException ex){}
		}
	}
}
