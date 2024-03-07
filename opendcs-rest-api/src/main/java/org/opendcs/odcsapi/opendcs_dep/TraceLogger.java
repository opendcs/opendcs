/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.opendcs_dep;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opendcs.odcsapi.beans.ApiLogMessage;
import org.opendcs.odcsapi.util.ApiConstants;

public class TraceLogger extends ilex.util.Logger
{
	ArrayList<ApiLogMessage> log;
	
	public TraceLogger(ArrayList<ApiLogMessage> log)
	{
		super("trace");
		this.log = log;
	}

	@Override
	public void close()
	{
	}

	@Override
	public void doLog(int priority, String msg)
	{
		String txt = standardMessage(priority, msg);
		log.add(new ApiLogMessage(new Date(), priorityName[priority], txt));

		// In addition to saving the message, write it to the API's log.
		Level level = priority == ilex.util.Logger.E_DEBUG3 ? Level.FINEST :
			priority == ilex.util.Logger.E_DEBUG2 ? Level.FINER :
			priority == ilex.util.Logger.E_DEBUG1 ? Level.FINE :		
			priority == ilex.util.Logger.E_INFORMATION ? Level.INFO :		
			priority == ilex.util.Logger.E_WARNING ? Level.WARNING :		
			priority == ilex.util.Logger.E_FAILURE ? Level.SEVERE :		
			priority == ilex.util.Logger.E_FATAL ? Level.SEVERE : Level.INFO;	
		Logger.getLogger(ApiConstants.loggerName).log(level, msg);
	}
}
