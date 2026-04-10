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
package decodes.cwms;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import ilex.util.PropertiesUtil;
import decodes.util.DecodesSettings;


public class CwmsDbConfig
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** URI containing host:portnumber:SID  (tnsName value) */
	private String DbUri;

	/** Path plus Name of file containing encrypted username &amp; password */
	public String DbAuthFile;

	/** The Cwms Time Series descriptor version default value */
	public String cwmsVersion;
	
	/** The Cwms Time Series Office ID */
	public String cwmsOfficeId;
	
	/** The Cwms Time Series default timezone */
	public String timeZone;
	
	/** Path plus name of file containing the Shef to Cwms codes mapping */
	public String shefCwmsParamFile;
	
	/** The static instance */
	private static CwmsDbConfig _instance = null;
	
	/** Private constructor -- call instance() to retrieve singleton. */
	private CwmsDbConfig()
	{	
		DbUri = null;
		DbAuthFile = "$DCSTOOL_USERDIR/.decodes.auth";
		timeZone = "GMT";
		cwmsOfficeId = null;
		cwmsVersion = "raw";
		shefCwmsParamFile = "$DCSTOOL_USERDIR/shefCwmsParam.prop";
	}

	/**
	 * @return the one and only instance.
	 */
	public static CwmsDbConfig instance()
	{
		if (_instance == null)
			_instance = new CwmsDbConfig();
		return _instance;
	}

	/**
	  Loads the configuration parameters from a properties file.
	  @param fileName the name of the file to load
	*/
	public void loadFromProperties(String fileName)	throws IOException
	{
		fileName = EnvExpander.expand(fileName);
		log.info("Loading config file '{}'.", fileName);
		try(FileInputStream fis = new FileInputStream(fileName)) 
		{
			Properties rawProps = new Properties();
			rawProps.load(fis);
			PropertiesUtil.loadFromProps(this, rawProps);
		}
	}

	/** @return URI containing host:portnumber:SID  (tnsName value) */
	public String getDbUri()
	{
		DecodesSettings ds = DecodesSettings.instance();
		if (DbUri == null && ds.editDatabaseTypeCode == DecodesSettings.DB_CWMS)
			return ds.editDatabaseLocation;

		return DbUri;
	}

	/**
	 *  Path plus Name of file containing encrypted username &amp; password 
	 */
	public String getDbAuthFile()
	{
		DecodesSettings ds = DecodesSettings.instance();
		if (DbAuthFile == null && ds.editDatabaseTypeCode == DecodesSettings.DB_CWMS)
			return ds.DbAuthFile;

		return DbAuthFile;
	}

	/** The Cwms Time Series default timezone */
	public String getTimeZone()
	{
		DecodesSettings ds = DecodesSettings.instance();
		if (ds.editDatabaseTypeCode == DecodesSettings.DB_CWMS)
			return ds.sqlTimeZone;
		return timeZone;
	}
	
	/** The Cwms Time Series descriptor version default value */
	public String getCwmsVersion()
	{
		return cwmsVersion;
	}
	
	/** The Cwms Time Series Office ID */
	public String getCwmsOfficeId()
	{
		return cwmsOfficeId;
	}
	
	/** Path plus name of file containing the Shef to Cwms codes mapping */
	public String getShefCwmsParamFile()
	{
		return shefCwmsParamFile;
	}
	
	public void initFromDecodesDb(CwmsSqlDatabaseIO cwmsDbIo)
	{
		DbUri = cwmsDbIo.getSqlDbLocation();
		DbAuthFile = DecodesSettings.instance().DbAuthFile;
		cwmsOfficeId = cwmsDbIo.getOfficeId();
		timeZone = "UTC";
	}
}
