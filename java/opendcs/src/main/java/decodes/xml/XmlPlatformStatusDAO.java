package decodes.xml;

import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import ilex.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import decodes.db.DatabaseObject;
import decodes.db.PlatformStatus;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dai.PlatformStatusDAI;


public class XmlPlatformStatusDAO 
	implements PlatformStatusDAI
{
	private XmlDatabaseIO parent = null;
	private File psDir = null;
	private File netlistDir = null;

	public XmlPlatformStatusDAO(XmlDatabaseIO parent)
	{
		this.parent = parent;
		psDir = new File(parent.xmldir, XmlDatabaseIO.PlatformStatusDir);
		netlistDir = new File(parent.xmldir, XmlDatabaseIO.NetworkListDir);
		if (!netlistDir.isDirectory())
			netlistDir.mkdirs();
		if (!psDir.isDirectory())
			psDir.mkdirs();
	}

	@Override
	public synchronized PlatformStatus readPlatformStatus(DbKey platformId) throws DbIoException
	{
		File f = new File(psDir,  makeFileName(platformId));
		try
		{
			DatabaseObject dbo = parent.getParser().parse(f);
			if (dbo instanceof PlatformStatus)
				return (PlatformStatus)dbo;
			else
				Logger.instance().warning("Ignoring non-PlatformStatus "
					+ "in file '" + f.getPath() + "'");
		}
		catch (Exception ex)
		{
			Logger.instance().warning("Error reading '" + f.getPath() + "': " + ex);
		}
		return null;
	}

	@Override
	public List<PlatformStatus> readPlatformStatusList(DbKey netlistId)
			throws DbIoException
	{
		try
		{
			ArrayList<PlatformStatus> ret = new ArrayList<>();
			File[] files = netlistDir.listFiles();
			File[] platformFiles = psDir.listFiles();
			for(File f : files)
			{
				if (!f.isFile())
					continue;
				try
				{
					DatabaseObject dbo = parent.getParser().parse(f);
					if (dbo instanceof NetworkList)
					{
						NetworkList netList = (NetworkList) dbo;
						for (Map.Entry<String, NetworkListEntry> entry : netList.networkListEntries.entrySet())
						{
							for (File pf : platformFiles)
							{
								if (!pf.isFile())
									continue;
								try
								{
									DatabaseObject dbo2 = parent.getParser().parse(pf);
									if (dbo2 instanceof PlatformStatus)
									{
										PlatformStatus ps = (PlatformStatus) dbo2;
										if (entry.getValue().getPlatformName().equals(ps.getPlatformName()))
										{
											ret.add(ps);
										}
									}
									else
									{
										Logger.instance().warning("Ignoring non-PlatformStatus "
												+ "in file '" + pf.getPath() + "'");
									}
								}
								catch (SAXException ex)
								{
									Logger.instance().warning("Error parsing '" + pf.getPath() + "': " + ex);
								}
							}
						}
					}
					else
					{
						Logger.instance().warning("Ignoring non-PlatformStatus "
								+ "in file '" + f.getPath() + "'");
					}
				}
				catch (SAXException ex)
				{
					Logger.instance().warning("Error parsing '" + f.getPath() + "': " + ex);
				}
			}
			return ret;
		}
		catch (IOException ex)
		{
			throw new DbIoException("Cannot list '" + psDir.getPath() + "' xml directory: " + ex);
		}
	}
	
	private String makeFileName(DbKey platformId)
	{
		return "ps-" + platformId + ".xml";
	}

	@Override
	public synchronized void writePlatformStatus(PlatformStatus platformStatus) throws DbIoException
	{
		File f = new File(psDir,  makeFileName(platformStatus.getId()));
		try
		{
			TopLevelParser.write(f, platformStatus);
		}
		catch (IOException ex)
		{
			throw new DbIoException("Cannot write '" + f.getPath() + "': " + ex);
		}
	}

	@Override
	public ArrayList<PlatformStatus> listPlatformStatus() throws DbIoException
	{
		try
		{
			ArrayList<PlatformStatus> ret = new ArrayList<PlatformStatus>();
			File files[] = psDir.listFiles();
			for(File f : files)
			{
				if (!f.isFile())
					continue;
				try
				{
					DatabaseObject dbo = parent.getParser().parse(f);
					if (dbo instanceof PlatformStatus)
						ret.add((PlatformStatus)dbo);
					else
						Logger.instance().warning("Ignoring non-PlatformStatus "
							+ "in file '" + f.getPath() + "'");
				}
				catch (SAXException ex)
				{
					Logger.instance().warning("Error parsing '" + f.getPath() + "': " + ex);
				}
			}
			return ret;
		}
		catch (IOException ex)
		{
			throw new DbIoException("Cannot list '" + psDir.getPath() + "' xml directory: " + ex);
		}
	}

	@Override
	public void close()
	{
	}

	@Override
	public void deletePlatformStatus(DbKey platformId) throws DbIoException
	{
		File f = new File(psDir,  makeFileName(platformId));
		if (f.exists())
			f.delete();
	}

}
