package decodes.datasource;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;

import decodes.util.PropertySpec;

public class FtpDataSource 
	extends FileDataSource
{
	private String module = "FtpDataSource";
	private PropertySpec[] ftpDsPropSpecs =
	{
		new PropertySpec("host", PropertySpec.HOSTNAME,
			"FTP Data Source: Host name or IP Address of FTP Server"),
		new PropertySpec("port", PropertySpec.INT,
			"FTP Data Source: Listening port on FTP Server (default = 21)"),
		new PropertySpec("username", PropertySpec.STRING,
			"FTP Data Source: User name with which to connect to FTP server"),
		new PropertySpec("password", PropertySpec.STRING,
			"FTP Data Source: Password on the FTP server"),
		new PropertySpec("remoteDir", PropertySpec.STRING,
			"FTP Data Source: remote directory - blank means root"),
		new PropertySpec("localDir", PropertySpec.DIRECTORY,
			"FTP Data Source: optional local directory in which to store downloaded"
			+ " file. If not supplied, received file is processed by DECODES but not"
			+ " saved locally."),
		new PropertySpec("xferMode", 
			PropertySpec.JAVA_ENUM + "decodes.datasource.FtpMode",
			"FTP Data Source: FTP transfer mode"),
		new PropertySpec("deleteFromServer", PropertySpec.BOOLEAN,
			"FTP Data Source: (default=false) Set to true to delete file from server "
			+ "after retrieval. (May be disallowed on some servers.)"),
		new PropertySpec("ftpActiveMode", PropertySpec.BOOLEAN,
			"FTP Data Source: (default=false for passive mode) Set to true to " +
			"use FTP active mode.")
	};
	
	private String host = null;
	private int port = 21;
	private String username = "anon";
	private String password = null;
	private String remoteDir = "";
	// String filename = null; Use protected 'filename' from FileDataSource base class.
	private String localDir = null;
	private FtpMode ftpMode = FtpMode.Binary;
	private boolean deleteFromServer = false;
	private boolean ftpActiveMode = false;
	
	@Override
	public boolean setProperty(String name, String value)
	{
		if (name.equalsIgnoreCase("host"))
			host = value;
		else if (name.equalsIgnoreCase("port"))
		{
			try { port = Integer.parseInt(value); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Non-numeric port '" + value
					+ "' -- will use default of 21.");
			}
		}
		else if (name.equalsIgnoreCase("username"))
			username = value;
		else if (name.equalsIgnoreCase("password"))
			password = value;
		else if (name.equalsIgnoreCase("remoteDir"))
			remoteDir = value;
		else if (name.equalsIgnoreCase("localDir"))
			localDir = value;
		else if (name.equalsIgnoreCase("ftpMode"))
			ftpMode = value.length() > 0 && value.toLowerCase().charAt(0) == 'a'
				? FtpMode.ASCII : FtpMode.Binary;
		else if (name.equalsIgnoreCase("deleteFromServer"))
			deleteFromServer = TextUtil.str2boolean(value);
		else if (name.equalsIgnoreCase("ftpActiveMode"))
			ftpActiveMode = TextUtil.str2boolean(value);
		else // Let super class FileDataSource process it.
			return super.setProperty(name, value);
		return true;
	}		
	
	/**
	 * The open method here first does the entire FTP download and then
	 * calls the super class FileDataSource.open to open and process the
	 * local copy of the file.
	 */
	@Override
	public BufferedInputStream open()
		throws DataSourceException
	{
		// First make sure all the required properties are set.
		if (host == null || host.trim().length() == 0)
			throw new DataSourceException("Missing required 'host' property.");
		if (username == null || username.trim().length() == 0)
			throw new DataSourceException("Missing required 'username' property.");
		if (password == null || password.trim().length() == 0)
			throw new DataSourceException("Missing required 'password' property.");
		
		// Next download the file using FTP client.
		FTPClient ftpClient = new FTPClient();
		String remote = remoteDir;
		if (remote.length() > 0 && !remote.endsWith("/"))
			remote += "/";
		remote += filename;
		File localFile = null;
		try
		{
			Logger.instance().debug1(module + " Connecting to FTP Server " + host + ":" + port
				+ " with username=" + username + ", using "
				+ (ftpActiveMode ? "Active" : "Passive") + " mode.");
			ftpClient.connect(host, port);
			ftpClient.login(username, password);
			if (ftpActiveMode)
				ftpClient.enterLocalActiveMode();
			else
				ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(
				ftpMode == FtpMode.Binary ? FTP.BINARY_FILE_TYPE : FTP.ASCII_FILE_TYPE);
			
			String local = localDir;
			if (local == null || local.length() == 0)
				local = EnvExpander.expand("$DCSTOOL_USERDIR/tmp");
			File localDirectory = new File(local);
			if (!localDirectory.isDirectory())
				localDirectory.mkdirs();
			localFile = new File(localDirectory, filename);
			
			BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(localFile));
			
			Logger.instance().debug1(module + " Downloading remote file '" + remote
				+ "' to '" + localFile.getPath() + "'");
			if (!ftpClient.retrieveFile(remote, bos))
				throw new DataSourceException("FTP download failed for "
					+ "host=" + host + ", user=" + username
					+ "remote=" + remote + ", local=" + localFile.getPath());
		}
		catch(SocketException ex)
		{
			throw new DataSourceException(
				"Connect failed to host '" + host + "' port=" + port + ": " + ex);
		}
		catch(FTPConnectionClosedException ex)
		{
			throw new DataSourceException(
				"Connection closed prematurely to host '" + host 
				+ "' port=" + port + ": " + ex);
			
		}
		catch (IOException ex)
		{
			throw new DataSourceException(
				"IOException in FTP transfer from host '" + host 
				+ "' port=" + port + ", remote='" + remote + "': " + ex);
		}
		finally
		{
			if (deleteFromServer)
			{
				try
				{
					if (!ftpClient.deleteFile(remote))
						Logger.instance().warning(module + " cannot delete '"
							+ remote + "' on server.");
				}
				catch(Exception ex) { /* ignore exceptions on delete */ }
			}

	        try 
	        {
	            if (ftpClient.isConnected())
	            {
	                ftpClient.logout();
	                ftpClient.disconnect();
	            }
	        }
	        catch (IOException ex)
	        {
	            ex.printStackTrace();
	        }
		}

		
		// Finally, delegate to super class FileDataSource to process the downloaded
		// local copy of the file.
		super.filename = localFile.getPath();
		return super.open();
	}

	/**
	 * Base class returns an empty array for backward compatibility.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return PropertiesUtil.combineSpecs(super.getSupportedProps(), ftpDsPropSpecs);
	}

}
