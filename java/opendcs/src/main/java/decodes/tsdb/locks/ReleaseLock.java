package decodes.tsdb.locks;

import java.util.List;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.LoadingAppDAI;

import decodes.tsdb.*;
import decodes.tsdb.procmonitor.ComputationQueueDao;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.db.Constants;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.TokenOptions;

public class ReleaseLock extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private BooleanToken clearQueueArg = new BooleanToken("Q",
		"Clear the application's computation queue after stopping it.", "",
		TokenOptions.optSwitch, false);

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
		boolean releasedLock = false;
		LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
		try
		{
			List<TsdbCompLock> locks = loadingAppDAO.getAllCompProcLocks();
			log.info("{} Locks Retrieved.", locks.size());
			for(TsdbCompLock lock : locks)
				if (lock.getAppId().equals(getAppId()))
				{
					loadingAppDAO.releaseCompProcLock(lock);
					releasedLock = true;
					break;
				}
		}
		finally
		{
			loadingAppDAO.close();
		}

		if (clearQueueArg.getValue())
		{
			if (!theDb.isCwms())
			{
				log.error("-Q is only supported for CWMS computation queues.");
				return;
			}
			if (releasedLock)
				Thread.sleep(6000L);
			try (LoadingAppDAI verificationDao = theDb.makeLoadingAppDAO())
			{
				for (TsdbCompLock lock : verificationDao.getAllCompProcLocks())
					if (lock.getAppId().equals(getAppId()) && !lock.isStale())
					{
						log.error("Application restarted before its computation queue could be cleared.");
						return;
					}
			}
			try (ComputationQueueDao queueDao = new ComputationQueueDao(theDb))
			{
				int deleted = queueDao.clearQueue(getAppId());
				log.info("Deleted {} queued records for application {}.", deleted, appNameArg.getValue());
			}
		}
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(clearQueueArg);
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
