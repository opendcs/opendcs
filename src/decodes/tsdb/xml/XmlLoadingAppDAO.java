package decodes.tsdb.xml;

import ilex.util.Logger;
import ilex.xml.XmlOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompMetaData;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbCompLock;
import decodes.xml.XmlDatabaseIO;
import opendcs.dai.LoadingAppDAI;

/**
 * This class writes Loading App records into the DECODES XML database.
 * Each loading app is stored in a separate XML file called "appname".xml
 * inside the loading-app sub directory.
 * 
 * This is only used by the DECODES XML interface and thus many of the methods
 * are not needed.
 * 
 * @author mmaloney Mike Maloney, Cove Software LLC
 */
public class XmlLoadingAppDAO implements LoadingAppDAI
{
	public final static String module = "XmlLoadingAppDAO";
	private File loadingAppDir = null;
	private CompXio compXio = new CompXio(module, null);
	
	public XmlLoadingAppDAO(String dbTop)
	{
		loadingAppDir = new File(dbTop, XmlDatabaseIO.LoadingAppDir);
		if (!loadingAppDir.isDirectory())
			loadingAppDir.mkdirs();
	}
	
	@Override
	public ArrayList<CompAppInfo> listComputationApps(boolean usedOnly)
		throws DbIoException
	{
		ArrayList<CompAppInfo> ret = new ArrayList<CompAppInfo>();
		File [] files = loadingAppDir.listFiles();
		for(File f : files)
		{
			if (!f.getName().toLowerCase().endsWith(".xml"))
				continue;
			try
			{
				ArrayList<CompMetaData> objs = compXio.readFile(f.getPath());
				for(CompMetaData obj : objs)
					if (obj instanceof CompAppInfo)
						ret.add((CompAppInfo)obj);
			}
			catch (DbXmlException ex)
			{
				String msg = module + ": " + "Error parsing loading app file '"
					+ f.getPath() + "': " + ex + " -- skipped.";
				Logger.instance().warning(msg);
				throw new DbIoException(msg);
			}
		}
		return ret;
	}

	@Override
	public CompAppInfo getComputationApp(String name) 
		throws DbIoException, NoSuchObjectException
	{
		File f = new File(loadingAppDir, name + ".xml");
		if (!f.canRead())
			throw new NoSuchObjectException(module + ": No such file '" + f.getPath() + "'");
		try
		{
			ArrayList<CompMetaData> objs = compXio.readFile(f.getPath());
			for(CompMetaData obj : objs)
				if (obj instanceof CompAppInfo)
					return (CompAppInfo)obj;
			String msg = module + ": File '" + f.getPath() + "' does not contain a loading app.";
			Logger.instance().warning(msg);
			throw new NoSuchObjectException(msg);
		}
		catch (DbXmlException ex)
		{
			String msg = module + ": " + "Error parsing loading app file '"
				+ f.getPath() + "': " + ex + " -- skipped.";
			Logger.instance().warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void writeComputationApp(CompAppInfo app) 
		throws DbIoException
	{
		File f = new File(loadingAppDir, app.getAppName() + ".xml");
		try
		{
			ArrayList<CompMetaData> objs = new ArrayList<CompMetaData>();
			objs.add(app);
			// Open an output stream wrapped by an XmlOutputStream
			FileOutputStream fos = new FileOutputStream(f);
			XmlOutputStream xos = 
				new XmlOutputStream(fos, CompXioTags.compMetaData);
			xos.writeXmlHeader();
			compXio.writeApp(xos, app);
			fos.close();
		}
		catch (IOException ex)
		{
			String msg = module + ": " + "Error writing loading app to file '"
				+ f.getPath() + "': " + ex;
			Logger.instance().warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void deleteComputationApp(CompAppInfo app) 
		throws DbIoException
	{
		File f = new File(loadingAppDir, app.getAppName() + ".xml");
		if (!f.delete())
		{
			String msg = module + ": " + "Cannot delete loading app file '"
				+ f.getPath() + "'";
			Logger.instance().warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public List<String> listComputationsByApplicationId(DbKey appId,
		boolean enabledOnly) throws DbIoException
	{
		// Not needed in XML
		return null;
	}

	@Override
	public ArrayList<CompAppInfo> ComputationAppsIn(String inList)
		throws DbIoException
	{
		// Not needed in XML
		return null;
	}

	@Override
	public CompAppInfo getComputationApp(DbKey id) throws DbIoException,
		NoSuchObjectException
	{
		// Not needed in XML
		return null;
	}

	@Override
	public DbKey lookupAppId(String name) throws DbIoException,
		NoSuchObjectException
	{
		// Not needed in XML
		return null;
	}

	@Override
	public void close()
	{
		// Nothing to do.
	}

	/**
	 * Locks are not supported in an xml database. Does nothing.
	 */
	@Override
	public void releaseCompProcLock(TsdbCompLock lock) throws DbIoException
	{
	}

	/**
	 * Locks are not supported in an xml database. Does nothing.
	 */
	@Override
	public void checkCompProcLock(TsdbCompLock lock) throws LockBusyException,
		DbIoException
	{
	}

	/**
	 * Locks are not supported in an xml database: always returns empty list.
	 */
	@Override
	public List<TsdbCompLock> getAllCompProcLocks()
		throws DbIoException
	{
		Logger.instance().warning("Locks not supported in XML database.");
		return new ArrayList<TsdbCompLock>();
	}

	/**
	 * Locks are not supported in an xml database: always throws lock busy.
	 */
	@Override
	public TsdbCompLock obtainCompProcLock(CompAppInfo appInfo, int pid,
		String host) throws LockBusyException, DbIoException
	{
		throw new LockBusyException("Locks not supported in XML database.");
	}

	@Override
	public boolean supportsLocks()
	{
		return false;
	}

	@Override
	public Date getLastModified(DbKey appId)
	{
		// Only used by dcpmon which is not supported in XML database.
		return null;
	}

}
