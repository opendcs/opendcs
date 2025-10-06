/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.xml;

import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import org.xml.sax.SAXException;

import decodes.db.DatabaseObject;
import decodes.db.PlatformStatus;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dai.PlatformStatusDAI;


public class XmlPlatformStatusDAO implements PlatformStatusDAI
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			{
				log.warn("Ignoring non-PlatformStatus in file '{}'",f.getPath());
			}
		}
		catch (Exception ex)
		{
			log.atWarn().setCause(ex).log("Error reading '{}'", f.getPath());
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
										log.warn("Ignoring non-PlatformStatus in file '{}'",  pf.getPath());
									}
								}
								catch (SAXException ex)
								{
									log.atWarn().setCause(ex).log("Error parsing '" + pf.getPath()+"'");
								}
							}
						}
					}
					else
					{
					  log.warn("Ignoring non-PlatformStatus in file '{}'", f.getPath());
					}
				}
				catch (SAXException ex)
				{
					log.atWarn().setCause(ex).log("Error parsing '{}'", f.getPath());
				}
			}
			return ret;
		}
		catch (IOException ex)
		{
			throw new DbIoException("Cannot list '" + psDir.getPath() + "' xml directory: ", ex);
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
			throw new DbIoException("Cannot write '" + f.getPath() + "'", ex);
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
						log.warn("Ignoring non-PlatformStatus in file '{}'", f.getPath());
				}
				catch (SAXException ex)
				{
					log.atWarn().setCause(ex).log("Error parsing '{}'", f.getPath());
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
