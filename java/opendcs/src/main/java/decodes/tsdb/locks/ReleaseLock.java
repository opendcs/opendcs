package decodes.tsdb.locks;

import java.util.List;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.LoadingAppDAI;

import decodes.tsdb.*;
import decodes.util.DecodesException;
import decodes.db.Constants;

public class ReleaseLock extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	public ReleaseLock()
	{
		super(null);
	}

	protected void runApp()
		throws Exception
	{
		if (getAppId() == Constants.undefinedId)
		{
			log.error("-a <appName> argument required -- No action taken!");
			return;
		}
		// Note, the -a arg will have us connect to the database as the
		// desired application.
		LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
		try
		{
			List<TsdbCompLock> locks = loadingAppDAO.getAllCompProcLocks();
			log.info("{} Locks Retrieved.", locks.size());
			for(TsdbCompLock lock : locks)
				if (lock.getAppId() == getAppId())
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
	
	public static void main(String args[]) throws Exception
	{
		ReleaseLock tp = new ReleaseLock();
		tp.execute(args);
	}
}
