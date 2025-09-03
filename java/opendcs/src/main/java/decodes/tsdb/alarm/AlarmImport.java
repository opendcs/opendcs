/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* Copyright 2017 Cove Software, LLC. All rights reserved.
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
package decodes.tsdb.alarm;

import opendcs.dai.AlarmDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.SiteDAI;
import opendcs.dao.AlarmDAO;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.TextUtil;

import java.util.ArrayList;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.alarm.xml.AlarmFile;
import decodes.tsdb.alarm.xml.AlarmXio;
import decodes.tsdb.xml.DbXmlException;
import decodes.util.CmdLineArgs;

public class AlarmImport extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "AlarmImport";
	private StringToken grpFileArg = new StringToken("", "Alarm Group XML File(s)",
		"", TokenOptions.optArgument | TokenOptions.optMultiple | TokenOptions.optRequired, "");

	public AlarmImport()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}

	public static void main(String[] args)
		throws Exception
	{
		AlarmImport alarmImport = new AlarmImport();
		alarmImport.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(grpFileArg);
	}


	@Override
	protected void runApp()
	{
		AlarmDAI alarmDAO = new AlarmDAO(TsdbAppTemplate.theDb);
		AlarmXio alarmXio = new AlarmXio();
		LoadingAppDAI appDAO = TsdbAppTemplate.theDb.makeLoadingAppDAO();
		ArrayList<CompAppInfo> apps = null;
		try
		{
			apps = appDAO.listComputationApps(false);
		}
		catch (DbIoException ex)
		{
			log.atError().setCause(ex).log("Unable to list computation aps.");
		}
		finally { appDAO.close(); }

		for(int i=0; i<grpFileArg.NumberOfValues(); i++)
		{
			String fn = grpFileArg.getValue(i);

			AlarmFile alarmFile = null;
			String action = null;
			try
			{
				action = "reading file '" + fn + "'";
				alarmFile = alarmXio.readAlarmFile(fn);

				// Write any groups first. If they are new, this will assign GroupID in the objects.
				for(AlarmGroup grp : alarmFile.getGroups())
				{
					action = "writing group '" + grp.getName() + "'";
					alarmDAO.write(grp);
				}

				SiteDAI siteDAO = theDb.makeSiteDAO();
				DataTypeDAI dtDAO = theDb.makeDataTypeDAO();
				try
				{
					for(AlarmScreening scrn : alarmFile.getScreenings())
					{
						// In the object read from XML, SiteNames will not be resolved to siteId. Do that.
						Site site = null;
						for(SiteName sn : scrn.getSiteNames())
							if ((site = siteDAO.getSiteBySiteName(sn)) != null)
							{
								scrn.setSiteId(site.getId());
								break;
							}

						// Site may be validly null to assign a default screening for a datatype.
						if (site == null && scrn.getSiteNames().size() > 0)
						{
							log.warn("Screening '{}'' is for non-existant site '{}' ignored.",
									 scrn.getScreeningName(), scrn.getSiteNames().get(0));
							continue;
						}

						// In the object read from XML, DataType will be in the object and may not have an
						// id because it may be new. If so, write it and get an ID. Set datatypeId in the object.
						if (scrn.getDataType() == null)
						{
							log.warn("Screening  '{}' is missing required datatype assignment -- ignored.",
									 scrn.getScreeningName());
							continue;
						}
						else if (DbKey.isNull(scrn.getDatatypeId()))
						{
							// Datatype is assigned but it doesn't exist in our database. Write it.
							dtDAO.writeDataType(scrn.getDataType());
							scrn.setDatatypeId(scrn.getDataType().getId());
						}

						// If a group assignment is made, associate the group ID.
						if (scrn.getGroupName() != null && DbKey.isNull(scrn.getAlarmGroupId()))
						{
							// It could be one of the groups from the same file, written above.
							for(AlarmGroup grp : alarmFile.getGroups())
								if (TextUtil.strEqual(scrn.getGroupName(), grp.getName()))
								{
									scrn.setAlarmGroupId(grp.getAlarmGroupId());
									break;
								}
							// If not, it could be an existing group
							if (DbKey.isNull(scrn.getAlarmGroupId()))
								scrn.setAlarmGroupId(alarmDAO.groupName2id(scrn.getGroupName()));
							if (DbKey.isNull(scrn.getAlarmGroupId()))
							{
								log.warn("Screening '{}' assigned to non-existant group '{}' " +
										 "-- screening group will be unassigned",
										 scrn.getScreeningName(), scrn.getGroupName());
							}
						}

						if (scrn.getAppName() != null)
						{
							for(CompAppInfo cai : apps)
								if (scrn.getAppName().equalsIgnoreCase(cai.getAppName()))
								{
									scrn.setAppInfo(cai);
									scrn.setAppId(cai.getAppId());
									break;
								}
						}

						action = "writing screening '" + scrn.getScreeningName() + "'";
						alarmDAO.writeScreening(scrn);
					}
				}
				finally
				{
					dtDAO.close();
					siteDAO.close();
				}
			}
			catch(Exception ex)
			{
				log.atError().setCause(ex).log("Error while {}", action);
			}
		}
		alarmDAO.close();
	}

}
