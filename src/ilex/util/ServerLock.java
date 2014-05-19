/*
*  $Id$
*/
package ilex.util;

import java.io.*;

/**
* ServerLock is used to ensure that only one instance of a given server
* is running at a time, and to provide an easy mechanism to signal the server
* that it needs to terminate..
* This class creates a lock file with a name provided by the server.  Periodically
* it checks to make sure the lock file still exists. If not, the Server will
* terminate. If the lock file still exists, its contents is overwritten with
* the current time.
*/
public class ServerLock implements Runnable
{
	private File myLockFile;
	private int updatePeriod;
	private Thread updateThread;
	private boolean active;
	private boolean shutdownViaLock;
	private ServerLockable lockable;
	private long lastLockTime = 0L;
	private int filePseudoPID = 0;
	private int myPseudoPID = 0;

	/**
	* Creates a new ServerLock object with the specified file path.
	* 'lockFilePath' is the name of the file that will be used as the
	* lock. A good convention is to use a unique server name and to
	* place the files in /tmp (on unix systems).
	* @param lockFilePath name of the file used as the lock
	*/
	public ServerLock( String lockFilePath )
	{
		myLockFile = new File(lockFilePath);
		updatePeriod = 10; // default = 10 seconds
		updateThread = null;
		active = false;
		lockable = null;
		myPseudoPID = (int)(System.currentTimeMillis() / 1000L);
	}

	/**
	* @return the period (in seconds) at which the lock file is updated
	* with the current time.
	*/
	public int getLockUpdatePeriod( ) { return updatePeriod; }

	/**
	* Sets the period (in seconds) at which the lock file is updated
	* with the current time.
	* @param period period in seconds to update the lock
	*/
	public void setLockUpdatePeriod( int period ) 
	{ 
		if (period > 0) updatePeriod = period;
	}

	/**
	* Lock the file and return true if successful, false if the lock is
	* used by another process.
	* Open the lock file and read the time. If it is within the threshold
	* assume another process has the lock. Return false.
	* <p>
	* If the file doesn't exist or the time is beyond the threshold, then
	* start a thread to update the file periodically.
	* <p>
	* @return true if lock is obtained, false if lock is busy.
	* @throws IOException if the lock file could not be written.
	*/
	public boolean obtainLock( )
	{
		try
		{
			if (isLocked())
			{
				// Lock is in use by another instance!
				System.err.println("Failed to get lock '" 
					+ myLockFile.getName() + 
					"': This service already running?");
				return false;
			}
			
			// Either lock file didn't exist or it's time-stamp was too old.
			// Grab the lock & start a thread to periodically update it.
			updateLock();
			updateThread = new Thread(this);
			active = true;
			updateThread.start();
		}
		catch(IOException ex)
		{
			System.err.println("IOException while trying to get lock '"
				+ myLockFile.getPath() + "': " + ex);
			System.exit(1);
		}
		return true;
	}

	/**
	* Obtain the lock and set a locable listener.
	* @param lb the lockable listener
	* @return true if success, false if lock is busy.
	* @see ServerLockable
	*/
	public boolean obtainLock( ServerLockable lb )
	{
		lockable = lb;
		return obtainLock();
	}

	/**
	* Deletes the lock file and stop periodic updates.
	*/
	public void releaseLock( )
	{
		active = false;
		if (updateThread != null)
			updateThread.interrupt();
	}

	/**
	* Adds a shutdown hook so that when this JVM terminates, the lock
	* will be released.
	*/
	public void releaseOnExit( )
	{
		Runtime.getRuntime().addShutdownHook(
			new Thread()
			{
				public void run() { releaseLock(); }
			});
	}

	/**
	* @throws IOException
	*/
	private void updateLock( ) throws IOException
	{
		DataOutputStream outs = 
			new DataOutputStream(new FileOutputStream(myLockFile));
		outs.writeLong(System.currentTimeMillis() / (long)1000);
		outs.writeInt(myPseudoPID);
		outs.writeBytes("     ");
		outs.close();
	}

	/**
	* This method returns true if the lock is currently active.
	* It may be called by clients wishing to find out if a given server is
	* running.
	* @return true if file is locked.
	*/
	public boolean isLocked( )
	{
		if (myLockFile.canRead())
		{
			DataInputStream ins = null;
			try
			{
				ins = new DataInputStream(new FileInputStream(myLockFile));
				long t = ins.readLong();
				lastLockTime = t;
				filePseudoPID = ins.readInt();
				long now = System.currentTimeMillis() / (long)1000;
				ins.close();

				// MJM 20080505 - If I am updating the lock, don't check for
				// timeout, just that the lock exists and it is my PID.
				// We saw when a system got very busy, it didn't do its update
				// in time, and then exited.
				if (updateThread != null)
					return true;

				// Timeout applies to initial obtainLock, and when checking
				// the lock of some other process. In both cases, updateThread
				// has not yet been created.
				if (now <= t + (updatePeriod * 2) && now >= t)
					return true;
//Logger.instance().info("Lock file '" + myLockFile.getName() + "' out of date.");
			}
			catch(IOException ioe)
			{
				if (ins != null)
				{
					try { ins.close(); }
					catch(IOException ex) {}
					Logger.instance().info(
						"Lock file I/O Error '" + myLockFile.getName() 
						+ " -- Assuming lock file removed.");
				}
			}
		}
//else Logger.instance().info("Cannot read lock file '" + myLockFile.getName()+"'.");
		return false;
	}

	/**
	 * For monitors of locks, this method returns the millisecond value
	 * of the last time the lock was read by 'isLocked()'.
	 */
	public long getLastLockTime()
	{
		return lastLockTime * 1000L;
	}

	/**
	* Deletes the server's lock file.
	* This may be called by clients wishing to signal STOP to the server.
	*/
	public void deleteLockFile( )
	{
		if (myLockFile.exists())
			myLockFile.delete();
	}

	/**
	* Continually updates the lock file and sleeps the specified period.
	*/
	public void run( )
	{
		shutdownViaLock = false;
		while (active)
		{
			if (!isLocked())
			{
				active = false;
				shutdownViaLock = true;
			}
			else if (filePseudoPID != myPseudoPID)
			{
				active = false;
				shutdownViaLock = true;
				Logger.instance().info("Lock file PID change - "
					+ "Assuming another instance grabbed the lock.");
			}
			else
			{
				try { updateLock(); }
				catch (IOException ioe)
				{
					System.err.println("Error updating server lock file '"
						+ myLockFile + "': " + ioe);
				}
				try { Thread.sleep(updatePeriod * (long)1000); }
				catch(InterruptedException ie) {}
			}
		}
		if (!shutdownViaLock)
			myLockFile.delete();
		if (lockable != null)
			lockable.lockFileRemoved();
		else
		{
			Logger.instance().info("Exiting -- lock file '" 
				+ myLockFile.getPath() + "' removed.");
			System.exit(0);
		}
	}

	/**
	* Test main.
	* @param args the args
	* @throws IOException on lock IO error
	*/
	public static void main( String[] args ) throws IOException
	{
		if (args.length < 1)
		{
			System.err.println("Usage: ServerLock <filename>");
			System.exit(1);
		}

		ServerLock mylock = new ServerLock(args[0]);
		if (mylock.obtainLock() == false)
		{
			System.out.println("Lock '" + args[0] + "' is in use.");
			System.exit(0);
		}
		mylock.releaseOnExit();
		System.out.println("I have the lock '" + args[0] + "'.");
		for(int i=0; i<90; i++)
		{
			System.out.println("test " + i);
			try { Thread.sleep(1000L); }
			catch (InterruptedException e) {}
		}
		System.exit(0);
	}

	/**
	* @return
	*/
	public boolean wasShutdownViaLock( )
	{
		return shutdownViaLock;
	}
}
