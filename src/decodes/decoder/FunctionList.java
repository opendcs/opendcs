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

/**
This class holds an expandable collection of 'functions' that can be
used inside DECODES format statements.
*/
public class FunctionList
{
	/** A set of functions available to the tokenizer. */
	private static HashMap<String, DecodesFunction> functions
		= new HashMap<String, DecodesFunction>();

	/**
	 * Adds a function to the list.
	 */
	public static void addFunction(DecodesFunction func)
	{
		functions.put(func.getFuncName().toLowerCase(), func);
	}

	/**
	 * If a function with matching name exists, return a usable copy of the
	 * function. Else return null.
	 */
	public static DecodesFunction lookup(String name)
	{
		DecodesFunction ret = functions.get(name.toLowerCase());
		if (ret == null)
			return null;
		return ret.makeCopy();
	}
}
