package decodes.datasource;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;

import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.util.PropertySpec;

public class FtpDataSource 
	extends DataSourceExec
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
		new PropertySpec("filenames", PropertySpec.STRING,
			"Space-separated list of files to download from server"),
		new PropertySpec("xferMode", 
			PropertySpec.JAVA_ENUM + "decodes.datasource.FtpMode",
			"FTP Data Source: FTP transfer mode"),
		new PropertySpec("deleteFromServer", PropertySpec.BOOLEAN,
			"FTP Data Source: (default=false) Set to true to delete file from server "
			+ "after retrieval. (May be disallowed on some servers.)"),
		new PropertySpec("ftpActiveMode", PropertySpec.BOOLEAN,
			"FTP Data Source: (default=false for passive mode) Set to true to " +
			"use FTP active mode."),
		new PropertySpec("nameIsMediumId", PropertySpec.BOOLEAN,
			"Use with OneMessageFile=true if the downloaded filename is to be treated as a medium ID"
			+ " in order to link this data with a platform."),
		new PropertySpec("ftps", PropertySpec.BOOLEAN, "(default=false) Use Secure FTP."),
		new PropertySpec("newerThan", PropertySpec.STRING, 
			"Either a Date/Time in the format [[[CC]YY] DDD] HH:MM[:SS], "
			+ "or a string of the form 'now - N incr',"
			+ " where N is an integer and incr is minutes, hours, or days."),

	};
	
	private String host = null;
	private int port = -1;
	private String username = "anon";
	private String password = null;
	private String remoteDir = "";
	private String filenames = "";
	// String filename = null; Use protected 'filename' from FileDataSource base class.
	private String localDir = null;
	private FtpMode ftpMode = FtpMode.Binary;
	private boolean deleteFromServer = false;
	private boolean ftpActiveMode = false;
	private Properties allProps = null;
	private ArrayList<File> downloadedFiles = new ArrayList<File>();
	private FileDataSource currentFileDS = null;
	private int downloadedFileIndex = 0;
	private String mySince=null, myUntil=null;
	private Vector<NetworkList> myNetworkLists;
	private File currentFile = null;
	private boolean ftps = false;
	private String newerThan = null;

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param dataSource
	 * @param decodesDatabase
	 */
	public FtpDataSource(DataSource source, Database db) {
		super(source, db);
	}
	
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
		else if (name.equalsIgnoreCase("filenames"))
			filenames = value;
		else if (name.equalsIgnoreCase("ftps"))
			ftps = TextUtil.str2boolean(value);
		else if (name.equalsIgnoreCase("newerThan"))
			newerThan = value.trim();
		return true;
	}		
	
	/**
	 * Base class returns an empty array for backward compatibility.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		// Remove 'filename' from file data source specs, but keep everything else.
		FileDataSource fds = new FileDataSource(null,null);
		PropertySpec[] x = fds.getSupportedProps();
		PropertySpec[] y = new PropertySpec[x.length-1];
		int xidx = 0, yidx = 0;
		for(; xidx < x.length; xidx++)
			if (!x[xidx].getName().equalsIgnoreCase("filename"))
				y[yidx++] = x[xidx];
		
		return PropertiesUtil.combineSpecs(y, ftpDsPropSpecs);
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
		FTPClient ftpClient = ftps ? new FTPSClient() : new FTPClient();
		Logger.instance().debug1("FTP client class is '" + ftpClient.getClass().getName() + "'");
		String remote = remoteDir;
		if (remote.length() > 0 && !remote.endsWith("/"))
			remote += "/";

		downloadedFiles.clear();
		downloadedFileIndex = 0;
		// split by whitespace* comma
		String fns[] = filenames.split(" ");
		Logger.instance().debug3(module + " there are " + fns.length + " filenames in the list:");
		for(String fn : fns) Logger.instance().debug3(module + "   '" + fn + "'");
		
		Logger.instance().debug1(module + " Connecting to FTP Server " + host + ":" + port
			+ " with username=" + username + ", using "
			+ (ftpActiveMode ? "Active" : "Passive") + " mode.");
			
		try
		{
			if (port == -1) // no port specified, use default for either ftp or ftps
				ftpClient.connect(host);
			else // a custom port has been specified
				ftpClient.connect(host, port);
		
// BCH ftps Server returns a 'malformed' reply.
//			Logger.instance().debug3("Connected, checking reply code.");
//			// It is recommended to check the reply code.
//			int reply = ftpClient.getReplyCode();
//			if (!FTPReply.isPositiveCompletion(reply))
//				throw new IOException("Unsuccessful reply code from client: " + reply);
			
			Logger.instance().debug3("Logging in with username='" + username + "' and pw='" + password + "'");
			ftpClient.login(username, password);
			if (ftpActiveMode)
				ftpClient.enterLocalActiveMode();
			else
				ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(
				ftpMode == FtpMode.Binary ? FTP.BINARY_FILE_TYPE : FTP.ASCII_FILE_TYPE);
		}
		catch(Exception ex)
		{
			if (ftpClient.isConnected())
			{
				try { ftpClient.disconnect(); } catch(Exception x) {}
			}
			throw new DataSourceException(module + 
				" Error connecting to FTP host '" + host 
				+ "' port=" + port + ", remote='" + remote + "': " + ex);
		}
			
		String local = localDir;
		if (local == null || local.length() == 0)
			local = "$DCSTOOL_USERDIR/tmp";
		local = EnvExpander.expand(local);
		File localDirectory = new File(local);
		if (!localDirectory.isDirectory())
			localDirectory.mkdirs();
		
		if (fns == null || fns.length == 0 
		 || (fns.length == 1 && fns[0].trim().length() == 0)
		 || (fns.length == 1 && fns[0].trim().equals("*")))
		{
			// get a directory listing and download all files or apply "newerThan" filter.
			FTPFile[] ftpFiles;
			try
			{
				if (newerThan == null || newerThan.length() == 0)
					ftpFiles = ftpClient.mlistDir(remoteDir);
				else
				{
					final Date since = IDateFormat.parse(newerThan);
					ftpFiles = ftpClient.mlistDir(remoteDir,
						new FTPFileFilter()
						{
							@Override
							public boolean accept(FTPFile f)
							{
								if (!f.getTimestamp().getTime().before(since))
									return true;
								Logger.instance().debug3(module + " Skipping '" + f.getName() + "' with time="
									+ f.getTimestamp().getTime());
								return false;
							}
						});
				}
				ArrayList<String> fa = new ArrayList<String>();
				for(int idx = 0; idx < ftpFiles.length; idx++)
				{
					String n = ftpFiles[idx].getName();
					if (n == null || n.equals(".") || n.equals(".."))
						continue;
					fa.add(n);
					Logger.instance().debug3(module + " Will process file '" + n + "'");
				}
				fns = new String[fa.size()];
				fns = fa.toArray(fns);
			}
			catch(IllegalArgumentException ex)
			{
				String msg = module + " Cannot parse newerThan time '"
					+ newerThan + "': " + ex;
				Logger.instance().failure(msg);
				throw new DataSourceException(msg);
			}
			catch(IOException ex)
			{
				String msg = module + " Cannot list directory on server '"
					+ remoteDir + "': " + ex;
				Logger.instance().failure(msg);
				throw new DataSourceException(msg);
			}
		}
			
		for(String filename : fns)
		{
			filename = filename.trim(); // remove any whitespace before or after the comma.
			String remoteName = remote + filename;
			File localFile = new File(localDirectory, filename);
			BufferedOutputStream bos = null;
		
			Logger.instance().debug1(module + " Downloading remote file '" + remoteName
				+ "' to '" + localFile.getPath() + "'");
			try
			{
				bos = new BufferedOutputStream(
					new FileOutputStream(localFile));

				if (ftpClient.retrieveFile(remoteName, bos))
				{
					downloadedFiles.add(localFile);
					if (deleteFromServer)
					{
						try
						{
							if (!ftpClient.deleteFile(remoteName))
								Logger.instance().warning(module + " cannot delete '"
									+ remoteName + "' on server.");
						}
						catch(Exception ex) { /* ignore exceptions on delete */ }
					}
				}
				else
					Logger.instance().warning(module + " Download failed for "
					+ "host=" + host + ", user=" + username
					+ "remote=" + remoteName + ", local=" + localFile.getPath());
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
					+ "' port=" + port + ", remote='" + remoteName + "': " + ex);
			}
			finally
			{
				try { if (bos != null) bos.close(); } catch(Exception ex) {}
			}
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
		currentFileDS = new FileDataSource(this.dbDataSource,this.db);
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
