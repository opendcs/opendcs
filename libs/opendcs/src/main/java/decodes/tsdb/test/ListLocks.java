package decodes.tsdb.test;

import java.util.List;

import lrgs.gui.DecodesInterface;
import opendcs.dai.LoadingAppDAI;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;

public class ListLocks 
	extends TsdbAppTemplate
{
	public static final String module = "ListLocks";
	
	public ListLocks()
	{
		super("util.log");
		appNameArg.setDefaultValue("utility");
	}

	@Override
	protected void runApp()
		throws Exception
	{
		LoadingAppDAI loadingAppDao = decodes.db.Database.getDb().getDbIo().makeLoadingAppDAO();
		List<TsdbCompLock> locks = loadingAppDao.getAllCompProcLocks();
		System.out.println("" + locks.size() + " Locks Retrieved:");

		for(TsdbCompLock lock : locks)
			System.out.println(lock.toString());
		loadingAppDao.close();
	}

	public static void main(String args[])
		throws Exception
	{
		ListLocks tp = new ListLocks();
		tp.execute(args);
	}
	
	@Override
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.initDecodes(cmdLineArgs.getPropertiesFile());
	}

	@Override
	public void createDatabase() {}
	
	@Override
	public void tryConnect() {}



}
