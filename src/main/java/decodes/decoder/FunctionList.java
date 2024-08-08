/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.decoder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.opendcs.spi.decodes.DecodesFunctionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
This class holds an expandable collection of 'functions' that can be
used inside DECODES format statements.
*/
public class FunctionList
{
	private static final Logger log = LoggerFactory.getLogger(FunctionList.class);

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
