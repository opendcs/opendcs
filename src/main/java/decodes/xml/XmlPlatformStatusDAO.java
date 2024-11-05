package decodes.xml;

import ilex.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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

	public XmlPlatformStatusDAO(XmlDatabaseIO parent)
	{
		this.parent = parent;
		psDir = new File(parent.xmldir, XmlDatabaseIO.PlatformStatusDir);
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
