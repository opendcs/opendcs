/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:32  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/12/20 00:32:51  mjmaloney
*  Implemented TimeoutInputStream.
*
*/
package ilex.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
This class extends BufferedInputStream by adding a timeout capability.
The read methods will block up to the specified amount of time waiting
for the requested number of bytes.
*/
public class TimeoutInputStream extends BufferedInputStream
{
	/** Timeout limit set by constructor or setTimeoutMsec(). Default=5000. */
	protected long timeoutmsec;

	/** Sleeps this amount of time in a loop waiting for data. Default = 50. */
	protected long sleepPeriod;

	private boolean isClosed;

	/**
	* Constructs stream with default timeout period == 5000L (5 sec).
	* @param in underlying input stream
	* @param bufsize buffer size
	*/
	public TimeoutInputStream( InputStream in, int bufsize )
	{
		this(in, bufsize, 5000L);
	}

	/**
	* @param in underlying input stream
	* @param bufsize buffer size
	* @param timeoutmsec the timeout in msec
	*/
	public TimeoutInputStream( InputStream in, int bufsize, long timeoutmsec )
	{
		super(in, bufsize);
		this.timeoutmsec = timeoutmsec;
		this.sleepPeriod = 50;
		isClosed = false;
	}

	/**
	* @return timeout in msec
	*/
	public long getTimeoutMsec( ) { return timeoutmsec; }

	/**
	* Sets the timeout.
	* @param ms the timeout in msec
	*/
	public void setTimeoutMsec( long ms ) { timeoutmsec = ms; }

	/**
	* @return the sleep period in msec
	*/
	public long getSleepPeriod( ) { return sleepPeriod; }

	/**
	* Sets the sleep period (time to wait between IO attempts).
	* @param sp the period in msec.
	*/
	public void setSleepPeriod( long sp ) { sleepPeriod = sp; }

	/**
	* Follows general contract for read method but adds timeout functionality.
	*/
	public int read( ) throws IOException
	{
		byte b[] = new byte[0];
		int r = read(b, 0, 1);
		return (int)b[0];
	}

	/**
	* Follows general contract for read method, but if requested number of
	* bytes is not available within specified timeout period,
	* throws IOException.
	*/
	public int read( byte[] b, int off, int len ) throws IOException
	{
		long start = System.currentTimeMillis();
		while(available() < len 
		   && System.currentTimeMillis() - start < timeoutmsec
		   && !isClosed)
		{
			try { Thread.sleep(sleepPeriod); }
			catch(InterruptedException ex) {}
		}
		if (isClosed)
			throw new IOException("Read failed because stream closed.");

		int avail = available();
		if (avail >= len)
			return super.read(b, off, len);
		else
			throw new IOException("Read operation timed out waiting for "
				+ len + " bytes, avail=" + avail + ".");
	}

	/**
	* Follows general contract for close()
	*/
	public void close( ) throws IOException
	{
		isClosed = true;
		super.close();
	}
}
