package decodes.tsdb.test;

import ilex.util.Logger;

import java.util.List;

import opendcs.dai.LoadingAppDAI;

import decodes.tsdb.*;
import decodes.util.DecodesException;
import decodes.db.Constants;

public class ReleaseLock 
	extends TsdbAppTemplate
{
	public ReleaseLock()
	{
		super(null);
	}

	protected void runApp()
		throws Exception
	{
		if (appId == Constants.undefinedId)
		{
			System.err.println(
				"-a <appName> argument required -- No action taken!");
			return;
		}
		// Note, the -a arg will have us connect to the database as the
		// desired application.
		LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
		try
		{
			List<TsdbCompLock> locks = loadingAppDAO.getAllCompProcLocks();
			Logger.instance().info("" + locks.size() + " Locks Retrieved.");
			for(TsdbCompLock lock : locks)
				if (lock.getAppId() == appId)
				{
					loadingAppDAO.releaseCompProcLock(lock);
					break;
				}
		}
		finally
		{
			loadingAppDAO.close();
		}
	}
	
	public void initDecodes()
		throws DecodesException
	{
	}
	
	public static void main(String args[])
		throws Exception
	{
		ReleaseLock tp = new ReleaseLock();
		tp.execute(args);
	}
}
