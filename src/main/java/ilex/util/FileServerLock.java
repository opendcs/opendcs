/*
*  $Id$
*/
package ilex.util;

import java.io.*;
import java.util.Date;

/**
* ServerLock is used to ensure that only one instance of a given server
* is running at a time, and to provide an easy mechanism to signal the server
* that it needs to terminate..
* This class creates a lock file with a name provided by the server.  Periodically
* it checks to make sure the lock file still exists. If not, the Server will
* terminate. If the lock file still exists, its contents is overwritten with
* the current time.
*/
public class FileServerLock implements ServerLock
{
    private File myLockFile;
    private int updateSeconds;
    private Thread updateThread;
    private boolean active;
    private boolean shutdownViaLock;
    private ServerLockable lockable;
    private long lastLockMsec = 0L;
    private int filePID = 0;
    private int myPID = 0;
    private boolean critical = true;
    private String appStatus = "";
    private static boolean isWindowsService = false;
    private int numConsecutiveFailures = 0;

    /**
    * Creates a new ServerLock object with the specified file path.
    * 'lockFilePath' is the name of the file that will be used as the
    * lock. A good convention is to use a unique server name and to
    * place the files in /tmp (on unix systems).
    * @param lockFilePath name of the file used as the lock
    */
    public FileServerLock( String lockFilePath )
    {
        myLockFile = new File(lockFilePath);
        updateSeconds = 10; // default = 10 seconds
        updateThread = null;
        active = false;
        lockable = null;
        myPID = determinePID();
        if (myPID == -1)
            myPID = (int)(System.currentTimeMillis() / 1000L);
    }

    /**
    * @return the period (in seconds) at which the lock file is updated
    * with the current time.
    */
    @Override
    public int getLockUpdatePeriod( ) { return updateSeconds; }

    /**
    * Sets the period (in seconds) at which the lock file is updated
    * with the current time.
    * @param period period in seconds to update the lock
    */
    @Override
    public void setLockUpdatePeriod( int period )
    {
        if (period > 0) updateSeconds = period;
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
    */
    @Override
    public boolean obtainLock( )
    {
        try
        {
            if (isLocked(true) && !isWindowsService)
            {
                // Lock is in use by another instance!
                if (critical)
                    System.err.println("Failed to get lock '"
                        + myLockFile.getName() +
                        "': This service already running?");
                else
                    Logger.instance().info("Non-critical lock '" + myLockFile.getName()
                        + "' is already used by another process.");
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
            if (critical)
                System.exit(1);
            else
                return isWindowsService ? true : false;
        }
        return true;
    }

    /**
    * Obtain the lock and set a locable listener.
    * @param lb the lockable listener
    * @return true if success, false if lock is busy.
    * @see ServerLockable
    */
    @Override
    public boolean obtainLock( ServerLockable lb )
    {
        lockable = lb;
        return obtainLock();
    }

    /**
    * Deletes the lock file and stop periodic updates.
    */
    @Override
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
    @Override
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
        DataOutputStream outs = null;
        try
        {
            outs = new DataOutputStream(new FileOutputStream(myLockFile));
            outs.writeLong(lastLockMsec = System.currentTimeMillis());
            outs.writeInt(myPID);
            outs.writeUTF(appStatus);
            outs.close();
        }
        finally
        {
            if (outs != null) try { outs.close(); } catch(Exception ex) {}
        }
    }

    /**
    * This method returns true if the lock is currently active.
    * It may be called by clients wishing to find out if a given server is
    * running.
    * @param checkTimeout if set, then return false if lock exists but has timed out.
    * @return true if file is locked.
    */
    @Override
    public boolean isLocked(boolean checkTimeout)
    {
        if (myLockFile.canRead())
        {
            DataInputStream ins = null;
            try
            {
                ins = new DataInputStream(new FileInputStream(myLockFile));
                lastLockMsec = ins.readLong();
                filePID = ins.readInt();
                try { appStatus = ins.readUTF(); }
                catch(Exception ex) { appStatus = ""; }
                long now = System.currentTimeMillis();

                // MJM 20080505 - If I am updating the lock, don't check for
                // timeout, just that the lock exists and it is my PID.
                // We saw when a system got very busy, it didn't do its update
                // in time, and then exited.
                if (!checkTimeout)
                {
                    numConsecutiveFailures = 0;
                    return true;
                }

                // Timeout applies to initial obtainLock, and when checking
                // the lock of some other process.
                if (now <= lastLockMsec + (updateSeconds * 2000L) && now >= lastLockMsec)
                {
                    numConsecutiveFailures = 0;
                    return true;
                }
            }
            catch(IOException ioe)
            {
                Logger.instance().info(
                        "isLocked() Lock file I/O Error '" + myLockFile.getName() + ": " + ioe);
            }
            finally
            {
                try { ins.close(); }
                catch(IOException ex) {}
            }
        }
        else
        {
            Logger.instance().info("Lock file '" + myLockFile.getPath() + "' does not exist or is not readable.");
            return false;
        }

        // Getting to here means that the lock check failed.
        if (isWindowsService)
            return true;
        else if (isWindows() && ++numConsecutiveFailures < 3)
            return true;
        return false;
    }

    /**
    * Deletes the server's lock file.
    * This may be called by clients wishing to signal STOP to the server.
    */
    @Override
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
            if (!isLocked(false))
            {
                active = false;
                shutdownViaLock = true;
            }
            else if (filePID != myPID)
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
                try { Thread.sleep(updateSeconds * (long)1000); }
                catch(InterruptedException ie) {}
            }
        }

        if (!shutdownViaLock)
            myLockFile.delete();
        if (lockable != null)
            lockable.lockFileRemoved();
        else
        {
            Logger.instance().info((critical ? "Exiting -- " : "")
                + "Lock file '"
                + myLockFile.getPath() + "' removed.");
            if (critical)
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

        FileServerLock mylock = new FileServerLock(args[0]);
        boolean t = mylock.isLocked(true);
        System.out.println("locked=" + t);
        System.out.println("lock msec=" + mylock.lastLockMsec + ", or "
            + new Date(mylock.lastLockMsec));

//        if (mylock.obtainLock() == false)
//        {
//            System.out.println("Lock '" + args[0] + "' is in use.");
//            System.out.println("lock msec=" + mylock.lastLockMsec + ", or "
//                + new Date(mylock.lastLockMsec));
//            System.exit(0);
//        }
//
//        if (mylock.lastLockMsec > 0L)
//        {
//            System.out.println("Lock file exists but is too old.");
//            System.out.println("lock msec=" + mylock.lastLockMsec + ", or "
//                + new Date(mylock.lastLockMsec));
//        }
//        mylock.releaseOnExit();
//        System.out.println("I have the lock '" + args[0] + "'.");
//        for(int i=0; i<90; i++)
//        {
//            System.out.println("test " + i);
//            try { Thread.sleep(1000L); }
//            catch (InterruptedException e) {}
//        }
        System.exit(0);
    }

    /**
    * @return
    */
    @Override
    public boolean wasShutdownViaLock( )
    {
        return shutdownViaLock;
    }

    @Override
    public void setCritical(boolean critical)
    {
        this.critical = critical;
    }

    public static int determinePID()
    {
        String pids = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        if (pids != null)
        {
            // String will be of the form 12345@username
            int idx = pids.indexOf('@');
            if (idx > 0)
            {
                try { return Integer.parseInt(pids.substring(0, idx)); }
                catch(Exception ex) {}
            }
        }
        return -1;
    }

    @Override
    public String getAppStatus()
    {
        return appStatus;
    }

    @Override
    public void setAppStatus(String appStatus)
    {
        if (appStatus == null)
            appStatus = "";
        this.appStatus = appStatus;
    }

    @Override
    public void setPID(int pid)
    {
        this.myPID = pid;
    }

    @Override
    public long getLastLockMsec()
    {
        return lastLockMsec;
    }

    @Override
    public int getFilePID()
    {
        return filePID;
    }

    public static void setWindowsService(boolean winsvc)
    {
        isWindowsService = winsvc;
    }

    public static boolean isWindows()
    {
        String osname = System.getProperty("os.name");
        return osname.toLowerCase().startsWith("win");
    }

}
