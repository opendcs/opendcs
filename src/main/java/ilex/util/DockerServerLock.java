package ilex.util;


/**
 * Used in the docker environment, or anywhere else
 * where it is known that the lock file is neither
 * necessary nor helpful.
 */
public class DockerServerLock implements ServerLock
{
    String status;
    int pid = -1;

    @Override
    public void run()
    {
    }

    @Override
    public int getLockUpdatePeriod()
    {
        return -1;
    }

    @Override
    public void setLockUpdatePeriod(int period)
    {
        /* do nothing */    
    }

    @Override
    public boolean obtainLock()
    {
        return true;
    }

    @Override
    public boolean obtainLock(ServerLockable lb)
    {
        return true;
    }

    @Override
    public void releaseLock()
    {
        /* do nothing */
    }

    @Override
    public void releaseOnExit()
    {
        /* do nothing */
    }

    @Override
    public boolean isLocked(boolean checkTimeout)
    {
        return false;
    }

    @Override
    public void deleteLockFile()
    {
        /* do nothing */
    }

    @Override
    public boolean wasShutdownViaLock()
    {
        return true; // ?
    }

    @Override
    public void setCritical(boolean critical)
    {
        /* do nothing */
    }

    @Override
    public String getAppStatus()
    {
        return status;
    }

    @Override
    public void setAppStatus(String appStatus)
    {
       this.status = appStatus;
    }

    @Override
    public void setPID(int pid)
    {
        this.pid = pid;
    }

    @Override
    public long getLastLockMsec()
    {
        return 0;        
    }

    @Override
    public int getFilePID()
    {
        return pid;
    }
}
