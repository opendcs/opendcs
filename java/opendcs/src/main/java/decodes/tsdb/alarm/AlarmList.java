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
import opendcs.dao.AlarmDAO;
import lrgs.gui.DecodesInterface;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;

/**
 * Lists names of alarm groups in the database.
 * @author mmaloney
 *
 */
public class AlarmList extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public AlarmList()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}

	public static void main(String[] args)
		throws Exception
	{
		AlarmList alarmList = new AlarmList();
		alarmList.execute(args);
	}

	@Override
	protected void runApp()
	{
		AlarmDAI alarmDAO = new AlarmDAO(TsdbAppTemplate.theDb);
		AlarmConfig cfg = new AlarmConfig();

		try
		{
			alarmDAO.check(cfg);
			for(AlarmGroup grp : cfg.getGroups())
				System.out.println(grp.getName());
		}
		catch(DbIoException ex)
		{
			log.atError().setCause(ex).log("Cannot read groups from database.");
			System.exit(1);
		}
		finally
		{
			alarmDAO.close();
		}
	}
}
