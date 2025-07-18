/*
 * Open source software by Cove Software, LLC.
 */
package decodes.datasource;

import java.io.BufferedInputStream;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import org.opendcs.utils.WebUtility;

/**
 * Extends StreamDataSource to read data from an URL opened on the internet.
 * @author mmaloney
 */
public class WebDataSource 
	extends StreamDataSource
	implements PropertiesOwner
{
	private String module = "WebDataSource";
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
			log(Logger.E_INFORMATION, module + " Opening '" + activeAddr + "'");
			return new BufferedInputStream(WebUtility.openUrl(activeAddr, 5000));
		}
		catch (Exception ex)
		{
			String msg = module + " Open failed on '" + activeAddr + "': " + ex;
			log(Logger.E_WARNING, msg);
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
