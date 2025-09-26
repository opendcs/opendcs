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
package decodes.tsdb;

import opendcs.dai.DaiBase;
import opendcs.dao.DaoBase;
import decodes.util.CmdLineArgs;

/**
Delete Time Series Data.

Main Command Line Args include:
	-a compprocname -- default = "compproc" triggers are deleted for this app only.

Following the options are any number of data IDs.
*/
public class DeleteTriggers extends TsdbAppTemplate
{
	public DeleteTriggers()
	{
		super("util.log");
		setSilent(true);
	}

	/** For cmdline version, adds argument specifications. */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("compproc");
	}

	protected void runApp()
		throws Exception
	{
		String q = "delete from cp_comp_tasklist where loading_application_id = " + getAppId();
		DaiBase dao = new DaoBase(theDb,"DeleteTriggers");
		dao.doModify(q);
		dao.close();
	}


	public void finalize()
	{
		shutdown();
	}

	public void shutdown()
	{
	}

	public static void main(String args[])
		throws Exception
	{
		DeleteTriggers tp = new DeleteTriggers();
		tp.execute(args);
	}
}
