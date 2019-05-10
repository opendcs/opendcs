/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.1  2019/03/05 14:53:00  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.2  2017/05/17 20:36:26  mmaloney
 * First working version.
 *
 * Revision 1.1  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import opendcs.dai.AlarmDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.SiteDAI;
import opendcs.dao.AlarmDAO;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.alarm.xml.AlarmFile;
import decodes.tsdb.alarm.xml.AlarmXio;
import decodes.tsdb.xml.DbXmlException;
import decodes.util.CmdLineArgs;
import hec.util.TextUtil;

public class AlarmImport
	extends TsdbAppTemplate
{
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
							warning("Screening '" + scrn.getScreeningName() 
								+ " is for non-existant site '" + scrn.getSiteNames().get(0)
								+ "' ignored.");
							continue;
						}
						
						// In the object read from XML, DataType will be in the object and may not have an
						// id because it may be new. If so, write it and get an ID. Set datatypeId in the object.
						if (scrn.getDataType() == null)
						{
							warning("Screening  '" + scrn.getScreeningName() 
								+ "' is missing required datatype assignment -- ignored.");
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
								if (TextUtil.equals(scrn.getGroupName(), grp.getName()))
								{
									scrn.setAlarmGroupId(grp.getAlarmGroupId());
									break;
								}
							// If not, it could be an existing group
							if (DbKey.isNull(scrn.getAlarmGroupId()))
								scrn.setAlarmGroupId(alarmDAO.groupName2id(scrn.getGroupName()));
							if (DbKey.isNull(scrn.getAlarmGroupId()))
								warning("Screening  '" + scrn.getScreeningName() 
									+ "' assigned to non-existant group '" + scrn.getGroupName() 
									+ "' -- screening group will be unassigned");
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
				String msg = module + ": Error while " + action + ": " + ex;
				Logger.instance().failure(msg);
				System.err.println(msg);
				ex.printStackTrace(System.err);
			}
		}
		alarmDAO.close();
	}

}
