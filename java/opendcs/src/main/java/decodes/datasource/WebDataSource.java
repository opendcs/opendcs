/*
* Open source software by Cove Software, LLC.
*
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

package decodes.datasource;

import java.io.BufferedInputStream;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import ilex.util.PropertiesUtil;
import org.opendcs.utils.WebUtility;

/**
 * Extends StreamDataSource to read data from an URL opened on the internet.
 * @author mmaloney
 */
public class WebDataSource extends StreamDataSource
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String activeAddr = null;
	
	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param ds data source
	 * @param db database
	 */
	public WebDataSource(DataSource ds, Database db)
	{
		super(ds,db);
	}

	private PropertySpec propSpecs[] =
	{
		new PropertySpec("url", PropertySpec.STRING, "The page to open")
	};
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return PropertiesUtil.combineSpecs(super.getSupportedProps(), propSpecs);
	}

	/**
	 * Base class return true for backward compatibility.
	 */
	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}


	/** Don't re-read after an URL is finished. */
	@Override
	public boolean tryAgainOnEOF()
	{
		return false;
	}

	/** Likewise -- don't reopen an URL. */
	@Override
	public boolean doReOpen() { return false; }
	
	@Override
	public boolean setProperty(String name, String value)
	{
		if (name.equalsIgnoreCase("url"))
		{
			activeAddr = value;
			return true;
		}
		return false;
	}

	@Override
	public BufferedInputStream open()
		throws DataSourceException
	{
		try
		{
			log.info("Opening '{}'", activeAddr);
			return new BufferedInputStream(WebUtility.openUrl(activeAddr, 5000));
		}
		catch (Exception ex)
		{
			String msg = "Open failed on '" + activeAddr;
			throw new DataSourceException(msg, ex);
		}
	}

	@Override
	public void close(BufferedInputStream str)
	{
		// silently close
		try 
		{
			str.close();
		}
		catch(Exception ex)
		{}
	}

	@Override
	public String getActiveSource()
	{
		return activeAddr;
	}
}
