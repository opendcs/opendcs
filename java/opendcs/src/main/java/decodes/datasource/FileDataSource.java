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
package decodes.datasource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;


import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.util.EnvExpander;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.util.PropertySpec;

/**
  This is the implementation of the DataSourceInterface for reading data
  out of a file. 
*/
public class FileDataSource extends StreamDataSource
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final int LARGEST_MSG_LENGTH = 18000;

    private File file;
	protected String filename;
	boolean gzip = false;

	private static final PropertySpec[] FDSprops =
	{
		new PropertySpec("filename", PropertySpec.STRING, 
			"(required) name of file to read."),
		new PropertySpec("gzip", PropertySpec.BOOLEAN, 
			"(default=false) set to true to un-gzip file before processing.")
	};
	
	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param ds data source
	 * @param db database
	 */
	public FileDataSource(DataSource ds, Database db)
	{
		super(ds,db);
        file = null;
		filename = null;
	}

	/**
	 * Pulls arguments necessary for FileDataSource.
	 * Does no validation. The file does not need to exist at the time this
	 * method is called.
	 */
	public void processDataSource()
		throws InvalidDatabaseException
	{
		super.processDataSource();
		log.debug("FileDataSource.processDataSource for '{}', args='{}'", getName(), dbDataSource.getDataSourceArg());
		filename = null;
	}

	/**
	  Sets a property by name &amp; value.
	  @param name the property name
	  @param value the property value
	  @return true if property is understood and accepted, false otherwise.
	*/
	public boolean setProperty(String name, String value)
	{
		name = name.toLowerCase();
		if (name.equalsIgnoreCase("filename")
		 || name.equalsIgnoreCase("file"))
        	filename = value;
		else if (name.equalsIgnoreCase("gzip"))
			gzip = TextUtil.str2boolean(value);
		else
			return false;
		return true;
	}

	/** Does nothing: Reopen is for sockets &amp; serial ports, not files. */
	public boolean doReOpen() { return false; }

	/**
	  Opens the file and returns input stream.
	  @return input stream.
	*/
	public BufferedInputStream open()
		throws DataSourceException
	{
		if (filename == null)
			filename = getName();
		String expFilename = EnvExpander.expand(filename);

		log.debug("FileDataSource.open for '{}', actual filename='{}', gzip={}", getName(), expFilename, gzip);

		file = new File(expFilename);

		// Open the input file.
		BufferedInputStream istr = null;
		try
		{
			InputStream fis = new FileInputStream(file);
			if (gzip)
				fis = new GZIPInputStream(fis);
			istr = new BufferedInputStream(fis);
		}
		catch(FileNotFoundException ex)
		{
			throw new DataSourceException("Cannot open file '" + file.getName()+ "'", ex);
		}
		catch(IOException ex)
		{
			throw new DataSourceException("Error opening file '"+file.getName()+"'", ex);
		}

		return istr;
	}

	/**
	  Closes the input file.
	  @param istr the input stream.
	*/
	public void close(BufferedInputStream istr)
	{
		if (istr != null)
		{
			try { istr.close(); }
			catch(Exception e) {}
		}
	}

	/**
	  Assume that when EOF is reached that the stream should exit.
	  @return false.
	*/
	public boolean tryAgainOnEOF()
	{
		return false;
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return PropertiesUtil.combineSpecs(super.getSupportedProps(), FDSprops);
	}

}
