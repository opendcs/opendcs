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
package decodes.decoder;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.StringTokenizer;

import decodes.db.DecodesScript;

public class SetMissingFunction extends DecodesFunction
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "setMissing";
	
	public SetMissingFunction()
	{
	}

	@Override
	public DecodesFunction makeCopy()
	{
		return new SetMissingFunction();
	}

	@Override
	public String getFuncName()
	{
		return module;
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage msg) throws DecoderException
	{
		// Execute doesn't do anything. Work is done in setArguments
	}

	@Override
	public void setArguments(String argString, DecodesScript script) throws ScriptFormatException
	{
		StringTokenizer st = new StringTokenizer(argString, ",");
		while(st.hasMoreTokens())
		{
			String t = st.nextToken();
			t = t.trim();
			script.addMissing(t);
			log.trace("Adding {} to missing values list.", t);
		}
	}

}
