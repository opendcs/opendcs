package decodes.tsdb.test;

import java.util.List;

import opendcs.dai.LoadingAppDAI;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;

public class ListLocks extends TestProg
{
	public ListLocks()
	{
		super(null);
	}

	protected void runTest()
		throws Exception
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		List<TsdbCompLock> locks = loadingAppDao.getAllCompProcLocks();
		System.out.println("" + locks.size() + " Locks Retrieved:");

		for(TsdbCompLock lock : locks)
		{
			String appName = "";
			try
			{
				CompAppInfo cai = loadingAppDao.getComputationApp(lock.getAppId());
				appName = cai.getAppName();
			}
			catch(Exception ex)
			{
				System.err.println("Cannot get app info for appId=" + lock.getAppId()
					+ ": " + ex);
			}
			System.out.println("" + lock + 
				(appName != null ? ", name=" + appName : ""));
		}
		loadingAppDao.close();
	}

	public static void main(String args[])
		throws Exception
	{
		TestProg tp = new ListLocks();
		tp.execute(args);
	}
}
