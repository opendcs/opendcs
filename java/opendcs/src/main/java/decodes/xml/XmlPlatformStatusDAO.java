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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
			throw new DbIoException("Cannot list '" + psDir.getPath() + "' xml directory", ex);
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
