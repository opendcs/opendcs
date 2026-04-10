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
package decodes.routing;

import java.util.Date;

import opendcs.dai.DacqEventDAI;
import opendcs.dao.DacqEventDAO;
import decodes.db.Database;
import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;

/**
 * @deprecated we are replacing the logging system. Output to external logging (the dacq event tables count)
 * 				should be handled through the logger implementations outputs.
 */
@Deprecated
public class DacqEventLogger
{
	public DacqEventLogger()
	{
		/* class getting removed */
	}

	public void close()
	{
		/* class getting removed */
	}


	public void doLog(int priority, String text)
	{
		/* class getting removed */
	}

	public void writeDacqEvent(DacqEvent evt)
	{
		/* class getting removed */
	}

	public void setSchedEntryStatusId(DbKey schedEntryStatusId)
	{
		/* class getting removed */
	}

	public void setPlatformId(DbKey platformId)
	{
		/* class getting removed */
	}

	public void setSubsystem(String subsystem)
	{
		/* class getting removed */
	}

	public void setAppId(DbKey appId)
	{
		/* class getting removed */
	}

	public void setMsgStart(Date timeStamp)
	{
		/* class getting removed */
	}

}
