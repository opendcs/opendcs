package ilex.util;

public interface ServerLock extends Runnable
{

    /**
    * @return the period (in seconds) at which the lock file is updated
    * with the current time.
    */
    int getLockUpdatePeriod();

    /**
    * Sets the period (in seconds) at which the lock file is updated
    * with the current time.
    * @param period period in seconds to update the lock
    */
    void setLockUpdatePeriod(int period);

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
    boolean obtainLock();

    /**
    * Obtain the lock and set a locable listener.
    * @param lb the lockable listener
    * @return true if success, false if lock is busy.
    * @see ServerLockable
    */
    boolean obtainLock(ServerLockable lb);

    /**
    * Deletes the lock file and stop periodic updates.
    */
    void releaseLock();

    /**
    * Adds a shutdown hook so that when this JVM terminates, the lock
    * will be released.
    */
    void releaseOnExit();

    /**
    * This method returns true if the lock is currently active.
    * It may be called by clients wishing to find out if a given server is
    * running.
    * @param checkTimeout if set, then return false if lock exists but has timed out.
    * @return true if file is locked.
    */
    boolean isLocked(boolean checkTimeout);

    /**
    * Deletes the server's lock file.
    * This may be called by clients wishing to signal STOP to the server.
    */
    void deleteLockFile();

    /**
    * @return
    */
    boolean wasShutdownViaLock();

    void setCritical(boolean critical);

    String getAppStatus();

    void setAppStatus(String appStatus);

    void setPID(int pid);

    long getLastLockMsec();

    int getFilePID();

}