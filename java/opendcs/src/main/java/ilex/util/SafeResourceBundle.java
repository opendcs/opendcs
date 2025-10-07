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
package ilex.util;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;

public class SafeResourceBundle extends ResourceBundle
{
	private static final org.slf4j.Logger log = OpenDcsLoggerFactory.getLogger();
	ResourceBundle theBundle;

	public SafeResourceBundle(ResourceBundle theBundle)
	{
		this.theBundle = theBundle;
	}

	/**
	 * Try to get an object from the bundle. If null, return the key.
	 * If the key is null return the string "null-resource-key".
	 * Thus this method will never return null or throw an exception so
	 * the higher-level getString method should never throw
	 * MissingResourceException.
	 */
	protected Object handleGetObject(String key)
	{
		if (key == null)
			return "null-resource-key";
		try
		{
			Object ret = theBundle.getObject(key);
			if (ret != null)
				return ret;
			log.warn("Unknown resource key '{}' - not found in resource file.", key);
			return key;
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Bad resource key '{}'", key);
			return "bad-key:" + key;
		}
	}

	public Enumeration<String> getKeys()
	{
		return theBundle.getKeys();
	}

	public Locale getLocale()
	{
		return theBundle.getLocale();
	}
}
