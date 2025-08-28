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

import java.util.HashMap;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.opendcs.spi.decodes.DecodesFunctionProvider;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
This class holds an expandable collection of 'functions' that can be
used inside DECODES format statements.
*/
public class FunctionList
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private static final ServiceLoader<DecodesFunctionProvider> loader = ServiceLoader.load(DecodesFunctionProvider.class);
	/** A set of functions available to the tokenizer. */
	private static HashMap<String, DecodesFunction> functions = new HashMap<>();

	private static void loadFunctions()
	{
		Iterator<DecodesFunctionProvider> providers = loader.iterator();
		while(providers.hasNext())
		{
			DecodesFunctionProvider dfp = providers.next();
			final String name = dfp.getName().toLowerCase();
			DecodesFunction newFunc = dfp.createInstance();
			if (functions.containsKey(name))
			{
				DecodesFunction func = functions.get(name);
				log.atWarn().log("Decodes Function List already contains a function named '{}' from '{}'."
								+"New Function is from class '{}'. Keeping first loaded.",
								 name, func.getClass().getName(), newFunc.getClass().getName());
			}
			else
			{
				functions.put(name, newFunc);
			}
		}

	}

	/**
	 * If a function with matching name exists, return a usable copy of the
	 * function. Else return null.
	 */
	public static DecodesFunction lookup(String name)
	{
		if (functions.isEmpty())
		{
			loadFunctions();
		}
		DecodesFunction ret = functions.get(name.toLowerCase());
		if (ret == null)
			return null;
		return ret.makeCopy();
	}
}
