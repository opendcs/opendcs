/*
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

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import decodes.util.DecodesSettings;

public class CwmsDbConfig
{
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
	
	private String module = "CwmsDbConfig";
	
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
	public void loadFromProperties(String fileName)
		throws IOException
	{
		fileName = EnvExpander.expand(fileName);
		Logger.instance().info(module +
			" Loading config file '" + fileName + "'");
		try 
		{
			Properties rawProps = new Properties();
			FileInputStream fis = new FileInputStream(fileName);
			rawProps.load(fis);
			fis.close();
			PropertiesUtil.loadFromProps(this, rawProps);
		}
		catch(IOException ex)
		{
			String msg = module +  
				" Cannot open config file '" + fileName + "': " + ex;
			throw new IOException(msg);
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
