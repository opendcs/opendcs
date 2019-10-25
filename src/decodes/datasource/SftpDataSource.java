package decodes.datasource;


import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.util.PropertySpec;

import com.jcraft.jsch.*;

public class SftpDataSource 
	extends DataSourceExec
{
	private String module = "SftpDataSource";
	private PropertySpec[] sftpDsPropSpecs =
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
		new PropertySpec("filenames", PropertySpec.STRING,
			"Required Space-separated list of files to download from server"),
		new PropertySpec("deleteFromServer", PropertySpec.BOOLEAN,
			"FTP Data Source: (default=false) Set to true to delete file from server "
			+ "after retrieval. (May be disallowed on some servers.)"),
		new PropertySpec("nameIsMediumId", PropertySpec.BOOLEAN,
			"Use with OneMessageFile=true if the downloaded filename is to be treated as a medium ID"
			+ " in order to link this data with a platform."),
		new PropertySpec("newerThan", PropertySpec.STRING, 
			"Either a Date/Time in the format [[[CC]YY] DDD] HH:MM[:SS], "
			+ "or a string of the form 'now - N incr',"
			+ " where N is an integer and incr is minutes, hours, or days.")
	};
	
	private String host = null;
	private int port = 22;
	private String username = "anon";
	private String password = null;
	private String remoteDir = "";
	private String filenames = "";
	private String localDir = null;
	private boolean deleteFromServer = false;
	private Properties allProps = null;
	private ArrayList<File> downloadedFiles = new ArrayList<File>();
	private FileDataSource currentFileDS = null;
	private int downloadedFileIndex = 0;
	private String mySince=null, myUntil=null;
	private Vector<NetworkList> myNetworkLists;
	private File currentFile = null;
//	private String newerThan = null;
	
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
					+ "' -- will use default of 22.");
				port = 22;
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
		else if (name.equalsIgnoreCase("deleteFromServer"))
			deleteFromServer = TextUtil.str2boolean(value);
		else if (name.equalsIgnoreCase("filenames"))
			filenames = value;
//		else if (name.equalsIgnoreCase("newerThan"))
//			newerThan = value.trim();
		return true;
	}
	
	class MyProgMon implements SftpProgressMonitor
	{
		long count = 0;

		@Override
		public boolean count(long count)
		{
			this.count = count;
			return true;
		}

		@Override
		public void end()
		{
		}

		@Override
		public void init(int op, String src, String dest, long max)
		{
			// TODO Auto-generated method stub
		}

		public long getCount()
		{
			return count;
		}
	};

	
	/**
	 * Base class returns an empty array for backward compatibility.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		// Remove 'filename' from file data source specs, but keep everything else.
		FileDataSource fds = new FileDataSource();
		PropertySpec[] x = fds.getSupportedProps();
		PropertySpec[] y = new PropertySpec[x.length-1];
		int xidx = 0, yidx = 0;
		for(; xidx < x.length; xidx++)
			if (!x[xidx].getName().equalsIgnoreCase("filename"))
				y[yidx++] = x[xidx];
		
		return PropertiesUtil.combineSpecs(y, sftpDsPropSpecs);
	}

	@Override
	public void processDataSource() throws InvalidDatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG3, 
			module + ".processDataSource '" + getName() 
			+ "', args='" +dbDataSource.getDataSourceArg()+"'");
	}

	@Override
	public void init(Properties routingSpecProps, String since, String until, Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		mySince = since;
		myUntil = until;
		myNetworkLists = networkLists;
		
		// Build a complete property set. Routing Spec props override DS props.
		allProps = new Properties(dbDataSource.arguments);
		for(Enumeration<?> it = routingSpecProps.propertyNames(); it.hasMoreElements();)
		{
			String name = (String)it.nextElement();
			String value = routingSpecProps.getProperty(name);
			allProps.setProperty(name, value);
		}
		for(Enumeration<?> it = allProps.propertyNames(); it.hasMoreElements();)
		{
			String name = (String)it.nextElement();
			String value = allProps.getProperty(name);
			name = name.trim().toLowerCase();
			setProperty(name, value);
		}
		
		downloadFiles();
		if (downloadedFiles.size() == 0)
			throw new DataSourceException(module + " Failed to download any files.");
		
		openNextFile();
	}


	private void downloadFiles()
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
		String action = " constructing JSch";
		JSch jsch = null;
		Session session = null;
		ChannelSftp chanSftp = null;
		try
		{
			Logger.instance().debug1(module + action);
			jsch = new JSch();
			
			action = " getting Session";
			Logger.instance().debug1(module + action);
			session = jsch.getSession(username, host, port);
			
			action = " setting Session Password";
			Logger.instance().debug1(module + action);
			session.setPassword(password);
			
			action = " connecting session";
			Logger.instance().debug1(module + action);
			session.connect();
			
			action = " getting SFTP channel";
			Logger.instance().debug1(module + action);
			chanSftp = (ChannelSftp)session.openChannel("sftp");
			
			action = " connecting SFTP channel";
			Logger.instance().debug1(module + action);
			chanSftp.connect();
		
			String remote = remoteDir;
			if (remote.length() > 0 && !remote.endsWith("/"))
				remote += "/";
	
			action = " constructing progress monitor";
			MyProgMon myProgMon = new MyProgMon();
	
	
			downloadedFiles.clear();
			downloadedFileIndex = 0;
			
			// split by whitespace* comma
			String fns[] = filenames.split(" ");
			Logger.instance().debug1(module + " there are " + fns.length + " filenames in the list:");
			for(String fn : fns) Logger.instance().debug1(module + "   '" + fn + "'");
			
			String local = localDir;
			if (local == null || local.length() == 0)
				local = "$DCSTOOL_USERDIR/tmp";
			local = EnvExpander.expand(local);
			File localDirectory = new File(local);
			if (!localDirectory.isDirectory())
				localDirectory.mkdirs();
		
			
			for(String filename : fns)
			{
				filename = filename.trim(); // remove any whitespace before or after the comma.
				String remoteName = remote + filename;
				File localFile = new File(localDirectory, filename);
			
				try
				{
					myProgMon.count(0L);
					action = " Downloading remote file '" + remoteName
							+ "' to '" + localFile.getPath() + "'";
					Logger.instance().debug1(module + action);
					chanSftp.get(remoteName, localFile.getPath(), myProgMon, ChannelSftp.OVERWRITE);
					Logger.instance().debug1(module + action + " SUCCESS, size=" + myProgMon.getCount());
					downloadedFiles.add(localFile);
					
					action = " Deleting '" + remoteName + "' from server";
					if (deleteFromServer)
						chanSftp.rm(remoteName);
				}
				catch(SftpException ex)
				{
					Logger.instance().warning(module + " Error while " + action + ": " + ex);
				}
			}
		}
		catch(JSchException ex)
		{
			String msg = module + " Error while" + action + ": " + ex;
			Logger.instance().warning(msg);
			throw new DataSourceException(msg);
		}
		finally
		{
			if (!chanSftp.isConnected())
				chanSftp.disconnect();
			if (session.isConnected())
				session.disconnect();
		}
		
        Logger.instance().info(module + " " + downloadedFiles.size() + " files downloaded.");
	}
	
	/**
	 * Opens the next file downloaded from FTP by constructing a FileDataSource delegate.
	 * @throws DataSourceEndException exception if there are no more files
	 * @throws DataSourceException exception if thrown in the init method of FileDataSource delegate.
	 */
	private void openNextFile()
		throws DataSourceException
	{
		currentFileDS = null;
		
		if (downloadedFileIndex >= downloadedFiles.size())
			throw new DataSourceEndException(module + " All " + downloadedFileIndex
				+ " files processed.");
		
		currentFile = downloadedFiles.get(downloadedFileIndex++);
		currentFileDS = new FileDataSource();
		allProps.setProperty("filename", currentFile.getPath());
		if (TextUtil.str2boolean(PropertiesUtil.getIgnoreCase(allProps, "NameIsMediumId")))
			allProps.setProperty("mediumid", currentFile.getName());
		
		currentFileDS.init(allProps, mySince, myUntil, myNetworkLists);
	}


	@Override
	public void close()
	{
		downloadedFiles.clear();
		if (currentFileDS != null)
			currentFileDS.close();
		currentFileDS = null;
	}

	@Override
	public RawMessage getRawMessage() 
		throws DataSourceException
	{
		if (currentFileDS == null)
			throw new DataSourceEndException(module + " file delegate aborted.");
		try
		{
			return currentFileDS.getRawMessage();
		}
		catch(DataSourceEndException ex)
		{
			Logger.instance().info(module + " End of file '" 
				+ currentFile.getPath() + "'");
			openNextFile();
			return getRawMessage(); // recursive call with newly opened file.
		}
		catch(DataSourceException ex)
		{
			Logger.instance().warning(module + " Error processing file '" 
				+ currentFile.getPath() + "': " + ex);
			openNextFile();
			return getRawMessage(); // recursive call with newly opened file.
		}
	}

}
