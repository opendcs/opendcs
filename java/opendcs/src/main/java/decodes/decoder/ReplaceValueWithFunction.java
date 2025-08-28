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

import decodes.db.DecodesScript;

/**
 * Decodes function used to replace a symbol with another value.
 * example:  replaceValueWith(T,0.0)   # replaces 'T' with 0.0  
 */
public class ReplaceValueWithFunction extends DecodesFunction
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "replaceValueWith";
	private String find;
	private String replace;
	private DecodesScript script;

	
	public ReplaceValueWithFunction()
	{
	}

	@Override
	public DecodesFunction makeCopy()
	{
		return new ReplaceValueWithFunction();
	}

	@Override
	public String getFuncName()
	{
		return module;
	}

	@Override
	public void execute(DataOperations dd, DecodedMessage msg) throws DecoderException
	{
		script.addReplace(find,replace);
	}

	/**
	 * get arguments from: replaceValue(find,replace)
	 */
	@Override
	public void setArguments(String argString, DecodesScript script) throws ScriptFormatException
	{
		log.debug(argString);
		String[] args = argString.split(",");
		if( args.length == 2)
		{
			find = args[0].trim();
			replace = args[1].trim();
			this.script = script;
		}
		else
		{
			throw new ScriptFormatException(module + " requires two arguments");
		}
 
	}

}
