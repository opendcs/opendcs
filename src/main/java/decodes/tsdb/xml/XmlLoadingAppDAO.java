package decodes.tsdb.xml;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.ServerLock;
import ilex.util.TextUtil;
import ilex.xml.XmlOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
	private ServerLock serverLock = null;
	
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
		if (files != null)
		{
			for(File f : files)
			{
				if (!f.getName().toLowerCase().endsWith(".xml"))
				{
					continue;
				}
				try
				{
					ArrayList<CompMetaData> objs = compXio.readFile(f.getPath());
					for(CompMetaData obj : objs)
					{
						if (obj instanceof CompAppInfo)
						{
							ret.add((CompAppInfo)obj);
						}
					}
				}
				catch (DbXmlException ex)
				{
					String msg = module + ": " + "Error parsing loading app file '"
						+ f.getPath() + "': " + ex + " -- skipped.";
					Logger.instance().warning(msg);
					throw new DbIoException(msg, ex);
				}
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
		return DbKey.NullKey;
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
		serverLock = lock.getServerLock();
		if (serverLock != null)
			serverLock.deleteLockFile();
	}

	@Override
	public void checkCompProcLock(TsdbCompLock lock) 
		throws LockBusyException, DbIoException
	{
		serverLock = lock.getServerLock();
		if (serverLock == null)
			return; // this is not an XML database lock.
		if (!serverLock.isLocked(true))
		{	
			// It is NOT locked, meaning lock has been removed or stolen by another PID
			throw new LockBusyException("Lock has been removed.");
		}
	}

	@Override
	public List<TsdbCompLock> getAllCompProcLocks()
		throws DbIoException
	{
		ArrayList<TsdbCompLock> ret = new ArrayList<TsdbCompLock>();
		File userdir = new File(EnvExpander.expand("$DCSTOOL_USERDIR"));
		File lockFiles[] = userdir.listFiles(
			new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String name)
				{
					return TextUtil.endsWithIgnoreCase(name, ".lock");
				}
			});
		ArrayList<CompAppInfo> applist = listComputationApps(false);
		for(File lf : lockFiles)
		{
//System.out.println("Checking lock file '" + lf.getName() + "'");
			String appName = lf.getName();
			// Guaranteed from above that it ends in ".lock"
			appName = appName.substring(0, appName.lastIndexOf('.'));
//System.out.println("Looking for match for appName '" + appName + "'");
			for(CompAppInfo cai : applist)
			{
				String fn = compressFileName(cai.getAppName());
//System.out.println("  ... checking '" + fn + "'");
				if (appName.equals(fn))
				{
//System.out.println("This lock is for app '" + appName + "'");
					ServerLock serverLock = new ServerLock(lf.getPath());
					// Don't care about result, the isLocked method reads the lock info.
					serverLock.isLocked(true);
					TsdbCompLock tcl = new TsdbCompLock(DbKey.NullKey, serverLock.getFilePID(),
						"", new Date(serverLock.getLastLockMsec()), serverLock.getAppStatus());
					tcl.setServerLock(serverLock);
					tcl.setAppName(appName);
					ret.add(tcl);
				}
			}
		}
		return ret;
	}

	@Override
	public TsdbCompLock obtainCompProcLock(CompAppInfo appInfo, int pid, String host)
		throws LockBusyException, DbIoException
	{
		String lockpath = makeFilePath(appInfo.getAppName());
		serverLock = new ServerLock(lockpath);
		serverLock.setPID(pid);
		if (!serverLock.obtainLock())
			throw new LockBusyException("Lock file '" + lockpath + "' is already busy.");
		TsdbCompLock ret = new TsdbCompLock(DbKey.NullKey, pid, host, 
			new Date(serverLock.getLastLockMsec()), serverLock.getAppStatus());
		ret.setServerLock(serverLock);
		ret.setAppName(appInfo.getAppName());
		return ret;
	}
	
	/**
	 * Collapse white space and resolve $DCSTOOL_USERDIR to return complete path to the lock
	 * file for a given app name.
	 * @param appName
	 * @return
	 */
	private String makeFilePath(String appName)
	{
		return EnvExpander.expand("$DCSTOOL_USERDIR/" + compressFileName(appName) + ".lock");
	}
	
	/**
	 * Collapse whitespace in appName in order to have something that works as a filename.
	 */
	private String compressFileName(String appName)
	{
		StringBuilder sb = new StringBuilder(appName);
		for(int idx = 0; idx < sb.length(); )
		{
			char c = sb.charAt(idx);
			if (!Character.isLetterOrDigit(c) && c != '-' && c != '_')
				sb.deleteCharAt(idx);
			else
				idx++;
		}
		return sb.toString();
	}

	@Override
	public boolean supportsLocks()
	{
		return true;
	}

	@Override
	public Date getLastModified(DbKey appId)
	{
		// Only used by dcpmon which is not supported in XML database.
		return null;
	}

	@Override
	public ResultSet doQuery(String q) throws DbIoException
	{
		// Only implemented for sql
		return null;
	}

	@Override
	public ResultSet doQuery2(String q) throws DbIoException
	{
		// Only implemented for sql
		return null;
	}

	@Override
	public int doModify(String q) throws DbIoException
	{
		// Only implemented for sql
		return 0;
	}

	@Override
	public void setManualConnection(Connection conn)
	{
		// Only implemented for sql
	}

}
